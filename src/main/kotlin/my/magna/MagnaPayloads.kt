package my.magna

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class SettingsPayload(
    val enabled: Boolean,
    val range: Int,
    val speed: Int,
    val targets: Int,
    val whitelist: Boolean,
    val filter: List<String>
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<SettingsPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Magna.MOD_ID, "settings"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> =
            StreamCodec.composite<RegistryFriendlyByteBuf, SettingsPayload, Boolean, Int, Int, Int, Boolean, List<String>>(
                ByteBufCodecs.BOOL, { p: SettingsPayload -> p.enabled },
                ByteBufCodecs.INT, { p: SettingsPayload -> p.range },
                ByteBufCodecs.INT, { p: SettingsPayload -> p.speed },
                ByteBufCodecs.INT, { p: SettingsPayload -> p.targets },
                ByteBufCodecs.BOOL, { p: SettingsPayload -> p.whitelist },
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), { p: SettingsPayload -> p.filter },
                { a, b, c, d, e, f -> SettingsPayload(a, b, c, d, e, f) }
            )
    }
}
