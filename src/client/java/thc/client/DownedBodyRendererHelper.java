package thc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Helper class to handle entity rendering with proper generics.
 * Works around Kotlin's star projection limitations.
 */
public final class DownedBodyRendererHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("thc.DownedBodyRendererHelper");
    private static Field cameraStateField = null;
    private static boolean fieldSearched = false;

    private DownedBodyRendererHelper() {}

    /**
     * Renders a DummyDownedPlayer entity using the entity renderer.
     * Uses the new MC 1.21+ render state system with SubmitNodeCollector.
     *
     * @param dummy The dummy player to render
     * @param poseStack The pose stack for transformations
     * @param context The world render context
     * @param partialTick The partial tick for interpolation
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void renderDummy(
            DummyDownedPlayer dummy,
            PoseStack poseStack,
            WorldRenderContext context,
            float partialTick
    ) {
        Minecraft client = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        // Get the renderer for this entity type
        EntityRenderer renderer = dispatcher.getRenderer(dummy);
        if (renderer == null) {
            LOGGER.warn("No renderer found for DummyDownedPlayer");
            return;
        }

        // Get render infrastructure from context
        SubmitNodeCollector collector = context.commandQueue();
        var worldState = context.worldState();

        // Find camera state field on worldState
        CameraRenderState cameraState = findCameraState(worldState);
        if (cameraState == null) {
            LOGGER.warn("Could not find camera state on world state");
            return;
        }

        // Create and populate render state
        EntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(dummy, state, partialTick);

        // Submit for rendering
        renderer.submit(state, poseStack, collector, cameraState);
    }

    private static CameraRenderState findCameraState(Object worldState) {
        if (!fieldSearched) {
            fieldSearched = true;
            // Search for a field of type CameraRenderState
            for (Field f : worldState.getClass().getFields()) {
                if (CameraRenderState.class.isAssignableFrom(f.getType())) {
                    LOGGER.info("Found camera state field: {} of type {}", f.getName(), f.getType().getSimpleName());
                    cameraStateField = f;
                    break;
                }
            }
            if (cameraStateField == null) {
                // Log all fields for debugging
                LOGGER.info("No CameraRenderState field found. Available fields on {}:", worldState.getClass().getSimpleName());
                for (Field f : worldState.getClass().getFields()) {
                    LOGGER.info("  {} : {}", f.getName(), f.getType().getSimpleName());
                }
            }
        }

        if (cameraStateField != null) {
            try {
                return (CameraRenderState) cameraStateField.get(worldState);
            } catch (IllegalAccessException e) {
                LOGGER.error("Failed to access camera state field", e);
            }
        }
        return null;
    }
}
