package my.magna

import kotlin.math.sqrt
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object Magna : ModInitializer {
    const val MOD_ID = "magna"
    const val MAX_RANGE = 12.0 // مدى الجذب بالمكعبات

    override fun onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerList.players) {
                // يتوقف السحب تلقائياً إذا اللاعب مسوي سنيك (حتى يقدر يرمي أغراض لربعه)
                if (player.isSpectator || player.isShiftKeyDown) continue
                handleAttraction(player)
            }
        }
    }

    private fun handleAttraction(player: ServerPlayer) {
        val level = player.level() as? ServerLevel ?: return
        val playerPos = player.eyePosition.subtract(0.0, 0.4, 0.0) // مركز الصدر
        val box = player.boundingBox.inflate(MAX_RANGE)

        // 1. جذب الأغراض والبلوكات الطايحة (ItemEntity)
        val items = level.getEntitiesOfClass(ItemEntity::class.java, box) {
            it.isAlive && it.distanceTo(player) <= MAX_RANGE
        }

        for (item in items) {
            // تصفير وقت المنع حتى يدخل بجيبك فوراً
            item.setPickUpDelay(0)
            pullEntity(level, item.position(), playerPos, item::setDeltaMovement)

            // توليد ذيل بارتكلز سحري وراء الغرض
            if (level.random.nextFloat() < 0.35f) {
                level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    item.x, item.y + 0.15, item.z,
                    1, 0.02, 0.02, 0.02, 0.01
                )
            }
        }

        // 2. جذب كرات الخبرة (ExperienceOrb)
        val orbs = level.getEntitiesOfClass(ExperienceOrb::class.java, box) {
            it.isAlive && it.distanceTo(player) <= MAX_RANGE
        }

        for (orb in orbs) {
            pullEntity(level, orb.position(), playerPos, orb::setDeltaMovement)
        }
    }

    // معادلة الجذب المنحني والتسارع الفيزيائي
    private fun pullEntity(
        level: ServerLevel,
        currentPos: Vec3,
        targetPos: Vec3,
        applyMovement: (Vec3) -> Unit
    ) {
        val delta = targetPos.subtract(currentPos)
        val dist = delta.length()
        if (dist < 0.35) return

        val direction = delta.normalize()

        // منحنى تسارع انسيابي: بطيء بالبعد (0.15) وسريع جداً بالقرب (0.85)
        val progress = (1.0 - (dist / MAX_RANGE)).coerceIn(0.0, 1.0)
        val speed = 0.15 + (0.70 * progress * progress)

        // حركة طفو للأعلى خفيفة لمقاومة الجاذبية والسحب بأناقة
        val lift = if (dist > 1.5) 0.08 * (1.0 - progress) else 0.0
        val velocity = Vec3(
            direction.x * speed,
            (direction.y * speed) + lift,
            direction.z * speed
        )

        applyMovement(velocity)
    }
}
