package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.desktop.internal.InternalMarker
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.text.NumberFormat
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.text.NumberFormatter

class CN13Spinner: JSpinner() {

    @get:JvmName("editor")
    lateinit var editor: Editor private set

    override fun getEditor(): Editor {
        return editor
    }

    override fun createEditor(model: SpinnerModel?): JComponent {
        val editor = Editor(this)
        this.editor = editor
        return editor
    }

    var adjustCaret: Boolean = false

    override fun updateUI() {
        super.updateUI()
        val model = this.model
        if (model is Formatter)
            model.fireChangeEvent(InternalMarker)
    }

    private val formatterFactory = FormatterFactory(this)

    private class FormatterFactory(val spinner: CN13Spinner):
        JFormattedTextField.AbstractFormatterFactory(), KeyListener {

        override fun getFormatter(tf: JFormattedTextField?): JFormattedTextField.AbstractFormatter? {
            return spinner.model as? JFormattedTextField.AbstractFormatter
        }

        override fun keyTyped(e: KeyEvent?) = Unit

        override fun keyPressed(e: KeyEvent?) = Unit

        override fun keyReleased(e: KeyEvent) {
            if (spinner.adjustCaret) {
                val ftf = e.component
                if (ftf is JFormattedTextField) {
                    val ch = e.keyChar
                    if (ch != '\uFFFF') {
                        val text = ftf.text
                        var i = ftf.caretPosition - 1
                        if (i >= 0) {
                            while(i < text.length) {
                                if (text[i] == ch) {
                                    ftf.caretPosition = i + 1
                                    break
                                }
                                i++
                            }
                        }
                    }
                }
            }
        }

    }

    abstract class Formatter: NumberFormatter, SpinnerModel {

        companion object;

        protected constructor()

        protected constructor(format: NumberFormat): super(format)

        private val listeners = mutableListOf<ChangeListener>()
        private val event by lazy(LazyThreadSafetyMode.PUBLICATION) { ChangeEvent(this) }

        protected fun fireChangeEvent() {
            for(i in (listeners.size - 1) downTo 0)
                listeners[i].stateChanged(event)
        }

        internal fun fireChangeEvent(marker: InternalMarker) {
            marker.ignore()
            fireChangeEvent()
        }

        inline fun addChangeListener(crossinline l: (ChangeEvent) -> Unit): ChangeListener {
            val listener = ChangeListener { l(it) }
            addChangeListener(listener)
            return listener
        }

        override fun addChangeListener(l: ChangeListener?) {
            if (l != null) listeners.add(l)
        }

        override fun removeChangeListener(l: ChangeListener?) {
            if (l != null) listeners.remove(l)
        }

    }

    class Editor(spinner: CN13Spinner): DefaultEditor(spinner) {
        init {
            val ftf: JFormattedTextField = textField
            ftf.isEditable = true
            ftf.formatterFactory = spinner.formatterFactory
            ftf.horizontalAlignment = JTextField.RIGHT
            ftf.addKeyListener(spinner.formatterFactory)
        }
    }

}