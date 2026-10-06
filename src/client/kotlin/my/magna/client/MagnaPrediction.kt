package my.magna.client

import my.magna.MagnetMath
import my.magna.PullSpeed
import my.magna.SettingsPayload
import my.magna.Targets
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.Vec3

/** Runs the same pull maths on your own screen, so the motion stays smooth between the server's updates. */
object MagnaPrediction {
    fun tick(client: Minecraft, player: LocalPlayer) {
        val s = ClientSettings.data
        if (!s.enabled || player.isShiftKeyDown || player.isSpectator) return
        // only when the server has this mod too, otherwise the items would snap back
        if (!runCatching { ClientPlayNetworking.canSend(SettingsPayload.TYPE) }.getOrDefault(true)) return

        val level = client.level ?: return
        val range = s.range.toDouble()
        val mult = PullSpeed.entries[s.speed].mult
        val targets = Targets.entries[s.targets]
        val target = player.eyePosition.subtract(0.0, 0.4, 0.0)
        val box = player.boundingBox.inflate(range)

        if (targets != Targets.XP) {
            // items younger than 2 seconds may still have a pickup delay the client cannot see
            for (item in level.getEntitiesOfClass(ItemEntity::class.java, box) { it.isAlive && it.tickCount > 40 }) {
                push(item, target, range, mult)
            }
        }
        if (targets != Targets.ITEMS) {
            for (orb in level.getEntitiesOfClass(ExperienceOrb::class.java, box) { it.isAlive }) {
                push(orb, target, range, mult)
            }
        }
    }

    private fun push(e: Entity, target: Vec3, range: Double, mult: Double) {
        val v = MagnetMath.nextVelocity(e.position(), e.deltaMovement, target, range, mult) ?: return
        e.setDeltaMovement(v)
    }
}
