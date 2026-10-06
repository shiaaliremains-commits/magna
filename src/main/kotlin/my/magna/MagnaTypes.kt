package my.magna

enum class PullSpeed(val label: String, val mult: Double) {
    SLOW("Slow", 0.6),
    NORMAL("Normal", 1.0),
    FAST("Fast", 1.6)
}

enum class Targets(val label: String) {
    BOTH("Items + XP"),
    ITEMS("Items only"),
    XP("XP only")
}

object MagnaLimits {
    const val MIN_RANGE = 2
    const val MAX_RANGE = 16
}

/** the player's settings (saved on the client, the server only needs the first four) */
class MagnetSettings {
    var enabled: Boolean = true
    var range: Int = 8
    var speed: Int = PullSpeed.NORMAL.ordinal
    var targets: Int = Targets.BOTH.ordinal
    var quality: Int = 2
}
