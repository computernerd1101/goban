@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.internal.*
import com.computernerd1101.goban.sgf.internal.DateTable12.Companion.strongUpdater
import com.computernerd1101.goban.sgf.internal.DateTable12.Companion.weakUpdater
import java.io.InvalidObjectException
import java.io.*
import java.lang.ref.*
import java.util.*
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.contracts.*

class Date private constructor(private val value: Int): Comparable<Date>, Serializable {

    internal constructor(value: Int, marker: InternalMarker): this(value) {
        marker.ignore()
    }

    constructor(year: Int, month: Int, day: Int): this(
        (when {
            year < MIN_YEAR -> MIN_YEAR
            year > MAX_YEAR -> MAX_YEAR
            else -> year
        } shl 9) or when {
            month < 0 || month > 12 -> 0
            else -> month
        }.let { m -> (m shl 5) or when {
                day < 0 || day > Private.daysInMonth[m] -> 0
                else -> day
            }
        }
    )

    constructor(cal: GregorianCalendar): this(
        cal.get(Calendar.YEAR) * when(cal.get(Calendar.ERA)) {
            GregorianCalendar.BC -> -1
            else -> 1
        },
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    @Suppress("unused")
    constructor(): this(GregorianCalendar())
    @Suppress("unused")
    constructor(date: java.util.Date): this(GregorianCalendar().apply { time = date })

    private object Private {

        @JvmField val daysInMonth = intArrayOf(
            0,
            31,  // January
            29,  // February (simplify Leap Day by pretending it happens every year)
            31,  // March
            30,  // April
            31,  // May
            30,  // June
            31,  // July
            31,  // August
            30,  // September
            31,  // October
            30,  // November
            31   // December
        )

    }

    companion object {

        const val MIN_YEAR = -1 shl 22
        const val MAX_YEAR = (1 shl 22) - 1

        @JvmStatic
        fun daysInMonth(month: Int): Int = if (month in 1..12) Private.daysInMonth[month] else 0

        @JvmStatic
        fun parse(s: String): Date? = InternalDate.parse(null, s)

        private const val serialVersionUID = 1L

    }

    val year: Int @JvmName("year") get() = value shr 9
    val month: Int @JvmName("month") get() = (value ushr 5) and 0xF
    val day: Int @JvmName("day") get() = value and 0x1F

    operator fun component1() = year
    operator fun component2() = month
    operator fun component3() = day

    override fun compareTo(other: Date): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Date && value == other.value)
    }

    override fun hashCode() = value

    override fun toString(): String {
        var year: Int = this.year
        val yPad: String = when {
            year <= -1000 -> {
                year = -year
                "-"
            }
            year <= -100 -> {
                year = -year
                "-0"
            }
            year <= -10 -> {
                year = -year
                "-00"
            }
            year < 0 -> {
                year = -year
                "-000"
            }
            year > 1000 -> ""
            year > 100 -> "0"
            year > 10 -> "00"
            else -> "000"
        }
        val month: Int = this.month
        return if (month == 0)
            "$yPad$year"
        else {
            val mPad = if (month < 10) "-0" else "-"
            val day: Int = this.day
            if (day == 0) "$yPad$year$mPad$month"
            else "$yPad$year$mPad$month${if (day < 10) "-0" else "-"}$day"
        }
    }

    @Suppress("unused")
    fun parseNext(s: String): Date {
        return InternalDate.parse(this, s) ?: this
    }

    private fun readResolve(): Any {
        val month = this.month
        val day = this.day
        return when {
            month == 0 && day == 0 -> this
            month <= 0 || month > 12 -> Date(year, 0, 0)
            day < 0 || day > Private.daysInMonth[month] -> Date(year, month, 0)
            else -> this
        }
    }

}

fun Date?.parseNext(s: String): Date? {
    return InternalDate.parse(this, s) ?: this
}

@Suppress("unused")
infix fun DateSet?.contentEquals(other: DateSet?) = DateSet.contentEquals(this, other)
@Suppress("unused")
infix fun DateSet?.contentNotEqual(other: DateSet?) = !DateSet.contentEquals(this, other)
@Suppress("unused")
fun DateSet?.contentHashCode() = this?.contentHashCode() ?: 0

@OptIn(ExperimentalContracts::class)
@Suppress("unused")
fun DateSet?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || isEmpty()
}

class DateSet(): MutableIterable<Date>, Serializable {

    fun isEmpty() = count == 0

    fun isNotEmpty() = !isEmpty()

    @Volatile
    private var _count: Int = 0

    val count: Int
        @JvmName("count")
        get() {
            expungeStaleReferences()
            return _count
        }

    private var queue: ReferenceQueue<*> = ReferenceQueue<Any>()
    private var table2d = DateTable128<Table2d>(queue)

    constructor(string: String): this() {
        val split = string.split(Private.SPLIT_COMMA).toTypedArray()
        var date: Date? = null
        for (s in split) {
            date = date.parseNext(s)
            if (date != null) addDate(date)
        }
    }

