package com.computernerd1101.goban.time

import com.computernerd1101.goban.annotations.*

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@PropertyAnnotation
annotation class TimeProperty(
    val name: String,
    val min: Long = 0L,
    val max: Long = Long.MAX_VALUE
) {

    companion object : PropertyManagerCompanion<TimeProperty, Long> {

        override fun makePropertyManager(annotation: TimeProperty): PropertyManager<Long> {
            return Manager(annotation.min, annotation.max)
        }

    }

    class Manager(val min: Long, val max: Long): PropertyManager<Long> {

        override fun toString(value: Long) = value.millisToStringSeconds()

        override fun parse(s: String): Long? {
            return try {
                s.secondsToMillis()
            } catch(e: RuntimeException) {
                null
            }
        }

        override fun increment(value: Long): Long? {
            var time = value
            if (time >= max) return null
            if (time == min) {
                var rem = time % 1000L
                if (rem < 0L) rem += 1000L
                if (rem != 0L) time -= rem
            }
            return if (max - time < 1000L) max else time + 1000L
        }

        override fun decrement(value: Long): Long? {
            if (value <= min) return null
            if (value - min < 1000L) return min
            return value - 1000L
        }

    }

}