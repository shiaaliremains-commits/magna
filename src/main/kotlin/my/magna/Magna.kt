package my.magna

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object Magna : ModInitializer {
    const val MOD_ID = "magna"
    const val MAX_RANGE = 10.0 // قصرنا المدى لـ 10 بلوكات

    override fun onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerList.players) {
                // يتوقف الجذب تلقائياً عند السنيك
                if (player.isSpectator || player.isShiftKeyDown) continue
                handleAttraction(player)
            }
        }
    }

    // فحص ما إذا كان لدى اللاعب مساحة بالحقيبة للغرض
    private fun canHoldItem(player: ServerPlayer, itemEntity: ItemEntity): Boolean {
        if (player.isCreative) return true
        val stack = itemEntity.item
        val inv = player.inventory

        for (i in 0 until 36) {
            val slot = inv.getItem(i)
            if (slot.isEmpty) return true
            if (slot.item == stack.item && slot.count < slot.maxStackSize) {
                return true
            }
        }
        return false
    }

    private fun handleAttraction(player: ServerPlayer) {
        val level = player.level() as? ServerLevel ?: return
        val playerPos = player.eyePosition.subtract(0.0, 0.45, 0.0) // مركز الصدر
        val box = player.boundingBox.inflate(MAX_RANGE)

        // 1. جذب الأغراض
        val items = level.getEntitiesOfClass(ItemEntity::class.java, box) {
            it.isAlive && it.distanceTo(player) <= MAX_RANGE && canHoldItem(player, it)
        }

        for (item in items) {
            item.setPickUpDelay(0)
            item.noPhysics = true
            pullEntitySmooth(item.position(), playerPos, item.deltaMovement, item::setDeltaMovement)

            // بارتكلز GLOW ناعمة وهادئة
            if (level.random.nextFloat() < 0.25f) {
                level.sendParticles(
                    ParticleTypes.GLOW,
                    item.x, item.y + 0.2, item.z,
                    1, 0.03, 0.03, 0.03, 0.01
                )
            }
        }

        // 2. جذب كرات الخبرة (XP)
        val orbs = level.getEntitiesOfClass(ExperienceOrb::class.java, box) {
            it.isAlive && it.distanceTo(player) <= MAX_RANGE
        }

        for (orb in orbs) {
            pullEntitySmooth(orb.position(), playerPos, orb.deltaMovement, orb::setDeltaMovement)
        }
    }

    // معادلة جذب فيزيائية بطيئة وانسيابية مع عزم دوران ومنحنى طفو
    private fun pullEntitySmooth(
        currentPos: Vec3,
        targetPos: Vec3,
        currentVel: Vec3,
        applyMovement: (Vec3) -> Unit
    ) {
        val delta = targetPos.subtract(currentPos)
        val dist = delta.length()
        if (dist < 0.35) return

        val direction = delta.normalize()

        // منحنى تسارع هادئ وبطيء: يبدأ بـ 0.08 ويتسارع تدريجياً لـ 0.28 كحد أقصى
        val progress = (1.0 - (dist / MAX_RANGE)).coerceIn(0.0, 1.0)
        val targetSpeed = 0.08 + (0.20 * progress * progress)

        // طفو ناعم للأعلى لمقاومة الجاذبية وإعطاء شعور انعدام الوزن
        val lift = if (dist > 1.2) 0.035 * (1.0 - progress) else 0.0

        val targetVel = Vec3(
            direction.x * targetSpeed,
            (direction.y * targetSpeed) + lift,
            direction.z * targetSpeed
        )

        // دمج السرعة الحالية مع الجديدة لصنع حركة منحنية ومرنة (Curved Momentum)
        val blendedVel = currentVel.scale(0.55).add(targetVel.scale(0.45))

        applyMovement(blendedVel)
    }
}
