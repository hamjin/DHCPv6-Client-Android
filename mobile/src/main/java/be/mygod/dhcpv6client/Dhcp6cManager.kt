package be.mygod.dhcpv6client

import android.os.Build
import android.util.Base64
import android.util.Log
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.room.Database
import be.mygod.dhcpv6client.room.InterfaceStatement
import be.mygod.dhcpv6client.util.Event1
import be.mygod.dhcpv6client.util.thread
import com.crashlytics.android.Crashlytics
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue

object Dhcp6cManager {
    private const val DHCP6C = "dhcp6c.so"
    private const val DHCP6CTL = "dhcp6ctl.so"

    private val addAddressParser = "ifaddrconf: add an address .+ on (.+)\$".toRegex()
    val dhcpv6Configured = Event1<String>()

    class NativeProcessError(message: String?) : IOException(message)

    private val root = app.deviceContext.noBackupFilesDir
    private val localdb = File(root, "localdb")
    private val sysconf = File(root, "sysconf")
    private val pidFile = File(root, "dhcp6c.pid")
    private val config = File(sysconf, "dhcp6c.conf")
    private val controlKey = File(sysconf, "dhcp6cctlkey")

    private var daemon: Process? = null

    private fun setupEnvironment() = synchronized(this) {
        check(localdb.mkdirs() || localdb.isDirectory)
        check(sysconf.mkdirs() || sysconf.isDirectory)
        if (!controlKey.exists()) {
            val bytes = ByteArray(16)   // HMAC-MD5 uses 128 bits
            (if (Build.VERSION.SDK_INT >= 26) SecureRandom.getInstanceStrong() else SecureRandom()).nextBytes(bytes)
            controlKey.writeBytes(Base64.encode(bytes, Base64.NO_WRAP))
        }
    }

    private fun updateConfig() = synchronized(this) {
        config.writeText(Database.interfaceStatementDao.list().mapIndexed { i, statement ->
            statement.statements = statement.statements.replace("%num", i.toString())
            statement
        }.joinToString("\n"))
    }

    private object Success : IOException()
    @Throws(IOException::class)
    private fun startDaemon(interfaces: List<String>) {
        val process = synchronized(this) {
            if (daemon != null) return
            Crashlytics.log(Log.DEBUG, DHCP6C, "Starting $interfaces...")
            val process = ProcessBuilder("su", "-c", "echo Success && " +
                    File(app.applicationInfo.nativeLibraryDir, DHCP6C).absolutePath +
                    " -Df -p ${pidFile.absolutePath} " + interfaces.joinToString(" "))  // TODO log level configurable?
                    .directory(root)
                    .redirectErrorStream(true)
                    .start()!!
            daemon = process
            process
        }
        val excQueue = ArrayBlockingQueue<IOException>(1)   // ArrayBlockingQueue doesn't want null
        thread(DHCP6C) {
            var pushed = false
            fun pushException(ioException: IOException) = if (pushed) {
                ioException.printStackTrace()
                Crashlytics.logException(ioException)
            } else {
                excQueue.put(ioException)
                pushed = true
            }
            try {
                val reader = process.inputStream.bufferedReader()
                val first = reader.readLine()
                if (first != "Success") throw IOException(reader.readText())
                pushException(Success)
                reader.useLines {
                    it.forEach {
                        Crashlytics.log(Log.INFO, DHCP6C, it)
                        val match = addAddressParser.find(it) ?: return@forEach
                        dhcpv6Configured(match.groupValues[1])
                    }
                }
            } catch (e: IOException) {
                pushException(e)
            }
            process.waitFor()
            val eval = process.exitValue()
            if (eval != 0) {
                val msg = "$DHCP6C exited with $eval"
                Crashlytics.log(Log.ERROR, DHCP6C, msg)
                Crashlytics.logException(NativeProcessError(msg))
            }
            synchronized(this) {
                check(daemon == process)
                daemon = null
            }
        }
        val exc = excQueue.take()
        if (exc !is Success) throw exc
        Thread.sleep(100)   // HACK: wait for dhcp6c spin up so that we can issue it commands
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
        if (result.isNotBlank()) {
            Crashlytics.log(Log.WARN, DHCP6CTL, result)
            Crashlytics.logException(NativeProcessError("$eval: $result"))
        }
    }

    /**
     * This command specifies the process to reload the configuration
     * file.  Existing bindings, if any, are intact.
     */
    fun reloadConfig() {
        updateConfig()
        if (daemon != null) sendControlCommand("reload")
    }

    /**
     * This command is only applicable to a client.  It tells the client
     * to release the current configuration information (if any) on the
     * interface ifname and restart the DHCPv6 configuration process on
     * the interface.
     */
    fun startInterface(iface: String) {
        setupEnvironment()
        // TODO: configurable default interface statement
        val updated = Database.interfaceStatementDao.createDefault(InterfaceStatement(iface, """{
	send ia-na %num;
	request domain-name-servers;
	request domain-name;
};
id-assoc na %num { };""")) != -1L
        if (updated) reloadConfig()
        synchronized(this) {
            // there is still an inevitable race condition here :|
            if (daemon == null) {
                stopDaemon()    // kill existing daemons if any
                if (!updated) updateConfig()
                startDaemon(listOf(iface))
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
     */
    fun stopDaemon() = try {
        sendControlCommand("stop")
    } catch (e: IOException) {
        Crashlytics.log(Log.INFO, DHCP6C, e.message)
    }
}
