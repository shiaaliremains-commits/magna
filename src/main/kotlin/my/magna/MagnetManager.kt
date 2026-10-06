package my.magna

import java.util.UUID
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
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
        val level = player.level() as? ServerLevel
        level?.playSound(
            null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
            0.8f, if (s.enabled) 1.5f else 0.7f
        )
    }

    private fun tick(server: MinecraftServer) {
        for (player in server.playerList.players) {
            val s = settings[player.uuid] ?: continue
            // sneaking pauses the magnet, so you can still drop things for friends
            if (!s.enabled || !player.isAlive || player.isSpectator || player.isShiftKeyDown) continue
            attract(player, s)
        }
    }

    private fun attract(player: ServerPlayer, s: Pull) {
        val level = player.level() as? ServerLevel ?: return
        val range = s.range.toDouble()
        val target = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)
        val mult = PullSpeed.entries[s.speed].mult
        val targets = Targets.entries[s.targets]

        if (targets != Targets.XP) {
            // items with a pickup delay (like the ones you just dropped) are left alone
            for (item in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive && !it.hasPickUpDelay() }) {
                pull(item, target, range, mult)
            }
        }
        if (targets != Targets.ITEMS) {
            for (orb in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                pull(orb, target, range, mult)
            }
        }
    }

    /** slow when far, fast when close, with a little lift so things glide instead of scraping the ground */
    private fun pull(entity: Entity, target: Vec3, range: Double, mult: Double) {
        val delta = target.subtract(entity.position())
        val dist = delta.length()
        if (dist > range || dist < 0.4) return

        val dir = delta.scale(1.0 / dist)
        val closeness = (1.0 - dist / range).coerceIn(0.0, 1.0)
        val speed = (0.12 + 0.55 * closeness * closeness) * mult
        val lift = if (dist > 1.5) 0.05 * (1.0 - closeness) else 0.0
        entity.setDeltaMovement(Vec3(dir.x * speed, dir.y * speed + lift, dir.z * speed))
    }
}
