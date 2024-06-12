package net.geforcemods.securitycraft.misc;

import java.util.Map;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IReinforcedBlock;
import net.geforcemods.securitycraft.items.UniversalBlockReinforcerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;

public class BlockUnreinforcingRecipe extends AbstractReinforcerRecipe {
	public BlockUnreinforcingRecipe(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public Map<Block, Block> getBlockMap() {
		return IReinforcedBlock.SECURITYCRAFT_TO_VANILLA;
	}

	@Override
	public boolean isCorrectReinforcer(ItemStack reinforcer) {
		return !UniversalBlockReinforcerItem.isReinforcing(reinforcer);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SCContent.BLOCK_UNREINFORCING_RECIPE_SERIALIZER.get();
	}
}
