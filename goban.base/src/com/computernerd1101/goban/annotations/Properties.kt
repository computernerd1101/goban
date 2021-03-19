package com.computernerd1101.goban.annotations

import com.computernerd1101.goban.internal.InternalMarker
import kotlin.jvm.internal.Reflection
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyAnnotation

interface PropertyManagerCompanion<in A: Annotation, T : Comparable<T>> {

    fun makePropertyManager(annotation: A): PropertyManager<T>

}

interface PropertyManager<T : Comparable<T>> {

    fun toString(value: T): String

    fun parse(s: String): T?

    fun increment(value: T): T?

    fun decrement(value: T): T?

}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyOrder(vararg val value: String)

interface PropertyTranslator {

    fun translateProperty(name: String, locale: Locale): String?

}

fun Any?.translateProperty(name: String, locale: Locale): String =
    (this as? PropertyTranslator)?.translateProperty(name, locale) ?: name

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

fun <T: Any> PropertyFactory(type: KClass<out T>) = PropertyFactory.propertyFactory(type)

@Suppress("unused")
inline fun <reified T: Any> PropertyFactory() = PropertyFactory(T::class)

class PropertyFactory<T: Any> private constructor(
    @Suppress("CanBeParameter") val type: KClass<out T>,
    cache: Cache
): Iterable<PropertyFactory.Entry<T>> {

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun <T: Any> propertyFactory(type: Class<out T>): PropertyFactory<T> {
            return propertyFactory(type.kotlin)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T: Any> propertyFactory(type: KClass<out T>): PropertyFactory<T> {
            var factory = Cache.FACTORY_MAP[type] as PropertyFactory<T>?
            if (factory == null) {
                factory = PropertyFactory(type, Cache)
                Cache.FACTORY_MAP[type] = factory
            }
            return factory
        }

    }

    sealed class Entry<T: Any>(
        @get:JvmName("name") val name: String
    ) {

        abstract operator fun get(t: T): Any?

        abstract operator fun set(t: T, value: Any)

        abstract fun getString(t: T): String

        abstract fun setString(t: T, string: String)

        abstract fun canIncrement(t: T): Boolean

        abstract fun increment(t: T)

        abstract fun canDecrement(t: T): Boolean

        abstract fun decrement(t: T)

        private class Generic<T, P>(
            name: String,
            private val manager: PropertyManager<P>,
            property: KMutableProperty1<T, P>
        ): Entry<T>(name) where T: Any, P: Comparable<P> {

            // If the property is annotated with @JvmField,
            // or was written in Java as a public field,
            // then the JVM has no way of guaranteeing
            // that its value is not null (unless it is primitive).
            private val getter: (T) -> P? = property.getter
            // The setter at least can count on the promise that
            // it won't receive null input from this object,
            // even if it's writing directly into a JVM field.
            private val setter: (T, P) -> Unit = property.setter

            override fun get(t: T): Any? {
                return getter(t)
            }

            @Suppress("UNCHECKED_CAST")
            override fun set(t: T, value: Any) {
                setter(t, value as P)
            }

            override fun getString(t: T): String {
                return getter(t)?.let { manager.toString(it) } ?: ""
            }

            override fun setString(t: T, string: String) {
                manager.parse(string)?.let { setter(t, it) }
            }

            override fun canIncrement(t: T): Boolean {
                return getter(t)?.let { manager.increment(it) } != null
            }

            override fun increment(t: T) {
                getter(t)?.let { manager.increment(it) }?.let { setter(t, it) }
            }

            override fun canDecrement(t: T): Boolean {
                return getter(t)?.let { manager.decrement(it) } != null
            }

            override fun decrement(t: T) {
                getter(t)?.let { manager.increment(it) }?.let { setter(t, it) }
            }

        }

        companion object {

            internal fun <T: Any, P: Comparable<P>> newEntry(
                name: String,
                manager: PropertyManager<P>,
                property: KMutableProperty1<T, P>,
                marker: InternalMarker
            ): Entry<T> {
                marker.ignore()
                return Generic(name, manager, property)
            }
        }

    }

    private val entryArray: Array<Entry<T>>

    init {
        val entries = mutableMapOf<String, EntryBuilder<T, *>>()
        for(property in type.memberProperties) {
            @Suppress("UNCHECKED_CAST")
            val prop = property as? KMutableProperty1<T, Comparable<Any>>
            if (prop?.visibility == KVisibility.PUBLIC &&
                prop.setter.visibility == KVisibility.PUBLIC)
                cache.getEntryAnnotation(entries, prop)
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

    private object Cache {

        @JvmField val FACTORY_MAP = WeakHashMap<KClass<*>, PropertyFactory<*>>()
        @JvmField val PROP_MAP = WeakHashMap<KClass<out Annotation>, PropertyManagerMaker<*>?>()

        @Suppress("UNCHECKED_CAST")
        fun <T: Any, P: Comparable<P>> getEntryAnnotation(
            entries: MutableMap<String, EntryBuilder<T, *>>,
            prop: KMutableProperty1<T, P>
        ): EntryBuilder<T, P>? {
            val type = prop.returnType
            if (!type.isSubtypeOf(Reflection.typeOf(Comparable::class.java, KTypeProjection.invariant(type))))
                return null
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
                                String::class == returnType.classifier)
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

    private class EntryBuilder<T, P>(
        val name: String,
        val pmm: PropertyManagerMaker<P>,
        val prop: KMutableProperty1<T, P>
    ) where T: Any, P: Comparable<P> {

        var propCount = 0

        fun build(): Entry<T>? = if (propCount == 1)
            Entry.newEntry(name, pmm.propertyManager.makePropertyManager(pmm.annotation), prop, InternalMarker)
        else null

    }

    private class PropertyManagerMaker<P: Comparable<P>> (
        val annotation: Annotation,
        val name: (Annotation) -> String,
        val propertyManager: PropertyManagerCompanion<Annotation, P>
    )

}
