package my.magna.client

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import my.magna.Targets
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object MagnaVisuals {
    private const val PERSIST = 55
    private const val XP_COLOR = 0x55FF55

    private class Target(val entity: Entity, val rgb: Int, val dist: Double)

    private fun argb(alpha: Int, rgb: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    // لون نيون مميز لكل آيتم
    private fun getItemColor(stack: ItemStack): Int {
        val name = stack.item.toString().lowercase()
        return when {
            "diamond" in name || "lapis" in name -> 0x00FFFF
            "emerald" in name -> 0x00FF66
            "redstone" in name -> 0xFF1E27
            "gold" in name || "raw_gold" in name -> 0xFFD700
            "iron" in name || "raw_iron" in name || "quartz" in name -> 0xFFFFFF
            "copper" in name || "raw_copper" in name -> 0xFF7A33
            "netherite" in name || "ancient_debris" in name || stack.isEnchanted -> 0xC840FF
            "amethyst" in name || "ender" in name -> 0xEA55FF
            "coal" in name -> 0x555555
            else -> 0xFFB300
        }
    }

    fun draw(client: Minecraft, player: LocalPlayer) {
        val s = ClientSettings.data
        val quality = s.quality
        if (!s.enabled || quality <= 0 || player.isShiftKeyDown) return
        val level = client.level ?: return

        val range = s.range.toDouble()
        val chest = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)
        val targets = Targets.entries[s.targets]
        val list = ArrayList<Target>()

        if (targets != Targets.XP) {
            for (e in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    list.add(Target(e, getItemColor(e.item), d))
                }
            }
        }
        if (targets != Targets.ITEMS) {
            for (e in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    list.add(Target(e, XP_COLOR, d))
                }
            }
        }

        list.sortBy { it.dist }
        val max = if (quality >= 2) 24 else 14
        val t = System.currentTimeMillis()

        for (i in 0 until minOf(max, list.size)) {
            drawBillboardGlowFrame(player, list[i], range, t)
        }
    }

    private fun drawBillboardGlowFrame(player: LocalPlayer, tg: Target, range: Double, t: Long) {
        val e = tg.entity
        val closeness = (1.0 - tg.dist / range).coerceIn(0.0, 1.0)
        val rgb = tg.rgb

        val center = e.position().add(0.0, 0.20, 0.0)
        val pulse = 0.5 + 0.5 * sin((t / 180.0) + e.id)
        val radius = 0.22 + 0.03 * pulse

        val alpha = (160 + 90 * closeness).toInt().coerceIn(0, 255)
        val color = argb(alpha, rgb)

        // 1. إطار هالة دائري ناعم يواجه عينك دائماً (Billboard) ملتصق بالآيتم
        drawCameraFacingRing(player, center, radius, color, 14, 2.2f)

        // 2. إطار داخلي ناعم يعطي عمق وتوهج للآيتم
        val innerAlpha = (80 + 70 * closeness).toInt().coerceIn(0, 255)
        drawCameraFacingRing(player, center, radius * 0.65, argb(innerAlpha, rgb), 10, 1.4f)

        // 3. لمعة متوهجة في قلب الآيتم نفسه
        Gizmos.point(center, argb((200 + 55 * closeness).toInt(), rgb), (5.0 + 3.0 * closeness).toFloat())
            .apply { persistForMillis(PERSIST); setAlwaysOnTop() }
    }

    // رسم حلقة متوهجة تواجه كاميرا اللاعب تلقائياً مهما التفت
    private fun drawCameraFacingRing(
        player: LocalPlayer,
        center: Vec3,
        r: Double,
        color: Int,
        segments: Int,
        width: Float
    ) {
        val toPlayer = player.eyePosition.subtract(center).normalize()
        val up = Vec3(0.0, 1.0, 0.0)
        val right = if (abs(toPlayer.y) > 0.95) Vec3(1.0, 0.0, 0.0) else toPlayer.cross(up).normalize()
        val realUp = right.cross(toPlayer).normalize()

        var prev: Vec3? = null
        var first: Vec3? = null

        for (i in 0 until segments) {
            val a = 2.0 * Math.PI * i / segments
            val offset = right.scale(cos(a) * r).add(realUp.scale(sin(a) * r))
            val cur = center.add(offset)

            if (first == null) first = cur
            if (prev != null) {
                Gizmos.line(prev, cur, color, width).apply { persistForMillis(PERSIST); setAlwaysOnTop() }
            }
            prev = cur
        }

        if (prev != null && first != null) {
            Gizmos.line(prev, first, color, width).apply { persistForMillis(PERSIST); setAlwaysOnTop() }
        }
    }
}
