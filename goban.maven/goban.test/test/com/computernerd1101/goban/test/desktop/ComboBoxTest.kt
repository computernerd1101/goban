package com.computernerd1101.goban.test.desktop

import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater {
        testComboBox(useCards=true)
    }
}

fun testComboBox(useCards: Boolean) {
    val frame = JFrame("TestComboBox")
    frame.setSize(500, 500)
    frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    frame.setLocationRelativeTo(null)
    frame.layout = BorderLayout()
    val combo = JComboBox(arrayOf("A", "B", "C", "D"))
    if (useCards) {
        val cardLayout = CardLayout()
        val cardPanel = JPanel(cardLayout)
        var panel = JPanel(BorderLayout())
        panel.add(combo, BorderLayout.NORTH)
        cardPanel.add(panel, "a")
        panel = JPanel(BorderLayout())
        panel.add(JLabel("foobar"), BorderLayout.NORTH)
        cardPanel.add(panel, "b")
        cardLayout.show(cardPanel, "a")
        frame.add(cardPanel, BorderLayout.CENTER)
        val button = JToggleButton("switch")
        button.addActionListener {
            cardLayout.show(
                cardPanel,
                if (button.isSelected) "b" else "a"
            )
        }
        frame.add(button, BorderLayout.SOUTH)
    } else {
        frame.add(combo, BorderLayout.NORTH)
    }
    frame.isVisible = true
}