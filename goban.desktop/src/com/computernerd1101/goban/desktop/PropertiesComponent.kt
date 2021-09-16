package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.annotations.*
import com.computernerd1101.goban.desktop.resources.gobanDesktopResources
import java.awt.*
import java.text.ParseException
import java.util.*
import javax.swing.*

class PropertiesComponent<T: Any>(
    data: T? = null,
    minRows: Int = 0
): JComponent() {

    var data: T? = data
        set(data) {
            field = data
            val count = overtimeProperties?.size ?: 0
            if (data != null)
                setNonNullOvertime(data, count)
            else {
                if (count > 0)
                    hideExtraRows(0, count)
                overtimeProperties = null
            }
        }

    var minRows: Int = minRows.coerceAtLeast(0)
        set(value) {
            val min = value.coerceAtLeast(0)
            field = min
            val count = overtimeProperties?.size ?: 0
            var rows = this.rows
            if (min > rows.size) {
                rows = rows.copyOf(min)
                this.rows = rows
            }
            for(y in count until min) {
                var row = rows[y]
                if (row == null) {
                    row = Row()
                    rows[y] = row
                    addRow(y, row)
                    row.spin.isVisible = false
                }
                row.label.text = " "
                row.label.isVisible = true
            }
        }

    private var overtimeProperties: PropertyList<T>? = null

    private val constraints = GridBagConstraints()

    init {
        layout = GridBagLayout()
    }

    @Suppress("UNCHECKED_CAST")
    private var rows: Array<Row?> =
        if (minRows <= 0) Rows.EMPTY as Array<Row?>
        else Array(minRows) { y ->
            val row = Row()
            addRow(y, row)
            row.spin.isVisible = false
            row
        }

    init {
        if (data != null)
            setNonNullOvertime(data, 0)
    }

    private fun setNonNullOvertime(overtime: T, oldCount: Int) {
        val properties = PropertyList(overtime::class)
        val count = properties.size
        overtimeProperties = properties
        var rows = this.rows
        if (count > rows.size) {
            rows = rows.copyOf(count)
            this.rows = rows
        }
        val locale = Locale.getDefault()
        val resources = gobanDesktopResources(locale)
        for(y in 0 until count) {
            var row = rows[y]
            if (row != null) {
                row.label.isVisible = true
                row.spin.isVisible = true
            } else {
                row = Row()
                rows[y] = row
                addRow(y, row)
            }
            val entry = properties[y]
            row.label.text = resources.getString("PropertyTranslator.Prefix") +
                    overtime.translateProperty(entry.name, locale) +
                    resources.getString("PropertyTranslator.Suffix")
            row.entry = entry
            row.spin.updateUI()
        }
        if (count < oldCount)
            hideExtraRows(count, oldCount)
    }

    private fun addRow(y: Int, row: Row) {
        constraints.anchor = GridBagConstraints.EAST
        constraints.fill = GridBagConstraints.NONE
        constraints.weightx = 0.0
        constraints.gridx = 0
        constraints.gridy = y
        constraints.gridwidth = 1
        constraints.gridheight = 1
        add(row.label, constraints)
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 1
        add(row.spin, constraints)
    }

    private fun hideExtraRows(start: Int, end: Int) {
        for(i in start until end)
            rows[i]?.apply {
                entry = null
                label.text = " "
                label.isVisible = i <= minRows
                spin.isVisible = false
            }
    }

    private object Rows {

        @JvmField val EMPTY = emptyArray<PropertiesComponent<*>.Row?>()

    }

    private inner class Row: CN13Spinner.Formatter() {

        var entry: PropertyList.Entry<T>? = null

        val label = JLabel(" ")
        val spin = CN13Spinner()

        init {
            spin.model = this
            allowsInvalid = true
            commitsOnValidEdit = false
        }

        override fun valueToString(value: Any?): String {
            val d = data
            val e = entry
            if (d == null || e == null)
                return super.valueToString(value)
            return e.getString(d)
        }

        override fun stringToValue(text: String): Any? {
            val d = data
            val e = entry
            if (d == null || e == null)
                return super.stringToValue(text)
            try {
                e.setString(d, text)
            } catch(ex: Exception) {
                throw ParseException(text, 0)
            }
            return e[d]
        }

        override fun getValue(): Any? {
            val d = data ?: return null
            val e = entry ?: return null
            return e[d]
        }

        override fun setValue(value: Any?) {
            if (value == null) return
            val d = data ?: return
            val e = entry ?: return
            when(value) {
                Direction.INCREMENT -> e.increment(d)
                Direction.DECREMENT -> e.decrement(d)
                else -> e[d] = value
            }
            fireChangeEvent()
        }

        override fun getPreviousValue(): Any? {
            val d = data ?: return null
            return if (entry?.canDecrement(d) == true) Direction.DECREMENT
            else null
        }

        override fun getNextValue(): Any? {
            val d = data ?: return null
            return  if (entry?.canIncrement(d) == true) Direction.INCREMENT
            else null
        }

    }

    private enum class Direction {
        INCREMENT, DECREMENT
    }

}