package com.bvanseg.gigeresque.mixins;

import com.bvanseg.gigeresque.common.Gigeresque;
import com.bvanseg.gigeresque.common.block.Blocks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Boston Vanseghi
 */
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    private static final Identifier BLACK_FLUID_TEXTURE = new Identifier(Gigeresque.MOD_ID, "textures/misc/black_fluid_overlay.png");

    @Inject(method = {"renderOverlays"}, at = {@At("RETURN")})
    private static void renderOverlays(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (!client.player.isSpectator()) {
            double d = client.player.getEyeY() - 0.1111111119389534D;
            BlockPos blockPos = new BlockPos(client.player.getX(), d, client.player.getZ());
            FluidState fluidState = client.player.world.getFluidState(blockPos);

            if (fluidState.getBlockState().getBlock() == Blocks.INSTANCE.getBLACK_FLUID()) {
                renderBlackFluidOverlay(client, matrices);
            }
        }
    }

    private static void renderBlackFluidOverlay(MinecraftClient client, MatrixStack matrices) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableTexture();
        RenderSystem.setShaderTexture(0, BLACK_FLUID_TEXTURE);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        float f = client.player.getBrightnessAtEyes();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(f, f, f, 0.95F);
        float m = -client.player.getYaw() / 64.0F;
        float n = client.player.getPitch() / 64.0F;
        Matrix4f matrix4f = matrices.peek().getModel();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f, -1.0F, -1.0F, -0.5F).texture(4.0F + m, 4.0F + n).next();
        bufferBuilder.vertex(matrix4f, 1.0F, -1.0F, -0.5F).texture(0.0F + m, 4.0F + n).next();
        bufferBuilder.vertex(matrix4f, 1.0F, 1.0F, -0.5F).texture(0.0F + m, 0.0F + n).next();
        bufferBuilder.vertex(matrix4f, -1.0F, 1.0F, -0.5F).texture(4.0F + m, 0.0F + n).next();
        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);
        RenderSystem.disableBlend();
    }
}
