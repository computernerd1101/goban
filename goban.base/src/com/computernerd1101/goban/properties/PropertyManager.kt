package com.computernerd1101.goban.properties

interface PropertyManager<T : Comparable<T>> {

    fun toString(value: T): String

    fun parse(s: String): T?

    fun increment(value: T): T?

    fun decrement(value: T): T?

}

interface PropertyManagerCompanion<in A: Annotation, T : Comparable<T>> {

    fun makePropertyManager(annotation: A): PropertyManager<T>

}