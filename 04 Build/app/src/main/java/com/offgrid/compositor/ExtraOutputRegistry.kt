package com.offgrid.compositor

internal class ExtraOutputRegistry<T> {
    private val targets = LinkedHashMap<String, T>()

    fun set(id: String, target: T?) {
        if (target == null) targets.remove(id) else targets[id] = target
    }

    fun remove(id: String): T? = targets.remove(id)

    fun values(): List<T> = targets.values.toList()

    fun clear(): List<T> = values().also { targets.clear() }
}