    fun copy(): DateSet {
        val copy = DateSet()
        val copyTable = copy.table2d
        for(i in 0..0x7F) {
            val strongUpdater = strongUpdater<Table2d>(i)
            strongUpdater[table2d]?.let {
                val t = copy.Table2d(copy.queue, it)
                strongUpdater[copyTable] = t
                weakUpdater<Table2d>(i)[copyTable] = WeakDateTable(t, copyTable, i)
            }
        }
        return copy
    }

    private fun expungeStaleReferences() {
        var r = queue.poll()
        while(r != null) {
            if (r is WeakDateTable<*>) r.expunge()
            r = queue.poll()
        }
    }

    override fun toString(): String {
        expungeStaleReferences()
        return buildString {
            var hasPrev = false
            for(i in 0..0x7F) {
                strongUpdater<Table2d>(i)[table2d]?.let { t ->
                    hasPrev = t.writeString(this, hasPrev)
                }
            }
        }
    }

    infix fun contentEquals(other: DateSet): Boolean {
        val count = this.count
        if (other.count != count) return false
        for(i in 0..0x7F) {
            val strongUpdater = strongUpdater<Table2d>(i)
            if (Private.contentNotEqual(strongUpdater[table2d], strongUpdater[other.table2d])) return false
        }
        return true
    }

    @Suppress("unused")
    infix fun contentNotEqual(other: DateSet) = !contentEquals(other)

    fun contentHashCode(): Int {
        var totalHash = 0
        for(i2d in 0..0x7F) strongUpdater<Table2d>(i2d)[table2d]?.let { t2d ->
            for(i1d in 0..0xFF) strongUpdater<Table2d.Table1d>(i1d)[t2d]?.let { t1d ->
                for(i in 0..0xFF) strongUpdater<Table2d.Table1d.YearTable>(i)[t1d]?.let { yt ->
                    var hash = yt.year.year
                    for(m in 0..11) {
                        hash *= 31
                        strongUpdater<Table2d.Table1d.YearTable.MonthTable>(m)[yt]?.let { mt ->
                            if (mt.day0 != null) hash++
                            else {
                                var bit = 2
                                for (d in 1..mt.lastDay) {
                                    if (Private.updateDays[d][mt] != null) hash += bit
                                    bit = bit shl 1
                                }
                            }
                        }
                    }
                    totalHash += hash
                }
            }
        }
        return totalHash
    }

    fun addDate(date: Date): Boolean {
        return try {
            val i = (date.year + YEAR_OFFSET) shr 16
            val strongUpdater = strongUpdater<Table2d>(i)
            var t = strongUpdater[table2d]
            if (t == null) {
                val weakUpdater = weakUpdater<Table2d>(i)
                val weak = weakUpdater[table2d]
                if (weak != null) t = weak.get()
                val updateWeak = if (t == null) {
                    t = Table2d(queue, i)
                    true
                } else false
                val tmp = strongUpdater.compareAndExchange(table2d, null, t)
                if (tmp != null) t = tmp
                if (updateWeak)
                    weakUpdater.compareAndSet(table2d, weak, WeakDateTable(t, table2d, i))
            }
            t.addDate(date)
        } finally {
            expungeStaleReferences()
        }
    }

    operator fun contains(date: Date): Boolean {
        expungeStaleReferences()
        return strongUpdater<Table2d>(
            (date.year + YEAR_OFFSET) shr 16
        )[table2d]?.contains(date, false) == true
    }

    @Suppress("unused")
    fun containsExact(date: Date): Boolean {
        expungeStaleReferences()
        return strongUpdater<Table2d>(
            (date.year + YEAR_OFFSET) shr 16
        )[table2d]?.contains(date, true) == true
    }

    fun remove(date: Date): Boolean {
        expungeStaleReferences()
        return strongUpdater<Table2d>(
                (date.year + YEAR_OFFSET) shr 16
        )[table2d]?.remove(date) == true
    }

    override fun iterator(): MutableIterator<Date> {
        return MetaItr(table2d)
    }

    companion object {

        private const val WRITE_START = 0
        private const val WRITE_YEAR = 1
        private const val WRITE_MONTH = 2
        private const val WRITE_DAY = 3

        private const val YEAR_OFFSET = -Date.MIN_YEAR

        init {
            Private.updateCount = atomicIntUpdater("_count")
        }

        @JvmStatic
        fun contentEquals(a: DateSet?, b: DateSet?) = when {
            a == null -> b == null
            b == null -> false
            else -> a contentEquals b
        }

        private const val serialVersionUID = 1L

    }

    private object Private {

        fun contentNotEqual(a: Table2d?, b: Table2d?) = when {
            a == null -> b != null
            b == null -> true
            else -> !a.contentEquals(b)
        }

        val SPLIT_COMMA = ",".toRegex()

