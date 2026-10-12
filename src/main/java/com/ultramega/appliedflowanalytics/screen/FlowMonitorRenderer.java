package com.ultramega.appliedflowanalytics.screen;

import com.ultramega.appliedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.appliedflowanalytics.part.FlowMonitorPart;
import com.ultramega.appliedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.appliedflowanalytics.screen.components.FlowGraph;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.appliedflowanalytics.screen.sidebuttons.MonitorItemVisibility;
import com.ultramega.appliedflowanalytics.support.ResourceRendering;

import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEKey;
import appeng.client.render.BlockEntityRenderHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.ultramega.appliedflowanalytics.util.AppliedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public final class FlowMonitorRenderer {
    public static final FlowMonitorRenderer INSTANCE = new FlowMonitorRenderer();
    private static final float GRAPH_WIDTH = 200;
    private static final float GRAPH_HEIGHT = 95;
    private static final float BLOCK_PIXEL = 1.0f / 16;
    private static final float GRAPH_FACE_WIDTH = 8 * BLOCK_PIXEL;
    private static final float ITEM_ICON_SCALE = 2 * BLOCK_PIXEL;
    private static final float MIN_LINE_PIXELS = 1.25f;
    private static final float LAYER_DEPTH = 0.0005f;
    private final Map<FlowMonitorPart, Display> displays = new WeakHashMap<>();

    public void render(final FlowMonitorPart monitor,
                       final float partialTick,
                       final PoseStack pose,
                       final MultiBufferSource buffers,
                       final int light,
                       final int overlay) {
        final AEKey resource = monitor.getConfiguredResource();
        if (monitor.getLevel() == null || !monitor.isDisplayActive() || resource == null) {
            return;
        }
        final LineStyle style = monitor.getLineStyle();
        Display display = this.displays.get(monitor);
        if (display == null || display.revision() != monitor.getDisplayRevision() || display.style() != style) {
            display = this.createDisplay(monitor, resource, style);
            this.displays.put(monitor, display);
        }

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        final Direction side = monitor.getSide() == null ? Direction.NORTH : monitor.getSide();
        BlockEntityRenderHelper.rotateToFace(pose, BlockOrientation.get(side, monitor.getSpin()));
        pose.translate(0, 0, 0.502);
        pose.pushPose();
        pose.translate(-GRAPH_FACE_WIDTH / 2, 0.14, 0);
        pose.scale(GRAPH_FACE_WIDTH / GRAPH_WIDTH, -0.0033f, 1);

        final float strokeScale = getStrokeScale(pose.last().pose());
        final VertexConsumer vertices = buffers.getBuffer(RenderType.debugQuads());
        for (final Quad quad : display.quads()) {
            quad.render(vertices, pose.last().pose(), strokeScale);
        }
        final Font font = Minecraft.getInstance().font;
        font.drawInBatch("0", -6, 44, 0xffa0a0a0, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();

        final var rendering = new ResourceRendering(resource);
        if (monitor.getItemVisibility() == MonitorItemVisibility.SHOW) {
            pose.pushPose();
            pose.translate(0, 0.185, 0.01);
            pose.scale(ITEM_ICON_SCALE, ITEM_ICON_SCALE, ITEM_ICON_SCALE);
            rendering.render(resource, pose, buffers, LightTexture.FULL_BRIGHT, monitor.getLevel());
            pose.popPose();
        }

        final long value = monitor.getFlowText().getValue(display.inflow(), display.outflow());
        final String caption = createFlowAnalyticsTranslation("gui", "flow_monitor." + monitor.getFlowText().name().toLowerCase(Locale.ROOT),
            rendering.formatAmount(value), value > 0 ? "+" : "", monitor.getGranularity().perStr()).getString();
        final int width = font.width(caption);
        final float scale = Math.min(0.0075f, 0.66f / Math.max(1, width));
        pose.pushPose();
        pose.translate(0, -0.185, 0.003);
        pose.scale(scale, -scale, scale);
        font.drawInBatch(caption, -width / 2f, 0, monitor.getFlowText().getColor(), false,
            pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
        pose.popPose();
    }

    private Display createDisplay(final FlowMonitorPart monitor, final AEKey resource, final LineStyle style) {
        final long[] inflow = monitor.getDisplayInflow();
        final long[] outflow = monitor.getDisplayOutflow();
        final FlowGraph graph = new FlowGraph();
        graph.setMinimumVisibleSamples(FlowHistoryNetworkComponent.MONITOR_SAMPLES);
        graph.lineStyle = style;
        graph.setGraphPos(0, (int) GRAPH_HEIGHT);
        graph.setData(Map.of(
            new ResourceChangeGranularityKey(resource, (short) 1, monitor.getGranularity().getTickAmount()), inflow,
            new ResourceChangeGranularityKey(resource, (short) -1, monitor.getGranularity().getTickAmount()), outflow
        ), monitor.getGranularity());
        final List<Quad> quads = new ArrayList<>();
        graph.drawGraphs((x1, y1, x2, y2, color, thickness, lineStyle) -> this.line(quads, x1, y1, x2, y2, color, thickness, lineStyle));
        return new Display(monitor.getDisplayRevision(), style, List.copyOf(quads),
            inflow.length == 0 ? 0 : inflow[inflow.length - 1],
            outflow.length == 0 ? 0 : outflow[outflow.length - 1]);
    }

    private void line(final List<Quad> quads,
                      final double x1,
                      final double y1,
                      final double x2,
                      final double y2,
                      final int color,
                      final int thickness,
                      final LineStyle style) {
        final Quad previous = quads.isEmpty() ? null : quads.getLast();
        final float z = previous == null ? LAYER_DEPTH : previous.z() + (previous.color() == color ? 0 : LAYER_DEPTH);
        final double half = thickness / 2.0;
        if (style == LineStyle.BLOCKY) {
            // Keep the final vertical step visible inside the last column of the panel
            final float sx = Math.clamp((float) Math.round(x1 - half), 0, GRAPH_WIDTH - thickness);
            final float ex = Math.clamp((float) Math.round(x2 - half), 0, GRAPH_WIDTH - thickness);
            final float sy = Math.round(y1 - half);
            final float ey = Math.round(y2 - half);
            quads.add(Quad.rectangle(Math.min(sx, ex), sy, Math.max(sx, ex) + thickness, sy + thickness, z, color, false));
            quads.add(Quad.rectangle(ex, Math.min(sy, ey), ex + thickness, Math.max(sy, ey) + thickness, z, color, true));
            return;
        }
        final double length = Math.hypot(x2 - x1, y2 - y1);
        if (length < 1.0e-6) {
            quads.add(Quad.rectangle((float) (x1 - half), (float) (y1 - half), (float) (x1 + half), (float) (y1 + half), z, color, false));
            return;
        }
        final double tx = (y2 - y1) / length * half;
        final double ty = -(x2 - x1) / length * half;
        quads.add(new Quad((float) (x1 + tx), (float) (y1 + ty), (float) (x1 - tx), (float) (y1 - ty),
            (float) (x2 - tx), (float) (y2 - ty), (float) (x2 + tx), (float) (y2 + ty), (float) tx, (float) ty, z, color));
    }

    private static float getStrokeScale(final Matrix4f model) {
        final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(model);
        final Vector3f center = projection.transformProject(new Vector3f(GRAPH_WIDTH / 2, GRAPH_HEIGHT / 2, 0));
        final Vector3f x = projection.transformProject(new Vector3f(GRAPH_WIDTH / 2 + 1, GRAPH_HEIGHT / 2, 0));
        final Vector3f y = projection.transformProject(new Vector3f(GRAPH_WIDTH / 2, GRAPH_HEIGHT / 2 + 1, 0));
        final var target = Minecraft.getInstance().getMainRenderTarget();
        final double xx = (x.x - center.x) * target.width / 2;
        final double xy = (x.y - center.y) * target.height / 2;
        final double yx = (y.x - center.x) * target.width / 2;
        final double yy = (y.y - center.y) * target.height / 2;
        // The smallest singular value gives pixels per graph unit along the most foreshortened axis
        final double determinant = xx * yy - xy * yx;
        final double squaredLength = xx * xx + xy * xy + yx * yx + yy * yy;
        final double largest = Math.sqrt((squaredLength + Math.sqrt(Math.max(0, squaredLength * squaredLength - 4 * determinant * determinant))) / 2);
        final double pixels = Math.abs(determinant) / largest;
        if (!Double.isFinite(pixels) || pixels < 1.0e-6) {
            return 1;
        }
        return (float) Math.max(1, MIN_LINE_PIXELS / pixels);
    }

    private record Display(long revision, LineStyle style, List<Quad> quads, long inflow, long outflow) {
    }

    private record Quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
                        float halfWidthX, float halfWidthY, float z, int color) {
        private static Quad rectangle(final float left,
                                      final float top,
                                      final float right,
                                      final float bottom,
                                      final float z,
                                      final int color,
                                      final boolean vertical) {
            if (vertical) {
                return new Quad(left, bottom, right, bottom, right, top, left, top, (left - right) / 2, 0, z, color);
            }
            return new Quad(left, top, left, bottom, right, bottom, right, top, 0, (top - bottom) / 2, z, color);
        }

        private void render(final VertexConsumer vertices, final Matrix4f matrix, final float strokeScale) {
            final float dx = this.halfWidthX * (strokeScale - 1);
            final float dy = this.halfWidthY * (strokeScale - 1);
            this.vertex(vertices, matrix, this.x1 + dx, this.y1 + dy);
            this.vertex(vertices, matrix, this.x2 - dx, this.y2 - dy);
            this.vertex(vertices, matrix, this.x3 - dx, this.y3 - dy);
            this.vertex(vertices, matrix, this.x4 + dx, this.y4 + dy);
        }

        private void vertex(final VertexConsumer vertices, final Matrix4f matrix, final float x, final float y) {
            vertices.addVertex(matrix, Math.clamp(x, 0, GRAPH_WIDTH), Math.clamp(y, 0, GRAPH_HEIGHT), this.z)
                .setColor(this.color);
        }
    }
}
