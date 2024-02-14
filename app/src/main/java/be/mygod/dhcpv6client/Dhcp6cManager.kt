package be.mygod.dhcpv6client

import android.os.FileObserver
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.room.Database
import be.mygod.dhcpv6client.room.InterfaceStatement
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object Dhcp6cManager {
    const val DHCP6C = "libdhcp6c.so"
    private const val DHCP6CTL = "libdhcp6ctl.so"
    private const val DHCP6C_PID = "dhcp6c.pid"

    private val lock = ReentrantLock()

    class NativeProcessError(message: String?) : IOException(message)

    val root: File = app.deviceStorage.noBackupFilesDir
    val pidFile = File(root, DHCP6C_PID)
    private val config = File(root, "dhcp6c.conf")
    private val duidFile = File(root, "dhcp6c-duid")

    var duid: ByteArray
        get() = DataInputStream(duidFile.inputStream()).use {
            // stupid DataInputStream uses big-endian
            ByteArray(it.readUnsignedByte() + 256 * it.readUnsignedByte()).apply { it.readFully(this) }
        }
        set(value) = DataOutputStream(duidFile.outputStream()).use {
            check(value.size <= 65535)
            it.writeByte(value.size)
            it.writeByte(value.size ushr 8)
            it.write(value)
        }
    var duidString: String
        get() = duid.joinToString(":") { "%02x".format(it) }
        set(value) { duid = value.split(':').map { Integer.parseInt(it, 16).toByte() }.toByteArray() }

    /**
     * Generates a type-1 DUID. See doc: https://tools.ietf.org/html/rfc3315#section-9
     */
    fun generateDuid() {
        duid = ByteBuffer.allocate(14).run {
            putShort(1)                                                     // type = 1
            putShort(1)                                                     // hardware type = ARPHRD_ETHER
            putInt((System.currentTimeMillis() / 1000 - 946684800).toInt()) // time is since 1/1/2000 UTC, modulo 2^32
            put(ByteArray(6).apply { Random().nextBytes(this) })            // link-layer address
            array()
        }
    }
    fun ensureDuid() {
        if (!duidFile.isFile) generateDuid()
        check(duidFile.isFile)
    }

    private var daemon: Dhcp6cDaemon? = null

    private fun ensureStatements(interfaces: Iterable<String>) = interfaces.map {
        // TODO: configurable default interface statement
        Database.interfaceStatementDao.createDefault(InterfaceStatement(it, """{
	send ia-na %num;
	request domain-name-servers;
	request domain-name;
};
id-assoc na %num { };""")) != -1L
    }.any { it }

    @Throws(IOException::class)
    private fun startDaemonLocked(interfaces: Iterable<String>) {
        check(daemon == null)
        ensureDuid()
        val newDaemon = Dhcp6cDaemon(interfaces.joinToString(" "))
        daemon = newDaemon
        newDaemon.startWatching {
            lock.withLock {
                if (daemon == this) daemon = null
            }
        }
    }

    @Throws(IOException::class)
    private fun sendControlCommand(vararg commands: String) {
        val process = ProcessBuilder(listOf(File(app.applicationInfo.nativeLibraryDir, DHCP6CTL).absolutePath) +
                commands)
                .directory(root)
                .redirectErrorStream(true)
                .start()
        val result = process.inputStream.bufferedReader().readText()
        process.waitFor()
        val eval = process.exitValue()
        if (eval != 0) throw NativeProcessError("$eval: $result")
    }

    /**
     * This command specifies the process to reload the configuration
     * file.  Existing bindings, if any, are intact.
     */
    private fun reloadConfigLocked() {
        config.writeText(Database.interfaceStatementDao.list().mapIndexed { i, statement ->
            statement.statements = statement.statements.replace("%num", i.toString())
            statement
        }.joinToString("\n"))
        if (daemon != null) sendControlCommand("reload")
    }

    /**
     * This command is only applicable to a client.  It tells the client
     * to release the current configuration information (if any) on the
     * interface ifname and restart the DHCPv6 configuration process on
     * the interface.
     */
    fun startInterface(iface: String) {
        val updated = ensureStatements(listOf(iface))
        lock.withLock {
            if (updated) reloadConfigLocked()
            if (daemon == null) {       // there is still an inevitable race condition here :|
                try {
                    stopDaemon()        // kill existing daemons if any
                } catch (e: NativeProcessError) {
                    e.printStackTrace()
                    Thread.sleep(1000)  // assume old process is dying or has died, either way wait a lil
                }
                startDaemonLocked(listOf(iface))
            } else {
                sendControlCommand("start", "interface", iface)
            }
        }
    }

    /**
     * This command is only applicable to a client.  It tells the client
     * to release the current configuration information (if any) on the
     * interface ifname.  Any timer running for the interface will be
     * stopped, and no more DHCPv6 messages will be sent on the
     * interface.  The configuration process can later be restarted by
     * the start command.
     */
    fun stopInterface(iface: String) = try {
        sendControlCommand("stop", "interface", iface)
    } catch (e: NativeProcessError) {
        e.printStackTrace()
    }

    /**
     * This command stops the specified process.  If the process is a
     * client, it will release all configuration information (if any)
     * and exits.
     *
     * This method also syncs but not as good as stopDaemonSync.
     */
    private fun stopDaemon() {
        if (!pidFile.isFile) return
        val barrier = ArrayBlockingQueue<Unit>(1)
        // we don't have read access to pid file so we have to observe its parent directory
        val observer = object : FileObserver(File(root.absolutePath), DELETE) {
            override fun onEvent(event: Int, path: String?) {
                if (event == DELETE && path == DHCP6C_PID) barrier.put(Unit)
                stopWatching()
            }
        }
        observer.startWatching()
        if (!pidFile.isFile) return
        sendControlCommand("stop")
        barrier.take()
    }

    fun stopDaemonSync() {
        daemon?.apply {
            stopDaemon()
            waitFor()
        }
        daemon = null
    }

    fun forceRestartDaemon(interfaces: Iterable<String>) = lock.withLock {
        daemon?.destroy()
        daemon = null
        startDaemonLocked(interfaces)
    }
}
