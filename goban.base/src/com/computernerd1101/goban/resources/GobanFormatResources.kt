@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.resources

import java.util.*

class GobanFormatResources: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("GoPointFormatter", GoPointFormatter),
            arrayOf("GobanDimensionFormatter.X", GobanDimensionFormatter.X),
            arrayOf("GobanDimensionFormatter.Y", GobanDimensionFormatter.Y),
            arrayOf(
                "players.GoSGFResumeException.InaccurateHandicap.Format",
                GoSGFResumeExceptionFormat.InaccurateHandicap
            ),
            arrayOf(
                "players.GoSGFResumeException.OverrideStone.Format",
                GoSGFResumeExceptionFormat.OverrideStone
            ),
            arrayOf(
                "players.GoSGFResumeException.Superko.Format",
                GoSGFResumeExceptionFormat.Superko
            ),
        )
    }

}

class GobanFormatResources_ja: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("GoPointFormatter", GoPointFormatter_ja),
            arrayOf("GobanDimensionFormatter.X", GobanDimensionFormatter_ja.X),
            arrayOf("GobanDimensionFormatter.Y", GobanDimensionFormatter_ja.Y)
        )
    }

}
