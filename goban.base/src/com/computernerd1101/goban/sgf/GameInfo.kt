package com.computernerd1101.goban.sgf

import com.computernerd1101.goban.*
import com.computernerd1101.goban.sgf.internal.*
import com.computernerd1101.goban.time.*
import com.computernerd1101.sgf.*
import java.io.*
import java.nio.charset.Charset
import kotlin.contracts.*

@Suppress("unused")
@OptIn(ExperimentalContracts::class)
fun GameInfo.Player?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this?.isEmpty() != false
}

class GameInfo: Serializable {

    @JvmInline
    value class Players internal constructor(val info: GameInfo) {

        operator fun get(color: GoColor): Player = info.getPlayer(color)

        operator fun set(color: GoColor, player: Player) {
            info.setPlayer(color, player)
        }

    }

    @get:JvmName("players")
    val player: Players get() = Players(this)

    fun getPlayer(color: GoColor): Player {
        return if (color == GoColor.BLACK) blackPlayer else whitePlayer
    }

    fun setPlayer(color: GoColor, player: Player) {
        if (color == GoColor.BLACK) blackPlayer = player
        else whitePlayer = player
    }

    fun swapPlayers() {
        val player = black
        black = white
        white = player
    }

    var blackPlayer: Player
        get() = black
        set(player) {
            if (white === player) white = player.copy()
            black = player
        }

    var whitePlayer: Player
        get() = white
        set(player) {
            if (black === player) black = player.copy()
            white = player
        }

    private var black: Player = Player()
    private var white: Player = Player()

    data class Player(
        var name: String = "",
        var team: String = "",
        var rank: String = ""
    ): Serializable {

        fun isEmpty(): Boolean {
            return name.isEmpty() && team.isEmpty() && rank.isEmpty()
        }

        fun isNotEmpty() = !isEmpty()

        companion object {
            private const val serialVersionUID = 1L
        }

    }

    var handicap: Int = 0
    var malformedHandicap: SGFProperty? = null
    fun parseHandicap(prop: SGFProperty, warnings: SGFWarningList?): Int {
        val bytes: SGFBytes = prop.values[0].parts[0]
        val s = bytes.toString()
        return try {
            val h = s.toInt()
            handicap = h
            h
        } catch(e: NumberFormatException) {
            warnings?.addWarning(SGFWarning(bytes.row, bytes.column,
                "Unable to parse handicap HA[$s]: $e", e))
            malformedHandicap = prop
            0
        }
    }

    private var komi2: Int = 0
    var komi: Double
        get() = komi2 * 0.5
        set(komi) {
            komi2 = when {
                komi.isNaN() -> 0
                komi >= Int.MAX_VALUE * 0.5 -> Int.MAX_VALUE
                komi <= Int.MIN_VALUE * 0.5 -> Int.MIN_VALUE
                else -> {
                    val komi1 = komi.toInt()
                    komi1*2 + when {
                        komi1.toDouble() == komi -> 0
                        komi > 0.0 -> 1
                        else -> -1
                    }
                }
            }
        }
    var malformedKomi: SGFProperty? = null
    fun parseKomi(prop: SGFProperty, warnings: SGFWarningList?): Double {
        val bytes: SGFBytes = prop.values[0].parts[0]
        val s = bytes.toString()
        val m = ParseKomi.REGEX.find(s)
        return if (m != null) {
            val negative = m.groups[ParseKomi.SIGN]?.value?.get(0) == '-'
            val iKomi = m.groups[ParseKomi.IPART] ?: m.groups[ParseKomi.INT]
            val fKomi = m.groups[ParseKomi.FPART]
            var lKomi = 0L
            if (iKomi != null) for(ch in iKomi.value) {
                lKomi = lKomi*10L + (ch - '0')*2L
                if (lKomi > Int.MAX_VALUE + 1L) {
                    lKomi = Int.MAX_VALUE + 1L
                    break
                }
            }
            if (fKomi != null && fKomi.value[0] != '0') lKomi++
            if (negative) lKomi = -lKomi
            val komi2 = when {
                lKomi < Int.MIN_VALUE -> Int.MIN_VALUE
                lKomi > Int.MAX_VALUE -> Int.MAX_VALUE
                else -> lKomi.toInt()
            }
            this.komi2 = komi2
            malformedKomi = null
            komi2*0.5
        } else {
            warnings?.addWarning(
                SGFWarning(bytes.row, bytes.column,
                "Unable to parse komi KM[$s]")
            )
            malformedKomi = prop
            0.0
        }
    }

