package be.mygod.dhcpv6client

import androidx.lifecycle.MutableLiveData
import be.mygod.dhcpv6client.App.Companion.app
import be.mygod.dhcpv6client.util.thread
import be.mygod.dhcpv6client.widget.SmartSnackbar
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

class Dhcp6cDaemon(interfaces: String) {
    companion object Success : IOException() {
        private val ifaddrconfParser = "ifaddrconf: (add|remove) an address ([^/]*)/(\\d+) on (.*)\$".toRegex()

        val addressLookup = HashMap<String, MutableSet<Pair<String, Int>>>()
        val addresses = MutableLiveData<Lazy<String>>()
        fun postAddressUpdate() = addresses.postValue(lazy {
            synchronized(addressLookup) {
                addressLookup.entries.flatMap { entry ->
                    entry.value.map { Triple(it.first, entry.key, it.second) }
                }.joinToString("\n") { "${it.first}%${it.second}/${it.third}" }
            }
        })
    }

    private val process = ProcessBuilder("su", "-c", "echo Success" +
            " && exec " + File(app.applicationInfo.nativeLibraryDir, Dhcp6cManager.DHCP6C).absolutePath +
            " -Df -p ${Dhcp6cManager.pidFile.absolutePath} $interfaces")    // TODO log level configurable?
            .directory(Dhcp6cManager.root)
            .redirectErrorStream(true)
            .start()!!
    private val excQueue = ArrayBlockingQueue<IOException>(1)   // ArrayBlockingQueue doesn't want null
    private lateinit var thread: Thread

    fun startWatching(onExit: Dhcp6cDaemon.() -> Unit) {
        thread = thread(Dhcp6cManager.DHCP6C) {
            var initializing = true
            fun pushException(ioException: IOException) {
                if (initializing) {
                    excQueue.put(ioException)
                    initializing = false
                } else {
                    ioException.printStackTrace()
                }
            }
            try {
                process.inputStream.bufferedReader().forEachLine {
                    if (it == "Success" && initializing) pushException(Success) else {
                        if (initializing) return@forEachLine
                        val match = ifaddrconfParser.find(it) ?: return@forEachLine
                        val address = Pair(match.groupValues[2], match.groupValues[3].toInt())
                        synchronized(addressLookup) {
                            if (addressLookup.getOrPut(match.groupValues[4]) { mutableSetOf() }.run {
                                        if (match.groupValues[1] == "add") add(address) else remove(address)
                                    }) postAddressUpdate()
                        }
                    }
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
            synchronized(addressLookup) {
                if (addressLookup.isEmpty()) return@synchronized
                addressLookup.clear()
                postAddressUpdate()
            }
            onExit(this)
        }
        val exc = excQueue.take()
        if (exc !== Success) throw exc
        Thread.sleep(100)   // HACK: wait for dhcp6c to spin up so that we can issue it commands
    }

    fun waitFor() = process.waitFor()
    fun destroy() {
        process.destroyForcibly()
        process.waitFor()
    }
}
