@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "NOTHING_TO_INLINE")

package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.internal.*
import com.computernerd1101.goban.sgf.internal.InternalDate
import java.io.InvalidObjectException
import java.io.*
import java.lang.ref.*
import java.util.*
import java.util.concurrent.atomic.AtomicReferenceArray
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
                day < 0 || day > daysInMonth[m] -> 0
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

    companion object {

        const val MIN_YEAR = -1 shl 22
        const val MAX_YEAR = (1 shl 22) - 1

        private val daysInMonth = intArrayOf(
            0,
            31,  // January
            29,  // February
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

        @JvmStatic
        fun daysInMonth(month: Int): Int = if (month in 1..12) daysInMonth[month] else 0

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
            day < 0 || day > daysInMonth[month] -> Date(year, month, 0)
            else -> this
        }
    }

}

fun Date?.parseNext(s: String): Date? {
    return InternalDate.parse(this, s) ?: this
}

@Suppress("unused")
inline infix fun DateSet?.contentEquals(other: DateSet?) = DateSet.contentEquals(this, other)
@Suppress("unused")
inline infix fun DateSet?.contentNotEqual(other: DateSet?) = !DateSet.contentEquals(this, other)
@Suppress("unused")
inline fun DateSet?.contentHashCode() = this?.contentHashCode() ?: 0

@OptIn(ExperimentalContracts::class)
@Suppress("unused")
inline fun DateSet?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || isEmpty()
}

class DateSet(): MutableIterable<Date>, Serializable {

    fun isEmpty() = count == 0

    inline fun isNotEmpty() = !isEmpty()

    @Volatile
    private var _count: Int = 0

    val count: Int
        @JvmName("count")
        get() {
            expungeStaleReferences()
            return _count
        }

    private var table2d = AtomicReferenceArray<Table2d?>(0x80)
    private var weak2d = AtomicReferenceArray<Weak<Table2d>?>(0x80)
    private var queue: ReferenceQueue<*> = ReferenceQueue<Any>()

    constructor(string: String): this() {
        val split = string.split(",".toRegex()).toTypedArray()
        var date: Date? = null
        for (s in split) {
            date = date.parseNext(s)
            if (date != null) addDate(date)
        }
    }

    fun copy(): DateSet {
        val copy = DateSet()
        for(i in 0..0x7F)
            table2d[i]?.let {
                val t = Table2d(it)
                copy.table2d[i] = t
                copy.weak2d[i] = Weak(t, copy.weak2d, i)
            }
        return copy
    }

    private fun expungeStaleReferences() {
        var r = queue.poll()
        while(r != null) {
            if (r is Weak<*>) r.expunge()
            r = queue.poll()
        }
    }

    override fun toString(): String {
        expungeStaleReferences()
        return buildString {
            var hasPrev = false
            for(i in 0..0x7F) {
                table2d[i]?.let { t ->
                    hasPrev = t.writeString(this, hasPrev)
                }
            }
        }
    }

    infix fun contentEquals(other: DateSet): Boolean {
        val count = this.count
        if (other.count != count) return false
        for(i in 0..0x7F)
            if (table2d[i] contentNotEqual other.table2d[i]) return false
        return true
    }

    @Suppress("unused")
    inline infix fun contentNotEqual(other: DateSet) = !contentEquals(other)