    @Suppress("SpellCheckingInspection")
    private object ParseKomi {

        const val SIGN = 1
        const val IPART = 3
        const val FPART = 4
        const val INT = 5
        @JvmField val REGEX = """([+\-])?((\d*)\.(\d)|(\d+)\.?)""".toRegex()

    }

    var result: GameResult? = null
    var malformedResult: SGFProperty? = null
    fun parseResult(prop: SGFProperty, warnings: SGFWarningList?): GameResult? {
        val bytes: SGFBytes = prop.values[0].parts[0]
        val s = bytes.toString()
        val result = GameResult.nullable(s)
        malformedResult = if (result == null) {
            warnings?.addWarning(SGFWarning(bytes.row, bytes.column,
                "Unable to parse result RE[$s]"))
            prop
        } else null
        this.result = result
        return result
    }

    var dates: DateSet = DateSet()
    var malformedDates: SGFProperty? = null
    fun parseDates(prop: SGFProperty, warnings: SGFWarningList?): DateSet {
        val dates = DateSet()
        var date: Date? = null
        for(value in prop.values) for(bytes in value.parts)
            date.parseNext(bytes.toString())?.let {
                date = it
                dates.addDate(it)
            }
        this.dates = dates
        malformedDates = if (dates.count == 0) {
            warnings?.addWarning(SGFWarning(prop.row, prop.column,
                "Unable to parse dates DT$prop"))
            prop
        } else null
        return dates
    }

    var timeLimit: Long = 0L
        set(time) {
            field = if (time < 0L) 0L else time
        }
    var malformedTimeLimit: SGFProperty? = null
    fun parseTimeLimit(prop: SGFProperty, warnings: SGFWarningList?): Long {
        val bytes = prop.values[0].parts[0]
        val s = bytes.toString()
        var isMalformed = false
        var time = try {
            s.secondsToMillis()
        } catch(e: NumberFormatException) {
            warnings?.addWarning(SGFWarning(bytes.row, bytes.column,
                "Unable to parse  time limit TM[$s]: $e", e))
            isMalformed = true
            0L
        }
        if (!isMalformed && time < 0L) {
            if (warnings != null) {
                val str = TimeLimit.REGEX.find(s)?.value ?: s
                warnings.addWarning(SGFWarning(bytes.row, bytes.column,
                    "Unable to parse time limit TM[$str]"))
            }
            isMalformed = true
            time = 0L
        }
        malformedTimeLimit = if (isMalformed) prop else null
        timeLimit = time
        return time
    }

    var overtime: Overtime? = null
        set(o) {
            field = o
            malformedOvertime = null
        }
    private var malformedOvertime: String? = null

    var overtimeString: String?
        get() = overtime?.toString() ?: malformedOvertime
        set(str) {
            if (str == null) {
                overtime = null
                malformedOvertime = null
            } else try {
                overtime = Overtime.parse(str)
            } catch(e: Exception) {
                overtime = null
                malformedOvertime = str
            }
        }

    fun parseOvertime(prop: SGFProperty, charset: Charset?, warnings: SGFWarningList?): Overtime? {
        val value: SGFValue = prop.values[0]
        val s: String = InternalGoSGF.parseSGFValue(value, charset, warnings)
        var isMalformed = false
        val o: Overtime? = try {
            Overtime.parse(s)
        } catch(e: Exception) {
            warnings?.addWarning(SGFWarning(value.row, value.column,
                "OT[$s] is not a valid overtime setting: $e", e))
            isMalformed = true
            null
        }
        overtime = o
        malformedOvertime = if (o != null) null
        else {
            if (!isMalformed)
                warnings?.addWarning(SGFWarning(value.row, value.column,
                    "OT[$s] is not a valid overtime setting"))
            s
        }
        return o
    }

    private var _rules = GoRules.DEFAULT
    private var _rulesString = ""

    var rules: GoRules
        get() = _rules
        set(value) {
            _rules = value
            _rulesString = value.toString()
        }
    var rulesString: String
        get() = _rulesString
        set(value) {
            _rules = GoRules(value)
            _rulesString = value
        }

    var gameName: String = ""
    var gameComment: String = ""
    var gameSource: String = ""
    var gameUser: String = ""
    var copyright: String = ""
    var gameLocation: String = ""
    var eventName: String = ""
    var roundType: String = ""
    var annotationProvider: String = ""
    var openingType: String = ""

