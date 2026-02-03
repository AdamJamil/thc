package thc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Helper class to render beacon-style beams at arbitrary world positions.
 * Used to make downed players visible from great distances.
 */
public final class BeaconBeamHelper {
    private static final Identifier BEAM_TEXTURE = Identifier.withDefaultNamespace("textures/entity/beacon_beam.png");

    // Beam dimensions
    private static final float INNER_BEAM_RADIUS = 0.1f;
    private static final float OUTER_GLOW_RADIUS = 0.25f;
    private static final int BEAM_HEIGHT = 256;  // Blocks tall

    private BeaconBeamHelper() {}

    /**
     * Renders a beacon-style beam at the given world position.
     *
     * @param stack The pose stack for transformations (already translated relative to camera)
     * @param bufferSource Buffer source for getting vertex consumers
     * @param x World X position (relative to camera)
     * @param y World Y position (relative to camera)
     * @param z World Z position (relative to camera)
     * @param color ARGB color (e.g., 0xFFFF0000 for red)
     * @param gameTime Current game time for animation
     */
    public static void renderBeam(
            PoseStack stack,
            MultiBufferSource bufferSource,
            double x, double y, double z,
            int color,
            long gameTime
    ) {
        float red = ARGB.redFloat(color);
        float green = ARGB.greenFloat(color);
        float blue = ARGB.blueFloat(color);

        stack.pushPose();
        stack.translate(x, y, z);

        // Animated UV offset for shimmer effect
        float time = (float)(gameTime % 40L) + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
        float uvOffset = -time * 0.025f;  // Scroll speed

        // Inner beam (opaque)
        renderBeamLayer(
            stack, bufferSource,
            INNER_BEAM_RADIUS,
            0, BEAM_HEIGHT,
            red, green, blue, 1.0f,
            uvOffset, false
        );

        // Outer glow (translucent)
        renderBeamLayer(
            stack, bufferSource,
            OUTER_GLOW_RADIUS,
            0, BEAM_HEIGHT,
            red, green, blue, 0.25f,
            uvOffset, true
        );

        stack.popPose();
    }

    /**
     * Renders one layer of the beacon beam (either inner solid or outer glow).
     */
    private static void renderBeamLayer(
            PoseStack stack,
            MultiBufferSource bufferSource,
            float radius,
            int yStart, int yEnd,
            float red, float green, float blue, float alpha,
            float uvOffset,
            boolean translucent
    ) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.beaconBeam(BEAM_TEXTURE, translucent));

        PoseStack.Pose pose = stack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // Height and UV calculations
        int height = yEnd - yStart;
        float vStart = uvOffset;
        float vEnd = uvOffset + (float)height;

        // Render 4 faces of the beam (rectangular prism)
        // Each face is a quad from yStart to yEnd

        // Face 1: -X facing (normal: -1, 0, 0)
        renderQuadFace(buffer, matrix, normal,
            -radius, yStart, radius,   // bottom-left
            -radius, yEnd, radius,     // top-left
            -radius, yEnd, -radius,    // top-right
            -radius, yStart, -radius,  // bottom-right
            red, green, blue, alpha,
            0, vStart, 1, vEnd,
            -1, 0, 0);

        // Face 2: +X facing (normal: 1, 0, 0)
        renderQuadFace(buffer, matrix, normal,
            radius, yStart, -radius,
            radius, yEnd, -radius,
            radius, yEnd, radius,
            radius, yStart, radius,
            red, green, blue, alpha,
            0, vStart, 1, vEnd,
            1, 0, 0);

        // Face 3: -Z facing (normal: 0, 0, -1)
        renderQuadFace(buffer, matrix, normal,
            radius, yStart, -radius,
            radius, yEnd, -radius,
            -radius, yEnd, -radius,
            -radius, yStart, -radius,
            red, green, blue, alpha,
            0, vStart, 1, vEnd,
            0, 0, -1);

        // Face 4: +Z facing (normal: 0, 0, 1)
        renderQuadFace(buffer, matrix, normal,
            -radius, yStart, radius,
            -radius, yEnd, radius,
            radius, yEnd, radius,
            radius, yStart, radius,
            red, green, blue, alpha,
            0, vStart, 1, vEnd,
            0, 0, 1);
    }

    /**
     * Renders a single quad face of the beam.
     */
    private static void renderQuadFace(
            VertexConsumer buffer,
            Matrix4f matrix, Matrix3f normal,
            float x1, float y1, float z1,  // bottom-left
            float x2, float y2, float z2,  // top-left
            float x3, float y3, float z3,  // top-right
            float x4, float y4, float z4,  // bottom-right
            float r, float g, float b, float a,
            float u1, float v1, float u2, float v2,
            float nx, float ny, float nz
    ) {
        // Vertex order for correct face winding
        buffer.addVertex(matrix, x1, y1, z1)
              .setColor(r, g, b, a)
              .setUv(u1, v1)
              .setOverlay(0)
              .setLight(15728880)  // Full bright
              .setNormal(normal, nx, ny, nz);

        buffer.addVertex(matrix, x2, y2, z2)
              .setColor(r, g, b, a)
              .setUv(u1, v2)
              .setOverlay(0)
              .setLight(15728880)
              .setNormal(normal, nx, ny, nz);

        buffer.addVertex(matrix, x3, y3, z3)
              .setColor(r, g, b, a)
              .setUv(u2, v2)
              .setOverlay(0)
              .setLight(15728880)
              .setNormal(normal, nx, ny, nz);

        buffer.addVertex(matrix, x4, y4, z4)
              .setColor(r, g, b, a)
              .setUv(u2, v1)
              .setOverlay(0)
              .setLight(15728880)
              .setNormal(normal, nx, ny, nz);
    }
}
