package com.offgrid.compositor

/** Keyed extra encoder outputs. A null value removes that id. The compositor draws into each one. */
internal class ExtraOutputRegistry<T> {
    private val targets = LinkedHashMap<String, T>()

    /** Stores [target], or removes [id] when [target] is null. Returns the previous value. */
    fun set(id: String, target: T?): T? {
        return if (target == null) targets.remove(id) else targets.put(id, target)
    }

    fun values(): List<T> = targets.values.toList()

    fun clear(): List<T> = values().also { targets.clear() }
}
