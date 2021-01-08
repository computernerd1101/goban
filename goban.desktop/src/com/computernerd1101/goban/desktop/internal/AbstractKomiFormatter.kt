package com.computernerd1101.goban.desktop.internal

import com.computernerd1101.goban.desktop.CN13Spinner
import com.computernerd1101.goban.sgf.GameInfo
import java.text.*
import javax.swing.*
import javax.swing.event.ListDataListener

abstract class AbstractKomiFormatter:
    CN13Spinner.Formatter(NumberFormat.getInstance()),
    ComboBoxModel<Any> {

    private var value: BoxedDouble? = Zero.plus

    init {
        commitsOnValidEdit = true
        allowsInvalid = false
    }

    abstract val gameInfo: GameInfo?
    abstract val spinKomi: CN13Spinner

    override fun stringToValue(text: String?): Any? {
        if (text == null || text.isEmpty()) {
            if (gameInfo?.komi == 0.0) throw ParseException("", 0)
            return null
        }
        val isNegative: Boolean
        var s: String = text
        if (s[0] == '-') {
            isNegative = true
            if (s.length == 1) return Zero.minus
            s = s.substring(1)
        } else isNegative = false
        val hasFraction: Boolean
        val i = s.indexOf('.')
        if (i >= 0) {
            hasFraction = true
            s = s.substring(0, i)
        } else hasFraction = false
        var d = try {
            s.toInt().toDouble()
        } catch(e: NumberFormatException) {
            throw ParseException(text, 0)
        }
        if (hasFraction) d += 0.5
        if (isNegative) d = -d
        return when {
            d != 0.0 -> d
            isNegative -> Zero.minus
            else -> Zero.plus
        }
    }

    override fun valueToString(value: Any?): String {
        return value?.toString()?.removeSuffix(".0") ?: ""
    }

    override fun getValue(): Double? {
        val info = gameInfo ?: return null
        var value = this.value
        val komi = info.komi
        if (value as Double? != komi) {
            value = komi as BoxedDouble
            this.value = value
        }
        return value as Double?
    }

    override fun setValue(value: Any?) {
        val info = gameInfo ?: return
        var boxed: BoxedDouble? = null
        var unboxed = 0.0
        if (value is BoxedDouble) {
            boxed = value
            unboxed = boxed as Double
        } else if (value is Number) {
            unboxed = value.toDouble()
        }
        if (unboxed != info.komi) {
            info.komi = unboxed
            val komi = info.komi
            if (komi != unboxed)
                boxed = if (komi == 0.0) Zero.plus else null
            if (boxed == null) boxed = komi as BoxedDouble
            this.value = boxed
            fireChangeEvent()
            if (komi == 0.0) SwingUtilities.invokeLater {
                spinKomi.editor.textField.caretPosition = 1
            }
        }
    }

    override fun getPreviousValue(): Any? {
        val info = gameInfo ?: return null
        val komi = info.komi - 0.5
        return if (komi == 0.0) Zero.plus else komi
    }

    override fun getNextValue(): Any? {
        val info = gameInfo ?: return null
        val komi = info.komi + 0.5
        return if (komi == 0.0) Zero.plus else komi
    }

    override fun getSelectedItem(): Any = "Komi: "

    override fun setSelectedItem(anItem: Any?) {
        if (anItem is Number) {
            val info = gameInfo
            if (info != null) {
                info.komi = anItem.toDouble()
                spinKomi.updateUI()
            }
        }
    }

    override fun getSize() = PRESETS.size

    override fun getElementAt(index: Int) = PRESETS[index]

    override fun addListDataListener(l: ListDataListener?) = Unit

    override fun removeListDataListener(l: ListDataListener?) = Unit

    companion object {

        @Suppress("RemoveExplicitTypeArguments")
        private val PRESETS = arrayOf<Any>(
            0, 0.5, 5.5, 6.5, 7.5
        )

    }


}