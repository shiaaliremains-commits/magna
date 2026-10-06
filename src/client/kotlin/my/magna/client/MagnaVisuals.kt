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
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

/**
 * Clean & Dynamic Visuals:
 * - No 3D boxes / cubes.
 * - Glowing cross-halo frame fitted tightly around the item silhouette.
 * - Vivid high-emissive neon colors that stay 100% visible in day and night!
 */
object MagnaVisuals {
    private const val PERSIST = 55
    private const val XP_COLOR = 0x55FF55

    private class Target(val entity: Entity, val rgb: Int, val dist: Double)

    private fun argb(alpha: Int, rgb: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    // ألوان نيون ساطعة ومميزة لكل نوع آيتم لتبقى واضحة في الشمس والعتمة
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
                    val color = getItemColor(e.item)
                    list.add(Target(e, color, d))
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
            drawItemGlowFrame(list[i], range, t)
        }
    }

    // رسم هالة توهج ناعمة تلتف حول شكل الغرض بدلاً من الصندوق المربع
    private fun drawItemGlowFrame(tg: Target, range: Double, t: Long) {
        val e = tg.entity
        val closeness = (1.0 - tg.dist / range).coerceIn(0.0, 1.0)
        val rgb = tg.rgb

        // مركز الغرض بدقة
        val center = e.position().add(0.0, 0.18, 0.0)

        // نبض تنفسي خفيف في حجم وشفافية التوهج
        val pulse = 0.5 + 0.5 * sin((t / 200.0) + e.id)
        val radius = 0.22 + 0.04 * pulse

        // شفافية ساطعة وواضحة جداً في ضوء النهار والعتمة
        val lineAlpha = (140 + 100 * closeness + 15 * pulse).toInt().coerceIn(0, 255)
        val color = argb(lineAlpha, rgb)

        // 1. حلقة توهج أفقية ناعمة تحيط بخصر الآيتم
        drawCircle(center, radius, color, isHorizontal = true)

        // 2. حلقة توهج عمودية متقاطعة تعطي شعور هالة ثلاثية الأبعاد بدون زوايا مربعة
        drawCircle(center, radius, color, isHorizontal = false)

        // 3. لمعة متوهجة ناعمة وواضحة جداً في مركز الآيتم نفسه
        Gizmos.point(center, argb((180 + 75 * closeness).toInt(), rgb), (4.5 + 3.0 * closeness).toFloat())
            .apply { persistForMillis(PERSIST); setAlwaysOnTop() }
    }

    private fun drawCircle(c: Vec3, r: Double, color: Int, isHorizontal: Boolean) {
        val segments = 14
        var prev = if (isHorizontal) Vec3(c.x + r, c.y, c.z) else Vec3(c.x, c.y + r, c.z)

        for (i in 1..segments) {
            val a = 2.0 * Math.PI * i / segments
            val cur = if (isHorizontal) {
                Vec3(c.x + r * cos(a), c.y, c.z + r * sin(a))
            } else {
                Vec3(c.x, c.y + r * cos(a), c.z + r * sin(a))
            }
            Gizmos.line(prev, cur, color, 2.0f).apply { persistForMillis(PERSIST); setAlwaysOnTop() }
            prev = cur
        }
    }
}
