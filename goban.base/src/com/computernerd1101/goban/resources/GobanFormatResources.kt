@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.resources

import java.util.*

internal class GobanFormatResources: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("GoPointFormatter", GoPointFormatter),
            arrayOf("GobanDimensionFormatter.X", GobanDimensionFormatter.X),
            arrayOf("GobanDimensionFormatter.Y", GobanDimensionFormatter.Y),
            arrayOf("ByoYomiFormatter", ByoYomiFormatter),
            arrayOf("CanadianOvertimeFormatter", CanadianOvertimeFormatter),
            arrayOf(
                "players.GoSGFResumeException.PlayerColor.Format",
                GoSGFResumeExceptionFormat.PlayerColor
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

internal class GobanFormatResources_ja: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("GoPointFormatter", GoPointFormatter_ja),
            arrayOf("GobanDimensionFormatter.X", GobanDimensionFormatter_ja.X),
            arrayOf("GobanDimensionFormatter.Y", GobanDimensionFormatter_ja.Y)
        )
    }

}
