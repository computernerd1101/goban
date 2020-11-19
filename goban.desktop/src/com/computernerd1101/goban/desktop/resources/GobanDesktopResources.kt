@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.desktop.resources

import java.util.*

fun gobanDesktopResources(locale: Locale): ResourceBundle {
    return ResourceBundle.getBundle(
        "com.computernerd1101.goban.desktop.resources.GobanDesktopResources",
        locale
    )
}

class GobanDesktopResources: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("NewGame", "New Game"),
            arrayOf("RulesPreset.JAPANESE", "Japanese Rules"),
            arrayOf("RulesPreset.AGA", "AGA Rules"),
            arrayOf("RulesPreset.NZ", "New Zealand Rules"),
            arrayOf("RulesPreset.GOE", "Ing Rules"),
            arrayOf("RulesPreset.CUSTOM", "Custom Rules..."),
            arrayOf("ScoreType.AREA", "Score by Area"),
            arrayOf("ScoreType.TERRITORY", "Score by Territory"),
            arrayOf("TimeLimit", "Time Limit: "),
            arrayOf("Overtime.Header", "Overtime..."),
            arrayOf("Overtime.Prefix", "Overtime: "),
            arrayOf("Overtime.Suffix", ""),
            arrayOf("PropertyTranslator.Prefix", ""),
            arrayOf("PropertyTranslator.Suffix", ": "),
            arrayOf("HandicapType.FIXED", "Fixed Handicap: "),
            arrayOf("HandicapType.FREE", "Free Handicap: "),
            arrayOf("SizeHeader.SIZE", "Size: "),
            arrayOf("SizeHeader.WIDTH", "Width: "),
            arrayOf("SizeHeader.HEIGHT", "Height: "),
            arrayOf("AllowSuicide", "Allow Suicide?")
        )
    }

}