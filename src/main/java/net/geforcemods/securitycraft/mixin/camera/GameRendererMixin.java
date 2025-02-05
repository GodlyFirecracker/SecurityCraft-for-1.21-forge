package net.geforcemods.securitycraft.mixin.camera;

import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;

import net.geforcemods.securitycraft.blockentities.SecurityCameraBlockEntity;
import net.geforcemods.securitycraft.entity.camera.SecurityCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(value = GameRenderer.class, priority = 1100)
public class GameRendererMixin {
	@Shadow
	@Final
	Minecraft minecraft;

	/**
	 * Makes sure the camera zooming works, because the fov is only updated when the camera entity is the player itself
	 */
	@ModifyConstant(method = "tickFov", constant = @Constant(floatValue = 1.0F))
	private float securitycraft$modifyInitialFValue(float f) {
		if (minecraft.cameraEntity instanceof SecurityCamera cam)
			return cam.getZoomAmount();
		else
			return f;
	}

	/**
	 * Renders the camera tint if a lens is installed. This cannot be done in a standard overlay, as the tint needs to exist even
	 * when the GUI is hidden with F1
	 */
	@Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;hideGui:Z", opcode = Opcodes.GETFIELD), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void securitycraft$renderCameraTint(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci, int i, int j, Window window, Matrix4f matrix4f, PoseStack posestack, GuiGraphics guiGraphics) {
		if (minecraft.cameraEntity instanceof SecurityCamera) {
			Level level = minecraft.level;
			BlockPos pos = minecraft.cameraEntity.blockPosition();

			if (!(level.getBlockEntity(pos) instanceof SecurityCameraBlockEntity be))
				return;

			ItemStack lens = be.getLensContainer().getItem(0);

			if (lens.getItem() instanceof DyeableLeatherItem item && item.hasCustomColor(lens))
				guiGraphics.fill(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight(), item.getColor(lens) + (be.getOpacity() << 24));
		}
	}
}
