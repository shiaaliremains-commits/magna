package my.magna

import java.util.UUID
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

/** Server side: pulls items and XP orbs towards players who turned the magnet on. */
object MagnetManager {
    private class Pull {
        var enabled = false
        var range = 8
        var speed = PullSpeed.NORMAL.ordinal
        var targets = Targets.BOTH.ordinal
        var synced = false
    }

    private val settings = HashMap<UUID, Pull>()

    fun init() {
        PayloadTypeRegistry.serverboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE) { payload, context ->
            receive(context.player(), payload)
        }
        ServerTickEvents.END_SERVER_TICK.register { server -> tick(server) }
        ServerLifecycleEvents.SERVER_STOPPED.register { settings.clear() }
    }

    private fun receive(player: ServerPlayer, p: SettingsPayload) {
        val s = settings.computeIfAbsent(player.uuid) { Pull() }
        val was = s.enabled
        s.enabled = p.enabled
        s.range = p.range.coerceIn(MagnaLimits.MIN_RANGE, MagnaLimits.MAX_RANGE)
        s.speed = p.speed.coerceIn(0, PullSpeed.entries.size - 1)
        s.targets = p.targets.coerceIn(0, Targets.entries.size - 1)
        if (s.synced && was != s.enabled) announce(player, s)
        s.synced = true
    }

    private fun announce(player: ServerPlayer, s: Pull) {
        val state = if (s.enabled) Component.literal("ON").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
        else Component.literal("OFF").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        val msg = Component.literal("\u26A1 Magnet ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(state)
            .append(
                Component.literal("  \u00B7  ${s.range} blocks  \u00B7  ${Targets.entries[s.targets].label}")
                    .withStyle(ChatFormatting.GRAY)
            )
        player.sendSystemMessage(msg, true)
        val level = player.serverLevel()
        level.playSound(
            null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
            0.8f, if (s.enabled) 1.5f else 0.7f
        )
    }

    private fun tick(server: MinecraftServer) {
        for (player in server.playerList.players) {
            val s = settings[player.uuid] ?: continue
            if (!s.enabled || !player.isAlive || player.isSpectator || player.isShiftKeyDown) continue
            attract(player, s)
        }
    }

    private fun attract(player: ServerPlayer, s: Pull) {
        val level = player.serverLevel()
        val range = s.range.toDouble()
        val target = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)
        val mult = PullSpeed.entries[s.speed].mult
        val targets = Targets.entries[s.targets]

        if (targets != Targets.XP) {
            val items = level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive }

            // 1. تصفير وقت المنع فوراً حتى ما ينتظر أي غرض دوره ويسحبون معاً
            for (item in items) {
                if (item.hasPickUpDelay()) {
                    item.setPickUpDelay(0)
                }
            }

            // 2. دمج كل الحبات المتشابهة المتبعثرة بالأرض في ستاك واحد عند أقرب نقطة للاعب
            mergeMatchingItems(player, items)

            // 3. سحب كل الأغراض المتبقية معاً بنفس الوقت دفعة واحدة
            for (item in items) {
                if (item.isAlive) {
                    pull(item, target, range, mult)
                }
            }
        }

        if (targets != Targets.ITEMS) {
            for (orb in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                pull(orb, target, range, mult)
            }
        }
    }

    // دمج فوري للأغراض المتطابقة المتبعثرة بالأرض (مثل الفحم أو الحديد المكسور)
    private fun mergeMatchingItems(player: ServerPlayer, items: List<ItemEntity>) {
        if (items.size < 2) return
        // فرز الأغراض بحيث الأقرب للاعب هو اللي يجمع الحبات البعيدة فيه
        val list = items.filter { it.isAlive }.sortedBy { it.distanceTo(player) }

        for (i in list.indices) {
            val primary = list[i]
            if (!primary.isAlive) continue

            val primaryStack = primary.item
            val max = primaryStack.maxStackSize
            if (primaryStack.count >= max) continue

            for (j in (i + 1) until list.size) {
                val other = list[j]
                if (!other.isAlive) continue

                val otherStack = other.item
                // إذا كان الغرضين من نفس النوع تماماً
                if (ItemStack.isSameItemSameComponents(primaryStack, otherStack)) {
                    val space = max - primaryStack.count
                    if (space <= 0) break

                    val take = minOf(space, otherStack.count)
                    primaryStack.grow(take)
                    otherStack.shrink(take)

                    // إذا فرغت الحبة المندمجة، نحذفها من العالم فوراً
                    if (otherStack.isEmpty) {
                        other.discard()
                    } else {
                        other.item = otherStack
                    }
                }
            }
            primary.item = primaryStack
        }
    }

    private fun pull(entity: Entity, target: Vec3, range: Double, mult: Double) {
        val v = MagnetMath.nextVelocity(entity.position(), entity.deltaMovement, target, range, mult) ?: return
        entity.setDeltaMovement(v)
    }
}
