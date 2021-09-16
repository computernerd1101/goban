package com.computernerd1101.goban.annotations

import java.util.*

interface PropertyTranslator {

    fun translateProperty(name: String, locale: Locale): String?

}

fun Any?.translateProperty(name: String, locale: Locale): String =
    (this as? PropertyTranslator)?.translateProperty(name, locale) ?: name