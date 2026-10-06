package my.magna

import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Magna : ModInitializer {
    const val MOD_ID = "magna"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        MagnetManager.init()
        LOGGER.info("Magna loaded.")
    }
}
