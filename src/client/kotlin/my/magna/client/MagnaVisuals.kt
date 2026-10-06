package my.magna.client

import kotlin.math.cos
import kotlin.math.sin
import my.magna.Targets
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.Vec3

/**
 * The magnet field: a soft ring with a ripple, and for every item a motion tail, a thin curved
 * path to you and a bright spark that flows along it (Full quality). Light quality skips the curves.
 */
object MagnaVisuals {
    private const val CYAN = 0x4DE3FF
    private const val GRAY = 0x9AA0A6
    private const val GOLD = 0xFFC83D
    private const val GREEN = 0x7CFF4D
    private const val PERSIST = 60

    private class Target(val entity: Entity, val rgb: Int, val dist: Double)

    private fun argb(alpha: Int, rgb: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    fun draw(client: Minecraft, player: LocalPlayer) {
        val s = ClientSettings.data
        val quality = s.quality
        if (!s.enabled || quality <= 0) return
        val level = client.level ?: return

        val paused = player.isShiftKeyDown
        val t = System.currentTimeMillis()
        val range = s.range.toDouble()
        val base = if (paused) GRAY else CYAN
        val center = Vec3(player.x, player.y + 0.06, player.z)

        val pulse = 0.5 + 0.5 * sin(t / 600.0)
        ring(center, range, argb((70 + 60 * pulse).toInt(), base), 2.0f, if (quality >= 2) 44 else 28)
        if (quality >= 2) {
            val phase = (t % 2600L) / 2600.0
            ring(center, range * phase, argb(((1.0 - phase) * 120).toInt(), base), 1.4f, 28)
        }
        if (paused) return

        val chest = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)
        val targets = Targets.entries[s.targets]
        val list = ArrayList<Target>()

        if (targets != Targets.XP) {
            for (e in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive }) {
                addTarget(list, e, GOLD, chest, range)
            }
        }
        if (targets != Targets.ITEMS) {
            for (e in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                addTarget(list, e, GREEN, chest, range)
            }
        }

        list.sortBy { it.dist }
        val max = if (quality >= 2) 20 else 12
        for (i in 0 until minOf(max, list.size)) drawTarget(list[i], chest, range, t, quality)
    }

    private fun addTarget(list: MutableList<Target>, e: Entity, rgb: Int, chest: Vec3, range: Double) {
        val d = e.position().distanceTo(chest)
        if (d in 0.6..range) list.add(Target(e, rgb, d))
    }

    private fun bezier(a: Vec3, c: Vec3, b: Vec3, u: Double): Vec3 {
        val v = 1.0 - u
        return Vec3(
            v * v * a.x + 2 * v * u * c.x + u * u * b.x,
            v * v * a.y + 2 * v * u * c.y + u * u * b.y,
            v * v * a.z + 2 * v * u * c.z + u * u * b.z
        )
    }

    private fun drawTarget(tg: Target, chest: Vec3, range: Double, t: Long, quality: Int) {
        val e = tg.entity
        val pos = e.position().add(0.0, 0.2, 0.0)
        val closeness = 1.0 - tg.dist / range
        val rgb = tg.rgb

        // the item itself
        Gizmos.point(pos, argb(255, rgb), (5.0 + 4.0 * closeness).toFloat()).persistForMillis(PERSIST)

        // motion tail: longer when it moves faster
        val vel = e.deltaMovement
        if (vel.lengthSqr() > 0.0009) {
            val tail = pos.subtract(vel.scale(5.0))
            Gizmos.line(tail, pos, argb((80 + 130 * closeness).toInt(), rgb), 3.0f).persistForMillis(PERSIST)
        }
        if (quality < 2) return

        // faint curved path to you
        val control = pos.add(chest).scale(0.5).add(0.0, 0.5 + tg.dist * 0.1, 0.0)
        var prev = pos
        for (i in 1..3) {
            val p = bezier(pos, control, chest, i / 3.0)
            Gizmos.line(prev, p, argb((28 + 40 * closeness).toInt(), rgb), 1.5f).persistForMillis(PERSIST)
            prev = p
        }

        // bright spark flowing along the path towards you
        val phase = ((t / 800.0) + (e.id % 13) / 13.0) % 1.0
        val a = bezier(pos, control, chest, phase)
        val b = bezier(pos, control, chest, minOf(1.0, phase + 0.16))
        Gizmos.line(a, b, argb(235, rgb), 3.5f).persistForMillis(PERSIST)
    }

    private fun ring(c: Vec3, r: Double, color: Int, width: Float, segments: Int) {
        if (r < 0.3) return
        var prev = Vec3(c.x + r, c.y, c.z)
        for (i in 1..segments) {
            val a = 2.0 * Math.PI * i / segments
            val cur = Vec3(c.x + r * cos(a), c.y, c.z + r * sin(a))
            Gizmos.line(prev, cur, color, width).persistForMillis(PERSIST)
            prev = cur
        }
    }
}
