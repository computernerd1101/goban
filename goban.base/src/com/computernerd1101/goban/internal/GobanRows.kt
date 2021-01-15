@file:Suppress("unused")

package com.computernerd1101.goban.internal

import java.util.concurrent.atomic.AtomicLongFieldUpdater

internal object GobanRows {
    @JvmField val empty = arrayOf(
        GobanRows1(), GobanRows2(),
        GobanRows3(), GobanRows4(),
        GobanRows5(), GobanRows6(),
        GobanRows7(), GobanRows8(),
        GobanRows9(), GobanRows10(),
        GobanRows11(), GobanRows12(),
        GobanRows13(), GobanRows14(),
        GobanRows15(), GobanRows16(),
        GobanRows17(), GobanRows18(),
        GobanRows19(), GobanRows20(),
        GobanRows21(), GobanRows22(),
        GobanRows23(), GobanRows24(),
        GobanRows25(), GobanRows26(),
        GobanRows27(), GobanRows28(),
        GobanRows29(), GobanRows30(),
        GobanRows31(), GobanRows32(),
        GobanRows33(), GobanRows34(),
        GobanRows35(), GobanRows36(),
        GobanRows37(), GobanRows38(),
        GobanRows39(), GobanRows40(),
        GobanRows41(), GobanRows42(),
        GobanRows43(), GobanRows44(),
        GobanRows45(), GobanRows46(),
        GobanRows47(), GobanRows48(),
        GobanRows49(), GobanRows50(),
        GobanRows51(), GobanRows52(),
        GobanRows54(), GobanRows56(),
        GobanRows58(), GobanRows60(),
        GobanRows62(), GobanRows64(),
        GobanRows66(), GobanRows68(),
        GobanRows70(), GobanRows72(),
        GobanRows74(), GobanRows76(),
        GobanRows78(), GobanRows80(),
        GobanRows82(), GobanRows84(),
        GobanRows86(), GobanRows88(),
        GobanRows90(), GobanRows92(),
        GobanRows94(), GobanRows96(),
        GobanRows98(), GobanRows100(),
        GobanRows102(), GobanRows104()
    )
    @JvmField val updaters = CharArray(6).let { buf ->
        buf[0] = 'r'
        buf[1] = 'o'
        buf[2] = 'w'
        // The true type of rowUpdaters
        Array<AtomicLongFieldUpdater<GobanRows1>>(104) { index ->
            var nBuf = 3
            if (index >= 100)
                buf[nBuf++] = '0' + index / 100
            if (index >= 10)
                buf[nBuf++] = '0' + (index / 10) % 10
            buf[nBuf++] = '0' + index % 10
            val classIndex: Int = if (index < 52) index
            else index / 2 + 26
            AtomicLongFieldUpdater.newUpdater(empty[classIndex].javaClass, String(buf, 0, nBuf))
        }
    }
}
internal open class GobanRows1 {
    open fun newInstance() = GobanRows1()
    open val size: Int get() = 1
    @Volatile @JvmField var row0: Long = 0L
    operator fun get(index: Int) = GobanRows.updaters[index][this]
    operator fun set(index: Int, value: Long) {
        GobanRows.updaters[index][this] = value
    }
}
internal open class GobanRows2: GobanRows1() {
    override fun newInstance() = GobanRows2()
    override val size: Int get() = 2
    @Volatile @JvmField var row1: Long = 0L
}
internal open class GobanRows3: GobanRows2() {
    override fun newInstance() = GobanRows3()
    override val size: Int get() = 3
    @Volatile @JvmField var row2: Long = 0L
}
internal open class GobanRows4: GobanRows3() {
    override fun newInstance() = GobanRows4()
    override val size: Int get() = 4
    @Volatile @JvmField var row3: Long = 0L
}
internal open class GobanRows5: GobanRows4() {
    override fun newInstance() = GobanRows5()
    override val size: Int get() = 5
    @Volatile @JvmField var row4: Long = 0L
}
internal open class GobanRows6: GobanRows5() {
    override fun newInstance() = GobanRows6()
    override val size: Int get() = 6
    @Volatile @JvmField var row5: Long = 0L
}
internal open class GobanRows7: GobanRows6() {
    override fun newInstance() = GobanRows7()
    override val size: Int get() = 7
    @Volatile @JvmField var row6: Long = 0L
}
internal open class GobanRows8: GobanRows7() {
    override fun newInstance() = GobanRows8()
    override val size: Int get() = 8
    @Volatile @JvmField var row7: Long = 0L
}
internal open class GobanRows9: GobanRows8() {
    override fun newInstance() = GobanRows9()
    override val size: Int get() = 9
    @Volatile @JvmField var row8: Long = 0L
}
internal open class GobanRows10: GobanRows9() {
    override fun newInstance() = GobanRows10()
    override val size: Int get() = 10
    @Volatile @JvmField var row9: Long = 0L
}
internal open class GobanRows11: GobanRows10() {
    override fun newInstance() = GobanRows11()
    override val size: Int get() = 11
    @Volatile @JvmField var row10: Long = 0L
}
internal open class GobanRows12: GobanRows11() {
    override fun newInstance() = GobanRows12()
    override val size: Int get() = 12
    @Volatile @JvmField var row11: Long = 0L
}
internal open class GobanRows13: GobanRows12() {
    override fun newInstance() = GobanRows13()
    override val size: Int get() = 13
    @Volatile @JvmField var row12: Long = 0L
}
internal open class GobanRows14: GobanRows13() {
    override fun newInstance() = GobanRows14()
    override val size: Int get() = 14
    @Volatile @JvmField var row13: Long = 0L
}
internal open class GobanRows15: GobanRows14() {
    override fun newInstance() = GobanRows15()
    override val size: Int get() = 15
    @Volatile @JvmField var row14: Long = 0L
}
internal open class GobanRows16: GobanRows15() {
    override fun newInstance() = GobanRows16()
    override val size: Int get() = 16
    @Volatile @JvmField var row15: Long = 0L
}
internal open class GobanRows17: GobanRows16() {
    override fun newInstance() = GobanRows17()
    override val size: Int get() = 17
    @Volatile @JvmField var row16: Long = 0L
}
internal open class GobanRows18: GobanRows17() {
    override fun newInstance() = GobanRows18()
    override val size: Int get() = 18
    @Volatile @JvmField var row17: Long = 0L
}
internal open class GobanRows19: GobanRows18() {
    override fun newInstance() = GobanRows19()
    override val size: Int get() = 19
    @Volatile @JvmField var row18: Long = 0L
}
internal open class GobanRows20: GobanRows19() {
    override fun newInstance() = GobanRows20()
    override val size: Int get() = 20
    @Volatile @JvmField var row19: Long = 0L
}
internal open class GobanRows21: GobanRows20() {
    override fun newInstance() = GobanRows21()
    override val size: Int get() = 21
    @Volatile @JvmField var row20: Long = 0L
}
internal open class GobanRows22: GobanRows21() {
    override fun newInstance() = GobanRows22()
    override val size: Int get() = 22
    @Volatile @JvmField var row21: Long = 0L
}
internal open class GobanRows23: GobanRows22() {
    override fun newInstance() = GobanRows23()
    override val size: Int get() = 23
    @Volatile @JvmField var row22: Long = 0L
}
internal open class GobanRows24: GobanRows23() {
    override fun newInstance() = GobanRows24()
    override val size: Int get() = 24
    @Volatile @JvmField var row23: Long = 0L
}
internal open class GobanRows25: GobanRows24() {
    override fun newInstance() = GobanRows25()
    override val size: Int get() = 25
    @Volatile @JvmField var row24: Long = 0L
}
internal open class GobanRows26: GobanRows25() {
    override fun newInstance() = GobanRows26()
    override val size: Int get() = 26
    @Volatile @JvmField var row25: Long = 0L
}
internal open class GobanRows27: GobanRows26() {
    override fun newInstance() = GobanRows27()
    override val size: Int get() = 27
    @Volatile @JvmField var row26: Long = 0L
}
internal open class GobanRows28: GobanRows27() {
    override fun newInstance() = GobanRows28()
    override val size: Int get() = 28
    @Volatile @JvmField var row27: Long = 0L
}
internal open class GobanRows29: GobanRows28() {
    override fun newInstance() = GobanRows29()
    override val size: Int get() = 29
    @Volatile @JvmField var row28: Long = 0L
}
internal open class GobanRows30: GobanRows29() {
    override fun newInstance() = GobanRows30()
    override val size: Int get() = 30
    @Volatile @JvmField var row29: Long = 0L
}
internal open class GobanRows31: GobanRows30() {
    override fun newInstance() = GobanRows31()
    override val size: Int get() = 31
    @Volatile @JvmField var row30: Long = 0L
}
internal open class GobanRows32: GobanRows31() {
    override fun newInstance() = GobanRows32()
    override val size: Int get() = 32
    @Volatile @JvmField var row31: Long = 0L
}
internal open class GobanRows33: GobanRows32() {
    override fun newInstance() = GobanRows33()
    override val size: Int get() = 33
    @Volatile @JvmField var row32: Long = 0L
}
internal open class GobanRows34: GobanRows33() {
    override fun newInstance() = GobanRows34()
    override val size: Int get() = 34
    @Volatile @JvmField var row33: Long = 0L
}
internal open class GobanRows35: GobanRows34() {
    override fun newInstance() = GobanRows35()
    override val size: Int get() = 35
    @Volatile @JvmField var row34: Long = 0L
}
internal open class GobanRows36: GobanRows35() {
    override fun newInstance() = GobanRows36()
    override val size: Int get() = 36
    @Volatile @JvmField var row35: Long = 0L
}
internal open class GobanRows37: GobanRows36() {
    override fun newInstance() = GobanRows37()
    override val size: Int get() = 37
    @Volatile @JvmField var row36: Long = 0L
}
internal open class GobanRows38: GobanRows37() {
    override fun newInstance() = GobanRows38()
    override val size: Int get() = 38
    @Volatile @JvmField var row37: Long = 0L
}
internal open class GobanRows39: GobanRows38() {
    override fun newInstance() = GobanRows39()
    override val size: Int get() = 39
    @Volatile @JvmField var row38: Long = 0L
}
internal open class GobanRows40: GobanRows39() {
    override fun newInstance() = GobanRows40()
    override val size: Int get() = 40
    @Volatile @JvmField var row39: Long = 0L
}
internal open class GobanRows41: GobanRows40() {
    override fun newInstance() = GobanRows41()
    override val size: Int get() = 41
    @Volatile @JvmField var row40: Long = 0L
}
internal open class GobanRows42: GobanRows41() {
    override fun newInstance() = GobanRows42()
    override val size: Int get() = 42
    @Volatile @JvmField var row41: Long = 0L
}
internal open class GobanRows43: GobanRows42() {
    override fun newInstance() = GobanRows43()
    override val size: Int get() = 43
    @Volatile @JvmField var row42: Long = 0L
}
internal open class GobanRows44: GobanRows43() {
    override fun newInstance() = GobanRows44()
    override val size: Int get() = 44
    @Volatile @JvmField var row43: Long = 0L
}
internal open class GobanRows45: GobanRows44() {
    override fun newInstance() = GobanRows45()
    override val size: Int get() = 45
    @Volatile @JvmField var row44: Long = 0L
}
internal open class GobanRows46: GobanRows45() {
    override fun newInstance() = GobanRows46()
    override val size: Int get() = 46
    @Volatile @JvmField var row45: Long = 0L
}
internal open class GobanRows47: GobanRows46() {
    override fun newInstance() = GobanRows47()
    override val size: Int get() = 47
    @Volatile @JvmField var row46: Long = 0L
}
internal open class GobanRows48: GobanRows47() {
    override fun newInstance() = GobanRows48()
    override val size: Int get() = 48
    @Volatile @JvmField var row47: Long = 0L
}
internal open class GobanRows49: GobanRows48() {
    override fun newInstance() = GobanRows49()
    override val size: Int get() = 49
    @Volatile @JvmField var row48: Long = 0L
}
internal open class GobanRows50: GobanRows49() {
    override fun newInstance() = GobanRows50()
    override val size: Int get() = 50
    @Volatile @JvmField var row49: Long = 0L
}
internal open class GobanRows51: GobanRows50() {
    override fun newInstance() = GobanRows51()
    override val size: Int get() = 51
    @Volatile @JvmField var row50: Long = 0L
}
internal open class GobanRows52: GobanRows51() {
    override fun newInstance() = GobanRows52()
    override val size: Int get() = 52
    @Volatile @JvmField var row51: Long = 0L
}
internal open class GobanRows54: GobanRows52() {
    override fun newInstance() = GobanRows54()
    override val size: Int get() = 54
    @Volatile @JvmField var row52: Long = 0L
    @Volatile @JvmField var row53: Long = 0L
}
internal open class GobanRows56: GobanRows54() {
    override fun newInstance() = GobanRows56()
    override val size: Int get() = 56
    @Volatile @JvmField var row54: Long = 0L
    @Volatile @JvmField var row55: Long = 0L
}
internal open class GobanRows58: GobanRows56() {
    override fun newInstance() = GobanRows58()
    override val size: Int get() = 58
    @Volatile @JvmField var row56: Long = 0L
    @Volatile @JvmField var row57: Long = 0L
}
internal open class GobanRows60: GobanRows58() {
    override fun newInstance() = GobanRows60()
    override val size: Int get() = 60
    @Volatile @JvmField var row58: Long = 0L
    @Volatile @JvmField var row59: Long = 0L
}
internal open class GobanRows62: GobanRows60() {
    override fun newInstance() = GobanRows62()
    override val size: Int get() = 62
    @Volatile @JvmField var row60: Long = 0L
    @Volatile @JvmField var row61: Long = 0L
}
internal open class GobanRows64: GobanRows62() {
    override fun newInstance() = GobanRows64()
    override val size: Int get() = 64
    @Volatile @JvmField var row62: Long = 0L
    @Volatile @JvmField var row63: Long = 0L
}
internal open class GobanRows66: GobanRows64() {
    override fun newInstance() = GobanRows66()
    override val size: Int get() = 66
    @Volatile @JvmField var row64: Long = 0L
    @Volatile @JvmField var row65: Long = 0L
}
internal open class GobanRows68: GobanRows66() {
    override fun newInstance() = GobanRows68()
    override val size: Int get() = 68
    @Volatile @JvmField var row66: Long = 0L
    @Volatile @JvmField var row67: Long = 0L
}
internal open class GobanRows70: GobanRows68() {
    override fun newInstance() = GobanRows70()
    override val size: Int get() = 70
    @Volatile @JvmField var row68: Long = 0L
    @Volatile @JvmField var row69: Long = 0L
}
internal open class GobanRows72: GobanRows70() {
    override fun newInstance() = GobanRows72()
    override val size: Int get() = 72
    @Volatile @JvmField var row70: Long = 0L
    @Volatile @JvmField var row71: Long = 0L
}
internal open class GobanRows74: GobanRows72() {
    override fun newInstance() = GobanRows74()
    override val size: Int get() = 74
    @Volatile @JvmField var row72: Long = 0L
    @Volatile @JvmField var row73: Long = 0L
}
internal open class GobanRows76: GobanRows74() {
    override fun newInstance() = GobanRows76()
    override val size: Int get() = 76
    @Volatile @JvmField var row74: Long = 0L
    @Volatile @JvmField var row75: Long = 0L
}
internal open class GobanRows78: GobanRows76() {
    override fun newInstance() = GobanRows78()
    override val size: Int get() = 78
    @Volatile @JvmField var row76: Long = 0L
    @Volatile @JvmField var row77: Long = 0L
}
internal open class GobanRows80: GobanRows78() {
    override fun newInstance() = GobanRows80()
    override val size: Int get() = 80
    @Volatile @JvmField var row78: Long = 0L
    @Volatile @JvmField var row79: Long = 0L
}
internal open class GobanRows82: GobanRows80() {
    override fun newInstance() = GobanRows82()
    override val size: Int get() = 82
    @Volatile @JvmField var row80: Long = 0L
    @Volatile @JvmField var row81: Long = 0L
}
internal open class GobanRows84: GobanRows82() {
    override fun newInstance() = GobanRows84()
    override val size: Int get() = 84
    @Volatile @JvmField var row82: Long = 0L
    @Volatile @JvmField var row83: Long = 0L
}
internal open class GobanRows86: GobanRows84() {
    override fun newInstance() = GobanRows86()
    override val size: Int get() = 86
    @Volatile @JvmField var row84: Long = 0L
    @Volatile @JvmField var row85: Long = 0L
}
internal open class GobanRows88: GobanRows86() {
    override fun newInstance() = GobanRows88()
    override val size: Int get() = 88
    @Volatile @JvmField var row86: Long = 0L
    @Volatile @JvmField var row87: Long = 0L
}
internal open class GobanRows90: GobanRows88() {
    override fun newInstance() = GobanRows90()
    override val size: Int get() = 90
    @Volatile @JvmField var row88: Long = 0L
    @Volatile @JvmField var row89: Long = 0L
}
internal open class GobanRows92: GobanRows90() {
    override fun newInstance() = GobanRows92()
    override val size: Int get() = 92
    @Volatile @JvmField var row90: Long = 0L
    @Volatile @JvmField var row91: Long = 0L
}
internal open class GobanRows94: GobanRows92() {
    override fun newInstance() = GobanRows94()
    override val size: Int get() = 94
    @Volatile @JvmField var row92: Long = 0L
    @Volatile @JvmField var row93: Long = 0L
}
internal open class GobanRows96: GobanRows94() {
    override fun newInstance() = GobanRows96()
    override val size: Int get() = 96
    @Volatile @JvmField var row94: Long = 0L
    @Volatile @JvmField var row95: Long = 0L
}
internal open class GobanRows98: GobanRows96() {
    override fun newInstance() = GobanRows98()
    override val size: Int get() = 98
    @Volatile @JvmField var row96: Long = 0L
    @Volatile @JvmField var row97: Long = 0L
}
internal open class GobanRows100: GobanRows98() {
    override fun newInstance() = GobanRows100()
    override val size: Int get() = 100
    @Volatile @JvmField var row98: Long = 0L
    @Volatile @JvmField var row99: Long = 0L
}
internal open class GobanRows102: GobanRows100() {
    override fun newInstance() = GobanRows102()
    override val size: Int get() = 102
    @Volatile @JvmField var row100: Long = 0L
    @Volatile @JvmField var row101: Long = 0L
}
internal class GobanRows104: GobanRows102() {
    override fun newInstance() = GobanRows104()
    override val size: Int get() = 104
    @Volatile @JvmField var row102: Long = 0L
    @Volatile @JvmField var row103: Long = 0L
}
