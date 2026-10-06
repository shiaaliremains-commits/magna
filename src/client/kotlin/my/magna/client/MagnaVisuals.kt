package my.magna.client

import kotlin.math.cos
import kotlin.math.sin
import my.magna.Targets
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.Vec3

/** Draws the magnet field: a pulsing ring with two ripples and a thin glowing beam from every item to you. */
object MagnaVisuals {
    private const val MAX_BEAMS = 48
    private const val CYAN = 0x4DE3FF
    private const val GRAY = 0x9AA0A6
    private const val GOLD = 0xFFC83D
    private const val GREEN = 0x7CFF4D

    private fun argb(alpha: Int, rgb: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    fun draw(client: Minecraft, player: LocalPlayer) {
        val s = ClientSettings.data
        if (!s.enabled || !s.effects) return
        val level = client.level ?: return

        val paused = player.isShiftKeyDown
        val t = System.currentTimeMillis()
        val range = s.range.toDouble()
        val base = if (paused) GRAY else CYAN
        val center = Vec3(player.x, player.y + 0.06, player.z)

        // main ring (breathing) + two ripples that spread outwards and fade
        val pulse = 0.5 + 0.5 * sin(t / 450.0)
        ring(center, range, argb((90 + 70 * pulse).toInt(), base), 2.0f)
        for (offset in doubleArrayOf(0.0, 0.5)) {
            val phase = ((t % 2200L) / 2200.0 + offset) % 1.0
            ring(center, range * phase, argb(((1.0 - phase) * 140).toInt(), base), 1.4f)
        }

        if (paused) return

        // beams from items / XP to the player
        val chest = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)
        val targets = Targets.entries[s.targets]
        var count = 0

        if (targets != Targets.XP) {
            for (e in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive }) {
                if (count++ >= MAX_BEAMS) break
                beam(e.position(), chest, range, GOLD)
            }
        }
        if (targets != Targets.ITEMS) {
            for (e in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                if (count++ >= MAX_BEAMS) break
                beam(e.position(), chest, range, GREEN)
            }
        }
    }

    private fun beam(from: Vec3, to: Vec3, range: Double, rgb: Int) {
        val d = from.distanceTo(to)
        if (d > range || d < 0.6) return
        val closeness = 1.0 - d / range
        val alpha = (55 + 175 * closeness).toInt()
        Gizmos.line(from, to, argb(alpha, rgb), 2.0f).persistForMillis(80)
        Gizmos.point(from, argb(255, rgb), 7.0f).persistForMillis(80)
    }

    private fun ring(c: Vec3, r: Double, color: Int, width: Float) {
        if (r < 0.3) return
        val segments = (24 + r * 3).toInt().coerceIn(24, 64)
        var prev = Vec3(c.x + r, c.y, c.z)
        for (i in 1..segments) {
            val a = 2.0 * Math.PI * i / segments
            val cur = Vec3(c.x + r * cos(a), c.y, c.z + r * sin(a))
            Gizmos.line(prev, cur, color, width).persistForMillis(80)
            prev = cur
        }
    }
}