        /** Updates [DateSet._count] */
        lateinit var updateCount: AtomicIntegerFieldUpdater<DateSet>
        /** Updates [DateSet.Table2d.count1d] */
        @JvmField val updateCount1d = atomicIntUpdater<Table2d>("count1d")
        /** Updates [DateSet.Table2d.Table1d.yearCount] */
        @JvmField val updateYearCount =
            atomicIntUpdater<Table2d.Table1d>("yearCount")
        /** Updates [DateSet.Table2d.Table1d.YearTable.monthCount] */
        @JvmField val updateMonthCount =
            atomicIntUpdater<Table2d.Table1d.YearTable>("monthCount")
        /** Updates [DateSet.Table2d.Table1d.YearTable.MonthTable.dayCount] */
        @JvmField val updateDayCount =
            atomicIntUpdater<Table2d.Table1d.YearTable.MonthTable>("dayCount")
        @JvmField val updateDays = CharArray(5).let { buf ->
            buf[0] = 'd'
            buf[1] = 'a'
            buf[2] = 'y'
            // The true type of updateDays
            Array<AtomicReferenceFieldUpdater<Table2d.Table1d.YearTable.MonthTable, Date>>(32) { index ->
                val nBuf = if (index < 10) {
                    buf[3] = '0' + index
                    4
                } else {
                    buf[3] = '0' + index / 10
                    buf[4] = '0' + index % 10
                    5
                }
                @Suppress("UNCHECKED_CAST")
                val type = when(index) {
                    30 -> Table2d.Table1d.YearTable.MonthTable30::class.java
                    31 -> Table2d.Table1d.YearTable.MonthTable31::class.java
                    else -> Table2d.Table1d.YearTable.MonthTable::class.java
                } as Class<Table2d.Table1d.YearTable.MonthTable>
                AtomicReferenceFieldUpdater.newUpdater(type, Date::class.java, String(buf, 0, nBuf))
            }
        }

        @JvmField val monthTableFactories = arrayOf(
            MonthTableFactory.THIRTY_ONE, // January
            MonthTableFactory.FEBRUARY,   // Exactly what it says on the tin
            MonthTableFactory.THIRTY_ONE, // March
            MonthTableFactory.THIRTY,     // April
            MonthTableFactory.THIRTY_ONE, // May
            MonthTableFactory.THIRTY,     // June
            MonthTableFactory.THIRTY_ONE, // July
            MonthTableFactory.THIRTY_ONE, // August
            MonthTableFactory.THIRTY,     // September
            MonthTableFactory.THIRTY_ONE, // October
            MonthTableFactory.THIRTY,     // November
            MonthTableFactory.THIRTY_ONE  // December
        )

    }

    private enum class MonthTableFactory: (Table2d.Table1d.YearTable, Date) -> Table2d.Table1d.YearTable.MonthTable {

        FEBRUARY {
            override fun invoke(yt: Table2d.Table1d.YearTable, month: Date) = yt.MonthTable(month)
        },
        THIRTY {
            override fun invoke(yt: Table2d.Table1d.YearTable, month: Date) = yt.MonthTable30(month)
        },
        THIRTY_ONE {
            override fun invoke(yt: Table2d.Table1d.YearTable, month: Date) = yt.MonthTable31(month)
        }

    }

    private class MetaItr<T : MutableIterable<Date>>(val array: DateTable12<T>): MutableIterator<Date> {

        var index: Int = 0
        var itr: MutableIterator<Date>? = null

        override fun hasNext(): Boolean {
            if (itr?.hasNext() == true) return true
            for(i in index until array.size) {
                strongUpdater<T>(i)[array]?.let { t: T ->
                    val itr = t.iterator()
                    if (itr.hasNext()) {
                        this.itr = itr
                        index = i + 1
                        return true
                    }
                }
            }
            return false
        }

        override fun next(): Date {
            val itr1 = itr
            if (itr1 != null && itr1.hasNext())
                return itr1.next()
            for(i in index until array.size) {
                strongUpdater<T>(i)[array]?.let { t: T ->
                    val itr2 = t.iterator()
                    if (itr2.hasNext()) {
                        itr = itr2
                        index = i + 1
                        return itr2.next()
                    }
                }
            }
            throw NoSuchElementException()
        }

        override fun remove() {
            itr?.remove() ?: throw IllegalStateException()
        }

    }

