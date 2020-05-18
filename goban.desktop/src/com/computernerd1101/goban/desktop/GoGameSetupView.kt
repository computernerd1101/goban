package com.computernerd1101.goban.desktop

import com.computernerd1101.goban.*
import com.computernerd1101.goban.annotations.PropertyFactory
import com.computernerd1101.goban.desktop.internal.*
import com.computernerd1101.goban.sgf.GameInfo
import com.computernerd1101.goban.time.Overtime
import java.awt.*
import java.awt.event.*
import java.text.NumberFormat
import java.util.*
import javax.swing.*
import javax.swing.event.ListDataListener

fun main() {
    SwingUtilities.invokeLater {
        val setup = GoGameSetupView().showDialog(null)
        if (setup != null)
            GoGameFrame(setup).isVisible = true
    }
}

class GoGameSetupView: JComponent() {

    private val comboSize = JComboBox<Any>()
    private val checkHeight = JCheckBox("Height:")
    private val spinWidth = CN13Spinner()
    private val spinHeight = CN13Spinner()
    private val comboHandicap = JComboBox<String>()
    private val spinHandicap = CN13Spinner()
    private val comboKomi = JComboBox<Any>()
    private val spinKomi = CN13Spinner()
    private val comboRules = JComboBox<RulesPreset>()
    private val comboScore = JComboBox<String>()
    private val checkSuicide = JCheckBox("Allow Suicide?")
    private val comboSuperko = JComboBox<Superko>()
    private val spinTimeLimit = CN13Spinner()
    private val comboOvertime = JComboBox<Any>()
    private val overtimeView = OvertimeComponent(minRows = OvertimeModel(comboOvertime.renderer).maxEntries)

    private val gameSetup: GoGameSetup
    private var territoryScore: Boolean
    private var superko: Superko

    init {
        val gameInfo = GameInfo()
        val rules = GoRules.JAPANESE
        gameInfo.rules = rules
        gameSetup = GoGameSetup(gameInfo = gameInfo)
        territoryScore = rules.territoryScore
        superko = rules.superko
        val sizeModel = SizeModel(comboSize.renderer)
        comboSize.renderer = sizeModel
        comboSize.model = sizeModel
        checkHeight.horizontalAlignment = SwingConstants.RIGHT
        checkHeight.addActionListener(sizeModel)
        spinWidth.model = WidthFormatter()
        spinHeight.model = HeightFormatter()
        spinHeight.isEnabled = false
        (comboKomi.renderer as? JLabel)?.horizontalAlignment = SwingConstants.RIGHT
        val komiModel = KomiFormatter()
        comboKomi.model = komiModel
        spinKomi.model = komiModel
        (comboHandicap.renderer as? JLabel)?.horizontalAlignment = SwingConstants.RIGHT
        val handicapModel = HandicapFormatter()
        comboHandicap.model = handicapModel
        spinHandicap.model = handicapModel
        val rulesModel = RulesModel()
        comboRules.model = rulesModel
        comboScore.model = ScoreModel()
        checkSuicide.addActionListener(rulesModel)
        val superkoModel = SuperkoModel(comboSuperko.renderer)
        comboSuperko.model = superkoModel
        comboSuperko.renderer = superkoModel
        spinTimeLimit.model = TimeLimitFormatter()
        spinTimeLimit.adjustCaret = true
        layout = GridBagLayout()
        var row = 0
        val gbc1 = GridBagConstraints()
        gbc1.fill = GridBagConstraints.HORIZONTAL
        gbc1.anchor = GridBagConstraints.EAST
        add(comboSize, gbc1)
        val gbc2 = GridBagConstraints()
        gbc2.gridx = 1
        gbc2.weightx = 1.0
        gbc2.fill = GridBagConstraints.HORIZONTAL
        add(spinWidth, gbc2)
        gbc1.gridy = ++row
        gbc1.fill = GridBagConstraints.NONE
        add(checkHeight, gbc1)
        gbc2.gridy = row
        add(spinHeight, gbc2)
        gbc1.gridy = ++row
        add(comboHandicap, gbc1)
        gbc2.gridy = row
        add(spinHandicap, gbc2)
        gbc1.gridy = ++row
        gbc1.fill = GridBagConstraints.HORIZONTAL
        add(comboKomi, gbc1)
        gbc2.gridy = row
        add(spinKomi, gbc2)
        gbc2.gridx = 0
        gbc2.gridy = ++row
        gbc2.gridwidth = 2
        add(comboRules, gbc2)
        gbc2.gridy = ++row
        add(comboScore, gbc2)
        gbc2.gridy = ++row
        add(checkSuicide, gbc2)
        gbc2.gridy = ++row
        add(comboSuperko, gbc2)
        gbc1.gridy = ++row
        gbc1.fill = GridBagConstraints.NONE
        add(JLabel("Time Limit: "), gbc1)
        gbc2.gridx = 1
        gbc2.gridy = row
        gbc2.gridwidth = 1
        add(spinTimeLimit, gbc2)
        gbc2.gridx = 0
        gbc2.gridy = ++row
        gbc2.gridwidth = 2
        add(comboOvertime, gbc2)
        gbc2.gridy = ++row
        add(overtimeView, gbc2)
    }