    fun copy(): GameInfo {
        val copy = GameInfo()
        copy.black = black.copy()
        copy.white = white.copy()
        copy.handicap = handicap
        copy.malformedHandicap = malformedHandicap?.copy(SGFCopyLevel.ALL)
        copy.komi2 = komi2
        copy.malformedKomi = malformedKomi?.copy(SGFCopyLevel.ALL)
        copy.result = result
        copy.malformedResult = malformedResult?.copy(SGFCopyLevel.ALL)
        copy.dates = dates.copy()
        copy.malformedDates = malformedDates?.copy(SGFCopyLevel.ALL)
        copy.timeLimit = timeLimit
        copy.malformedTimeLimit = malformedTimeLimit?.copy(SGFCopyLevel.ALL)
        copy.overtime = overtime?.clone()
        copy.malformedOvertime = malformedOvertime
        copy.rulesString = rulesString
        copy.gameName = gameName
        copy.gameComment = gameComment
        copy.gameSource = gameSource
        copy.gameUser = gameUser
        copy.copyright = copyright
        copy.gameLocation = gameLocation
        copy.eventName = eventName
        copy.roundType = roundType
        copy.annotationProvider = annotationProvider
        copy.openingType = openingType
        return copy
    }
    
    fun isEmpty(): Boolean {
        return black.isEmpty() && white.isEmpty() &&
                handicap == 0 &&
                malformedHandicap == null &&
                komi2 == 0 &&
                malformedKomi == null &&
                result == null &&
                malformedResult == null &&
                dates.isEmpty() &&
                malformedDates == null &&
                timeLimit == 0L &&
                malformedTimeLimit == null &&
                overtime == null &&
                malformedOvertime.isNullOrEmpty() &&
                rulesString.isEmpty() &&
                gameName.isEmpty() &&
                gameComment.isEmpty() &&
                gameSource.isEmpty() &&
                gameUser.isEmpty() &&
                copyright.isEmpty() &&
                gameLocation.isEmpty() &&
                eventName.isEmpty() &&
                roundType.isEmpty() &&
                annotationProvider.isEmpty() &&
                openingType.isEmpty()
    }

    fun writeSGFNode(node: SGFNode, charset: Charset?) {
        val propMap = node.properties
        blackPlayer.writeSGFNode(propMap, charset, "PB", "BT", "BR")
        whitePlayer.writeSGFNode(propMap, charset, "PW", "WT", "WR")
        var i = handicap
        var prop: SGFProperty? = if (i != 0) SGFProperty(SGFValue(SGFBytes(i.toString())))
        else malformedHandicap?.copy(SGFCopyLevel.ALL)
        if (prop != null) propMap["HA"] = prop
        i = komi2
        prop = if (i != 0) SGFProperty(SGFValue(SGFBytes(
            if (i and 1 == 0) (i shr 1).toString() // i is even
            else (i * 0.5).toString()
        ))) else malformedKomi?.copy(SGFCopyLevel.ALL)
        if (prop != null) propMap["KM"] = prop
        val ds = dates
        prop = if (ds.count != 0) SGFProperty(SGFValue(SGFBytes(ds.toString())))
        else malformedDates?.copy(SGFCopyLevel.ALL)
        if (prop != null) propMap["DT"] = prop
        val l = timeLimit
        prop = if (l != 0L) SGFProperty(SGFValue(SGFBytes(l.millisToStringSeconds())))
        else malformedTimeLimit?.copy(SGFCopyLevel.ALL)
        if (prop != null) propMap["TM"] = prop
        var s: String? = overtimeString
        if (s != null) propMap["OT"] = SGFProperty(SGFValue(s, charset))
        val re = result
        prop = if (re != null) SGFProperty(SGFValue(SGFBytes(re.toString())))
        else malformedResult?.copy(SGFCopyLevel.ALL)
        if (prop != null) propMap["RE"] = prop
        s = rulesString
        if (s.isNotEmpty()) propMap["RU"] = SGFProperty(SGFValue(s, charset))
        s = gameName
        if (s.isNotBlank()) propMap["GN"] = SGFProperty(SGFValue(s, charset))
        s = gameComment
        if (s.isNotEmpty()) propMap["GC"] = SGFProperty(SGFValue(s, charset))
        s = gameSource
        if (s.isNotEmpty()) propMap["SO"] = SGFProperty(SGFValue(s, charset))
        s = gameUser
        if (s.isNotEmpty()) propMap["US"] = SGFProperty(SGFValue(s, charset))
        s = copyright
        if (s.isNotEmpty()) propMap["CP"] = SGFProperty(SGFValue(s, charset))
        s = gameLocation
        if (s.isNotEmpty()) propMap["PC"] = SGFProperty(SGFValue(s, charset))
        s = eventName
        if (s.isNotEmpty()) propMap["EV"] = SGFProperty(SGFValue(s, charset))
        s = roundType
        if (s.isNotEmpty()) propMap["RO"] = SGFProperty(SGFValue(s, charset))
        s = annotationProvider
        if (s.isNotEmpty()) propMap["AN"] = SGFProperty(SGFValue(s, charset))
        s = openingType
        if (s.isNotEmpty()) propMap["ON"] = SGFProperty(SGFValue(s, charset))
    }

