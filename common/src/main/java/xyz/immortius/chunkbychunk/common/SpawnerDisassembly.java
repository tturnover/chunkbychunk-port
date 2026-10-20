package xyz.immortius.chunkbychunk.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import xyz.immortius.chunkbychunk.common.blocks.SpawnChunkBlock;
import xyz.immortius.chunkbychunk.interop.Services;

import java.util.Optional;

/**
 * Shift + left clicking a placed chunk spawner with a pickaxe reverts it to a World Core block and drops the other
 * ingredients of its crafting recipe.
 */
public final class SpawnerDisassembly {

    private SpawnerDisassembly() {
    }

    /**
     * Called when a player starts to left click a block, on both client and server.
     * @return Whether the click disassembled a spawner, in which case the normal block breaking must be cancelled
     */
    public static boolean onLeftClickBlock(Player player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ItemStack tool = player.getMainHandItem();
        if (!player.isShiftKeyDown() || !tool.is(ItemTags.PICKAXES) || !(state.getBlock() instanceof SpawnChunkBlock)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            disassemble(serverLevel, pos, state, player, tool);
        }
        return true;
    }

    private static void disassemble(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
        Block worldCore = Services.PLATFORM.worldCoreBlock();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        // Spawner recipes share the spawner's id
        Optional<? extends Recipe<?>> recipe = level.getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, blockId))
                .map(holder -> holder.value());
        if (recipe.isPresent() && recipe.get() instanceof ShapedRecipe shapedRecipe) {
            for (Optional<Ingredient> ingredient : shapedRecipe.getIngredients()) {
                ingredient.flatMap(i -> i.items().findFirst()).map(Holder::value).ifPresent(item -> {
                    if (item != worldCore.asItem() && !leavesRemainder(item)) {
                        Block.popResource(level, pos, new ItemStack(item));
                    }
                });
            }
        }
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.setBlock(pos, worldCore.defaultBlockState(), Block.UPDATE_ALL);
        if (!player.getAbilities().instabuild) {
            tool.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Ingredients like water buckets give back a container when crafted, so the crafting already returned them
     */
    @SuppressWarnings("deprecation")
    private static boolean leavesRemainder(Item item) {
        return item.getCraftingRemainder() != null;
    }
}
