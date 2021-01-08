@file:Suppress("UNCHECKED_CAST", "unused")

package com.computernerd1101.goban.internal

import java.util.concurrent.atomic.AtomicLongFieldUpdater

internal object GobanRows {
    @JvmField val init = arrayOf(
        GobanRows1, GobanRows2,
        GobanRows2, GobanRows4,
        GobanRows3, GobanRows6,
        GobanRows4, GobanRows8,
        GobanRows5, GobanRows10,
        GobanRows6, GobanRows12,
        GobanRows7, GobanRows14,
        GobanRows8, GobanRows16,
        GobanRows9, GobanRows18,
        GobanRows10, GobanRows20,
        GobanRows11, GobanRows22,
        GobanRows12, GobanRows24,
        GobanRows13, GobanRows26,
        GobanRows14, GobanRows28,
        GobanRows15, GobanRows30,
        GobanRows16, GobanRows32,
        GobanRows17, GobanRows34,
        GobanRows18, GobanRows36,
        GobanRows19, GobanRows38,
        GobanRows20, GobanRows40,
        GobanRows21, GobanRows42,
        GobanRows22, GobanRows44,
        GobanRows23, GobanRows46,
        GobanRows24, GobanRows48,
        GobanRows25, GobanRows50,
        GobanRows26, GobanRows52,
        GobanRows27, GobanRows54,
        GobanRows28, GobanRows56,
        GobanRows29, GobanRows58,
        GobanRows30, GobanRows60,
        GobanRows31, GobanRows62,
        GobanRows32, GobanRows64,
        GobanRows33, GobanRows66,
        GobanRows34, GobanRows68,
        GobanRows35, GobanRows70,
        GobanRows36, GobanRows72,
        GobanRows37, GobanRows74,
        GobanRows38, GobanRows76,
        GobanRows39, GobanRows78,
        GobanRows40, GobanRows80,
        GobanRows41, GobanRows82,
        GobanRows42, GobanRows84,
        GobanRows43, GobanRows86,
        GobanRows44, GobanRows88,
        GobanRows45, GobanRows90,
        GobanRows46, GobanRows92,
        GobanRows47, GobanRows94,
        GobanRows48, GobanRows96,
        GobanRows49, GobanRows98,
        GobanRows50, GobanRows100,
        GobanRows51, GobanRows102,
        GobanRows52, GobanRows104
    )
    @JvmField val updaters = arrayOf(
        GobanRows1.update0, GobanRows2.update1,
        GobanRows1.update0, GobanRows2.update1,
        GobanRows3.update2, GobanRows4.update3,
        GobanRows5.update4, GobanRows6.update5,
        GobanRows7.update6, GobanRows8.update7,
        GobanRows9.update8, GobanRows10.update9,
        GobanRows11.update10, GobanRows12.update11,
        GobanRows13.update12, GobanRows14.update13,
        GobanRows15.update14, GobanRows16.update15,
        GobanRows17.update16, GobanRows18.update17,
        GobanRows19.update18, GobanRows20.update19,
        GobanRows21.update20, GobanRows22.update21,
        GobanRows23.update22, GobanRows24.update23,
        GobanRows25.update24, GobanRows26.update25,
        GobanRows27.update26, GobanRows28.update27,
        GobanRows29.update28, GobanRows30.update29,
        GobanRows31.update30, GobanRows32.update31,
        GobanRows33.update32, GobanRows34.update33,
        GobanRows35.update34, GobanRows36.update35,
        GobanRows37.update36, GobanRows38.update37,
        GobanRows39.update38, GobanRows40.update39,
        GobanRows41.update40, GobanRows42.update41,
        GobanRows43.update42, GobanRows44.update43,
        GobanRows45.update44, GobanRows46.update45,
        GobanRows47.update46, GobanRows48.update47,
        GobanRows49.update48, GobanRows50.update49,
        GobanRows51.update50, GobanRows52.update51,
        GobanRows54.update52, GobanRows54.update53,
        GobanRows56.update54, GobanRows56.update55,
        GobanRows58.update56, GobanRows58.update57,
        GobanRows60.update58, GobanRows60.update59,
        GobanRows62.update60, GobanRows62.update61,
        GobanRows64.update62, GobanRows64.update63,
        GobanRows66.update64, GobanRows66.update65,
        GobanRows68.update66, GobanRows68.update67,
        GobanRows70.update68, GobanRows70.update69,
        GobanRows72.update70, GobanRows72.update71,
        GobanRows74.update72, GobanRows74.update73,
        GobanRows76.update74, GobanRows76.update75,
        GobanRows78.update76, GobanRows78.update77,
        GobanRows80.update78, GobanRows80.update79,
        GobanRows82.update80, GobanRows82.update81,
        GobanRows84.update82, GobanRows84.update83,
        GobanRows86.update84, GobanRows86.update85,
        GobanRows88.update86, GobanRows88.update87,
        GobanRows90.update88, GobanRows90.update89,
        GobanRows92.update90, GobanRows92.update91,
        GobanRows94.update92, GobanRows94.update93,
        GobanRows96.update94, GobanRows96.update95,
        GobanRows98.update96, GobanRows98.update97,
        GobanRows100.update98, GobanRows100.update99,
        GobanRows102.update100, GobanRows102.update101,
        GobanRows104.update102, GobanRows104.update103
    )
}
internal open class GobanRows1 {
    open val size: Int get() = 1
    @Volatile private var row0: Long = 0L
    operator fun get(index: Int) = GobanRows.updaters[index][this]
    operator fun set(index: Int, value: Long) {
        GobanRows.updaters[index][this] = value
    }
    companion object: () -> GobanRows1 {
        @JvmField internal val update0 = atomicLongUpdater<GobanRows1>("row0")
        override fun invoke() = GobanRows1()
    }
}
internal open class GobanRows2: GobanRows1() {
    override val size: Int get() = 2
    @Volatile private var row1: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update1 = atomicLongUpdater<GobanRows2>("row1") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows2()
    }
}
internal open class GobanRows3: GobanRows2() {
    override val size: Int get() = 3
    @Volatile private var row2: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update2 = atomicLongUpdater<GobanRows3>("row2") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows3()
    }
}
internal open class GobanRows4: GobanRows3() {
    override val size: Int get() = 4
    @Volatile private var row3: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update3 = atomicLongUpdater<GobanRows4>("row3") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows4()
    }
}
internal open class GobanRows5: GobanRows4() {
    override val size: Int get() = 5
    @Volatile private var row4: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update4 = atomicLongUpdater<GobanRows5>("row4") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows5()
    }
}
internal open class GobanRows6: GobanRows5() {
    override val size: Int get() = 6
    @Volatile private var row5: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update5 = atomicLongUpdater<GobanRows6>("row5") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows6()
    }
}
internal open class GobanRows7: GobanRows6() {
    override val size: Int get() = 7
    @Volatile private var row6: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update6 = atomicLongUpdater<GobanRows7>("row6") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows7()
    }
}
internal open class GobanRows8: GobanRows7() {
    override val size: Int get() = 8
    @Volatile private var row7: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update7 = atomicLongUpdater<GobanRows8>("row7") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows8()
    }
}
internal open class GobanRows9: GobanRows8() {
    override val size: Int get() = 9
    @Volatile private var row8: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update8 = atomicLongUpdater<GobanRows9>("row8") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows9()
    }
}
internal open class GobanRows10: GobanRows9() {
    override val size: Int get() = 10
    @Volatile private var row9: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update9 = atomicLongUpdater<GobanRows10>("row9") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows10()
    }
}
internal open class GobanRows11: GobanRows10() {
    override val size: Int get() = 11
    @Volatile private var row10: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update10 = atomicLongUpdater<GobanRows11>("row10") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows11()
    }
}
internal open class GobanRows12: GobanRows11() {
    override val size: Int get() = 12
    @Volatile private var row11: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update11 = atomicLongUpdater<GobanRows12>("row11") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows12()
    }
}
internal open class GobanRows13: GobanRows12() {
    override val size: Int get() = 13
    @Volatile private var row12: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update12 = atomicLongUpdater<GobanRows13>("row12") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows13()
    }
}
internal open class GobanRows14: GobanRows13() {
    override val size: Int get() = 14
    @Volatile private var row13: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update13 = atomicLongUpdater<GobanRows14>("row13") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows14()
    }
}
internal open class GobanRows15: GobanRows14() {
    override val size: Int get() = 15
    @Volatile private var row14: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update14 = atomicLongUpdater<GobanRows15>("row14") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows15()
    }
}
internal open class GobanRows16: GobanRows15() {
    override val size: Int get() = 16
    @Volatile private var row15: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update15 = atomicLongUpdater<GobanRows16>("row15") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows16()
    }
}
internal open class GobanRows17: GobanRows16() {
    override val size: Int get() = 17
    @Volatile private var row16: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update16 = atomicLongUpdater<GobanRows17>("row16") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows17()
    }
}
internal open class GobanRows18: GobanRows17() {
    override val size: Int get() = 18
    @Volatile private var row17: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update17 = atomicLongUpdater<GobanRows18>("row17") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows18()
    }
}
internal open class GobanRows19: GobanRows18() {
    override val size: Int get() = 19
    @Volatile private var row18: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update18 = atomicLongUpdater<GobanRows19>("row18") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows19()
    }
}
internal open class GobanRows20: GobanRows19() {
    override val size: Int get() = 20
    @Volatile private var row19: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update19 = atomicLongUpdater<GobanRows20>("row19") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows20()
    }
}
internal open class GobanRows21: GobanRows20() {
    override val size: Int get() = 21
    @Volatile private var row20: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update20 = atomicLongUpdater<GobanRows21>("row20") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows21()
    }
}
internal open class GobanRows22: GobanRows21() {
    override val size: Int get() = 22
    @Volatile private var row21: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update21 = atomicLongUpdater<GobanRows22>("row21") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows22()
    }
}
internal open class GobanRows23: GobanRows22() {
    override val size: Int get() = 23
    @Volatile private var row22: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update22 = atomicLongUpdater<GobanRows23>("row22") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows23()
    }
}
internal open class GobanRows24: GobanRows23() {
    override val size: Int get() = 24
    @Volatile private var row23: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update23 = atomicLongUpdater<GobanRows24>("row23") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows24()
    }
}
internal open class GobanRows25: GobanRows24() {
    override val size: Int get() = 25
    @Volatile private var row24: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update24 = atomicLongUpdater<GobanRows25>("row24") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows25()
    }
}
internal open class GobanRows26: GobanRows25() {
    override val size: Int get() = 26
    @Volatile private var row25: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update25 = atomicLongUpdater<GobanRows26>("row25") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows26()
    }
}
internal open class GobanRows27: GobanRows26() {
    override val size: Int get() = 27
    @Volatile private var row26: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update26 = atomicLongUpdater<GobanRows27>("row26") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows27()
    }
}
internal open class GobanRows28: GobanRows27() {
    override val size: Int get() = 28
    @Volatile private var row27: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update27 = atomicLongUpdater<GobanRows28>("row27") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows28()
    }
}
internal open class GobanRows29: GobanRows28() {
    override val size: Int get() = 29
    @Volatile private var row28: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update28 = atomicLongUpdater<GobanRows29>("row28") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows29()
    }
}
internal open class GobanRows30: GobanRows29() {
    override val size: Int get() = 30
    @Volatile private var row29: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update29 = atomicLongUpdater<GobanRows30>("row29") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows30()
    }
}
internal open class GobanRows31: GobanRows30() {
    override val size: Int get() = 31
    @Volatile private var row30: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update30 = atomicLongUpdater<GobanRows31>("row30") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows31()
    }
}
internal open class GobanRows32: GobanRows31() {
    override val size: Int get() = 32
    @Volatile private var row31: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update31 = atomicLongUpdater<GobanRows32>("row31") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows32()
    }
}
internal open class GobanRows33: GobanRows32() {
    override val size: Int get() = 33
    @Volatile private var row32: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update32 = atomicLongUpdater<GobanRows33>("row32") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows33()
    }
}
internal open class GobanRows34: GobanRows33() {
    override val size: Int get() = 34
    @Volatile private var row33: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update33 = atomicLongUpdater<GobanRows34>("row33") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows34()
    }
}
internal open class GobanRows35: GobanRows34() {
    override val size: Int get() = 35
    @Volatile private var row34: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update34 = atomicLongUpdater<GobanRows35>("row34") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows35()
    }
}
internal open class GobanRows36: GobanRows35() {
    override val size: Int get() = 36
    @Volatile private var row35: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update35 = atomicLongUpdater<GobanRows36>("row35") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows36()
    }
}
internal open class GobanRows37: GobanRows36() {
    override val size: Int get() = 37
    @Volatile private var row36: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update36 = atomicLongUpdater<GobanRows37>("row36") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows37()
    }
}
internal open class GobanRows38: GobanRows37() {
    override val size: Int get() = 38
    @Volatile private var row37: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update37 = atomicLongUpdater<GobanRows38>("row37") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows38()
    }
}
internal open class GobanRows39: GobanRows38() {
    override val size: Int get() = 39
    @Volatile private var row38: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update38 = atomicLongUpdater<GobanRows39>("row38") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows39()
    }
}
internal open class GobanRows40: GobanRows39() {
    override val size: Int get() = 40
    @Volatile private var row39: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update39 = atomicLongUpdater<GobanRows40>("row39") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows40()
    }
}
internal open class GobanRows41: GobanRows40() {
    override val size: Int get() = 41
    @Volatile private var row40: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update40 = atomicLongUpdater<GobanRows41>("row40") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows41()
    }
}
internal open class GobanRows42: GobanRows41() {
    override val size: Int get() = 42
    @Volatile private var row41: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update41 = atomicLongUpdater<GobanRows42>("row41") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows42()
    }
}
internal open class GobanRows43: GobanRows42() {
    override val size: Int get() = 43
    @Volatile private var row42: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update42 = atomicLongUpdater<GobanRows43>("row42") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows43()
    }
}
internal open class GobanRows44: GobanRows43() {
    override val size: Int get() = 44
    @Volatile private var row43: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update43 = atomicLongUpdater<GobanRows44>("row43") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows44()
    }
}
internal open class GobanRows45: GobanRows44() {
    override val size: Int get() = 45
    @Volatile private var row44: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update44 = atomicLongUpdater<GobanRows45>("row44") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows45()
    }
}
internal open class GobanRows46: GobanRows45() {
    override val size: Int get() = 46
    @Volatile private var row45: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update45 = atomicLongUpdater<GobanRows46>("row45") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows46()
    }
}
internal open class GobanRows47: GobanRows46() {
    override val size: Int get() = 47
    @Volatile private var row46: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update46 = atomicLongUpdater<GobanRows47>("row46") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows47()
    }
}
internal open class GobanRows48: GobanRows47() {
    override val size: Int get() = 48
    @Volatile private var row47: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update47 = atomicLongUpdater<GobanRows48>("row47") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows48()
    }
}
internal open class GobanRows49: GobanRows48() {
    override val size: Int get() = 49
    @Volatile private var row48: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update48 = atomicLongUpdater<GobanRows49>("row48") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows49()
    }
}
internal open class GobanRows50: GobanRows49() {
    override val size: Int get() = 50
    @Volatile private var row49: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update49 = atomicLongUpdater<GobanRows50>("row49") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows50()
    }
}
internal open class GobanRows51: GobanRows50() {
    override val size: Int get() = 51
    @Volatile private var row50: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update50 = atomicLongUpdater<GobanRows51>("row50") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows51()
    }
}
internal open class GobanRows52: GobanRows51() {
    override val size: Int get() = 52
    @Volatile private var row51: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update51 = atomicLongUpdater<GobanRows52>("row51") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows52()
    }
}
internal open class GobanRows54: GobanRows52() {
    override val size: Int get() = 54
    @Volatile private var row52: Long = 0L
    @Volatile private var row53: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update52 = atomicLongUpdater<GobanRows54>("row52") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update53 = atomicLongUpdater<GobanRows54>("row53") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows54()
    }
}
internal open class GobanRows56: GobanRows54() {
    override val size: Int get() = 56
    @Volatile private var row54: Long = 0L
    @Volatile private var row55: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update54 = atomicLongUpdater<GobanRows56>("row54") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update55 = atomicLongUpdater<GobanRows56>("row55") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows56()
    }
}
internal open class GobanRows58: GobanRows56() {
    override val size: Int get() = 58
    @Volatile private var row56: Long = 0L
    @Volatile private var row57: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update56 = atomicLongUpdater<GobanRows58>("row56") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update57 = atomicLongUpdater<GobanRows58>("row57") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows58()
    }
}
internal open class GobanRows60: GobanRows58() {
    override val size: Int get() = 60
    @Volatile private var row58: Long = 0L
    @Volatile private var row59: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update58 = atomicLongUpdater<GobanRows60>("row58") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update59 = atomicLongUpdater<GobanRows60>("row59") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows60()
    }
}
internal open class GobanRows62: GobanRows60() {
    override val size: Int get() = 62
    @Volatile private var row60: Long = 0L
    @Volatile private var row61: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update60 = atomicLongUpdater<GobanRows62>("row60") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update61 = atomicLongUpdater<GobanRows62>("row61") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows62()
    }
}
internal open class GobanRows64: GobanRows62() {
    override val size: Int get() = 64
    @Volatile private var row62: Long = 0L
    @Volatile private var row63: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update62 = atomicLongUpdater<GobanRows64>("row62") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update63 = atomicLongUpdater<GobanRows64>("row63") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows64()
    }
}
internal open class GobanRows66: GobanRows64() {
    override val size: Int get() = 66
    @Volatile private var row64: Long = 0L
    @Volatile private var row65: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update64 = atomicLongUpdater<GobanRows66>("row64") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update65 = atomicLongUpdater<GobanRows66>("row65") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows66()
    }
}
internal open class GobanRows68: GobanRows66() {
    override val size: Int get() = 68
    @Volatile private var row66: Long = 0L
    @Volatile private var row67: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update66 = atomicLongUpdater<GobanRows68>("row66") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update67 = atomicLongUpdater<GobanRows68>("row67") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows68()
    }
}
internal open class GobanRows70: GobanRows68() {
    override val size: Int get() = 70
    @Volatile private var row68: Long = 0L
    @Volatile private var row69: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update68 = atomicLongUpdater<GobanRows70>("row68") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update69 = atomicLongUpdater<GobanRows70>("row69") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows70()
    }
}
internal open class GobanRows72: GobanRows70() {
    override val size: Int get() = 72
    @Volatile private var row70: Long = 0L
    @Volatile private var row71: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update70 = atomicLongUpdater<GobanRows72>("row70") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update71 = atomicLongUpdater<GobanRows72>("row71") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows72()
    }
}
internal open class GobanRows74: GobanRows72() {
    override val size: Int get() = 74
    @Volatile private var row72: Long = 0L
    @Volatile private var row73: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update72 = atomicLongUpdater<GobanRows74>("row72") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update73 = atomicLongUpdater<GobanRows74>("row73") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows74()
    }
}
internal open class GobanRows76: GobanRows74() {
    override val size: Int get() = 76
    @Volatile private var row74: Long = 0L
    @Volatile private var row75: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update74 = atomicLongUpdater<GobanRows76>("row74") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update75 = atomicLongUpdater<GobanRows76>("row75") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows76()
    }
}
internal open class GobanRows78: GobanRows76() {
    override val size: Int get() = 78
    @Volatile private var row76: Long = 0L
    @Volatile private var row77: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update76 = atomicLongUpdater<GobanRows78>("row76") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update77 = atomicLongUpdater<GobanRows78>("row77") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows78()
    }
}
internal open class GobanRows80: GobanRows78() {
    override val size: Int get() = 80
    @Volatile private var row78: Long = 0L
    @Volatile private var row79: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update78 = atomicLongUpdater<GobanRows80>("row78") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update79 = atomicLongUpdater<GobanRows80>("row79") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows80()
    }
}
internal open class GobanRows82: GobanRows80() {
    override val size: Int get() = 82
    @Volatile private var row80: Long = 0L
    @Volatile private var row81: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update80 = atomicLongUpdater<GobanRows82>("row80") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update81 = atomicLongUpdater<GobanRows82>("row81") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows82()
    }
}
internal open class GobanRows84: GobanRows82() {
    override val size: Int get() = 84
    @Volatile private var row82: Long = 0L
    @Volatile private var row83: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update82 = atomicLongUpdater<GobanRows84>("row82") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update83 = atomicLongUpdater<GobanRows84>("row83") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows84()
    }
}
internal open class GobanRows86: GobanRows84() {
    override val size: Int get() = 86
    @Volatile private var row84: Long = 0L
    @Volatile private var row85: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update84 = atomicLongUpdater<GobanRows86>("row84") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update85 = atomicLongUpdater<GobanRows86>("row85") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows86()
    }
}
internal open class GobanRows88: GobanRows86() {
    override val size: Int get() = 88
    @Volatile private var row86: Long = 0L
    @Volatile private var row87: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update86 = atomicLongUpdater<GobanRows88>("row86") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update87 = atomicLongUpdater<GobanRows88>("row87") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows88()
    }
}
internal open class GobanRows90: GobanRows88() {
    override val size: Int get() = 90
    @Volatile private var row88: Long = 0L
    @Volatile private var row89: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update88 = atomicLongUpdater<GobanRows90>("row88") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update89 = atomicLongUpdater<GobanRows90>("row89") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows90()
    }
}
internal open class GobanRows92: GobanRows90() {
    override val size: Int get() = 92
    @Volatile private var row90: Long = 0L
    @Volatile private var row91: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update90 = atomicLongUpdater<GobanRows92>("row90") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update91 = atomicLongUpdater<GobanRows92>("row91") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows92()
    }
}
internal open class GobanRows94: GobanRows92() {
    override val size: Int get() = 94
    @Volatile private var row92: Long = 0L
    @Volatile private var row93: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update92 = atomicLongUpdater<GobanRows94>("row92") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update93 = atomicLongUpdater<GobanRows94>("row93") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows94()
    }
}
internal open class GobanRows96: GobanRows94() {
    override val size: Int get() = 96
    @Volatile private var row94: Long = 0L
    @Volatile private var row95: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update94 = atomicLongUpdater<GobanRows96>("row94") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update95 = atomicLongUpdater<GobanRows96>("row95") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows96()
    }
}
internal open class GobanRows98: GobanRows96() {
    override val size: Int get() = 98
    @Volatile private var row96: Long = 0L
    @Volatile private var row97: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update96 = atomicLongUpdater<GobanRows98>("row96") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update97 = atomicLongUpdater<GobanRows98>("row97") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows98()
    }
}
internal open class GobanRows100: GobanRows98() {
    override val size: Int get() = 100
    @Volatile private var row98: Long = 0L
    @Volatile private var row99: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update98 = atomicLongUpdater<GobanRows100>("row98") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update99 = atomicLongUpdater<GobanRows100>("row99") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows100()
    }
}
internal open class GobanRows102: GobanRows100() {
    override val size: Int get() = 102
    @Volatile private var row100: Long = 0L
    @Volatile private var row101: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update100 = atomicLongUpdater<GobanRows102>("row100") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update101 = atomicLongUpdater<GobanRows102>("row101") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows102()
    }
}
internal class GobanRows104: GobanRows102() {
    override val size: Int get() = 104
    @Volatile private var row102: Long = 0L
    @Volatile private var row103: Long = 0L
    companion object: () -> GobanRows1 {
        @JvmField
        internal val update102 = atomicLongUpdater<GobanRows104>("row102") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update103 = atomicLongUpdater<GobanRows104>("row103") as AtomicLongFieldUpdater<GobanRows1>
        override fun invoke() = GobanRows104()
    }
}
