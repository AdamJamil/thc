package thc.client

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.OptionInstance
import net.minecraft.client.Options
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import java.nio.file.Files

object MobHealthBarConfig {
    private val logger = LoggerFactory.getLogger("thc")
    private const val CONFIG_FILE = "thc-mob-health-bar.txt"
    private const val DEFAULT_SCALE = 6

    val mobHealthBarScale: OptionInstance<Int> = OptionInstance(
        "thc.options.mobHealthBarScale",
        OptionInstance.noTooltip(),
        { component, integer -> Options.genericValueLabel(component, Component.literal("${integer}%")) },
        OptionInstance.IntRange(2, 20),
        DEFAULT_SCALE,
        { save() }
    )

    fun getScalePercent(): Int = mobHealthBarScale.get()

    fun load() {
        try {
            val configDir = FabricLoader.getInstance().configDir
            val configPath = configDir.resolve(CONFIG_FILE)
            if (Files.exists(configPath)) {
                val line = Files.readString(configPath).trim()
                if (line.startsWith("mobHealthBarScale:")) {
                    val value = line.substringAfter("mobHealthBarScale:").trim().toIntOrNull()
                    if (value != null && value in 2..20) {
                        mobHealthBarScale.set(value)
                        logger.info("[THC] Loaded mob health bar scale: {}%", value)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("[THC] Failed to load mob health bar config, using defaults", e)
        }
    }

    fun save() {
        try {
            val configDir = FabricLoader.getInstance().configDir
            val configPath = configDir.resolve(CONFIG_FILE)
            Files.createDirectories(configDir)
            Files.writeString(configPath, "mobHealthBarScale:${mobHealthBarScale.get()}\n")
        } catch (e: Exception) {
            logger.warn("[THC] Failed to save mob health bar config", e)
        }
    }
}