    private inner class Table2d(queue: ReferenceQueue<*>, val index2d: Int):
        DateTable256<Table2d.Table1d>(queue), MutableIterable<Date> {

        @Volatile @JvmField var count1d: Int = 0

        constructor(queue: ReferenceQueue<*>, other: Table2d): this(queue, other.index2d) {
            for(i in 0..0xFF) {
                val strongUpdater = strongUpdater<Table1d>(i)
                strongUpdater[other]?.let {
                    Private.updateCount1d.incrementAndGet(this)
                    val t = Table1d(it)
                    strongUpdater[this] = t
                    weakUpdater<Table1d>(i)[this] = WeakDateTable(t, this, i)
                }
            }
        }

        fun addDate(date: Date): Boolean {
            val i = (date.year shr 8) and 0xFF
            val strongUpdater = strongUpdater<Table1d>(i)
            val weakUpdater = weakUpdater<Table1d>(i)
            var t = strongUpdater[this]
            if (t == null) {
                val weak = weakUpdater[this]
                if (weak != null) t = weak.get()
                val updateWeak = if (t == null) {
                    t = Table1d(i)
                    true
                } else false
                if (strongUpdater.compareAndSet(this, null, t)) {
                    Private.updateCount1d.incrementAndGet(this)
                }
                val tmp = strongUpdater.compareAndExchange(this, null, t)
                if (tmp == null) Private.updateCount1d.incrementAndGet(this)
                else t = tmp
                if (updateWeak)
                    weakUpdater.compareAndSet(this, weak, WeakDateTable(t, this, i))
            }
            return t.addDate(date)
        }

        fun contains(date: Date, exact: Boolean): Boolean {
            return strongUpdater<Table1d>(
                (date.year shr 8) and 0xFF
            )[this]?.contains(date, exact) == true
        }

        fun remove(date: Date): Boolean {
            return strongUpdater<Table1d>(
                (date.year shr 8) and 0xFF
            )[this]?.remove(date) == true
        }

        fun remove2d() {
            strongUpdater<Table2d>(index2d).compareAndSet(table2d, this, null)
        }

        fun writeString(sb: StringBuilder, hasPrev: Boolean): Boolean {
            var prev = hasPrev
            for(i in 0..0xFF)
                strongUpdater<Table1d>(i)[this]?.let { t ->
                    prev = t.writeString(sb, prev)
                }
            return prev
        }

        override fun iterator(): MutableIterator<Date> = MetaItr(this)

        fun contentEquals(other: Table2d): Boolean {
            val count = count1d
            if (other.count1d != count) return false
            for(i in 0..0xFF) {
                val strongUpdater = strongUpdater<Table1d>(i)
                if (strongUpdater[this] contentNotEqual strongUpdater[other]) return false
            }
            return true
        }

        infix fun Table1d?.contentNotEqual(other: Table1d?) = when {
            this == null -> other != null
            other == null -> true
            else -> !contentEquals(other)
        }

        inner class Table1d(val index1d: Int): DateTable256<Table1d.YearTable>(queue), MutableIterable<Date> {

            @Volatile @JvmField var yearCount: Int = 0

            constructor(other: Table1d): this(other.index1d) {
                for(i in 0..0xFF) {
                    val strongUpdater = strongUpdater<YearTable>(i)
                    strongUpdater[other]?.let {
                        Private.updateYearCount.incrementAndGet(this)
                        val yt = YearTable(it)
                        strongUpdater[this] = yt
                        weakUpdater<YearTable>(i)[this] = WeakDateTable(yt, this, i)
                    }
                }
            }

            fun addDate(date: Date): Boolean {
                val i = date.year and 0xFF
                val strongUpdater = strongUpdater<YearTable>(i)
                var yt = strongUpdater[this]
                if (yt == null) {
                    val weakUpdater = weakUpdater<YearTable>(i)
                    val weak = weakUpdater[this]
                    if (weak != null) yt = weak.get()
                    val updateWeak = if (yt == null) {
                        yt = YearTable(date)
                        true
                    } else false
                    val tmp = strongUpdater.compareAndExchange(this, null, yt)
                    if (tmp == null) Private.updateYearCount.incrementAndGet(this)
                    else yt = tmp
                    if (updateWeak)
                        weakUpdater.compareAndSet(this, weak, WeakDateTable(yt, this, i))
                    if (tmp == null && date.month == 0)
                        return true
                }
                return yt.addDate(date)
            }

            fun contains(date: Date, exact: Boolean): Boolean {
                return strongUpdater<YearTable>(
                    date.year and 0xFF
                )[this]?.contains(date, exact) == true
            }

            fun remove(date: Date): Boolean {
                return strongUpdater<YearTable>(
                    date.year and 0xFF
                )[this]?.remove(date) == true
            }

            fun remove1d() {
                if (strongUpdater<Table1d>(index1d).compareAndSet(this@Table2d, this, null) &&
                        Private.updateCount1d.decrementAndGet(this@Table2d) == 0)
                    remove2d()
            }

            fun writeString(sb: StringBuilder, hasPrev: Boolean): Boolean {
                var prev = hasPrev
                for(i in 0..0xFF)
                    strongUpdater<YearTable>(i)[this]?.let { yt ->
                        prev = yt.writeString(sb, prev)
                    }
                return prev
            }

            override fun iterator(): MutableIterator<Date> = MetaItr(this)

            fun contentEquals(other: Table1d): Boolean {
                val count = yearCount
                if (other.yearCount != count) return false
                for(i in 0..0xFF) {
                    val strongUpdater = strongUpdater<YearTable>(i)
                    if (strongUpdater[this] contentNotEqual strongUpdater[other]) return false
                }
                return true
            }

            private infix fun YearTable?.contentNotEqual(other: YearTable?) = when {
                this == null -> other != null
                other == null -> true
                else -> !contentEquals(other)
            }

            inner class YearTable: DateTable12<YearTable.MonthTable>, MutableIterable<Date> {

                val year: Date
                @Volatile @JvmField var monthCount: Int = 0

                constructor(year: Int): super(this@Table1d.queue) {
                    this.year = Date(year, 0, 0)
                }

                constructor(date: Date): super(this@Table1d.queue) {
                    val m = date.month
                    if (m == 0) {
                        year = date
                        monthCount = -1
                        Private.updateCount.incrementAndGet(this@DateSet)
                    } else {
                        year = Date(date.hashCode() and -0x200, InternalMarker)
                    }
                }

                constructor(other: YearTable): super(this@Table1d.queue) {
                    year = other.year
                    if (other.monthCount == -1) {
                        monthCount = -1
                        Private.updateCount.incrementAndGet(this@DateSet)
                    } else
                        for(i in 0..11) {
                            Private.updateMonthCount.incrementAndGet(this)
                            val strongUpdater = strongUpdater<MonthTable>(i)
                            strongUpdater[other]?.let {
                                val mt = it.copy(this)
                                strongUpdater[this] = mt
                                weakUpdater<MonthTable>(i)[this] = WeakDateTable(mt, this, i)
                            }
                        }
                }

                fun addDate(date: Date): Boolean {
                    val m = date.month
                    if (m == 0) return when(Private.updateMonthCount.getAndSet(this, -1)) {
                        -1 -> false
                        0 -> {
                            Private.updateCount.incrementAndGet(this@DateSet)
                            true
                        }
                        else -> {
                            var delta = 1
                            for (i in 0..11)
                                strongUpdater<MonthTable>(i).getAndSet(this, null)?.let {
                                    delta -= it.dayCount
                                }
                            if (delta != 0) Private.updateCount.addAndGet(this@DateSet, delta)
                            true
                        }
                    }
                    val strongUpdater = strongUpdater<MonthTable>(m - 1)
                    var modified = false
                    var mt = strongUpdater[this]
                    if (mt == null) {
                        val weakUpdater = weakUpdater<MonthTable>(m - 1)
                        val weak = weakUpdater[this]
                        if (weak != null) mt = weak.get()
                        val updateWeak = if (mt == null) {
                            mt = Private.monthTableFactories[m - 1](this, date)
                            true
                        } else false
                        val tmp = strongUpdater.compareAndExchange(this, null, mt)
                        if (tmp != null) mt = tmp
                        else {
                            if (!Private.updateMonthCount.compareAndSet(this, -1, 1))
                                Private.updateMonthCount.incrementAndGet(this)
                            if (updateWeak)
                                weakUpdater.compareAndSet(this, weak, WeakDateTable(mt, this, m - 1))
                            modified = true
                        }
                    }
                    if (mt.addDate(date)) modified = true
                    return modified
                }

                fun contains(date: Date, exact: Boolean): Boolean {
                    val m = date.month
                    if (m == 0) {
                        val n = monthCount
                        return if (exact) n == -1 else n != 0
                    }
                    return (!exact && monthCount == -1) ||
                            strongUpdater<MonthTable>(m - 1)[this]?.contains(date, exact) == true
                }

                fun remove(date: Date): Boolean {
                    val m = date.month
                    return if (m == 0) {
                        var n = 0
                        for(i in 0..11)
                            strongUpdater<MonthTable>(i).getAndSet(this, null)?.apply {
                                n += dayCount
                            }
                        removeYear()
                        Private.updateMonthCount.getAndSet(this, 0) == -1 || n > 0
                    } else strongUpdater<MonthTable>(m - 1)[this]?.remove(date) == true
                }

                fun removeYear() {
                    if (strongUpdater<YearTable>(year.year and 0xFF)
                            .compareAndSet(this@Table1d, this, null) &&
                        Private.updateYearCount.decrementAndGet(this@Table1d) == 0)
                        remove1d()
                }

                fun writeString(sb: StringBuilder, hasPrev: Boolean): Boolean {
                    when(monthCount) {
                        -1 -> {
                            if (hasPrev) sb.append(',')
                            writeYear(sb, year.year)
                            return true
                        }
                        0 -> return hasPrev
                    }
                    var progress = if (hasPrev) WRITE_YEAR else WRITE_START
                    for(i in 0..11)
                        strongUpdater<MonthTable>(i)[this]?.let { mt ->
                            progress = mt.writeString(sb, progress)
                        }
                    return progress > WRITE_START
                }

                fun writeYear(sb: StringBuilder, year: Int) {
                    if (year !in -999..999) {
                        sb.append(year)
                        return
                    }
                    val abs = if (year < 0) {
                        sb.append('-')
                        -year
                    } else year
                    if (abs < 10) sb.append('0')
                    if (abs < 100) sb.append('0')
                    sb.append('0')
                    sb.append(abs)
                }

                override fun iterator(): MutableIterator<Date> {
                    val count = monthCount
                    return if (count > 0) MetaItr(this)
                    else object : MutableIterator<Date> {

                        var stage = if (count == -1) 0 else 2

                        override fun hasNext() = stage == 0

                        override fun next(): Date {
                            if (stage != 0) throw NoSuchElementException()
                            stage = 1
                            return year
                        }

                        override fun remove() {
                            if (stage != 1) throw IllegalStateException()
                            stage = 2
                            if (!Private.updateMonthCount.compareAndSet(this@YearTable, -1, 0))
                                throw ConcurrentModificationException()
                            removeYear()
                        }

                    }
                }

                fun contentEquals(other: YearTable): Boolean {
                    val count = monthCount
                    if (other.monthCount != count) return false
                    for(i in 0..11) {
                        val strongUpdater = strongUpdater<MonthTable>(i)
                        if (strongUpdater[this] contentNotEqual strongUpdater[other]) return false
                    }
                    return true
                }

                private infix fun MonthTable?.contentNotEqual(other: MonthTable?) = when {
                    this == null -> other != null
                    other == null -> true
                    else -> !contentEquals(other)
                }

                open inner class MonthTable: MutableIterable<Date> {

                    val month: Date
                    @Volatile @JvmField var dayCount: Int = 0
                    @Volatile @JvmField var day0: Date?
                    @Volatile @JvmField var day1: Date?
                    @Volatile @JvmField var day2: Date?
                    @Volatile @JvmField var day3: Date?
                    @Volatile @JvmField var day4: Date?
                    @Volatile @JvmField var day5: Date?
                    @Volatile @JvmField var day6: Date?
                    @Volatile @JvmField var day7: Date?
                    @Volatile @JvmField var day8: Date?
                    @Volatile @JvmField var day9: Date?
                    @Volatile @JvmField var day10: Date?
                    @Volatile @JvmField var day11: Date?
                    @Volatile @JvmField var day12: Date?
                    @Volatile @JvmField var day13: Date?
                    @Volatile @JvmField var day14: Date?
                    @Volatile @JvmField var day15: Date?
                    @Volatile @JvmField var day16: Date?
                    @Volatile @JvmField var day17: Date?
                    @Volatile @JvmField var day18: Date?
                    @Volatile @JvmField var day19: Date?
                    @Volatile @JvmField var day20: Date?
                    @Volatile @JvmField var day21: Date?
                    @Volatile @JvmField var day22: Date?
                    @Volatile @JvmField var day23: Date?
                    @Volatile @JvmField var day24: Date?
                    @Volatile @JvmField var day25: Date?
                    @Volatile @JvmField var day26: Date?
                    @Volatile @JvmField var day27: Date?
                    @Volatile @JvmField var day28: Date?
                    @Volatile @JvmField var day29: Date?

                    constructor(date: Date) {
                        month = if (date.day == 0) date
                        else {
                            Date(date.hashCode() and -0x20, InternalMarker)
                        }
                        day0 = null
                        day1 = null
                        day2 = null
                        day3 = null
                        day4 = null
                        day5 = null
                        day6 = null
                        day7 = null
                        day8 = null
                        day9 = null
                        day10 = null
                        day11 = null
                        day12 = null
                        day13 = null
                        day14 = null
                        day15 = null
                        day16 = null
                        day17 = null
                        day18 = null
                        day19 = null
                        day20 = null
                        day21 = null
                        day22 = null
                        day23 = null
                        day24 = null
                        day25 = null
                        day26 = null
                        day27 = null
                        day28 = null
                        day29 = null
                    }

                    protected constructor(other: MonthTable) {
                        month = other.month
                        dayCount = other.dayCount
                        day0 = accountFor(other.day0)
                        day1 = accountFor(other.day1)
                        day2 = accountFor(other.day2)
                        day3 = accountFor(other.day3)
                        day4 = accountFor(other.day4)
                        day5 = accountFor(other.day5)
                        day6 = accountFor(other.day6)
                        day7 = accountFor(other.day7)
                        day8 = accountFor(other.day8)
                        day9 = accountFor(other.day9)
                        day10 = accountFor(other.day10)
                        day11 = accountFor(other.day11)
                        day12 = accountFor(other.day12)
                        day13 = accountFor(other.day13)
                        day14 = accountFor(other.day14)
                        day15 = accountFor(other.day15)
                        day16 = accountFor(other.day16)
                        day17 = accountFor(other.day17)
                        day18 = accountFor(other.day18)
                        day19 = accountFor(other.day19)
                        day20 = accountFor(other.day20)
                        day21 = accountFor(other.day21)
                        day22 = accountFor(other.day22)
                        day23 = accountFor(other.day23)
                        day24 = accountFor(other.day24)
                        day25 = accountFor(other.day25)
                        day26 = accountFor(other.day26)
                        day27 = accountFor(other.day27)
                        day28 = accountFor(other.day28)
                        day29 = accountFor(other.day29)
                    }

                    fun accountFor(date: Date?): Date? {
                        if (date != null) {
                            Private.updateDayCount.incrementAndGet(this)
                            Private.updateCount.incrementAndGet(this@DateSet)
                        }
                        return date
                    }

                    open fun copy(yt: YearTable): MonthTable = yt.MonthTable(this)

                    open val lastDay: Int get() = 29

                    fun addDate(date: Date): Boolean {
                        val d = date.day
                        var delta: Int
                        if (d == 0) {
                            if (!Private.updateDays[0].compareAndSet(this, null, month)) return false
                            delta = 1
                            for(i in 1..lastDay) {
                                if (Private.updateDays[i].getAndSet(this, null) != null) delta--
                            }
                        } else {
                            delta = if (Private.updateDays[d].getAndSet(this, date) == null) 1 else 0
                            if (Private.updateDays[0].getAndSet(this, null) != null) delta--
                        }
                        return if (delta != 0) {
                            Private.updateDayCount.addAndGet(this, delta)
                            Private.updateCount.addAndGet(this@DateSet, delta)
                            true
                        } else false
                    }

                    fun contains(date: Date, exact: Boolean): Boolean {
                        val d = date.day
                        return Private.updateDays[d][this] != null ||
                                (!exact && if (d == 0) dayCount > 0 else Private.updateDays[0][this] != null)
                    }

                    fun remove(date: Date): Boolean {
                        val d = date.day
                        if (d == 0) {
                            removeMonth()
                            for(i in 0..lastDay) Private.updateDays[i][this] = null
                            return Private.updateDayCount.getAndSet(this, 0) != 0
                        }
                        if (Private.updateDays[d].getAndSet(this, null) == null) return false
                        Private.updateCount.decrementAndGet(this@DateSet)
                        if (Private.updateDayCount.decrementAndGet(this) == 0) removeMonth()
                        return true
                    }

                    fun removeMonth() {
                        if (strongUpdater<MonthTable>(month.month - 1)
                                .compareAndSet(this@YearTable, this, null) &&
                            Private.updateMonthCount.decrementAndGet(this@YearTable) == 0)
                            removeYear()
                    }

                    fun writeString(sb: StringBuilder, startProgress: Int): Int {
                        var progress = startProgress
                        for(day in 0..lastDay) {
                            val date: Date = Private.updateDays[day][this] ?: continue
                            if (progress > WRITE_START)
                                sb.append(',')
                            if (progress == WRITE_DAY && day == 0)
                                progress = WRITE_YEAR
                            if (progress <= WRITE_MONTH) {
                                if (progress <= WRITE_YEAR) {
                                    writeYear(sb, date.year)
                                    sb.append('-')
                                }
                                writeMonthOrDay(sb, date.month)
                                if (day == 0) return WRITE_MONTH
                                sb.append('-')
                            }
                            writeMonthOrDay(sb, day)
                            progress = WRITE_DAY
                        }
                        return progress
                    }

                    private fun writeMonthOrDay(sb: StringBuilder, i: Int) {
                        if (i < 10) sb.append('0')
                        sb.append(i)
                    }

                    override fun iterator() = object : MutableIterator<Date> {

                        var index: Int = 0
                        var lastReturned: Int = -1

                        override fun hasNext(): Boolean {
                            for(i in index..lastDay) {
                                if (Private.updateDays[i][this@MonthTable] != null) {
                                    index = i
                                    return true
                                }
                            }
                            return false
                        }

                        override fun next(): Date {
                            val last = lastDay
                            for(i in index..last) {
                                val date: Date = Private.updateDays[i][this@MonthTable] ?: continue
                                index = 1 + if (i == 0) last else i
                                lastReturned = i
                                return date
                            }
                            throw NoSuchElementException()
                        }

                        override fun remove() {
                            val i = lastReturned
                            if (i < 0) throw IllegalStateException()
                            lastReturned = -1
                            if (Private.updateDays[i].getAndSet(this@MonthTable, null) == null)
                                throw ConcurrentModificationException()
                            Private.updateCount.decrementAndGet(this@DateSet)
                            if (Private.updateDayCount.decrementAndGet(this@MonthTable) == 0)
                                removeMonth()
                        }

                    }

                    fun contentEquals(other: MonthTable): Boolean {
                        return when {
                            day0 != null -> other.day0 != null
                            other.day0 != null || lastDay != other.lastDay -> false
                            else -> {
                                for(i in 1..lastDay) {
                                    val updateDay = Private.updateDays[i]
                                    if ((updateDay[this] == null) xor (updateDay[other] == null)) return false
                                }
                                true
                            }
                        }
                    }

                }

                open inner class MonthTable30: MonthTable {

                    @Volatile @JvmField var day30: Date?

                    constructor(date: Date): super(date) {
                        day30 = null
                    }

                    protected constructor(other: MonthTable30): super(other) {
                        day30 = accountFor(other.day30)
                    }

                    override fun copy(yt: YearTable): MonthTable = yt.MonthTable30(this)

                    override val lastDay: Int get() = 30

                }

                inner class MonthTable31: MonthTable30 {

                    @Volatile @JvmField var day31: Date?

                    constructor(date: Date): super(date) {
                        day31 = null
                    }

                    private constructor(other: MonthTable31): super(other) {
                        day31 = accountFor(other.day31)
                    }

                    override fun copy(yt: YearTable): MonthTable = yt.MonthTable31(this)

                    override val lastDay: Int get() = 31

                }

            }

        }

    }