    fun showDialog(parentComponent: Component?): GoGameSetup? {
        return if (JOptionPane.showConfirmDialog(
                parentComponent,
                this,
                "New Game",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            ) == JOptionPane.OK_OPTION) gameSetup
        else null
    }

    companion object {

        private val SIZE_PRESETS = intArrayOf(9, 13, 19)

        private const val FIXED_HANDICAP = "Fixed Handicap: "
        private const val FREE_HANDICAP = "Free Handicap: "

        private val RULES_PRESETS = enumValues<RulesPreset>()
        private val SUPERKO_VALUES = enumValues<Superko>()

        private val rulesMap = EnumMap<GoRules, RulesPreset>(GoRules::class.java).apply {
            for(preset in RULES_PRESETS)
                if (preset != RulesPreset.CUSTOM) this[preset.rules] = preset
        }

        private val superkoMap = EnumMap<Superko, String>(Superko::class.java).apply {
            this[Superko.NATURAL] = "Natural Situational Superko"
            this[Superko.SITUATIONAL] = "Situational Superko"
            this[Superko.POSITIONAL] = "Positional Superko"
        }

        private const val TERRITORY_SCORE = "Score by Territory"
        private const val AREA_SCORE = "Score by Area"

    }

    private enum class RulesPreset(
        private val string: String,
        val rules: GoRules
    ) {

        JAPANESE("Japanese Rules", GoRules.JAPANESE),
        AGA("AGA Rules", GoRules.AGA),
        NZ("New Zealand Rules", GoRules.NEW_ZEALAND),
        GOE("Ing Rules", GoRules.ING),
        CUSTOM("Custom Rules...", GoRules.DEFAULT);

        override fun toString(): String {
            return string
        }

    }

    private inner class RulesModel: ComboBoxModel<RulesPreset>, ActionListener {

        override fun getSelectedItem(): RulesPreset {
            return rulesMap[gameSetup.gameInfo.rules] ?: RulesPreset.CUSTOM
        }

        override fun setSelectedItem(anItem: Any?) {
            if (anItem !is RulesPreset || anItem == RulesPreset.CUSTOM) return
            val rules = anItem.rules
            territoryScore = rules.territoryScore
            comboScore.updateUI()
            checkSuicide.isSelected = rules.allowSuicide
            comboSuperko.updateUI()
            gameSetup.gameInfo.rules = anItem.rules
        }

        override fun getSize(): Int {
            var size = RULES_PRESETS.size
            if (selectedItem != RulesPreset.CUSTOM) size--
            return size
        }

        override fun getElementAt(index: Int) = RULES_PRESETS[index]

        override fun addListDataListener(l: ListDataListener?) = Unit

        override fun removeListDataListener(l: ListDataListener?) = Unit

        override fun actionPerformed(e: ActionEvent?) {
            comboRules.updateUI()
            gameSetup.gameInfo.rules = GoRules(
                superko,
                territoryScore = territoryScore,
                allowSuicide = checkSuicide.isSelected)
        }

    }

