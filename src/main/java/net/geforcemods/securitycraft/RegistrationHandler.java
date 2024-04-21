package net.geforcemods.securitycraft;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;

import net.geforcemods.securitycraft.blockentities.AbstractKeypadFurnaceBlockEntity;
import net.geforcemods.securitycraft.blockentities.BlockPocketManagerBlockEntity;
import net.geforcemods.securitycraft.blockentities.ClaymoreBlockEntity;
import net.geforcemods.securitycraft.blockentities.InventoryScannerBlockEntity;
import net.geforcemods.securitycraft.blockentities.KeypadBarrelBlockEntity;
import net.geforcemods.securitycraft.blockentities.KeypadChestBlockEntity;
import net.geforcemods.securitycraft.blockentities.LaserBlockBlockEntity;
import net.geforcemods.securitycraft.blockentities.ReinforcedDispenserBlockEntity;
import net.geforcemods.securitycraft.blockentities.ReinforcedDropperBlockEntity;
import net.geforcemods.securitycraft.blockentities.ReinforcedHopperBlockEntity;
import net.geforcemods.securitycraft.blockentities.SecurityCameraBlockEntity;
import net.geforcemods.securitycraft.blockentities.TrophySystemBlockEntity;
import net.geforcemods.securitycraft.misc.SCSounds;
import net.geforcemods.securitycraft.network.client.BlockPocketManagerFailedActivation;
import net.geforcemods.securitycraft.network.client.OpenScreen;
import net.geforcemods.securitycraft.network.client.PlayAlarmSound;
import net.geforcemods.securitycraft.network.client.RefreshDisguisableModel;
import net.geforcemods.securitycraft.network.client.SetCameraView;
import net.geforcemods.securitycraft.network.client.SetTrophySystemTarget;
import net.geforcemods.securitycraft.network.client.UpdateLaserColors;
import net.geforcemods.securitycraft.network.client.UpdateLogger;
import net.geforcemods.securitycraft.network.server.AssembleBlockPocket;
import net.geforcemods.securitycraft.network.server.CheckBriefcasePasscode;
import net.geforcemods.securitycraft.network.server.CheckPasscode;
import net.geforcemods.securitycraft.network.server.ClearChangeDetectorServer;
import net.geforcemods.securitycraft.network.server.ClearLoggerServer;
import net.geforcemods.securitycraft.network.server.DismountCamera;
import net.geforcemods.securitycraft.network.server.MountCamera;
import net.geforcemods.securitycraft.network.server.RemoteControlMine;
import net.geforcemods.securitycraft.network.server.RemoveCameraTag;
import net.geforcemods.securitycraft.network.server.RemoveMineFromMRAT;
import net.geforcemods.securitycraft.network.server.RemovePositionFromSSS;
import net.geforcemods.securitycraft.network.server.RemoveSentryFromSRAT;
import net.geforcemods.securitycraft.network.server.SetBriefcasePasscodeAndOwner;
import net.geforcemods.securitycraft.network.server.SetCameraPowered;
import net.geforcemods.securitycraft.network.server.SetDefaultCameraViewingDirection;
import net.geforcemods.securitycraft.network.server.SetGhostSlot;
import net.geforcemods.securitycraft.network.server.SetKeycardUses;
import net.geforcemods.securitycraft.network.server.SetListModuleData;
import net.geforcemods.securitycraft.network.server.SetPasscode;
import net.geforcemods.securitycraft.network.server.SetSentryMode;
import net.geforcemods.securitycraft.network.server.SetStateOnDisguiseModule;
import net.geforcemods.securitycraft.network.server.SyncAlarmSettings;
import net.geforcemods.securitycraft.network.server.SyncBlockChangeDetector;
import net.geforcemods.securitycraft.network.server.SyncBlockPocketManager;
import net.geforcemods.securitycraft.network.server.SyncBlockReinforcer;
import net.geforcemods.securitycraft.network.server.SyncKeycardSettings;
import net.geforcemods.securitycraft.network.server.SyncLaserSideConfig;
import net.geforcemods.securitycraft.network.server.SyncProjector;
import net.geforcemods.securitycraft.network.server.SyncRiftStabilizer;
import net.geforcemods.securitycraft.network.server.SyncSSSSettingsOnServer;
import net.geforcemods.securitycraft.network.server.SyncTrophySystem;
import net.geforcemods.securitycraft.network.server.ToggleBlockPocketManager;
import net.geforcemods.securitycraft.network.server.ToggleModule;
import net.geforcemods.securitycraft.network.server.ToggleNightVision;
import net.geforcemods.securitycraft.network.server.ToggleOption;
import net.geforcemods.securitycraft.network.server.UpdateSliderValue;
import net.geforcemods.securitycraft.util.RegisterItemBlock;
import net.geforcemods.securitycraft.util.Reinforced;
import net.geforcemods.securitycraft.util.SCItemGroup;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.common.Mod.EventBusSubscriber.Bus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.MutableHashedLinkedMap;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = SecurityCraft.MODID, bus = Bus.MOD)
public class RegistrationHandler {
	private RegistrationHandler() {}

