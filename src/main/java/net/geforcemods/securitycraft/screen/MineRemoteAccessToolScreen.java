package net.geforcemods.securitycraft.screen;

import com.mojang.blaze3d.platform.InputConstants;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.IExplosive;
import net.geforcemods.securitycraft.network.server.RemoteControlMine;
import net.geforcemods.securitycraft.network.server.RemoteControlMine.Action;
import net.geforcemods.securitycraft.network.server.RemoveMineFromMRAT;
import net.geforcemods.securitycraft.screen.components.PictureButton;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

public class MineRemoteAccessToolScreen extends Screen {
	private static final ResourceLocation TEXTURE = new ResourceLocation("securitycraft:textures/gui/container/mrat.png");
	private static final ResourceLocation EXPLOSIVE_SPRITE = new ResourceLocation(SecurityCraft.MODID, "sc_manual/explosive_highlighted");
	private static final int DEFUSE = 0, ACTIVATE = 1, DETONATE = 2, UNBIND = 3;
	private ItemStack mrat;
	private Button[][] guiButtons = new Button[6][4]; //6 mines, 4 actions (defuse, prime, detonate, unbind)
	private int xSize = 256, ySize = 184, leftPos, topPos;
	private final Component notBound = Utils.localize("gui.securitycraft:mrat.notBound");
	private final Component[] lines = new Component[6];
	private final int[] lengths = new int[6];

	public MineRemoteAccessToolScreen(ItemStack item) {
		super(item.getHoverName());

		mrat = item;
	}

	@Override
	public void init() {
		super.init();
		leftPos = (width - xSize) / 2;
		topPos = (height - ySize) / 2;

		int padding = 25;
		int y = 50;
		int id = 0;
		int startX = (width - xSize) / 2;
		int startY = (height - ySize) / 2;

		for (int i = 0; i < 6; i++) {
			y += 25;

			// initialize buttons
			for (int j = 0; j < 4; j++) {
				int btnX = startX + j * padding + 154;
				int btnY = startY + y - 48;
				int mine = id / 4;
				int action = id % 4;

				id++;

				switch (j) {
					case DEFUSE -> guiButtons[i][j] = new PictureButton(btnX, btnY, 20, 20, new ItemStack(SCContent.WIRE_CUTTERS.get()), b -> buttonClicked(mine, action));
					case ACTIVATE -> guiButtons[i][j] = new PictureButton(btnX, btnY, 20, 20, new ItemStack(Items.FLINT_AND_STEEL), b -> buttonClicked(mine, action));
					case DETONATE -> guiButtons[i][j] = new PictureButton(btnX, btnY, 20, 20, EXPLOSIVE_SPRITE, 0, 1, 18, 18, b -> buttonClicked(mine, action));
					case UNBIND -> guiButtons[i][j] = new Button(btnX, btnY, 20, 20, Component.literal("X"), b -> buttonClicked(mine, action), Button.DEFAULT_NARRATION);
					default -> throw new IllegalArgumentException("Mine actions can only range from 0-3 (inclusive)");
				}

				guiButtons[i][j].active = false;
				addRenderableWidget(guiButtons[i][j]);
			}

			BlockPos minePos = getMineCoordinates(i);

			if (minePos != null) {
				guiButtons[i][UNBIND].active = true;
				lines[i] = Utils.localize("gui.securitycraft:mrat.mineLocations", minePos);

				if (Minecraft.getInstance().player.level().isLoaded(minePos)) {
					Block block = minecraft.level.getBlockState(minePos).getBlock();

					if (block instanceof IExplosive explosive) {
						boolean active = explosive.isActive(minecraft.level, minePos);
						boolean defusable = explosive.isDefusable();

						guiButtons[i][DEFUSE].active = active && defusable;
						guiButtons[i][ACTIVATE].active = !active && defusable;
						guiButtons[i][DETONATE].active = active;
						guiButtons[i][DEFUSE].setTooltip(Tooltip.create(Utils.localize("gui.securitycraft:mrat.defuse")));
						guiButtons[i][ACTIVATE].setTooltip(Tooltip.create(Utils.localize("gui.securitycraft:mrat.activate")));
						guiButtons[i][DETONATE].setTooltip(Tooltip.create(Utils.localize("gui.securitycraft:mrat.detonate")));
						guiButtons[i][UNBIND].setTooltip(Tooltip.create(Utils.localize("gui.securitycraft:mrat.unbind")));
					}
					else {
						removeTagFromToolAndUpdate(mrat, minePos);

						for (int j = 0; j < 4; j++) {
							guiButtons[i][j].active = false;
						}
					}
				}
				else {
					for (int j = 0; j < 3; j++) {
						guiButtons[i][j].setTooltip(Tooltip.create(Utils.localize("gui.securitycraft:mrat.outOfRange")));
					}

					guiButtons[i][UNBIND].setTooltip(Tooltip.create(Utils.localize("gui.securitycraft:mrat.unbind")));
				}
			}
			else
				lines[i] = notBound;

			lengths[i] = font.width(lines[i]);
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.drawString(font, title, leftPos + xSize / 2 - font.width(title) / 2, topPos + 6, 4210752, false);

		for (int i = 0; i < 6; i++) {
			guiGraphics.drawString(font, lines[i], leftPos + xSize / 2 - lengths[i] + 25, topPos + i * 25 + 33, 4210752, false);
		}
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		renderTransparentBackground(guiGraphics);
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, xSize, ySize);
	}

