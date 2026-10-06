package my.magna.client

import kotlin.math.sin
import my.magna.Targets
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack

/**
 * Clean & Dynamic Visuals:
 * - No ground circle under feet.
 * - No messy tracer lines to player.
 * - Exact bounding-box edge glow matching the REAL color of each item!
 */
object MagnaVisuals {
    private const val PERSIST = 55
    private const val XP_COLOR = 0x7CFF4D // أخضر ليموني سحري للخبرة

    private class Target(val entity: Entity, val rgb: Int, val dist: Double)

    private fun argb(alpha: Int, rgb: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    // استخراج لون التوهج الخاص بكل آيتم حسب نوعه ومعدنه
    private fun getItemColor(stack: ItemStack): Int {
        val name = stack.item.toString().lowercase()
        return when {
            "diamond" in name || "lapis" in name -> 0x00E5FF
            "emerald" in name -> 0x00FF66
            "redstone" in name -> 0xFF2222
            "gold" in name || "raw_gold" in name -> 0xFFD700
            "iron" in name || "raw_iron" in name || "quartz" in name -> 0xF0F0F0
            "copper" in name || "raw_copper" in name -> 0xFF7733
            "netherite" in name || "ancient_debris" in name || stack.isEnchanted -> 0xBA43FF
            "amethyst" in name || "ender" in name -> 0xE055FF
            "coal" in name -> 0x444444
            else -> 0xFFC83D // لون ذهبي دافئ افتراضي لباقي الموارد
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
            drawItemEdgeGlow(list[i], range, t)
        }
    }

    private fun drawItemEdgeGlow(tg: Target, range: Double, t: Long) {
        val e = tg.entity
        val closeness = (1.0 - tg.dist / range).coerceIn(0.0, 1.0)
        val rgb = tg.rgb

        // إحاطة حواف الآيتم بالملي مع نبض تنفسي خفيف جداً
        val pulse = 0.5 + 0.5 * sin((t / 220.0) + e.id)
        val box = e.boundingBox.inflate(0.02 + 0.015 * pulse)

        val strokeAlpha = (110 + 110 * closeness + 25 * pulse).toInt().coerceIn(0, 255)
        val fillAlpha = (25 + 35 * closeness).toInt().coerceIn(0, 255)

        // 1. رسم التوهج على حواف وزوايا الآيتم بلونه الخاص
        Gizmos.cuboid(box, net.minecraft.gizmos.GizmoStyle.stroke(argb(strokeAlpha, rgb))).persistForMillis(PERSIST)

        // 2. تعبئة شفافة ناعمة جداً داخل جسم الآيتم
        Gizmos.cuboid(box, net.minecraft.gizmos.GizmoStyle.fill(argb(fillAlpha, rgb))).persistForMillis(PERSIST)

        // 3. لمعة خفيفة في سنتر الآيتم
        Gizmos.point(box.center, argb(strokeAlpha, rgb), (3.5 + 3.0 * closeness).toFloat()).persistForMillis(PERSIST)
    }
}
