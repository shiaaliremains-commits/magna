package my.magna.client

import my.magna.Targets
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity

/**
 * Pure & Clean Aesthetics:
 * Absolutely NO ugly wire rings, circles, or boxes!
 * Just a smooth, soft, magical in-game particle glow aura drifting around the item.
 */
object MagnaVisuals {

    fun draw(client: Minecraft, player: LocalPlayer) {
        val s = ClientSettings.data
        if (!s.enabled || s.quality <= 0 || player.isShiftKeyDown) return
        val level = client.level ?: return

        val range = s.range.toDouble()
        val chest = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)
        val targets = Targets.entries[s.targets]
        val activeEntities = ArrayList<Entity>()

        if (targets != Targets.XP) {
            for (e in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    activeEntities.add(e)
                }
            }
        }
        if (targets != Targets.ITEMS) {
            for (e in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    activeEntities.add(e)
                }
            }
        }

        // إطلاق هالة توهج ناعمة وطبيعية تحيط بجسم الآيتم نفسه
        for (e in activeEntities) {
            spawnItemGlowParticles(level, e)
        }
    }

    private fun spawnItemGlowParticles(level: net.minecraft.client.multiplayer.ClientLevel, e: Entity) {
        val pos = e.position().add(0.0, 0.18, 0.0)
        val rand = level.random

        // 1. نجوم مضيئة ناعمة (GLOW) تدور وتتلألأ حول مجسم الآيتم
        if (rand.nextFloat() < 0.45f) {
            val ox = (rand.nextDouble() - 0.5) * 0.28
            val oy = (rand.nextDouble() - 0.5) * 0.28
            val oz = (rand.nextDouble() - 0.5) * 0.28
            level.addParticle(
                ParticleTypes.GLOW,
                pos.x + ox, pos.y + oy, pos.z + oz,
                0.0, 0.01, 0.0
            )
        }

        // 2. هالة سحرية ناعمة للأغراض النادرة والمطورة (مثل الميس والنيذرايت)
        if (e is ItemEntity) {
            val name = e.item.item.toString().lowercase()
            if ("mace" in name || "netherite" in name || e.item.isEnchanted) {
                if (rand.nextFloat() < 0.30f) {
                    val ox = (rand.nextDouble() - 0.5) * 0.22
                    val oz = (rand.nextDouble() - 0.5) * 0.22
                    level.addParticle(
                        ParticleTypes.ENCHANT,
                        pos.x + ox, pos.y, pos.z + oz,
                        (rand.nextDouble() - 0.5) * 0.1, 0.05, (rand.nextDouble() - 0.5) * 0.1
                    )
                }
            }
        }
    }
}
