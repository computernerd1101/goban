@file:Suppress("unused", "RemoveExplicitTypeArguments", "ClassName")

package com.computernerd1101.goban.resources

import java.util.*

class GobanResources: ListResourceBundle() {

    override fun getContents(): Array<Array<Any>> {
        return arrayOf(
            arrayOf<Any>("overtime.ByoYomi", "Byo-Yomi"),
            arrayOf<Any>("overtime.Canadian", "Canadian")
        )
    }

}

class GobanResources_ja: ListResourceBundle() {

    override fun getContents(): Array<Array<Any>> {
        return arrayOf(
            arrayOf<Any>("overtime.ByoYomi", "\u79d2\u8aad\u307f"),
            arrayOf<Any>("overtime.Canadian", "\u30ab\u30ca\u30c0")

        )
    }

}