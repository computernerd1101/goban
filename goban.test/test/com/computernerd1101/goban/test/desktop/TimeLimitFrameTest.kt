package com.computernerd1101.goban.test.desktop

import com.computernerd1101.goban.time.*
import com.computernerd1101.goban.time0.TimeLimit
import java.awt.*
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater {
        TimeLimitFrame(5000L, CanadianOvertime(5000L, 3))
    }
}

class TimeLimitFrame(
    mainTime: Long,
    overtime: Overtime?
): JFrame() {

    private val blackTimeLimit = TimeLimit(mainTime, overtime)
    private val whiteTimeLimit = TimeLimit(mainTime, overtime)
    private var isBlack = true

    private val labelBlack = JLabel("Black")
    private val labelWhite = JLabel("White")
    private val labelBlackTime = JLabel()
    private val labelWhiteTime  = JLabel()
    private val labelBlackOvertime = JLabel()
    private val labelWhiteOvertime = JLabel()
    private val buttonSubmit = JButton("Submit move")

    init {
        blackTimeLimit.addTimeListener(
            DisplayTimeListener(
            labelBlack, labelBlackTime, labelBlackOvertime
        )
        )
        whiteTimeLimit.addTimeListener(
            DisplayTimeListener(
            labelWhite, labelWhiteTime, labelWhiteOvertime
        )
        )
        labelBlack.foreground = Color.RED
        buttonSubmit.addActionListener {
            if (isBlack) {
                isBlack = false
                blackTimeLimit.isTicking = false
                whiteTimeLimit.isTicking = true
            } else {
                isBlack = true
                whiteTimeLimit.isTicking = false
                blackTimeLimit.isTicking = true
            }
        }
        val panel = JPanel(GridLayout(3, 3))
        panel.add(JLabel())
        panel.add(JLabel("Time"))
        panel.add(JLabel("Overtime"))
        panel.add(labelBlack)
        panel.add(labelBlackTime)
        panel.add(labelBlackOvertime)
        panel.add(labelWhite)
        panel.add(labelWhiteTime)
        panel.add(labelWhiteOvertime)
        this.layout = BorderLayout()
        add(panel, BorderLayout.CENTER)
        add(buttonSubmit, BorderLayout.SOUTH)
        setSize(500, 300)
        setLocationRelativeTo(null)
        title = "Testing Time Limit"
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        isVisible = true
        blackTimeLimit.isTicking = true
    }

    private class DisplayTimeListener(
        val labelPlayer: JLabel,
        val labelTime: JLabel,
        val labelOvertime: JLabel
    ): TimeListener {

        override fun timeElapsed(e: TimeEvent) {
            labelPlayer.foreground = if (e.isTicking) Color.RED else Color.BLACK
            labelTime.text = e.timeRemaining.millisToStringSeconds()
            labelOvertime.text = when {
                e.isExpired -> "You lose!"
                e.isOvertime -> e.overtimeCode.toString()
                else -> ""
            }
        }

    }

}