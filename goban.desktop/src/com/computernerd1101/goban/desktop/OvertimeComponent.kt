package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.annotations.*
import com.computernerd1101.goban.time.Overtime
import java.awt.*
import java.text.ParseException
import javax.swing.*

class OvertimeComponent(
    overtime: Overtime? = null,
    minRows: Int = 0
): JComponent() {

    var overtime: Overtime? = overtime
        set(overtime) {
            field = overtime
            val count = overtimeProperties?.entryCount ?: 0
            if (overtime != null)
                setNonNullOvertime(overtime, count)
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
            val count = overtimeProperties?.entryCount ?: 0
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

    private var overtimeProperties: PropertyFactory<Overtime>? = null

    private val constraints = GridBagConstraints()

    init {
        layout = GridBagLayout()
    }

    private var rows: Array<Row?> =
        if (minRows <= 0) Rows.empty
        else Array(minRows) { y ->
            val row = Row()
            addRow(y, row)
            row.spin.isVisible = false
            row
        }

    init {
        if (overtime != null)
            setNonNullOvertime(overtime, 0)
    }

    private fun setNonNullOvertime(overtime: Overtime, oldCount: Int) {
        val properties = PropertyFactory(overtime::class)
        val count = properties.entryCount
        overtimeProperties = properties
        var rows = this.rows
        if (count > rows.size) {
            rows = rows.copyOf(count)
            this.rows = rows
        }
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
            row.label.text = "${entry.name}: "
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

        val empty = emptyArray<Row?>()

    }

    private inner class Row: CN13Spinner.Formatter() {

        var entry: PropertyFactory.Entry<Overtime>? = null

        val label = JLabel(" ")
        val spin = CN13Spinner()

        init {
            spin.model = this
            allowsInvalid = true
            commitsOnValidEdit = false
        }

        override fun valueToString(value: Any?): String {
            val o = overtime
            val e = entry
            if (o == null || e == null)
                return super.valueToString(value)
            return e.getString(o)
        }

        override fun stringToValue(text: String): Any? {
            val o = overtime
            val e = entry
            if (o == null || e == null)
                return super.stringToValue(text)
            try {
                e.setString(o, text)
            } catch(ex: Exception) {
                throw ParseException(text, 0)
            }
            return e[o]
        }

        override fun getValue(): Any? {
            val o = overtime ?: return null
            val e = entry ?: return null
            return e[o]
        }

        override fun setValue(value: Any?) {
            if (value == null) return
            val o = overtime ?: return
            val e = entry ?: return
            when(value) {
                Direction.INCREMENT -> e.increment(o)
                Direction.DECREMENT -> e.decrement(o)
                else -> e[o] = value
            }
            fireChangeEvent()
        }

        override fun getPreviousValue(): Any? {
            val o = overtime ?: return null
            return if (entry?.canDecrement(o) == true) Direction.DECREMENT
            else null
        }

        override fun getNextValue(): Any? {
            val o = overtime ?: return null
            return  if (entry?.canIncrement(o) == true) Direction.INCREMENT
            else null
        }

    }

    private enum class Direction {
        INCREMENT, DECREMENT
    }

}