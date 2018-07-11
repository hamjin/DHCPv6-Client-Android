package be.mygod.dhcpv6client.util

import android.widget.Toast
import be.mygod.dhcpv6client.App.Companion.app
import com.crashlytics.android.Crashlytics

/**
 * Wrapper for kotlin.concurrent.thread that silences uncaught exceptions.
 */
fun thread(name: String? = null, start: Boolean = true, isDaemon: Boolean = false,
           contextClassLoader: ClassLoader? = null, priority: Int = -1, block: () -> Unit): Thread {
    val thread = kotlin.concurrent.thread(false, isDaemon, contextClassLoader, name, priority, block)
    thread.setUncaughtExceptionHandler { _, e ->
        Toast.makeText(app, e.message, Toast.LENGTH_LONG).show()
        Crashlytics.logException(e)
    }
    if (start) thread.start()
    return thread
}
