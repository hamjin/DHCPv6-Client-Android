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
    private object Success : IOException()

    private val process = ProcessBuilder("su", "-c", "echo Success && exec " +
            File(app.applicationInfo.nativeLibraryDir, Dhcp6cManager.DHCP6C).absolutePath +
            " -Df -p ${Dhcp6cManager.pidFile.absolutePath} $interfaces")    // TODO log level configurable?
            .directory(Dhcp6cManager.root)
            .redirectErrorStream(true)
            .start()!!
    private val excQueue = ArrayBlockingQueue<IOException>(1)   // ArrayBlockingQueue doesn't want null
    private lateinit var thread: Thread

    fun startWatching(onExit: Dhcp6cDaemon.() -> Unit) {
        thread = thread(Dhcp6cManager.DHCP6C) {
            var initializing = true
            fun pushException(ioException: IOException) = if (initializing) {
                excQueue.put(ioException)
                initializing = false
            } else {
                ioException.printStackTrace()
                Crashlytics.logException(ioException)
            }
            try {
                process.inputStream.bufferedReader().forEachLine {
                    if (it == "Success" && initializing) pushException(Success)
                    else Crashlytics.log(if (initializing) Log.ERROR else Log.INFO, Dhcp6cManager.DHCP6C, it)
                }
                process.waitFor()
                val eval = process.exitValue()
                if (initializing || eval != 0 && eval != 143) {
                    val msg = "${Dhcp6cManager.DHCP6C} exited with $eval"
                    SmartSnackbar.make(msg).show()
                    throw Dhcp6cManager.NativeProcessError(msg)
                }
            } catch (e: IOException) {
                pushException(e)
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
