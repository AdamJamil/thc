package thc.village

import net.minecraft.server.MinecraftServer

/**
 * Thread-safe server reference holder for village deregistration.
 *
 * Provides access to the server instance from contexts that don't
 * have direct server access (like Brain.setMemory in mixins).
 */
object ServerHolder {
    @Volatile
    private var currentServer: MinecraftServer? = null

    /**
     * Set the server reference. Called during SERVER_STARTED event.
     */
    fun setServer(server: MinecraftServer) {
        currentServer = server
    }

    /**
     * Get the current server instance, or null if not started.
     */
    fun getServer(): MinecraftServer? = currentServer
}
