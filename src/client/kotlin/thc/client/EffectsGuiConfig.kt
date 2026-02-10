package thc.client

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.OptionInstance
import net.minecraft.client.Options
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import java.nio.file.Files

object EffectsGuiConfig {
    private val logger = LoggerFactory.getLogger("thc")
    private const val CONFIG_FILE = "thc-effects-gui.txt"
    private const val DEFAULT_SCALE = 8

    val effectsGuiScale: OptionInstance<Int> = OptionInstance(
        "thc.options.effectsGuiScale",
        OptionInstance.noTooltip(),
        { component, integer -> Options.genericValueLabel(component, Component.literal("${integer}%")) },
        OptionInstance.IntRange(2, 20),
        DEFAULT_SCALE,
        { save() }
    )

    fun getScalePercent(): Int = effectsGuiScale.get()

    fun load() {
        try {
            val configDir = FabricLoader.getInstance().configDir
            val configPath = configDir.resolve(CONFIG_FILE)
            if (Files.exists(configPath)) {
                val line = Files.readString(configPath).trim()
                if (line.startsWith("effectsGuiScale:")) {
                    val value = line.substringAfter("effectsGuiScale:").trim().toIntOrNull()
                    if (value != null && value in 2..20) {
                        effectsGuiScale.set(value)
                        logger.info("[THC] Loaded effects GUI scale: {}%", value)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("[THC] Failed to load effects GUI config, using defaults", e)
        }
    }

    fun save() {
        try {
            val configDir = FabricLoader.getInstance().configDir
            val configPath = configDir.resolve(CONFIG_FILE)
            Files.createDirectories(configDir)
            Files.writeString(configPath, "effectsGuiScale:${effectsGuiScale.get()}\n")
        } catch (e: Exception) {
            logger.warn("[THC] Failed to save effects GUI config", e)
        }
    }
}
