package com.computernerd1101.goban.desktop.internal

import com.computernerd1101.goban.desktop.OvertimeComponent
import com.computernerd1101.goban.sgf.GameInfo
import com.computernerd1101.goban.time.Overtime
import java.awt.Component
import javax.swing.*
import javax.swing.event.ListDataListener

abstract class AbstractOvertimeModel(
    val renderer: ListCellRenderer<in Any>
): ComboBoxModel<Any>, ListCellRenderer<Any> {

    val items: Array<Any>

    init {
        val types = Overtime.loadTypes()
        @Suppress("RemoveExplicitTypeArguments")
        items = Array<Any>(1 + types.size) { index ->
            when(index) {
                0 -> "Overtime..."
                else -> types[index - 1]
            }
        }
        (renderer as? JLabel)?.horizontalAlignment = SwingConstants.CENTER
        for(item in types) {
            @Suppress("LeakingThis")
            initType(item)
        }
    }

    protected open fun initType(item: Overtime) = Unit

    abstract val gameInfo: GameInfo?
    abstract val overtimeView: OvertimeComponent

    override fun getListCellRendererComponent(
        list: JList<out Any>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component = renderer.getListCellRendererComponent(
        list,
        if (value is Overtime) "Overtime: ${value.typeString}" else value,
        index,
        isSelected,
        cellHasFocus
    )

    override fun getSelectedItem(): Any {
        val overtime = gameInfo?.overtime ?: return items[0]
        var index = items.indexOfFirst { it.javaClass == overtime.javaClass }
        if (index < 0) index = 0
        return items[index]
    }

    override fun setSelectedItem(anItem: Any?) {
        val info = gameInfo ?: return
        var overtime: Overtime? = anItem as? Overtime
        if (info.overtime?.javaClass == overtime?.javaClass) return
        overtime = overtime?.clone()
        info.overtime = overtime
        overtimeView.overtime = overtime
        overtimeView.updateUI()
    }

    override fun getSize() = items.size

    override fun getElementAt(index: Int) = items[index]

    override fun addListDataListener(l: ListDataListener?) = Unit

    override fun removeListDataListener(l: ListDataListener?) = Unit

}