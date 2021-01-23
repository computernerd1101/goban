@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.desktop.resources

import java.util.*

class GobanDesktopFormatResources: ListResourceBundle() {

    override fun getContents(): Array<Array<Any>> {
        return arrayOf(
            arrayOf("GoPointFormatter", GoPointFormatter),
            arrayOf("SGFNodeFormatter", SGFNodeFormatter),
            arrayOf("GobanSizeFormatter.SHORT", GobanSizeFormatter.SHORT),
            arrayOf("GobanSizeFormatter.LONG", GobanSizeFormatter.LONG),
            arrayOf("GobanDimensionFormatter.X", GobanDimensionFormatter.X),
            arrayOf("GobanDimensionFormatter.Y", GobanDimensionFormatter.Y)
        )
    }

}

class GobanDesktopFormatResources_ja: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("GoPointFormatter", GoPointFormatter_ja),
            arrayOf("GobanDimensionFormatter.X", GobanDimensionFormatter_ja.X),
            arrayOf("GobanDimensionFormatter.Y", GobanDimensionFormatter_ja.Y)
        )
    }

}