	@SubscribeEvent
	public static void onRegister(RegisterEvent event) {
		event.register(Registries.ITEM, helper -> {
			//register item blocks from annotated fields
			for (Field field : SCContent.class.getFields()) {
				try {
					if (field.isAnnotationPresent(Reinforced.class) && field.getAnnotation(Reinforced.class).registerBlockItem()) {
						SCItemGroup group = field.getAnnotation(Reinforced.class).itemGroup();
						Block block = ((DeferredBlock<Block>) field.get(null)).get();
						Item blockItem = new BlockItem(block, new Item.Properties().fireResistant());

						helper.register(Utils.getRegistryName(block), blockItem);

						if (group != SCItemGroup.MANUAL)
							SCCreativeModeTabs.STACKS_FOR_ITEM_GROUPS.get(group).add(new ItemStack(blockItem));
					}
					else if (field.isAnnotationPresent(RegisterItemBlock.class)) {
						SCItemGroup group = field.getAnnotation(RegisterItemBlock.class).value();
						Block block = ((DeferredBlock<Block>) field.get(null)).get();
						Item blockItem = new BlockItem(block, new Item.Properties());

						helper.register(Utils.getRegistryName(block), blockItem);

						if (group != SCItemGroup.MANUAL)
							SCCreativeModeTabs.STACKS_FOR_ITEM_GROUPS.get(group).add(new ItemStack(blockItem));
					}
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		});

		event.register(Registries.SOUND_EVENT, helper -> {
			for (int i = 0; i < SCSounds.values().length; i++) {
				SCSounds sound = SCSounds.values()[i];

				helper.register(sound.location, sound.event);
			}
		});
	}

	@SubscribeEvent
	public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
		event.put(SCContent.SENTRY_ENTITY.get(), Mob.createMobAttributes().build());
	}

	@SubscribeEvent
	public static void registerPackets(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(SecurityCraft.MODID).versioned(SecurityCraft.getVersion());

		clientPacket(registrar, BlockPocketManagerFailedActivation.TYPE, BlockPocketManagerFailedActivation.STREAM_CODEC, BlockPocketManagerFailedActivation::handle);
		clientPacket(registrar, OpenScreen.TYPE, OpenScreen.STREAM_CODEC, OpenScreen::handle);
		clientPacket(registrar, PlayAlarmSound.TYPE, PlayAlarmSound.STREAM_CODEC, PlayAlarmSound::handle);
		clientPacket(registrar, RefreshDisguisableModel.TYPE, RefreshDisguisableModel.STREAM_CODEC, RefreshDisguisableModel::handle);
		clientPacket(registrar, SetCameraView.TYPE, SetCameraView.STREAM_CODEC, SetCameraView::handle);
		clientPacket(registrar, SetTrophySystemTarget.TYPE, SetTrophySystemTarget.STREAM_CODEC, SetTrophySystemTarget::handle);
		clientPacket(registrar, UpdateLaserColors.TYPE, UpdateLaserColors.STREAM_CODEC, UpdateLaserColors::handle);
		clientPacket(registrar, UpdateLogger.TYPE, UpdateLogger.STREAM_CODEC, UpdateLogger::handle);
		serverPacket(registrar, AssembleBlockPocket.TYPE, AssembleBlockPocket.STREAM_CODEC, AssembleBlockPocket::handle);
		serverPacket(registrar, CheckPasscode.TYPE, CheckPasscode.STREAM_CODEC, CheckPasscode::handle);
		serverPacket(registrar, ClearChangeDetectorServer.TYPE, ClearChangeDetectorServer.STREAM_CODEC, ClearChangeDetectorServer::handle);
		serverPacket(registrar, ClearLoggerServer.TYPE, ClearLoggerServer.STREAM_CODEC, ClearLoggerServer::handle);
		serverPacket(registrar, DismountCamera.TYPE, DismountCamera.STREAM_CODEC, DismountCamera::handle);
		serverPacket(registrar, MountCamera.TYPE, MountCamera.STREAM_CODEC, MountCamera::handle);
		serverPacket(registrar, CheckBriefcasePasscode.TYPE, CheckBriefcasePasscode.STREAM_CODEC, CheckBriefcasePasscode::handle);
		serverPacket(registrar, RemoteControlMine.TYPE, RemoteControlMine.STREAM_CODEC, RemoteControlMine::handle);
		serverPacket(registrar, RemoveCameraTag.TYPE, RemoveCameraTag.STREAM_CODEC, RemoveCameraTag::handle);
		serverPacket(registrar, RemoveMineFromMRAT.TYPE, RemoveMineFromMRAT.STREAM_CODEC, RemoveMineFromMRAT::handle);
		serverPacket(registrar, RemovePositionFromSSS.TYPE, RemovePositionFromSSS.STREAM_CODEC, RemovePositionFromSSS::handle);
		serverPacket(registrar, RemoveSentryFromSRAT.TYPE, RemoveSentryFromSRAT.STREAM_CODEC, RemoveSentryFromSRAT::handle);
		serverPacket(registrar, SyncAlarmSettings.TYPE, SyncAlarmSettings.STREAM_CODEC, SyncAlarmSettings::handle);
		serverPacket(registrar, SetBriefcasePasscodeAndOwner.TYPE, SetBriefcasePasscodeAndOwner.STREAM_CODEC, SetBriefcasePasscodeAndOwner::handle);
		serverPacket(registrar, SetCameraPowered.TYPE, SetCameraPowered.STREAM_CODEC, SetCameraPowered::handle);
		serverPacket(registrar, SetGhostSlot.TYPE, SetGhostSlot.STREAM_CODEC, SetGhostSlot::handle);
		serverPacket(registrar, SetKeycardUses.TYPE, SetKeycardUses.STREAM_CODEC, SetKeycardUses::handle);
		serverPacket(registrar, SetListModuleData.TYPE, SetListModuleData.STREAM_CODEC, SetListModuleData::handle);
		serverPacket(registrar, SetPasscode.TYPE, SetPasscode.STREAM_CODEC, SetPasscode::handle);
		serverPacket(registrar, SetSentryMode.TYPE, SetSentryMode.STREAM_CODEC, SetSentryMode::handle);
		serverPacket(registrar, SetStateOnDisguiseModule.TYPE, SetStateOnDisguiseModule.STREAM_CODEC, SetStateOnDisguiseModule::handle);
		serverPacket(registrar, SyncBlockChangeDetector.TYPE, SyncBlockChangeDetector.STREAM_CODEC, SyncBlockChangeDetector::handle);
		serverPacket(registrar, SyncBlockReinforcer.TYPE, SyncBlockReinforcer.STREAM_CODEC, SyncBlockReinforcer::handle);
		serverPacket(registrar, SyncBlockPocketManager.TYPE, SyncBlockPocketManager.STREAM_CODEC, SyncBlockPocketManager::handle);
		serverPacket(registrar, SyncKeycardSettings.TYPE, SyncKeycardSettings.STREAM_CODEC, SyncKeycardSettings::handle);
		serverPacket(registrar, SyncLaserSideConfig.TYPE, SyncLaserSideConfig.STREAM_CODEC, SyncLaserSideConfig::handle);
		serverPacket(registrar, SyncProjector.TYPE, SyncProjector.STREAM_CODEC, SyncProjector::handle);
		serverPacket(registrar, SyncRiftStabilizer.TYPE, SyncRiftStabilizer.STREAM_CODEC, SyncRiftStabilizer::handle);
		serverPacket(registrar, SyncSSSSettingsOnServer.TYPE, SyncSSSSettingsOnServer.STREAM_CODEC, SyncSSSSettingsOnServer::handle);
		serverPacket(registrar, SyncTrophySystem.TYPE, SyncTrophySystem.STREAM_CODEC, SyncTrophySystem::handle);
		serverPacket(registrar, ToggleBlockPocketManager.TYPE, ToggleBlockPocketManager.STREAM_CODEC, ToggleBlockPocketManager::handle);
		serverPacket(registrar, ToggleModule.TYPE, ToggleModule.STREAM_CODEC, ToggleModule::handle);
		serverPacket(registrar, ToggleNightVision.TYPE, ToggleNightVision.STREAM_CODEC, ToggleNightVision::handle);
		serverPacket(registrar, ToggleOption.TYPE, ToggleOption.STREAM_CODEC, ToggleOption::handle);
		serverPacket(registrar, SetDefaultCameraViewingDirection.TYPE, SetDefaultCameraViewingDirection.STREAM_CODEC, SetDefaultCameraViewingDirection::handle);
		serverPacket(registrar, UpdateSliderValue.TYPE, UpdateSliderValue.STREAM_CODEC, UpdateSliderValue::handle);
	}

	private static final <T extends CustomPacketPayload> void clientPacket(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, IPayloadContext> handler) {
		registrar.playToClient(type, codec, (packet, ctx) -> ctx.enqueueWork(() -> handler.accept(packet, ctx)));
	}

	private static final <T extends CustomPacketPayload> void serverPacket(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, IPayloadContext> handler) {
		registrar.playToServer(type, codec, (packet, ctx) -> ctx.enqueueWork(() -> handler.accept(packet, ctx)));
	}

	@SubscribeEvent
	public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.KEYPAD_BLAST_FURNACE_BLOCK_ENTITY.get(), AbstractKeypadFurnaceBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.KEYPAD_FURNACE_BLOCK_ENTITY.get(), AbstractKeypadFurnaceBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.KEYPAD_SMOKER_BLOCK_ENTITY.get(), AbstractKeypadFurnaceBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.BLOCK_POCKET_MANAGER_BLOCK_ENTITY.get(), BlockPocketManagerBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.CLAYMORE_BLOCK_ENTITY.get(), ClaymoreBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.INVENTORY_SCANNER_BLOCK_ENTITY.get(), InventoryScannerBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.KEYPAD_BARREL_BLOCK_ENTITY.get(), KeypadBarrelBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.KEYPAD_CHEST_BLOCK_ENTITY.get(), KeypadChestBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.LASER_BLOCK_BLOCK_ENTITY.get(), LaserBlockBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.REINFORCED_HOPPER_BLOCK_ENTITY.get(), ReinforcedHopperBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.TROPHY_SYSTEM_BLOCK_ENTITY.get(), TrophySystemBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.REINFORCED_DISPENSER_BLOCK_ENTITY.get(), ReinforcedDispenserBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.REINFORCED_DROPPER_BLOCK_ENTITY.get(), ReinforcedDropperBlockEntity::getCapability);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SCContent.SECURITY_CAMERA_BLOCK_ENTITY.get(), SecurityCameraBlockEntity::getCapability);
	}

	@SubscribeEvent
	public static void onCreativeModeTabRegister(BuildCreativeModeTabContentsEvent event) {
		MutableHashedLinkedMap<ItemStack, TabVisibility> entries = event.getEntries();

		//@formatter:off
		if (event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
			entries.putAfter(new ItemStack(Items.LEVER), new ItemStack(SCContent.REINFORCED_LEVER.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(Items.STONE_BUTTON), new ItemStack(SCContent.REINFORCED_OAK_BUTTON.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.REINFORCED_OAK_BUTTON.get()), new ItemStack(SCContent.REINFORCED_STONE_BUTTON.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.REINFORCED_STONE_BUTTON.get()), new ItemStack(SCContent.PANIC_BUTTON.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(Items.STONE_PRESSURE_PLATE), new ItemStack(SCContent.REINFORCED_OAK_PRESSURE_PLATE.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.REINFORCED_OAK_PRESSURE_PLATE.get()), new ItemStack(SCContent.REINFORCED_STONE_PRESSURE_PLATE.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(Items.STICKY_PISTON), new ItemStack(SCContent.REINFORCED_PISTON.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.REINFORCED_PISTON.get()), new ItemStack(SCContent.REINFORCED_STICKY_PISTON.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(Items.DROPPER), new ItemStack(SCContent.REINFORCED_DISPENSER.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.REINFORCED_DISPENSER.get()), new ItemStack(SCContent.REINFORCED_DROPPER.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(Items.HOPPER), new ItemStack(SCContent.REINFORCED_HOPPER.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(Items.LECTERN), new ItemStack(SCContent.REINFORCED_LECTERN.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
		}
		else if (event.getTabKey().equals(CreativeModeTabs.COLORED_BLOCKS)) {
			//@formatter:off
			event.acceptAll(List.of(
					new ItemStack(SCContent.REINFORCED_WHITE_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_GRAY_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_GRAY_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_BLACK_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_BROWN_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_RED_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_ORANGE_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_YELLOW_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_LIME_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_GREEN_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_CYAN_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_BLUE_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_BLUE_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_PURPLE_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_MAGENTA_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_PINK_WOOL.get()),
					new ItemStack(SCContent.REINFORCED_WHITE_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_GRAY_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_GRAY_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_BLACK_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_BROWN_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_RED_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_ORANGE_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_YELLOW_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_LIME_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_GREEN_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_CYAN_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_BLUE_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_BLUE_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_PURPLE_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_MAGENTA_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_PINK_CARPET.get()),
					new ItemStack(SCContent.REINFORCED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_WHITE_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_GRAY_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_GRAY_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_BLACK_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_BROWN_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_RED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_ORANGE_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_YELLOW_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_LIME_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_GREEN_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_CYAN_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_BLUE_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_BLUE_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_PURPLE_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_MAGENTA_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_PINK_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_WHITE_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_GRAY_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_GRAY_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_BLACK_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_BROWN_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_RED_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_ORANGE_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_YELLOW_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_LIME_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_GREEN_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_CYAN_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_BLUE_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_BLUE_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_PURPLE_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_MAGENTA_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_PINK_CONCRETE.get()),
					new ItemStack(SCContent.REINFORCED_WHITE_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_GRAY_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_GRAY_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_BLACK_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_BROWN_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_RED_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_ORANGE_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_YELLOW_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_LIME_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_GREEN_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_CYAN_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_BLUE_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_BLUE_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_PURPLE_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_MAGENTA_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_PINK_GLAZED_TERRACOTTA.get()),
					new ItemStack(SCContent.REINFORCED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_TINTED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_WHITE_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_GRAY_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_GRAY_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_BLACK_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_BROWN_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_RED_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_ORANGE_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_YELLOW_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_LIME_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_GREEN_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_CYAN_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_BLUE_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_BLUE_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_PURPLE_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_MAGENTA_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_PINK_STAINED_GLASS.get()),
					new ItemStack(SCContent.REINFORCED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_WHITE_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_GRAY_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_GRAY_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_BLACK_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_BROWN_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_RED_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_ORANGE_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_YELLOW_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_LIME_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_GREEN_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_CYAN_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_LIGHT_BLUE_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_BLUE_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_PURPLE_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_MAGENTA_STAINED_GLASS_PANE.get()),
					new ItemStack(SCContent.REINFORCED_PINK_STAINED_GLASS_PANE.get())));
			//@formatter:on
		}
		else if (event.getTab().equals(SCCreativeModeTabs.DECORATION_TAB.get())) {
			entries.putAfter(new ItemStack(SCContent.REINFORCED_CHISELED_BOOKSHELF.get()), new ItemStack(SCContent.SECRET_OAK_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_OAK_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_OAK_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_OAK_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_SPRUCE_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_SPRUCE_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_SPRUCE_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_SPRUCE_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_BIRCH_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_BIRCH_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_BIRCH_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_BIRCH_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_JUNGLE_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_JUNGLE_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_JUNGLE_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_JUNGLE_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_ACACIA_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_ACACIA_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_ACACIA_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_ACACIA_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_DARK_OAK_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_DARK_OAK_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_DARK_OAK_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_DARK_OAK_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_MANGROVE_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_MANGROVE_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_MANGROVE_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_MANGROVE_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_CHERRY_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_CHERRY_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_CHERRY_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_CHERRY_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_BAMBOO_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_BAMBOO_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_BAMBOO_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_BAMBOO_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_CRIMSON_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_CRIMSON_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_CRIMSON_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_CRIMSON_HANGING_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_WARPED_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.putAfter(new ItemStack(SCContent.SECRET_WARPED_SIGN_ITEM.get()), new ItemStack(SCContent.SECRET_WARPED_HANGING_SIGN_ITEM.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
		}
		else if (event.getTabKey().equals(CreativeModeTabs.OP_BLOCKS)) {
			entries.put(new ItemStack(SCContent.ADMIN_TOOL.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
			entries.put(new ItemStack(SCContent.CODEBREAKER.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
		PotionBrewing.Builder builder = event.getBuilder();

		builder.addRecipe(Ingredient.of(Items.WATER_BUCKET), getPotionIngredient(Potions.HARMING, Potions.STRONG_HARMING), new ItemStack(SCContent.FAKE_WATER_BUCKET.get()));
		builder.addRecipe(Ingredient.of(Items.LAVA_BUCKET), getPotionIngredient(Potions.HEALING, Potions.STRONG_HEALING), new ItemStack(SCContent.FAKE_LAVA_BUCKET.get()));
	}

	private static Ingredient getPotionIngredient(Holder<Potion> normalPotion, Holder<Potion> strongPotion) {
		ItemStack normalPotionStack = PotionContents.createItemStack(Items.POTION, normalPotion);
		ItemStack strongPotionStack = PotionContents.createItemStack(Items.POTION, strongPotion);
		ItemStack normalSplashPotionStack = PotionContents.createItemStack(Items.SPLASH_POTION, normalPotion);
		ItemStack strongSplashPotionStack = PotionContents.createItemStack(Items.SPLASH_POTION, strongPotion);
		ItemStack normalLingeringPotionStack = PotionContents.createItemStack(Items.LINGERING_POTION, normalPotion);
		ItemStack strongLingeringPotionStack = PotionContents.createItemStack(Items.LINGERING_POTION, strongPotion);

		return Ingredient.of(normalPotionStack, strongPotionStack, normalSplashPotionStack, strongSplashPotionStack, normalLingeringPotionStack, strongLingeringPotionStack);
	}
}