    private inner class ScoreModel: ComboBoxModel<String> {

        override fun getSelectedItem(): String {
            return if (territoryScore) TERRITORY_SCORE else AREA_SCORE
        }

        override fun setSelectedItem(anItem: Any?) {
            val territory = when(anItem) {
                AREA_SCORE -> false
                TERRITORY_SCORE -> true
                else -> return
            }
            territoryScore = territory
            comboRules.updateUI()
            val gameInfo = gameSetup.gameInfo
            gameInfo.rules = gameInfo.rules.copy(territoryScore = territory)
        }

        override fun getSize() = 2

        override fun getElementAt(index: Int) = when(index) {
            0 -> TERRITORY_SCORE
            1 -> AREA_SCORE
            else -> throw IndexOutOfBoundsException("$index")
        }

        override fun addListDataListener(l: ListDataListener?) = Unit

        override fun removeListDataListener(l: ListDataListener?) = Unit

    }

    private inner class SuperkoModel(
        parentRenderer: ListCellRenderer<in Superko>
    ): ListCellRenderer<Superko>, ComboBoxModel<Superko> {

        @Suppress("UNCHECKED_CAST")
        val parentRenderer = parentRenderer as ListCellRenderer<Any?>

        override fun getListCellRendererComponent(
            list: JList<out Superko>,
            value: Superko,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component = parentRenderer.getListCellRendererComponent(
            list,
            superkoMap[value],
            index,
            isSelected,
            cellHasFocus
        )

        override fun getSelectedItem(): Superko {
            return superko
        }

        override fun setSelectedItem(anItem: Any?) {
            if (anItem is Superko) {
                superko = anItem
                comboRules.updateUI()
                val gameInfo = gameSetup.gameInfo
                gameInfo.rules = gameInfo.rules.copy(superko = anItem)
            }
        }

        override fun getSize() = SUPERKO_VALUES.size

        override fun getElementAt(index: Int) = SUPERKO_VALUES[index]

        override fun addListDataListener(l: ListDataListener?) = Unit

        override fun removeListDataListener(l: ListDataListener?) = Unit

    }

    private inner class SizeModel(val renderer: ListCellRenderer<in Any>):
        ListCellRenderer<Any>, ComboBoxModel<Any>, ActionListener {

        init {
            (renderer as? JLabel)?.horizontalAlignment = SwingConstants.RIGHT
        }

        override fun getListCellRendererComponent(
            list: JList<out Any>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = renderer.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus
            )
            if (value is Number && component is JLabel)
                component.text = "${value}x$value"
            return component
        }

        override fun getSelectedItem(): Any = if (checkHeight.isSelected) "Width: " else "Size: "

        override fun setSelectedItem(anItem: Any?) {
            checkHeight.isSelected = false
            spinWidth.value = anItem
            spinHeight.value = anItem
            spinHeight.isEnabled = false
        }

        override fun getSize() = SIZE_PRESETS.size

        override fun getElementAt(index: Int) = SIZE_PRESETS[index]

        override fun addListDataListener(l: ListDataListener?) = Unit

        override fun removeListDataListener(l: ListDataListener?) = Unit

        override fun actionPerformed(e: ActionEvent?) {
            comboSize.updateUI()
            val setHeight = checkHeight.isSelected
            if (!setHeight)
                spinHeight.value = spinWidth.value
            spinHeight.isEnabled = setHeight
        }

    }

