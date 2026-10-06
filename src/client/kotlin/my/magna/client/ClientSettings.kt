package my.magna.client

import com.google.gson.GsonBuilder
import my.magna.MagnaLimits
import my.magna.MagnetSettings
import my.magna.PullSpeed
import my.magna.Targets
import net.fabricmc.loader.api.FabricLoader

/** Loads and saves the settings in config/magna.json */
object ClientSettings {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    var data = MagnetSettings()

    private fun file() = FabricLoader.getInstance().configDir.resolve("magna.json").toFile()

    fun load() {
        val f = file()
        try {
            if (f.exists()) data = gson.fromJson(f.readText(), MagnetSettings::class.java) ?: MagnetSettings()
        } catch (e: Exception) {
            data = MagnetSettings()
        }
        data.range = data.range.coerceIn(MagnaLimits.MIN_RANGE, MagnaLimits.MAX_RANGE)
        data.speed = data.speed.coerceIn(0, PullSpeed.entries.size - 1)
        data.targets = data.targets.coerceIn(0, Targets.entries.size - 1)
        data.quality = data.quality.coerceIn(0, 2)
        save()
    }

    fun save() {
        try {
            val f = file()
            f.parentFile.mkdirs()
            f.writeText(gson.toJson(data))
        } catch (e: Exception) {
            // ignore: settings just will not be remembered
        }
    }
}
