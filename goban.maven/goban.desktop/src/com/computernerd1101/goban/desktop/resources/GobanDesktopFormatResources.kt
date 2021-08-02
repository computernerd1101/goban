@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.desktop.resources

import java.util.*

class GobanDesktopFormatResources: ListResourceBundle() {

    override fun getContents(): Array<Array<Any>> {
        return arrayOf(
            arrayOf("SGFNodeFormatter", SGFNodeFormatter),
            arrayOf("GobanSizeFormatter.SHORT", GobanSizeFormatter.SHORT),
            arrayOf("GobanSizeFormatter.LONG", GobanSizeFormatter.LONG),
            arrayOf("TimeLimitFormatter", TimeLimitFormatter)
        )
    }

}
