package my.magna.client

import my.magna.Targets
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity

object MagnaVisuals {
    // تتبع الكائنات المتوهجة حالياً لإلغاء التوهج فوراً عند خروجها من المدى
    private val glowingEntities = HashSet<Int>()

    fun draw(client: Minecraft, player: LocalPlayer) {
        val s = ClientSettings.data
        val level = client.level

        // إذا المغناطيس مطفأ أو اللاعب مسوي سنيك، نظف كل التوهج فوراً
        if (!s.enabled || s.quality <= 0 || player.isShiftKeyDown || level == null) {
            clearAll(level)
            return
        }

        val range = s.range.toDouble()
        val chest = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)
        val targets = Targets.entries[s.targets]
        val activeEntities = ArrayList<Entity>()

        // 1. فحص الآيتمات المسحوبة
        if (targets != Targets.XP) {
            for (e in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    activeEntities.add(e)
                }
            }
        }

        // 2. فحص كرات الـ XP المسحوبة
        if (targets != Targets.ITEMS) {
            for (e in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    activeEntities.add(e)
                }
            }
        }

        val activeIds = HashSet<Int>()

        // تفعيل التوهج المباشر على مجسم الآيتم نفسه
        for (e in activeEntities) {
            activeIds.add(e.id)
            if (!glowingEntities.contains(e.id)) {
                e.setGlowingTag(true)
                glowingEntities.add(e.id)
            }
        }

        // إزالة التوهج عن أي كائن يخرج من المدى أو يتم التقاطه
        val iterator = glowingEntities.iterator()
        while (iterator.hasNext()) {
            val id = iterator.next()
            if (!activeIds.contains(id)) {
                val ent = level.getEntity(id)
                ent?.setGlowingTag(false)
                iterator.remove()
            }
        }
    }

    private fun clearAll(level: net.minecraft.world.level.Level?) {
        if (level == null || glowingEntities.isEmpty()) return
        for (id in glowingEntities) {
            level.getEntity(id)?.setGlowingTag(false)
        }
        glowingEntities.clear()
    }
}
