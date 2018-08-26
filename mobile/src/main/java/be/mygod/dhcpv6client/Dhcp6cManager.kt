package be.mygod.dhcpv6client

import android.os.FileObserver
import android.util.Log
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.room.Database
import be.mygod.dhcpv6client.room.InterfaceStatement
import be.mygod.dhcpv6client.util.Event1
import com.crashlytics.android.Crashlytics
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object Dhcp6cManager {
    const val DHCP6C = "libdhcp6c.so"
    private const val DHCP6CTL = "libdhcp6ctl.so"
    private const val DHCP6C_PID = "dhcp6c.pid"

    private val lock = ReentrantLock()
    val dhcpv6Configured = Event1<String>()

    class NativeProcessError(message: String?) : IOException(message)

    val root = app.deviceStorage.noBackupFilesDir
    val pidFile = File(root, DHCP6C_PID)
    private val config = File(root, "dhcp6c.conf")

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
        Crashlytics.log(Log.DEBUG, DHCP6C, "Starting ${interfaces.joinToString()}...")
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
        if (result.isNotBlank()) Crashlytics.log(Log.WARN, DHCP6CTL, result)
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
            if (daemon == null) {   // there is still an inevitable race condition here :|
                stopDaemon()        // kill existing daemons if any
                startDaemonLocked(listOf(iface))
            } else {
                Crashlytics.log(Log.DEBUG, DHCP6CTL, "Requesting $iface...")
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
    fun stopInterface(iface: String) = sendControlCommand("stop", "interface", iface)

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
        val observer = object : FileObserver(root.absolutePath, FileObserver.DELETE) {
            override fun onEvent(event: Int, path: String?) {
                if (event == FileObserver.DELETE && path == DHCP6C_PID) barrier.put(Unit)
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
