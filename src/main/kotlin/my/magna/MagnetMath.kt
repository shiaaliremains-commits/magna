package my.magna

import net.minecraft.world.phys.Vec3

/** The pull maths, used by the server and by your own screen so both agree. */
object MagnetMath {
    /** returns the new velocity of an item/orb at [pos], or null when it is out of range or already here */
    fun nextVelocity(pos: Vec3, current: Vec3, target: Vec3, range: Double, mult: Double): Vec3? {
        val delta = target.subtract(pos)
        val dist = delta.length()
        if (dist > range || dist < 0.25) return null

        val dir = delta.scale(1.0 / dist)
        val closeness = (1.0 - dist / range).coerceIn(0.0, 1.0)
        // never slow: starts at 0.2 blocks/tick, speeds up when closer, eases out at the end (no overshoot)
        val speed = minOf((0.20 + 0.50 * closeness) * mult, dist * 0.55 + 0.05)
        val lift = if (dist > 1.2) 0.035 else 0.0

        val wx = dir.x * speed
        val wy = dir.y * speed + lift
        val wz = dir.z * speed
        val k = 0.45 // smooth acceleration instead of sudden jumps
        return Vec3(
            current.x + (wx - current.x) * k,
            current.y + (wy - current.y) * k,
            current.z + (wz - current.z) * k
        )
    }
}
