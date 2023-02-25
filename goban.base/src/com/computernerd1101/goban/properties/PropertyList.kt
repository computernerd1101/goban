package com.computernerd1101.goban.properties

import com.computernerd1101.goban.internal.InternalMarker
import java.util.WeakHashMap
import java.util.Arrays
import kotlin.collections.AbstractList
import kotlin.collections.RandomAccess
import kotlin.jvm.internal.Reflection
import kotlin.reflect.*
import kotlin.reflect.full.*

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyOrder(vararg val value: String)

fun <T: Any> PropertyList(type: KClass<out T>) = PropertyList.propertyList(type)

@Suppress("unused")
inline fun <reified T: Any> PropertyList() = PropertyList(T::class)

class PropertyList<T: Any> private constructor(
    @Suppress("CanBeParameter") val type: KClass<out T>,
    cache: Cache
): AbstractList<PropertyList.Entry<T>>(), RandomAccess {

    companion object {

        fun <T: Any> propertyList(type: KClass<out T>): PropertyList<T> {
            // KClass is an interface, so there's no guarantee that all instances
            // are provided by the kotlin.reflect module. Call me paranoid, but
            // the type keys are cached in a static map, so uniqueness is extra-important.
            return propertyList(type.java)
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T: Any> propertyList(type: Class<out T>): PropertyList<T> {
            var list = Cache.CACHE[type] as PropertyList<T>?
            if (list == null) {
                // This instance of the KClass interface is guaranteed to be unique.
                list = PropertyList(type.kotlin, Cache)
                Cache.CACHE[type] = list
            }
            return list
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

        override fun toString(): String = name

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

        internal companion object {

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

    private object Cache {

        @JvmField val CACHE = WeakHashMap<Class<*>, PropertyList<*>>()
        private val PROP_MAP = WeakHashMap<Class<out Annotation>, PropertyManagerMaker<*>?>()

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
                    if (found) return null
                    found = true
                    pmm = manager
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

        @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private fun <P> propertyManagerMaker(annotation: Annotation): PropertyManagerMaker<P>?
                where P: Comparable<P> {
            val javaType = (annotation as java.lang.annotation.Annotation).annotationType() as Class<Annotation>
            val pmm: PropertyManagerMaker<P>?
            if (javaType in PROP_MAP) {
                pmm = PROP_MAP[javaType] as PropertyManagerMaker<P>?
            } else {
                val type = javaType.kotlin
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
                PROP_MAP[javaType] = pmm
            }
            return pmm
        }

        @JvmStatic fun checkRange(fromIndex: Int, toIndex: Int, size: Int) {
            if (fromIndex < 0 || toIndex > size)
                throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
            if (fromIndex > toIndex)
                throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
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

    override val size: Int get() = entryArray.size

    override fun get(index: Int) = entryArray[index]

    public override fun toArray(): Array<Any?> {
        return Arrays.copyOf(entryArray, entryArray.size, Array<Any?>::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    public override fun <T> toArray(array: Array<T>): Array<T> {
        val size = this.size
        if (array.size < size) return Arrays.copyOf(entryArray, size, array.javaClass)
        (entryArray as Array<T>).copyInto(array)
        if (array.size > size) array[size] = null as T
        return array
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Entry<T>> {
        val size = this.size
        Cache.checkRange(fromIndex, toIndex, size)
        if (fromIndex == 0 && toIndex == size) return this
        if (fromIndex == toIndex) return emptyList()
        return SubList(entryArray, fromIndex, toIndex - fromIndex)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            if (other is PropertyList<*>)
                isEmpty() && other.isEmpty()
            else other !is SubList<*> && super.equals(other)

    private var hasHashCode: Boolean = false
    private var hashCode: Int = 0

    override fun hashCode(): Int {
        val hash: Int
        if (hasHashCode) hash = hashCode
        else {
            hash = super.hashCode()
            hashCode = hash
            hasHashCode = true
        }
        return hash
    }

    private var string: String? = null

    override fun toString(): String = string ?: super.toString().also { string = it }

    private class SubList<T: Any>(
        val entryArray: Array<Entry<T>>,
        val offset: Int,
        override val size: Int
    ): AbstractList<Entry<T>>(), RandomAccess {

        override fun get(index: Int): Entry<T> {
            if (index !in 0 until size)
                throw IndexOutOfBoundsException("index: $index, size: $size")
            return entryArray[index + offset]
        }

        @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
        override fun toArray(): Array<Any?> {
            val offset = this.offset
            return Arrays.copyOfRange(entryArray, offset, offset + size, Array<Any?>::class.java)
        }

        @Suppress("UNCHECKED_CAST", "ReplaceJavaStaticMethodWithKotlinAnalog")
        override fun <T> toArray(array: Array<T>): Array<T> {
            val size = this.size
            val from = offset
            val to = from + size
            if (array.size < size) return Arrays.copyOfRange(entryArray, from, to, array.javaClass)
            (entryArray as Array<T>).copyInto(array, startIndex = from, endIndex = to)
            if (array.size > size) array[size] = null as T
            return array
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<Entry<T>> {
            val offset = this.offset
            val size = this.size
            Cache.checkRange(fromIndex, toIndex, size)
            if (fromIndex == 0 && toIndex == size) return this
            if (fromIndex == toIndex) return emptyList()
            return SubList(entryArray, fromIndex + offset, toIndex - fromIndex)
        }

        override fun equals(other: Any?): Boolean = this === other ||
                if (other is PropertyList.SubList<*>)
                    entryArray === other.entryArray && offset == other.offset && size == other.size
                else other !is PropertyList<*> && super.equals(other)

        private var hasHashCode: Boolean = false
        private var hashCode: Int = 0

        override fun hashCode(): Int {
            val hash: Int
            if (hasHashCode) hash = hashCode
            else {
                hash = super.hashCode()
                hashCode = hash
                hasHashCode = true
            }
            return hash
        }

        private var string: String? = null

        override fun toString(): String = string ?: super.toString().also { string = it }

    }

}
