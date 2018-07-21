package be.mygod.dhcpv6client

import android.util.Log
import android.widget.Toast
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.room.Database
import be.mygod.dhcpv6client.room.InterfaceStatement
import be.mygod.dhcpv6client.util.Event1
import be.mygod.dhcpv6client.util.thread
import com.crashlytics.android.Crashlytics
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

object Dhcp6cManager {
    private const val DHCP6C = "dhcp6c.so"
    private const val DHCP6CTL = "dhcp6ctl.so"

    private val addAddressParser = "ifaddrconf: add an address .+ on (.+)\$".toRegex()
    val dhcpv6Configured = Event1<String>()

    class NativeProcessError(message: String?) : IOException(message)

    private val root = app.deviceContext.noBackupFilesDir
    private val pidFile = File(root, "dhcp6c.pid")
    private val config = File(root, "dhcp6c.conf")

    private var daemon: Process? = null
    private var daemonDaemon: Thread? = null

    private fun updateConfig() = synchronized(this) {
        config.writeText(Database.interfaceStatementDao.list().mapIndexed { i, statement ->
            statement.statements = statement.statements.replace("%num", i.toString())
            statement
        }.joinToString("\n"))
    }

    private fun ensureStatements(interfaces: Iterable<String>) = interfaces.map {
        // TODO: configurable default interface statement
        Database.interfaceStatementDao.createDefault(InterfaceStatement(it, """{
	send ia-na %num;
	request domain-name-servers;
	request domain-name;
};
id-assoc na %num { };""")) != -1L
    }.any { it }

    private object Success : IOException()
    @Throws(IOException::class)
    private fun startDaemonLocked(interfaces: Iterable<String>) {
        val allInterfaces = Database.interfaceStatementDao.list().map { it.iface }.toSet() + interfaces
        if (ensureStatements(interfaces)) updateConfig()
        check(daemon == null)
        Crashlytics.log(Log.DEBUG, DHCP6C, "Starting ${allInterfaces.joinToString()}...")
        val process = ProcessBuilder("su", "-c", "echo Success && " +
                File(app.applicationInfo.nativeLibraryDir, DHCP6C).absolutePath +
                " -Df -p ${pidFile.absolutePath} " + allInterfaces.joinToString(" "))  // TODO log level configurable?
                .directory(root)
                .redirectErrorStream(true)
                .start()!!
        daemon = process
        val excQueue = ArrayBlockingQueue<IOException>(1)   // ArrayBlockingQueue doesn't want null
        daemonDaemon = thread(DHCP6C) {
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
                app.handler.post { Toast.makeText(app, msg, Toast.LENGTH_LONG).show() }
                Crashlytics.log(Log.ERROR, DHCP6C, msg)
                Crashlytics.logException(NativeProcessError(msg))
            }
            synchronized(this) {
                check(daemon == process)
                daemon = null
                daemonDaemon = null
            }
        }
        val exc = excQueue.take()
        if (exc !== Success) throw exc
        Thread.sleep(100)   // HACK: wait for dhcp6c to spin up so that we can issue it commands
    }
    fun startDaemon(interfaces: Iterable<String>) = synchronized(this) { startDaemonLocked(interfaces) }

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
        val updated = ensureStatements(listOf(iface))
        synchronized(this) {
            when {
                updated -> {
                    stopDaemonSync()    // there's no way to load new interfaces afaic
                    updateConfig()
                    startDaemonLocked(listOf(iface))
                }
                daemon == null -> {     // there is still an inevitable race condition here :|
                    stopDaemon()        // kill existing daemons if any
                    updateConfig()
                    startDaemonLocked(listOf(iface))
                }
                else -> {
                    Crashlytics.log(Log.DEBUG, DHCP6CTL, "Requesting $iface...")
                    sendControlCommand("start", "interface", iface)
                }
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
    private fun stopDaemon() = try {
        sendControlCommand("stop")
        Thread.sleep(1000)  // TODO better ways to ensure dhcp6c is shut down
    } catch (e: IOException) {
        Crashlytics.log(Log.INFO, DHCP6C, e.message)
    }

    fun stopDaemonSync() {
        if (daemon == null) return
        stopDaemon()
        daemonDaemon?.join()
    }
}
