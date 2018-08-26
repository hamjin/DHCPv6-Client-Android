package be.mygod.dhcpv6client

import android.os.Build
import android.util.Log
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.util.thread
import be.mygod.dhcpv6client.widget.SmartSnackbar
import com.crashlytics.android.Crashlytics
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

class Dhcp6cDaemon(interfaces: String) {
    companion object {
        private val addAddressParser = "ifaddrconf: add an address .+ on (.+)\$".toRegex()
    }
    private object Success : IOException()

    private val process = ProcessBuilder("su", "-c", "echo Success && " +
            File(app.applicationInfo.nativeLibraryDir, Dhcp6cManager.DHCP6C).absolutePath +
            " -Df -p ${Dhcp6cManager.pidFile.absolutePath} $interfaces")    // TODO log level configurable?
            .directory(Dhcp6cManager.root)
            .redirectErrorStream(true)
            .start()!!
    private val excQueue = ArrayBlockingQueue<IOException>(1)   // ArrayBlockingQueue doesn't want null
    private lateinit var thread: Thread

    fun startWatching(onExit: Dhcp6cDaemon.() -> Unit) {
        thread = thread(Dhcp6cManager.DHCP6C) {
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
                if (first != "Success") throw IOException("$first\n${reader.use { it.readText() }}")
                pushException(Success)
                reader.forEachLine {
                    Crashlytics.log(Log.INFO, Dhcp6cManager.DHCP6C, it)
                    val match = addAddressParser.find(it) ?: return@forEachLine
                    Dhcp6cManager.dhcpv6Configured(match.groupValues[1])
                }
            } catch (e: IOException) {
                pushException(e)
            }
            process.waitFor()
            val eval = process.exitValue()
            if (eval != 0 && eval != 143) {
                val msg = "${Dhcp6cManager.DHCP6C} exited with $eval"
                SmartSnackbar.make(msg).show()
                Crashlytics.log(Log.ERROR, Dhcp6cManager.DHCP6C, msg)
                Crashlytics.logException(Dhcp6cManager.NativeProcessError(msg))
            }
            onExit(this)
        }
        val exc = excQueue.take()
        if (exc !== Success) throw exc
        Thread.sleep(100)   // HACK: wait for dhcp6c to spin up so that we can issue it commands
    }

    fun waitFor() = process.waitFor()
    fun destroy() {
        if (Build.VERSION.SDK_INT >= 26) process.destroyForcibly() else process.destroy()
        process.waitFor()
    }
}
