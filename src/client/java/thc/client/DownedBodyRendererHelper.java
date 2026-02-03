package thc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Helper class to handle entity rendering with proper generics.
 * Works around Kotlin's star projection limitations.
 */
public final class DownedBodyRendererHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("thc.DownedBodyRendererHelper");
    private static boolean submitMethodChecked = false;
    private static Method submitMethod = null;

    private DownedBodyRendererHelper() {}

    /**
     * Renders a DummyDownedPlayer entity using the entity dispatcher.
     * Uses the new MC 1.21+ rendering system with SubmitNodeCollector.
     *
     * @param dummy The dummy player to render
     * @param poseStack The pose stack for transformations
     * @param context The world render context (provides collector and camera state)
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

        // Use raw type to avoid generic issues
        EntityRenderer renderer = dispatcher.getRenderer(dummy);

        if (renderer == null) {
            LOGGER.warn("No renderer found for DummyDownedPlayer");
            return;
        }

        // Get render infrastructure from context
        SubmitNodeCollector collector = context.commandQueue();
        var worldState = context.worldState();

        // Create render state and extract from entity
        EntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(dummy, state, partialTick);

        // Try to find and call the submit method using reflection to discover signature
        try {
            if (!submitMethodChecked) {
                submitMethodChecked = true;
                // Log available submit methods for debugging
                for (Method m : renderer.getClass().getMethods()) {
                    if (m.getName().equals("submit")) {
                        LOGGER.info("Found submit method: {} with {} params: {}",
                            m.getReturnType().getSimpleName(),
                            m.getParameterCount(),
                            java.util.Arrays.toString(m.getParameterTypes()));
                    }
                }
            }

            // Try calling submit with different signatures
            // First try: state, poseStack, collector (3 args)
            try {
                var submitMethod3 = renderer.getClass().getMethod("submit",
                    EntityRenderState.class, PoseStack.class, SubmitNodeCollector.class);
                submitMethod3.invoke(renderer, state, poseStack, collector);
                return;
            } catch (NoSuchMethodException e) {
                // Try other signatures
            }

            // If 3-arg didn't work, try finding a 4-arg version with reflection
            for (Method m : renderer.getClass().getMethods()) {
                if (m.getName().equals("submit") && m.getParameterCount() == 4) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params[0].isAssignableFrom(state.getClass()) &&
                        params[1].isAssignableFrom(PoseStack.class) &&
                        params[2].isAssignableFrom(SubmitNodeCollector.class)) {
                        // Found a 4-arg submit, try to get the 4th param from worldState
                        Object fourthArg = null;
                        for (var field : worldState.getClass().getFields()) {
                            if (params[3].isAssignableFrom(field.getType())) {
                                fourthArg = field.get(worldState);
                                break;
                            }
                        }
                        if (fourthArg != null) {
                            m.invoke(renderer, state, poseStack, collector, fourthArg);
                            return;
                        }
                    }
                }
            }

            LOGGER.warn("Could not find suitable submit method for renderer {}", renderer.getClass().getName());

        } catch (Exception e) {
            LOGGER.error("Error calling submit on renderer", e);
        }
    }
}
