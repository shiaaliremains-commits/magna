package my.magna.client

import my.magna.Targets
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.scores.PlayerTeam

/**
 * Native Minecraft Glowing Outline directly ON the item entity silhouette!
 * Colored by Minecraft's internal team shader based on the exact item type.
 */
object MagnaVisuals {
    // تتبع الكائنات المتوهجة حالياً لتنظيفها فوراً عند خروجها من المدى
    private val glowingEntities = HashMap<Int, Pair<Entity, PlayerTeam?>>()

    // تحديد لون توهج ماينكرافت الرسمي لكل نوع آيتم
    private fun getItemChatColor(stack: ItemStack): ChatFormatting {
        val name = stack.item.toString().lowercase()
        return when {
            "diamond" in name || "lapis" in name -> ChatFormatting.AQUA
            "emerald" in name -> ChatFormatting.GREEN
            "redstone" in name -> ChatFormatting.RED
            "gold" in name || "raw_gold" in name -> ChatFormatting.YELLOW
            "iron" in name || "raw_iron" in name || "quartz" in name -> ChatFormatting.WHITE
            "copper" in name || "raw_copper" in name -> ChatFormatting.GOLD
            "netherite" in name || "ancient_debris" in name || stack.isEnchanted -> ChatFormatting.LIGHT_PURPLE
            "amethyst" in name || "ender" in name -> ChatFormatting.DARK_PURPLE
            "coal" in name -> ChatFormatting.DARK_GRAY
            else -> ChatFormatting.YELLOW
        }
    }

    private fun getOrCreateTeam(level: Level, color: ChatFormatting): PlayerTeam {
        val sb = level.scoreboard
        val teamName = "mg_" + color.name.lowercase()
        return sb.getPlayerTeam(teamName) ?: sb.addPlayerTeam(teamName).apply {
            this.color = color
        }
    }

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
        val activeIds = HashSet<Int>()

        // 1. فحص الآيتمات المسحوبة
        if (targets != Targets.XP) {
            for (e in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    activeIds.add(e.id)
                    applyGlow(level, e, getItemChatColor(e.item))
                }
            }
        }

        // 2. فحص كرات الـ XP المسحوبة
        if (targets != Targets.ITEMS) {
            for (e in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                val d = e.position().distanceTo(chest)
                if (d in 0.35..range) {
                    activeIds.add(e.id)
                    applyGlow(level, e, ChatFormatting.GREEN)
                }
            }
        }

        // 3. إزالة التوهج فوراً عن أي آيتم خرج من المدى أو تم التقاطه
        val iterator = glowingEntities.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!activeIds.contains(entry.key)) {
                val (ent, team) = entry.value
                ent.setGlowingTag(false)
                if (team != null) {
                    runCatching { level.scoreboard.removePlayerFromTeam(ent.scoreboardName, team) }
                }
                iterator.remove()
            }
        }
    }

    private fun applyGlow(level: Level, entity: Entity, color: ChatFormatting) {
        if (!glowingEntities.containsKey(entity.id)) {
            val team = getOrCreateTeam(level, color)
            runCatching { level.scoreboard.addPlayerToTeam(entity.scoreboardName, team) }
            entity.setGlowingTag(true)
            glowingEntities[entity.id] = Pair(entity, team)
        }
    }

    private fun clearAll(level: Level?) {
        if (level == null || glowingEntities.isEmpty()) return
        for ((_, pair) in glowingEntities) {
            val (ent, team) = pair
            ent.setGlowingTag(false)
            if (team != null) {
                runCatching { level.scoreboard.removePlayerFromTeam(ent.scoreboardName, team) }
            }
        }
        glowingEntities.clear()
    }
}
