package net.geforcemods.securitycraft.blockentities;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.CustomizableBlockEntity;
import net.geforcemods.securitycraft.api.IEMPAffectedBE;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.Option.BooleanOption;
import net.geforcemods.securitycraft.api.Option.DisabledOption;
import net.geforcemods.securitycraft.api.Option.DoubleOption;
import net.geforcemods.securitycraft.api.Option.IntOption;
import net.geforcemods.securitycraft.blocks.SecurityCameraBlock;
import net.geforcemods.securitycraft.entity.camera.SecurityCamera;
import net.geforcemods.securitycraft.inventory.InsertOnlyInvWrapper;
import net.geforcemods.securitycraft.inventory.LensContainer;
import net.geforcemods.securitycraft.inventory.SingleLensMenu;
import net.geforcemods.securitycraft.inventory.SingleLensMenu.SingleLensContainer;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.geforcemods.securitycraft.util.ITickingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class SecurityCameraBlockEntity extends CustomizableBlockEntity implements ITickingBlockEntity, IEMPAffectedBE, MenuProvider, ContainerListener, SingleLensContainer {
	private double cameraRotation = 0.0D;
	private double oCameraRotation = 0.0D;
	private boolean addToRotation = true;
	private boolean down = false, downSet = false;
	private int playersViewing = 0;
	private boolean shutDown = false;
	private DoubleOption rotationSpeedOption = new DoubleOption("rotationSpeed", 0.018D, 0.01D, 0.025D, 0.001D, true);
	private BooleanOption shouldRotateOption = new BooleanOption("shouldRotate", true);
	private DoubleOption customRotationOption = new DoubleOption("customRotation", getCameraRotation(), 1.55D, -1.55D, rotationSpeedOption.get(), true);
	private DisabledOption disabled = new DisabledOption(false);
	private IntOption opacity = new IntOption("opacity", 100, 0, 255, 1, true);
	private LensContainer lens = new LensContainer(1);

	public SecurityCameraBlockEntity(BlockPos pos, BlockState state) {
		super(SCContent.SECURITY_CAMERA_BLOCK_ENTITY.get(), pos, state);
		lens.addListener(this);
	}

	@Override
	public void tick(Level level, BlockPos pos, BlockState state) {
		if (!downSet) {
			down = state.getValue(SecurityCameraBlock.FACING) == Direction.DOWN;
			downSet = true;
		}

		if (!shutDown) {
			oCameraRotation = getCameraRotation();

			if (!shouldRotateOption.get()) {
				cameraRotation = customRotationOption.get();
				return;
			}

			if (addToRotation && getCameraRotation() <= 1.55F)
				cameraRotation = getCameraRotation() + rotationSpeedOption.get();
			else
				addToRotation = false;

			if (!addToRotation && getCameraRotation() >= -1.55F)
				cameraRotation = getCameraRotation() - rotationSpeedOption.get();
			else
				addToRotation = true;
		}
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("shutDown", shutDown);
		tag.put("lens", lens.createTag());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		shutDown = tag.getBoolean("shutDown");
		lens.fromTag(tag.getList("lens", Tag.TAG_COMPOUND));
	}

	public static IItemHandler getCapability(SecurityCameraBlockEntity be, Direction side) {
		return BlockUtils.isAllowedToExtractFromProtectedBlock(side, be) ? new InvWrapper(be.lens) : new InsertOnlyInvWrapper(be.lens);
	}

	@Override
	public void containerChanged(Container container) {
		if (level == null)
			return;

		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new SingleLensMenu(id, level, worldPosition, inventory);
	}

	@Override
	public Component getDisplayName() {
		return super.getDisplayName();
	}

	@Override
	public Container getLensContainer() {
		return lens;
	}

	@Override
	public ModuleType[] acceptedModules() {
		return new ModuleType[] {
				ModuleType.REDSTONE, ModuleType.ALLOWLIST
		};
	}

	@Override
	public Option<?>[] customOptions() {
		return new Option[] {
				rotationSpeedOption, shouldRotateOption, customRotationOption, disabled, opacity
		};
	}

	@Override
	public void onModuleRemoved(ItemStack stack, ModuleType module, boolean toggled) {
		super.onModuleRemoved(stack, module, toggled);

		if (module == ModuleType.REDSTONE)
			level.setBlockAndUpdate(worldPosition, getBlockState().setValue(SecurityCameraBlock.POWERED, false));
	}

	@Override
	public void onOptionChanged(Option<?> option) {
		//make players stop viewing the camera when it's disabled
		if (option.getName().equals("disabled") && !level.isClientSide && ((BooleanOption) option).get()) {
			for (ServerPlayer player : ((ServerLevel) level).players()) {
				if (player.getCamera() instanceof SecurityCamera camera && camera.blockPosition().equals(worldPosition))
					camera.stopViewing(player);
			}
		}
	}

	@Override
	public void shutDown() {
		BlockState state = level.getBlockState(worldPosition);

		IEMPAffectedBE.super.shutDown();

		if (state.getBlock() == SCContent.SECURITY_CAMERA.get() && state.getValue(SecurityCameraBlock.POWERED))
			level.setBlockAndUpdate(worldPosition, state.setValue(SecurityCameraBlock.POWERED, false));
	}

	@Override
	public boolean isShutDown() {
		return shutDown;
	}

	@Override
	public void setShutDown(boolean shutDown) {
		this.shutDown = shutDown;
	}

	public void startViewing() {
		if (playersViewing++ == 0)
			level.setBlockAndUpdate(worldPosition, getBlockState().setValue(SecurityCameraBlock.BEING_VIEWED, true));
	}

	public void stopViewing() {
		if (--playersViewing == 0)
			level.setBlockAndUpdate(worldPosition, getBlockState().setValue(SecurityCameraBlock.BEING_VIEWED, false));
	}

	public boolean isDisabled() {
		return disabled.get();
	}

	public double getCameraRotation() {
		return cameraRotation;
	}

	public double getOriginalCameraRotation() {
		return oCameraRotation;
	}

	public boolean isDown() {
		return down;
	}

	public int getOpacity() {
		return opacity.get();
	}
}
