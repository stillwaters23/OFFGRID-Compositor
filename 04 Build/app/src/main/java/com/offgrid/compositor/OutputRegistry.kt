package com.offgrid.compositor

internal class OutputRegistry<T> {
    private val entries = LinkedHashMap<String, T>()

    fun put(id: String, output: T): T? = entries.put(id, output)

    fun remove(id: String): T? = entries.remove(id)

    fun values(): List<T> = entries.values.toList()

    fun clear(): List<T> = entries.values.toList().also { entries.clear() }
}