    private fun Player.writeSGFNode(propMap: MutableMap<String, SGFProperty>, charset: Charset?,
                                    nameProp: String, teamProp: String, rankProp: String) {
        var s = name
        if (s.isNotEmpty()) propMap[nameProp] = SGFProperty(SGFValue(s, charset))
        s = team
        if (s.isNotEmpty()) propMap[teamProp] = SGFProperty(SGFValue(s, charset))
        s = rank
        if (s.isNotEmpty()) propMap[rankProp] = SGFProperty(SGFValue(s, charset))
    }

    fun parseSGFProperty(name: String, prop: SGFProperty, charset: Charset?, warnings: SGFWarningList?) {
        var player: Player? = null
        var playerProp = 0
        when(name) {
            "PB" -> player = black
            "PW" -> player = white
            "BT" -> {
                player = black
                playerProp = 1
            }
            "WT" -> {
                player = white
                playerProp = 1
            }
            "BR" -> {
                player = black
                playerProp = 2
            }
            "WR" -> {
                player = white
                playerProp = 2
            }
            "HA" -> parseHandicap(prop, warnings)
            "KM" -> parseKomi(prop, warnings)
            "DT" -> parseDates(prop, warnings)
            "TM" -> parseTimeLimit(prop, warnings)
            "OT" -> parseOvertime(prop, charset, warnings)
            "RE" -> parseResult(prop, warnings)
            "RU" -> rulesString = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "GN" -> gameName = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "GC" -> gameComment = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "SO" -> gameSource = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "US" -> gameUser = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "CP" -> copyright = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "PC" -> gameLocation = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "EV" -> eventName = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "RO" -> roundType = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "AN" -> annotationProvider = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            "ON" -> openingType = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
        }
        if (player != null) {
            val s = InternalGoSGF.parseSGFValue(prop.values[0], charset, warnings)
            when(playerProp) {
                0 -> player.name = s
                1 -> player.team = s
                2 -> player.rank = s
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is GameInfo &&
                black == other.black && white == other.white &&
                handicap == other.handicap && komi2 == other.komi2 &&
                malformedKomi?.values == other.malformedKomi?.values &&
                result == other.result &&
                malformedResult?.values == other.malformedResult?.values &&
                dates contentEquals other.dates &&
                malformedDates?.values == other.malformedDates?.values &&
                timeLimit == other.timeLimit &&
                malformedTimeLimit?.values == other.malformedTimeLimit?.values &&
                overtimeString == other.overtimeString &&
                rulesString == other.rulesString && gameName == other.gameName &&
                gameUser == other.gameUser && copyright == other.copyright &&
                gameLocation == other.gameLocation && eventName == other.eventName &&
                roundType == other.roundType &&
                annotationProvider == other.annotationProvider &&
                openingType == other.openingType)
    }

    override fun hashCode(): Int {
        return ((((((((((((((((((((
                black.hashCode() * (31 * 31 * 31) + white.hashCode()) * 31 +
                handicap) * 31 +
                komi2) * 31 +
                malformedKomi?.values.hashCode()) * 31 +
                result.hashCode()) * 31 +
                dates.contentHashCode()) * 31 +
                malformedDates?.values.hashCode()) * 31 +
                timeLimit.hashCode()) * 31 +
                malformedTimeLimit?.values.hashCode()) * 31 +
                overtimeString.hashCode()) * 31 +
                rulesString.hashCode()) * 31 +
                gameName.hashCode()) * 31 +
                gameComment.hashCode()) * 31 +
                gameSource.hashCode()) * 31 +
                gameUser.hashCode()) * 31 +
                copyright.hashCode()) * 31 +
                gameLocation.hashCode()) * 31 +
                eventName.hashCode()) * 31 +
                roundType.hashCode()) * 31 +
                annotationProvider.hashCode()) * 31 +
                openingType.hashCode()
    }

    companion object {

        private const val serialVersionUID = 1L

        private val serialPersistentFields = arrayOf(
            ObjectStreamField("black", Player::class.java, true),
            ObjectStreamField("white", Player::class.java, true),
            ObjectStreamField("handicap", Int::class.java),
            ObjectStreamField("malformedHandicap", SGFProperty::class.java, true),
            ObjectStreamField("komi", Double::class.java),
            ObjectStreamField("malformedKomi", SGFProperty::class.java, true),
            ObjectStreamField("result", GameResult::class.java),
            ObjectStreamField("malformedResult", SGFProperty::class.java, true),
            ObjectStreamField("dates", DateSet::class.java),
            ObjectStreamField("malformedDates", SGFProperty::class.java, true),
            ObjectStreamField("timeLimit", Long::class.java),
            ObjectStreamField("malformedTimeLimit", SGFProperty::class.java, true),
            ObjectStreamField("overtime", String::class.java),
            ObjectStreamField("rules", String::class.java),
            ObjectStreamField("gameName", String::class.java),
            ObjectStreamField("gameComment", String::class.java),
            ObjectStreamField("gameSource", String::class.java),
            ObjectStreamField("gameUser", String::class.java),
            ObjectStreamField("copyright", String::class.java),
            ObjectStreamField("gameLocation", String::class.java),
            ObjectStreamField("eventName", String::class.java),
            ObjectStreamField("roundType", String::class.java),
            ObjectStreamField("annotationProvider", String::class.java),
            ObjectStreamField("openingType", String::class.java)
        )

    }
    
    private fun writeObject(oos: ObjectOutputStream) {
        val fields: ObjectOutputStream.PutField = oos.putFields()
        fields.put("black", black)
        fields.put("white", white)
        fields.put("handicap", handicap)
        fields.put("malformedHandicap", malformedHandicap)
        fields.put("komi", komi)
        fields.put("malformedKomi", malformedKomi)
        fields.put("result", result)
        fields.put("malformedResult", malformedResult)
        fields.put("dates", dates)
        fields.put("malformedDates", malformedDates)
        fields.put("timeLimit", timeLimit)
        fields.put("malformedTimeLimit", malformedTimeLimit)
        fields.put("overtime", overtimeString)
        fields.put("rules", rulesString)
        fields.put("gameName", gameName)
        fields.put("gameComment", gameComment)
        fields.put("gameSource", gameSource)
        fields.put("gameUser", gameUser)
        fields.put("copyright", copyright)
        fields.put("gameLocation", gameLocation)
        fields.put("eventName", eventName)
        fields.put("roundType", roundType)
        fields.put("annotationProvider", annotationProvider)
        fields.put("openingType", openingType)
        oos.writeFields()
    }
    
    private fun readObject(ois: ObjectInputStream) {
        val fields: ObjectInputStream.GetField = ois.readFields()
        black = fields["black", null] as? Player ?: Player()
        white = fields["white", null] as? Player ?: Player()
        handicap = fields["handicap", 0]
        malformedHandicap = fields["malformedHandicap", null] as? SGFProperty
        komi = fields["komi", 0.0]
        malformedKomi = fields["malformedKomi", null] as? SGFProperty
        result = fields["result", null] as? GameResult
        malformedResult = fields["malformedResult", null] as? SGFProperty
        dates = fields["dates", null] as? DateSet ?: DateSet()
        malformedDates = fields["malformedDates", null] as? SGFProperty
        timeLimit = fields["timeLimit", 0L]
        malformedTimeLimit = fields["malformedTimeLimit", null] as? SGFProperty
        var overtimeString: String? = null
        try {
            overtimeString = fields["overtime", null]?.toString()
            if (overtimeString != null) {
                val overtime = Overtime.parse(overtimeString)
                if (overtime != null) {
                    this.overtime = overtime
                    overtimeString = null
                }
            }
        } catch(e: Exception) { }
        malformedOvertime = overtimeString
        rulesString = fields.getString("rules")
        gameName = fields.getString("gameName")
        gameComment = fields.getString("gameComment")
        gameSource = fields.getString("gameSource")
        gameUser = fields.getString("gameUser")
        copyright = fields.getString("copyright")
        gameLocation = fields.getString("gameLocation")
        eventName = fields.getString("eventName")
        roundType = fields.getString("roundType")
        annotationProvider = fields.getString("annotationProvider")
        openingType = fields.getString("openingType")
    }

}