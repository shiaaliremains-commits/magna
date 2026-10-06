package my.magna.client

import com.mojang.blaze3d.platform.InputConstants
import my.magna.Magna
import my.magna.SettingsPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.Identifier

object MagnaClient : ClientModInitializer {
    private lateinit var toggleKey: KeyMapping
    private lateinit var settingsKey: KeyMapping
    private var synced = false

    override fun onInitializeClient() {
        ClientSettings.load()

        val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Magna.MOD_ID, "main"))
        toggleKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping("key.magna.toggle", InputConstants.KEY_M, category)
        )
        settingsKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping("key.magna.settings", InputConstants.KEY_N, category)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player
            if (player == null) {
                synced = false
                return@register
            }
            if (!synced) {
                pushSettings()
                synced = true
            }

            while (toggleKey.consumeClick()) {
                ClientSettings.data.enabled = !ClientSettings.data.enabled
                ClientSettings.save()
                pushSettings()
            }
            while (settingsKey.consumeClick()) {
                openScreen(MagnaScreen())
            }

            MagnaPrediction.tick(client, player)
            MagnaVisuals.draw(client, player)
        }
    }

    /** sends the settings to the server so it knows how to pull */
    fun pushSettings() {
        val d = ClientSettings.data
        runCatching { ClientPlayNetworking.send(SettingsPayload(d.enabled, d.range, d.speed, d.targets)) }
    }

    /** the screen-opening method moved between versions (Minecraft.setScreen or gui.setScreen), so find it by name */
    fun openScreen(screen: Screen?) {
        val mc = Minecraft.getInstance()
        val gui = runCatching { mc.javaClass.getField("gui").get(mc) }.getOrNull()
        if (!invokeSetScreen(gui, screen)) invokeSetScreen(mc, screen)
    }

    private fun invokeSetScreen(target: Any?, screen: Screen?): Boolean {
        if (target == null) return false
        val method = target.javaClass.methods.firstOrNull {
            (it.name == "setScreen" || it.name == "setScreenAndShow") && it.parameterCount == 1
        } ?: return false
        method.invoke(target, screen)
        return true
    }
}
