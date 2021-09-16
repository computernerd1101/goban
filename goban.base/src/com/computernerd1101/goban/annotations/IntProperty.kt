package com.computernerd1101.goban.annotations

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

        override fun parse(s: String): Int? = s.toIntOrNull()

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