	private void buttonClicked(int mine, int action) {
		BlockPos pos = getMineCoordinates(mine);

		if (pos != null) {
			switch (action) {
				case DEFUSE:
					((IExplosive) Minecraft.getInstance().player.level().getBlockState(pos).getBlock()).defuseMine(Minecraft.getInstance().player.level(), pos);
					PacketDistributor.sendToServer(new RemoteControlMine(pos, Action.DEFUSE));
					guiButtons[mine][DEFUSE].active = false;
					guiButtons[mine][ACTIVATE].active = true;
					guiButtons[mine][DETONATE].active = false;
					break;
				case ACTIVATE:
					((IExplosive) Minecraft.getInstance().player.level().getBlockState(pos).getBlock()).activateMine(Minecraft.getInstance().player.level(), pos);
					PacketDistributor.sendToServer(new RemoteControlMine(pos, Action.ACTIVATE));
					guiButtons[mine][DEFUSE].active = true;
					guiButtons[mine][ACTIVATE].active = false;
					guiButtons[mine][DETONATE].active = true;
					break;
				case DETONATE:
					PacketDistributor.sendToServer(new RemoteControlMine(pos, Action.DETONATE));
					removeTagFromToolAndUpdate(mrat, pos);

					for (int i = 0; i < 4; i++) {
						guiButtons[mine][i].active = false;
					}

					break;
				case UNBIND:
					removeTagFromToolAndUpdate(mrat, pos);

					for (int i = 0; i < 4; i++) {
						guiButtons[mine][i].active = false;
					}

					break;
				default:
					throw new IllegalArgumentException("Mine actions can only range from 0-3 (inclusive)");
			}
		}
	}

	/**
	 * @param mine 0 based
	 */
	private BlockPos getMineCoordinates(int mine) {
		mine++; //mines are stored starting by mine1 up to mine6

		if (mrat.getItem() == SCContent.MINE_REMOTE_ACCESS_TOOL.get() && mrat.has(DataComponents.CUSTOM_DATA)) {
			int[] coords = Utils.getTag(mrat).getIntArray("mine" + mine);

			if (coords.length == 3)
				return new BlockPos(coords[0], coords[1], coords[2]);
		}

		return null;
	}

	private void removeTagFromToolAndUpdate(ItemStack stack, BlockPos pos) {
		if (stack.has(DataComponents.CUSTOM_DATA)) {
			CompoundTag tag = Utils.getTag(stack);

			for (int i = 1; i <= 6; i++) {
				int[] coords = tag.getIntArray("mine" + i);

				if (coords.length == 3 && coords[0] == pos.getX() && coords[1] == pos.getY() && coords[2] == pos.getZ()) {
					tag.remove("mine" + i);
					CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
					PacketDistributor.sendToServer(new RemoveMineFromMRAT(i));
					return;
				}
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
			onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