    private fun writeObject(oos: ObjectOutputStream) {
        val yearList = mutableListOf<Table2d.Table1d.YearTable>()
        for(i2d in 0..0x7F) {
            val t2d = strongUpdater<Table2d>(i2d)[table2d]
            if (t2d != null) for(i1d in 0..0xFF) {
                val t1d = strongUpdater<Table2d.Table1d>(i1d)[t2d]
                if (t1d != null) for(i in 0..0xFF) {
                    val yt = strongUpdater<Table2d.Table1d.YearTable>(i)[t1d]
                    if (yt != null) yearList.add(yt)
                }
            }
        }
        oos.writeInt(yearList.size)
        for(yt in yearList) {
            oos.writeInt(yt.year.year)
            if (yt.monthCount > 0) for(m in 0..11) {
                var bits = 0
                strongUpdater<Table2d.Table1d.YearTable.MonthTable>(m)[yt]?.let { mt ->
                    var bit = 1
                    for(d in 0..mt.lastDay) {
                        if (Private.updateDays[d][mt] != null) bits = bits or bit
                        bit = bit shl 1
                    }
                }
                oos.writeInt(bits)
            }
        }
    }

    private fun readObject(ois: ObjectInputStream) {
        queue = ReferenceQueue<Any>()
        table2d = DateTable128(queue)
        val yearCount = ois.readInt()
        repeat(yearCount) {
            val y = ois.readInt()
            val i2d = (y + YEAR_OFFSET) shr 16
            val strong2d = strongUpdater<Table2d>(i2d)
            val t2d = strong2d[table2d] ?: Table2d(queue, i2d).apply {
                strong2d[table2d] = this
                weakUpdater<Table2d>(i2d)[table2d] = WeakDateTable(this, table2d, i2d)
            }
            val i1d = (y shr 8) and 0xFF
            val strong1d = strongUpdater<Table2d.Table1d>(i1d)
            val t1d = strong1d[t2d] ?: t2d.Table1d(i1d).apply {
                strong1d[t2d] = this
                weakUpdater<Table2d.Table1d>(i1d)[t2d] = WeakDateTable(this, t2d, i1d)
                Private.updateCount1d.incrementAndGet(t2d)
            }
            val i = y and 0xFF
            val strongYear = strongUpdater<Table2d.Table1d.YearTable>(i)
            if (strongYear[t1d] != null)
                throw InvalidObjectException("Duplicate year $y")
            val yt = t1d.YearTable(y)
            strongYear[t1d] = yt
            weakUpdater<Table2d.Table1d.YearTable>(i)[t1d] = WeakDateTable(yt, t1d, i)
            Private.updateYearCount.incrementAndGet(t1d)
            var daysInYear = 0
            for(m in 1..12) {
                var bits = ois.readInt().and(2.shl(Date.daysInMonth(m)) - 1)
                if (bits != 0) {
                    val mt = Private.monthTableFactories[m - 1](yt, Date(y, m, 0))
                    if (bits == 1) {
                        mt.dayCount = 1
                        daysInYear++
                        mt.day0 = mt.month
                    } else {
                        bits = bits and -2
                        val dayCount = bits.countOneBits()
                        mt.dayCount = dayCount
                        daysInYear += dayCount
                        while(bits != 0) {
                            val bit = bits and -bits
                            bits -= bit
                            val d = trailingZerosPow2(bit)
                            Private.updateDays[d][mt] = Date(y, m, d)
                        }
                    }
                    strongUpdater<Table2d.Table1d.YearTable.MonthTable>(m - 1)[yt] = mt
                    weakUpdater<Table2d.Table1d.YearTable.MonthTable>(m - 1)[yt] = WeakDateTable(mt, yt, m - 1)
                    Private.updateMonthCount.incrementAndGet(yt)
                }
            }
            if (Private.updateMonthCount.compareAndSet(yt, 0, -1))
                daysInYear++
            Private.updateCount.addAndGet(this, daysInYear)
        }
    }

}