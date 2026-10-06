package my.magna.client

import my.magna.MagnaLimits
import my.magna.MagnetSettings
import my.magna.PullSpeed
import my.magna.Targets
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Settings screen: opens with the N key. Every change is saved and sent to the server right away. */
class MagnaScreen : Screen(Component.literal("Magna")) {
    private lateinit var toggleBtn: Button
    private lateinit var rangeBtn: Button
    private lateinit var speedBtn: Button
    private lateinit var targetsBtn: Button
    private lateinit var effectsBtn: Button

    private val qualityNames = listOf("Off", "Light", "Full")

    private val d: MagnetSettings get() = ClientSettings.data

    private fun onOff(on: Boolean): Component =
        if (on) Component.literal("ON").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
        else Component.literal("OFF").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)

    private fun refresh() {
        toggleBtn.message = Component.literal("Magnet: ").append(onOff(d.enabled))
        rangeBtn.message = Component.literal("Range: ${d.range} blocks")
        speedBtn.message = Component.literal("Pull speed: ${PullSpeed.entries[d.speed].label}")
        targetsBtn.message = Component.literal("Collect: ${Targets.entries[d.targets].label}")
        effectsBtn.message = Component.literal("Effects: ${qualityNames[d.quality]}")
    }

    private fun commit() {
        ClientSettings.save()
        MagnaClient.pushSettings()
        refresh()
    }

    override fun init() {
        val w = 220
        val left = width / 2 - w / 2
        var y = maxOf(6, height / 2 - 84)

        val header = Button.builder(
            Component.literal("Magna  \u2022  Item Magnet").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ) { _ -> }.bounds(left, y, w, 20).build()
        header.active = false
        addRenderableWidget(header)
        y += 28

        toggleBtn = Button.builder(Component.empty()) { _ ->
            d.enabled = !d.enabled
            commit()
        }.bounds(left, y, w, 20).build()
        addRenderableWidget(toggleBtn)
        y += 24

        val minus = Button.builder(Component.literal("-")) { _ ->
            d.range = (d.range - 1).coerceAtLeast(MagnaLimits.MIN_RANGE)
            commit()
        }.bounds(left, y, 20, 20).build()
        rangeBtn = Button.builder(Component.empty()) { _ -> }.bounds(left + 24, y, w - 48, 20).build()
        val plus = Button.builder(Component.literal("+")) { _ ->
            d.range = (d.range + 1).coerceAtMost(MagnaLimits.MAX_RANGE)
            commit()
        }.bounds(left + w - 20, y, 20, 20).build()
        addRenderableWidget(minus)
        addRenderableWidget(rangeBtn)
        addRenderableWidget(plus)
        y += 24

        speedBtn = Button.builder(Component.empty()) { _ ->
            d.speed = (d.speed + 1) % PullSpeed.entries.size
            commit()
        }.bounds(left, y, w, 20).build()
        addRenderableWidget(speedBtn)
        y += 24

        targetsBtn = Button.builder(Component.empty()) { _ ->
            d.targets = (d.targets + 1) % Targets.entries.size
            commit()
        }.bounds(left, y, w, 20).build()
        addRenderableWidget(targetsBtn)
        y += 24

        effectsBtn = Button.builder(Component.empty()) { _ ->
            d.quality = (d.quality + 1) % 3
            commit()
        }.bounds(left, y, w, 20).build()
        addRenderableWidget(effectsBtn)
        y += 30

        addRenderableWidget(
            Button.builder(Component.literal("Done")) { _ -> onClose() }.bounds(left, y, w, 20).build()
        )

        refresh()
    }
}
