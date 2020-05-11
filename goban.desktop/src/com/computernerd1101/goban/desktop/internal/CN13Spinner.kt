package com.computernerd1101.goban.desktop.internal

import com.computernerd1101.goban.desktop.CN13Spinner

object InternalSpinner {

    interface FormatterSecrets {
        fun fireChangeEvent(formatter: CN13Spinner.Formatter)
    }
    lateinit var formatterSecrets: FormatterSecrets

}