package com.computernerd1101.goban.annotations

import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyAnnotation

interface PropertyManagerCompanion<in A: Annotation, T : Comparable<T>> {

    fun makePropertyManager(annotation: A): PropertyManager<T>

}

interface PropertyManager<T> where T : Comparable<T> {

    fun toString(value: T): String

    fun parse(s: String): T?

    fun increment(value: T): T?

    fun decrement(value: T): T?

}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyOrder(vararg val value: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@PropertyAnnotation
annotation class IntProperty(
    val name: String,
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE
) {

    companion object: PropertyManagerCompanion<IntProperty, Int> {

        override fun makePropertyManager(annotation: IntProperty) =
            Manager(annotation.min, annotation.max)

    }

    class Manager(val min: Int, val max: Int): PropertyManager<Int> {

        override fun toString(value: Int) = value.toString()

        override fun parse(s: String): Int? {
            return try {
                s.toInt()
            } catch(e: NumberFormatException) {
                null
            }
        }

        override fun increment(value: Int): Int? {
            return if (value >= max) null
            else value + 1
        }

        override fun decrement(value: Int): Int? {
            return if (value <= min) null
            else value - 1
        }

    }

}

@Suppress("FunctionName", "NOTHING_TO_INLINE")
inline fun <T: Any> PropertyFactory(type: KClass<out T>) = PropertyFactory.propertyFactory(type)

class PropertyFactory<T: Any> private constructor(
    @Suppress("CanBeParameter") val type: KClass<out T>,
    @Suppress("UNUSED_PARAMETER") unit: Unit
): Iterable<PropertyFactory.Entry<T>> {

    sealed class Entry<T: Any>(
        @get:JvmName("name") val name: String
    ) {

        abstract operator fun get(t: T): Any?

        abstract operator fun set(t: T, value: Any?)

        abstract fun getString(t: T): String

        abstract fun setString(t: T, string: String)

        abstract fun canIncrement(t: T): Boolean

        abstract fun increment(t: T)

        abstract fun canDecrement(t: T): Boolean

        abstract fun decrement(t: T)

        private class Generic<T, P>(
            name: String,
            private val manager: PropertyManager<P>,
            private val property: KMutableProperty1<T, P>
        ): Entry<T>(name) where T: Any, P: Comparable<P> {

            override fun get(t: T): Any? {
                return property.get(t)
            }

            @Suppress("UNCHECKED_CAST")
            override fun set(t: T, value: Any?) {
                property.set(t, value as P)
            }

            override fun getString(t: T): String {
                return manager.toString(property.get(t))
            }

            override fun setString(t: T, string: String) {
                manager.parse(string)?.let { property.set(t, it) }
            }

            override fun canIncrement(t: T): Boolean {
                return manager.increment(property.get(t)) != null
            }

            override fun increment(t: T) {
                manager.increment(property.get(t))?.let { property.set(t, it) }
            }

            override fun canDecrement(t: T): Boolean {
                return manager.decrement(property.get(t)) != null
            }

            override fun decrement(t: T) {
                manager.decrement(property.get(t))?.let { property.set(t, it) }
            }

        }

        companion object {
            init {
                newEntry = object: EntryConstructor {
                    override fun <T: Any, P: Comparable<P>> init(
                        name: String,
                        manager: PropertyManager<P>,
                        property: KMutableProperty1<T, P>
                    ): Entry<T> = Generic(name, manager, property)
                }
            }
        }

    }

    private interface EntryConstructor {

        fun <T: Any, P: Comparable<P>> init(
            name: String,
            manager: PropertyManager<P>,
            property: KMutableProperty1<T, P>
        ): Entry<T>

    }

    private val entryArray: Array<Entry<T>>

    init {
        val entries = mutableMapOf<String, EntryBuilder<T, *>>()
        for(property in type.memberProperties) {
            @Suppress("UNCHECKED_CAST")
            (property as? KMutableProperty1<T, Comparable<Any>>)?.let { prop ->
                if (prop.visibility == KVisibility.PUBLIC)
                    getEntryAnnotation(entries, prop)
            }
        }
        val list = mutableListOf<Entry<T>>()
        type.findAnnotation<PropertyOrder>()?.let { order ->
            for(name in order.value)
                entries.remove(name)?.build()?.let { list.add(it) }
        }
        for(entry in entries.values)
            entry.build()?.let { list.add(it) }
        entryArray = list.toTypedArray()
    }

    @Suppress("unused")
    val entries: Array<Entry<T>>
        @JvmName("entries")
        get() = entryArray.clone()

    val entryCount: Int
        @JvmName("entryCount")
        get() = entryArray.size

    @JvmName("getEntry")
    operator fun get(index: Int) = entryArray[index]

    override fun iterator() = entryArray.iterator()

    private class EntryBuilder<T, P>(
        val name: String,
        val pmm: PropertyManagerMaker<P>,
        val prop: KMutableProperty1<T, P>
    ) where T: Any, P: Comparable<P> {

        var propCount = 0

        fun build(): Entry<T>? = if (propCount == 1) {
            Entry
            newEntry.init(name, pmm.propertyManager.makePropertyManager(pmm.annotation), prop)
        }
        else null

    }

    companion object {

        private lateinit var newEntry: EntryConstructor

        private val FACTORY_MAP =
            WeakHashMap<KClass<*>, PropertyFactory<*>>()
        private val PROP_MAP =
            WeakHashMap<KClass<out Annotation>, PropertyManagerMaker<*>?>()

        @JvmStatic
        @Suppress("unused")
        fun <T: Any> propertyFactory(type: Class<out T>): PropertyFactory<T> {
            return propertyFactory(type.kotlin)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T: Any> propertyFactory(type: KClass<out T>): PropertyFactory<T> {
            var factory = FACTORY_MAP[type] as PropertyFactory<T>?
            if (factory == null) {
                factory = PropertyFactory(type, Unit)
                FACTORY_MAP[type] = factory
            }
            return factory
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T: Any, P: Comparable<P>> getEntryAnnotation(
            entries: MutableMap<String, EntryBuilder<T, *>>,
            prop: KMutableProperty1<T, P>
        ): EntryBuilder<T, P>? {
            var pmm: PropertyManagerMaker<P>? = null
            var found = false
            for(annotation in prop.annotations) {
                val manager = propertyManagerMaker<P>(annotation)
                if (manager != null) {
                    pmm = if (found) null
                    else {
                        found = true
                        manager
                    }
                }
            }
            if (pmm != null) {
                val name = pmm.name(pmm.annotation)
                var entry = entries[name] as EntryBuilder<T, P>?
                if (entry == null) {
                    entry = EntryBuilder(name, pmm, prop)
                    entries[name] = entry
                }
                entry.propCount++
                return entry
            }
            return null
        }

        @Suppress("UNCHECKED_CAST")
        private fun <P> propertyManagerMaker(annotation: Annotation): PropertyManagerMaker<P>?
                where P: Comparable<P> {
            val type = annotation.annotationClass
            val pmm: PropertyManagerMaker<P>?
            if (type in PROP_MAP) {
                pmm = PROP_MAP[type] as PropertyManagerMaker<P>?
            } else {
                pmm = (type.companionObjectInstance as?
                        PropertyManagerCompanion<Annotation, P>)?.let setPmm@{ companion ->
                    if (type.findAnnotation<PropertyAnnotation>() == null)
                        return@setPmm null
                    val pName: KProperty1<Annotation, String> =
                        type.memberProperties.firstOrNull {
                            it.name == "name"
                        }?.let {
                            val returnType = it.returnType
                            if (!returnType.isMarkedNullable &&
                                returnType.classifier == String::class)
                                it as KProperty1<Annotation, String>
                            else null
                        } ?: return@setPmm null
                    PropertyManagerMaker(annotation, pName.getter, companion)
                }
                PROP_MAP[type] = pmm
            }
            return pmm
        }

    }

    private class PropertyManagerMaker<P: Comparable<P>> (
        val annotation: Annotation,
        val name: (Annotation) -> String,
        val propertyManager: PropertyManagerCompanion<Annotation, P>
    )

}
