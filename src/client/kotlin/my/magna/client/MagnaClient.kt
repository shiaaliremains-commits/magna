package my.magna.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

object MagnaClient : ClientModInitializer {
    private const val MOD_ID = "magna"
    private lateinit var toggleKey: KeyMapping
    private var enabled = true

    override fun onInitializeClient() {
        val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))
        // زر حرف M لتفعيل/تعطيل المغناطيس
        toggleKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping("key.magna.toggle", InputConstants.KEY_M, category)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register

            while (toggleKey.consumeClick()) {
                enabled = !enabled
                val status = if (enabled) Component.literal("ON").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                else Component.literal("OFF").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                player.sendSystemMessage(
                    Component.literal("[Magna] ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                        .append(Component.literal("Magnet: ").withStyle(ChatFormatting.WHITE))
                        .append(status)
                )
            }
        }
    }
}