    fun contentHashCode(): Int {
        var totalHash = 0
        for(i2d in 0..0x7F) table2d[i2d]?.let { t2d ->
            for(i1d in 0..0xFF) t2d.table1d[i1d]?.let { t1d ->
                for(i in 0..0xFF) t1d.years[i]?.let { yt ->
                    var hash = yt.year.year
                    for(m in 0..11) {
                        hash *= 31
                        yt.months[m]?.let { mt ->
                            if (mt.days[0] != null) hash++
                            else {
                                var bit = 2
                                for (d in 1 until mt.days.length()) {
                                    if (mt.days[d] != null) hash += bit
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

    companion object {

        private fun writeYear(sb: StringBuilder, year: Int) {
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

        private fun writeMonthOrDay(sb: StringBuilder, i: Int) {
            if (i < 10) sb.append('0')
            sb.append(i)
        }

        private const val WRITE_START = 0
        private const val WRITE_YEAR = 1
        private const val WRITE_MONTH = 2
        private const val WRITE_DAY = 3

        private const val YEAR_OFFSET = -Date.MIN_YEAR

        /** Updates [DateSet._count] */
        private val updateCount = atomicIntUpdater<DateSet>("_count")
        /** Updates [DateSet.Table2d.count1d] */
        private val update1d = atomicIntUpdater<Table2d>("count1d")
        /** Updates [DateSet.Table2d.Table1d.yearCount] */
        private val updateYears =
            atomicIntUpdater<Table2d.Table1d>("yearCount")
        /** Updates [DateSet.Table2d.Table1d.YearTable.monthCount] */
        private val updateMonths =
            atomicIntUpdater<Table2d.Table1d.YearTable>("monthCount")
        /** Updates [DateSet.Table2d.Table1d.YearTable.MonthTable.dayCount] */
        private val updateDays =
            atomicIntUpdater<Table2d.Table1d.YearTable.MonthTable>("dayCount")

        @JvmStatic
        fun contentEquals(a: DateSet?, b: DateSet?) = when {
            a == null -> b == null
            b == null -> false
            else -> a contentEquals b
        }

        private infix fun Table2d?.contentNotEqual(other: Table2d?) = when {
            this == null -> other != null
            other == null -> true
            else -> !contentEquals(other)
        }

        private infix fun Table2d.Table1d?.contentNotEqual(other: Table2d.Table1d?) = when {
            this == null -> other != null
            other == null -> true
            else -> !contentEquals(other)
        }

        private infix fun Table2d.Table1d.YearTable?.contentNotEqual(
            other: Table2d.Table1d.YearTable?
        ) = when {
            this == null -> other != null
            other == null -> true
            else -> !contentEquals(other)
        }

        private infix fun Table2d.Table1d.YearTable.MonthTable?.contentNotEqual(
            other: Table2d.Table1d.YearTable.MonthTable?
        ) = when {
            this == null -> other != null
            other == null -> true
            else -> !contentEquals(other)
        }

        private const val serialVersionUID = 1L

    }

    fun addDate(date: Date): Boolean {
        return try {
            val i = (date.year + YEAR_OFFSET) shr 16
            var t = table2d[i]
            if (t == null) {
                val weak = weak2d[i]
                if (weak != null) t = weak.get()
                val updateWeak = if (t == null) {
                    t = Table2d(i)
                    true
                } else false
                val tmp = table2d.compareAndExchange(i, null, t)
                if (tmp != null) t = tmp
                if (updateWeak)
                    weak2d.compareAndSet(i, weak, Weak(t, weak2d, i))
            }
            t.addDate(date)
        } finally {
            expungeStaleReferences()
        }
    }

    operator fun contains(date: Date): Boolean {
        expungeStaleReferences()
        return table2d[(date.year + YEAR_OFFSET) shr 16]?.contains(date, false) == true
    }

    @Suppress("unused")
    fun containsExact(date: Date): Boolean {
        expungeStaleReferences()
        return table2d[(date.year + YEAR_OFFSET) shr 16]?.contains(date, true) == true
    }

    fun remove(date: Date): Boolean {
        expungeStaleReferences()
        return table2d[(date.year + YEAR_OFFSET) shr 16]?.remove(date) == true
    }

    override fun iterator(): MutableIterator<Date> {
        return MetaItr(table2d)
    }

    @Suppress("UNCHECKED_CAST")
    inner class Weak<T>(referent: T, val array: AtomicReferenceArray<Weak<T>?>, val index: Int):
        WeakReference<T>(referent, queue as ReferenceQueue<in T>) {

        fun expunge() {
            array.compareAndSet(index, this, null)
        }

    }

    class MetaItr<T : MutableIterable<Date>>(val array: AtomicReferenceArray<T?>): MutableIterator<Date> {

        var index: Int = 0
        var itr: MutableIterator<Date>? = null

        override fun hasNext(): Boolean {
            if (itr?.hasNext() == true) return true
            for(i in (index + 1) until array.length()) {
                array[i]?.let { t: T ->
                    val itr = t.iterator()
                    if (itr.hasNext()) {
                        this.itr = itr
                        index = i
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
            for(i in (index + 1) until array.length()) {
                array[i]?.let { t: T ->
                    val itr2 = t.iterator()
                    if (itr2.hasNext()) {
                        itr = itr2
                        index = i
                        return itr2.next()
                    }
                }
            }
            throw NoSuchElementException()
        }

        override fun remove() {
            itr?.remove() ?: throw IllegalStateException()
            itr = null
        }

    }

    private inner class Table2d(val index2d: Int): MutableIterable<Date> {

        @Volatile @JvmField var count1d: Int = 0
        val table1d = AtomicReferenceArray<Table1d?>(0x100)
        val weak1d = AtomicReferenceArray<Weak<Table1d>?>(0x100)

        constructor(other: Table2d): this(other.index2d) {
            for(i in 0..0xFF)
                other.table1d[i]?.let {
                    update1d.incrementAndGet(this)
                    val t = Table1d(it)
                    table1d[i] = t
                    weak1d[i] = Weak(t, weak1d, i)
                }
        }

        fun addDate(date: Date): Boolean {
            val i = (date.year shr 8) and 0xFF
            var t = table1d[i]
            if (t == null) {
                val weak = weak1d[i]
                if (weak != null) t = weak.get()
                val updateWeak = if (t == null) {
                    t = Table1d(i)
                    true
                } else false
                val tmp = table1d.compareAndExchange(i, null, t)
                if (tmp == null) update1d.incrementAndGet(this)
                else t = tmp
                if (updateWeak)
                    weak1d.compareAndSet(i, weak, Weak(t, weak1d, i))
            }
            return t.addDate(date)
        }

        fun contains(date: Date, exact: Boolean): Boolean {
            return table1d[(date.year shr 8) and 0xFF]?.contains(date, exact) == true
        }

        fun remove(date: Date): Boolean {
            return table1d[(date.year shr 8) and 0xFF]?.remove(date) == true
        }

        fun remove2d() {
            table2d.compareAndSet(index2d, this, null)
        }

        fun writeString(sb: StringBuilder, hasPrev: Boolean): Boolean {
            var prev = hasPrev
            for(i in 0..0xFF)
                table1d[i]?.let { t ->
                    prev = t.writeString(sb, prev)
                }
            return prev
        }

        override fun iterator(): MutableIterator<Date> = MetaItr(table1d)

        fun contentEquals(other: Table2d): Boolean {
            val count = count1d
            if (other.count1d != count) return false
            for(i in 0..0xFF)
                if (table1d[i] contentNotEqual other.table1d[i]) return false
            return true
        }

        inner class Table1d(val index1d: Int): MutableIterable<Date> {

            @Volatile @JvmField var yearCount: Int = 0
            val years = AtomicReferenceArray<YearTable?>(0x100)
            val weakYears = AtomicReferenceArray<Weak<YearTable>?>(0x100)

            constructor(other: Table1d): this(other.index1d) {
                for(i in 0..0xFF)
                    other.years[i]?.let {
                        updateYears.incrementAndGet(this)
                        val yt = YearTable(it)
                        years[i] = yt
                        weakYears[i] = Weak(yt, weakYears, i)
                    }
            }

            fun addDate(date: Date): Boolean {
                val i = date.year and 0xFF
                var yt = years[i]
                if (yt == null) {
                    val weak = weakYears[i]
                    if (weak != null) yt = weak.get()
                    val updateWeak = if (yt == null) {
                        yt = YearTable(date)
                        true
                    } else false
                    val tmp = years.compareAndExchange(i, null, yt)
                    if (tmp == null) updateYears.incrementAndGet(this)
                    else yt = tmp
                    if (updateWeak)
                        weakYears.compareAndSet(i, weak, Weak(yt, weakYears, i))
                    if (tmp == null && date.month == 0)
                        return true
                }
                return yt.addDate(date)
            }

            fun contains(date: Date, exact: Boolean): Boolean {
                return years[date.year and 0xFF]?.contains(date, exact) == true
            }

            fun remove(date: Date): Boolean {
                return years[date.year and 0xFF]?.remove(date) == true
            }

            fun remove1d() {
                if (table1d.compareAndSet(index1d, this, null) &&
                    update1d.decrementAndGet(this@Table2d) == 0)
                    remove2d()
            }

            fun writeString(sb: StringBuilder, hasPrev: Boolean): Boolean {
                var prev = hasPrev
                for(i in 0..0xFF)
                    years[i]?.let { yt ->
                        prev = yt.writeString(sb, prev)
                    }
                return prev
            }

            override fun iterator(): MutableIterator<Date> = MetaItr(years)

            fun contentEquals(other: Table1d): Boolean {
                val count = yearCount
                if (other.yearCount != count) return false
                for(i in 0..0xFF)
                    if (years[i] contentNotEqual other.years[i]) return false
                return true
            }

            inner class YearTable: MutableIterable<Date> {

                val year: Date
                @Volatile @JvmField var monthCount: Int = 0
                val months = AtomicReferenceArray<MonthTable?>(12)
                val weakMonths = AtomicReferenceArray<Weak<MonthTable>?>(12)

                constructor(year: Int) {
                    this.year = Date(year, 0, 0)
                }

                constructor(date: Date) {
                    val m = date.month
                    if (m == 0) {
                        year = date
                        monthCount = -1
                        updateCount.incrementAndGet(this@DateSet)
                    } else {
                        year = Date(date.hashCode() and -0x20000, InternalMarker)
                    }
                }

                constructor(other: YearTable) {
                    year = other.year
                    if (other.monthCount == -1) {
                        monthCount = -1
                        updateCount.incrementAndGet(this@DateSet)
                    } else
                        for(i in 0..11) {
                            other.months[i]?.let {
                                val mt = MonthTable(it)
                                months[i] = mt
                                weakMonths[i] = Weak(mt, weakMonths, i)
                            }
                        }
                }

                fun addDate(date: Date): Boolean {
                    val m = date.month
                    if (m == 0) return when(updateMonths.getAndSet(this, -1)) {
                        -1 -> false
                        0 -> {
                            updateCount.incrementAndGet(this@DateSet)
                            true
                        }
                        else -> {
                            var delta = 1
                            for (i in 0..11)
                                months.getAndSet(i, null)?.let {
                                    delta -= it.dayCount
                                }
                            if (delta != 0) updateCount.addAndGet(this@DateSet, delta)
                            true
                        }
                    }
                    var mt = months[m - 1]
                    if (mt == null) {
                        val weak = weakMonths[m - 1]
                        if (weak != null) mt = weak.get()
                        val updateWeak = if (mt == null) {
                            mt = MonthTable(date)
                            true
                        } else false
                        val tmp = months.compareAndExchange(m - 1, null, mt)
                        if (tmp != null) mt = tmp
                        else {
                            if (!updateMonths.compareAndSet(this, -1, 1))
                                updateMonths.incrementAndGet(this)
                            if (updateWeak)
                                weakMonths.compareAndSet(m - 1, weak, Weak(mt, weakMonths, m - 1))
                            return true
                        }
                    }
                    return mt.addDate(date)
                }

                fun contains(date: Date, exact: Boolean): Boolean {
                    val m = date.month
                    if (m == 0) {
                        val n = monthCount
                        return if (exact) n == -1 else n != 0
                    }
                    return (!exact && monthCount == -1) ||
                            months[m - 1]?.contains(date, exact) == true
                }

                fun remove(date: Date): Boolean {
                    val m = date.month
                    return if (m == 0) {
                        var n = 0
                        for(i in 0..11)
                            months.getAndSet(i, null)?.apply {
                                n += dayCount
                            }
                        removeYear()
                        updateMonths.getAndSet(this, 0) == -1 || n > 0
                    } else months[m - 1]?.remove(date) == true
                }

                fun removeYear() {
                    if (years.compareAndSet(year.year and 0xFF, this, null) &&
                            updateYears.decrementAndGet(this@Table1d) == 0)
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
                        months[i]?.let { mt ->
                            progress = mt.writeString(sb, progress)
                        }
                    return progress > WRITE_START
                }

                override fun iterator(): MutableIterator<Date> {
                    val count = monthCount
                    return if (count > 0) MetaItr(months)
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
                            if (!updateMonths.compareAndSet(this@YearTable, -1, 0))
                                throw ConcurrentModificationException()
                            removeYear()
                        }

                    }
                }

                fun contentEquals(other: YearTable): Boolean {
                    val count = monthCount
                    if (other.monthCount != count) return false
                    for(i in 0..11) {
                        if (months[i] contentNotEqual other.months[i]) return false
                    }
                    return true
                }

                inner class MonthTable: MutableIterable<Date> {

                    val month: Date
                    @Volatile @JvmField var dayCount: Int = 0
                    val days: AtomicReferenceArray<Date?>

                    constructor(date: Date) {
                        month = if (date.day == 0) date
                        else {
                            Date(date.hashCode() and -0x20, InternalMarker)
                        }
                        days = AtomicReferenceArray(Date.daysInMonth(date.month) + 1)
                    }

                    constructor(other: MonthTable) {
                        month = other.month
                        dayCount = other.dayCount
                        val cap = other.days.length()
                        days = AtomicReferenceArray(cap)
                        for(i in 0 until cap) {
                            other.days[i]?.let { d ->
                                days[i] = d
                                updateDays.incrementAndGet(this)
                                updateCount.incrementAndGet(this@DateSet)
                            }
                        }
                    }

                    fun addDate(date: Date): Boolean {
                        val d = date.day
                        var delta: Int
                        if (d == 0) {
                            if (!days.compareAndSet(0, null, month)) return false
                            delta = 1
                            for(i in 1 until days.length()) {
                                if (days.getAndSet(i, null) != null) delta--
                            }
                        } else {
                            delta = if (days.getAndSet(d, date) == null) 1 else 0
                            if (days.getAndSet(0, null) != null) delta--
                        }
                        return if (delta != 0) {
                            updateDays.addAndGet(this, delta)
                            updateCount.addAndGet(this@DateSet, delta)
                            true
                        } else false
                    }

                    fun contains(date: Date, exact: Boolean): Boolean {
                        val d = date.day
                        return days[d] != null ||
                                (!exact && if (d == 0) dayCount > 0 else days[0] != null)
                    }

                    fun remove(date: Date): Boolean {
                        val d = date.day
                        if (d == 0) {
                            removeMonth()
                            for(i in 0 until days.length()) days[i] = null
                            return updateDays.getAndSet(this, 0) != 0
                        }
                        if (days.getAndSet(d, null) == null) return false
                        updateCount.decrementAndGet(this@DateSet)
                        if (updateDays.decrementAndGet(this) == 0) removeMonth()
                        return true
                    }

                    fun removeMonth() {
                        if (months.compareAndSet(month.month - 1, this, null) &&
                                updateMonths.decrementAndGet(this@YearTable) == 0)
                            removeYear()
                    }

                    fun writeString(sb: StringBuilder, startProgress: Int): Int {
                        var progress = startProgress
                        for(day in 0 until days.length()) {
                            val date: Date = days[day] ?: continue
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

                    override fun iterator() = object : MutableIterator<Date> {

                        var index: Int = 0
                        var lastReturned: Int = -1

                        override fun hasNext(): Boolean {
                            for(i in index until days.length()) {
                                if (days[i] != null) {
                                    index = i
                                    return true
                                }
                            }
                            return false
                        }

                        override fun next(): Date {
                            val length = days.length()
                            for(i in index until length) {
                                val date: Date = days[i] ?: continue
                                index = if (i == 0) length else i + 1
                                lastReturned = i
                                return date
                            }
                            throw NoSuchElementException()
                        }

                        override fun remove() {
                            val i = lastReturned
                            if (i < 0) throw IllegalStateException()
                            lastReturned = -1
                            if (days.getAndSet(i, null) == null)
                                throw ConcurrentModificationException()
                            updateCount.decrementAndGet(this@DateSet)
                            if (updateDays.decrementAndGet(this@MonthTable) == 0)
                                removeMonth()
                        }

                    }

                    fun contentEquals(other: MonthTable): Boolean {
                        return when {
                            days[0] != null -> other.days[0] != null
                            other.days[0] != null || days.length() != other.days.length() -> false
                            else -> {
                                for(i in 1 until days.length())
                                    if ((days[i] == null) xor (other.days[i] == null)) return false
                                true
                            }
                        }
                    }

                }

            }

        }

    }

    private fun writeObject(oos: ObjectOutputStream) {
        val yearList = mutableListOf<Table2d.Table1d.YearTable>()
        for(i2d in 0..0x7F) {
            val t2d = table2d[i2d]
            if (t2d != null) for(i1d in 0..0xFF) {
                val t1d = t2d.table1d[i1d]
                if (t1d != null) for(i in 0..0xFF) {
                    val yt = t1d.years[i]
                    if (yt != null) yearList.add(yt)
                }
            }
        }
        oos.writeInt(yearList.size)
        for(yt in yearList) {
            oos.writeInt(yt.year.year)
            if (yt.monthCount > 0) for(m in 0..11) {
                var bits = 0
                yt.months[m]?.days?.let { days ->
                    var bit = 1
                    for(d in 0 until days.length()) {
                        if (days[d] != null) bits = bits or bit
                        bit = bit shl 1
                    }
                }
                oos.writeInt(bits)
            }
        }
    }

    private fun readObject(ois: ObjectInputStream) {
        table2d = AtomicReferenceArray(0x80)
        weak2d = AtomicReferenceArray(0x80)
        queue = ReferenceQueue<Any>()
        val yearCount = ois.readInt()
        repeat(yearCount) {
            val y = ois.readInt()
            val i2d = (y + YEAR_OFFSET) shr 16
            val t2d = table2d[i2d] ?: Table2d(i2d).apply {
                table2d[i2d] = this
                weak2d[i2d] = Weak(this, weak2d, i2d)
            }
            val i1d = (y shr 8) and 0xFF
            val t1d = t2d.table1d[i1d] ?: t2d.Table1d(i1d).apply {
                t2d.table1d[i1d] = this
                t2d.weak1d[i1d] = Weak(this, t2d.weak1d, i1d)
                update1d.incrementAndGet(t2d)
            }
            val i = y and 0xFF
            if (t1d.years[i] != null)
                throw InvalidObjectException("Duplicate year $y")
            val yt = t1d.YearTable(y)
            t1d.years[i] = yt
            t1d.weakYears[i] = Weak(yt, t1d.weakYears, i)
            updateYears.incrementAndGet(t1d)
            var daysInYear = 0
            for(m in 1..12) {
                var bits = ois.readInt().and(1.shl(Date.daysInMonth(m) + 1) - 1)
                if (bits != 0) {
                    val mt = yt.MonthTable(Date(y, m, 0))
                    if (bits == 1) {
                        mt.dayCount = 1
                        daysInYear++
                        mt.days[0] = mt.month
                    } else {
                        bits = bits and -2
                        val dayCount = bits.countOneBits()
                        mt.dayCount = dayCount
                        daysInYear += dayCount
                        while(bits != 0) {
                            val bit = bits and -bits
                            bits -= bit
                            val d = trailingZerosPow2(bit)
                            mt.days[d] = Date(y, m, d)
                        }
                    }
                    yt.months[m - 1] = mt
                    yt.weakMonths[m - 1] = Weak(mt, yt.weakMonths, m - 1)
                    updateMonths.incrementAndGet(yt)
                }
            }
            if (updateMonths.compareAndSet(yt, 0, -1))
                daysInYear++
            updateCount.addAndGet(this, daysInYear)
        }
    }

}