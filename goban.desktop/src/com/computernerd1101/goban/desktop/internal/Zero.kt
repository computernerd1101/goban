@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.computernerd1101.goban.desktop.internal

typealias BoxedDouble = java.lang.Double

object Zero {

    @JvmField val plus = 0.0 as BoxedDouble
    @JvmField val minus = -0.0 as BoxedDouble

}