    private abstract inner class SizeFormatter:
        CN13Spinner.Formatter(NumberFormat.getIntegerInstance()) {

        init {
            commitsOnValidEdit = true
            allowsInvalid = false
        }

        abstract override fun getValue(): Int

        override fun setValue(value: Any?) {
            if (value is Number) {
                val n = value.toInt()
                if (n in 1..52 && n != this.value) {
                    setInt(n)
                    fireChangeEvent()
                    val handicap = gameSetup.gameInfo.handicap
                    val maxHandicap = gameSetup.maxHandicap
                    if (handicap > maxHandicap)
                        spinHandicap.value = maxHandicap
                }
            }
        }

        protected abstract fun setInt(value: Int)

        override fun getPreviousValue(): Any? {
            val value = this.value
            return if (value > 1) value - 1 else null
        }

        override fun getNextValue(): Any? {
            val value = this.value
            return if (value < 52) value + 1 else null
        }

    }

    private inner class WidthFormatter: SizeFormatter() {

        override fun getValue() = gameSetup.width

        override fun setInt(value: Int) {
            gameSetup.width = value
            if (!checkHeight.isSelected) spinHeight.value = value
        }

    }

    private inner class HeightFormatter: SizeFormatter() {

        override fun getValue() = gameSetup.height

        override fun setInt(value: Int) {
            gameSetup.height = value
        }

    }

    private inner class KomiFormatter: AbstractKomiFormatter() {

        override val gameInfo: GameInfo
            get() = gameSetup.gameInfo

        override val spinKomi: CN13Spinner
            get() = this@GoGameSetupView.spinKomi

    }

    private inner class HandicapFormatter:
        CN13Spinner.Formatter(NumberFormat.getIntegerInstance()),
        ComboBoxModel<String> {

        override fun getValue() = gameSetup.gameInfo.handicap

        override fun setValue(value: Any?) {
            if (value is Number) {
                val max = gameSetup.maxHandicap
                var handicap = value.toInt()
                if (handicap > max) handicap = max
                if (handicap <= 1) handicap = 0
                val gameInfo = gameSetup.gameInfo
                if (handicap != gameInfo.handicap) {
                    gameInfo.handicap = handicap
                    fireChangeEvent()
                }
            }
        }

        override fun getPreviousValue(): Int? {
            val handicap = value - 1
            return when {
                handicap < 0 -> null
                handicap <= 1 -> 0
                else -> handicap
            }
        }

        override fun getNextValue(): Int? {
            var handicap = value
            handicap = if (handicap <= 0) 2 else handicap + 1
            val max = gameSetup.maxHandicap
            return if (handicap > max) null else handicap
        }

        override fun getSelectedItem(): String {
            return if (gameSetup.isFreeHandicap) FREE_HANDICAP else FIXED_HANDICAP
        }

        override fun setSelectedItem(anItem: Any?) {
            val free = when(anItem) {
                FIXED_HANDICAP -> false
                FREE_HANDICAP -> true
                else -> return
            }
            gameSetup.isFreeHandicap = free
            val max = gameSetup.maxHandicap
            if (value > max) value = max
        }

        override fun getSize() = 2

        override fun getElementAt(index: Int) = when(index) {
            0 -> FIXED_HANDICAP
            1 -> FREE_HANDICAP
            else -> throw IndexOutOfBoundsException("$index")
        }

        override fun addListDataListener(l: ListDataListener?) = Unit

        override fun removeListDataListener(l: ListDataListener?) = Unit

    }

    private inner class TimeLimitFormatter: AbstractTimeLimitFormatter() {

        override val gameInfo: GameInfo
            get() = gameSetup.gameInfo

    }

    private inner class OvertimeModel(renderer: ListCellRenderer<in Any>):
        AbstractOvertimeModel(renderer) {

        init {
            comboOvertime.model = this
            comboOvertime.renderer = this
        }

        var maxEntries: Int = 0; private set

        override fun initType(item: Overtime) {
            val properties = PropertyFactory(item::class)
            val count = properties.entryCount
            if (count > maxEntries) maxEntries = count
        }

        override val gameInfo: GameInfo
            get() = gameSetup.gameInfo

        override val overtimeView: OvertimeComponent
            get() = this@GoGameSetupView.overtimeView

    }

}