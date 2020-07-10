@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.resources

import java.util.*

class GobanResources: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("overtime.ByoYomi", "Byo-Yomi"),
            arrayOf("overtime.Canadian", "Canadian")
        )
    }

}

class GobanResources_ja: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("overtime.ByoYomi", "\u79d2\u8aad\u307f"),
            arrayOf("overtime.Canadian", "\u30ab\u30ca\u30c0")
        )
    }

}