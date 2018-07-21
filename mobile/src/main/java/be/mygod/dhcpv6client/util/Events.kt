package be.mygod.dhcpv6client.util

import java.util.concurrent.ConcurrentHashMap

/**
 * These class are based off https://github.com/1blustone/kotlin-events.
 */
open class Event1<T> : ConcurrentHashMap<Any, (T) -> Unit>() {
    operator fun invoke(arg: T) {
        for ((_, handler) in this) handler(arg)
    }
}

class StickyEvent1<T>(private val fire: () -> T) : Event1<T>() {
    override fun put(key: Any, value: (T) -> Unit): ((T) -> Unit)? {
        val result = super.put(key, value)
        if (result == null) value(fire())
        return result
    }
}
