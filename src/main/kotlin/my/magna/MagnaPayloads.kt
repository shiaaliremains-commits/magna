package my.magna

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/** client -> server: the magnet settings of this player */
class SettingsPayload(val enabled: Boolean, val range: Int, val speed: Int, val targets: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<SettingsPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Magna.MOD_ID, "settings"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> =
            StreamCodec.composite<RegistryFriendlyByteBuf, SettingsPayload, Boolean, Int, Int, Int>(
                ByteBufCodecs.BOOL, { p: SettingsPayload -> p.enabled },
                ByteBufCodecs.INT, { p: SettingsPayload -> p.range },
                ByteBufCodecs.INT, { p: SettingsPayload -> p.speed },
                ByteBufCodecs.INT, { p: SettingsPayload -> p.targets },
                { a: Boolean, b: Int, c: Int, d: Int -> SettingsPayload(a, b, c, d) }
            )
    }
}
