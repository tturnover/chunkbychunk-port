package xyz.immortius.chunkbychunk.common.blockEntities;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xyz.immortius.chunkbychunk.common.menus.WorldForgeMenu;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;
import xyz.immortius.chunkbychunk.interop.Services;

import java.util.List;
import java.util.Map;

/**
 * World Forge Block Entity - this holds the input and output of the world forge,
 * and handles dissolving the input to crystalise the output
 */
public class WorldForgeBlockEntity extends BaseFueledBlockEntity {
    public static final int NUM_ITEM_SLOTS = 2;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESULT = 1;

    public static final int NUM_DATA_ITEMS = 2;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_GOAL = 1;

    public static final Map<Item, FuelValueSupplier> FUEL;
    public static final Map<TagKey<Item>, FuelValueSupplier> FUEL_TAGS;
    // The forge only produces World Fragments: fusing them into Shards, Crystals and Cores is done by crafting
    private static final Item PRODUCED_ITEM = Services.PLATFORM.worldFragmentItem();

    private static final int[] SLOTS_FOR_UP = new int[]{SLOT_INPUT};
    private static final int[] SLOTS_FOR_DOWN = new int[]{SLOT_RESULT};

    // Fuel is taken from existing vanilla and common (c:) item tags, so blocks from other mods in those tags work too
    private static final List<String> SOIL_FUEL_TAGS = List.of(
            "minecraft:dirt", "minecraft:grass_blocks", "minecraft:moss_blocks", "minecraft:mud", "minecraft:sand",
            "minecraft:soul_fire_base_blocks", "c:sands", "c:gravels");
    private static final List<String> STONE_FUEL_TAGS = List.of(
            "minecraft:stone_crafting_materials", "minecraft:terracotta",
            "c:stones", "c:cobblestones", "c:netherracks", "c:end_stones", "c:sandstone/blocks");

    private int progress;
    private int goal;

    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int id) {
            return switch (id) {
                case DATA_PROGRESS -> progress;
                case DATA_GOAL -> goal;
                default -> 0;
            };
        }

        public void set(int id, int value) {
            switch (id) {
                case DATA_PROGRESS -> progress = value;
                case DATA_GOAL -> goal = value;
            }

        }

        public int getCount() {
            return NUM_DATA_ITEMS;
        }
    };

    static {
        ImmutableMap.Builder<Item, FuelValueSupplier> fuelBuilder = ImmutableMap.builder();

        fuelBuilder.put(Services.PLATFORM.worldFragmentItem(), () -> ChunkByChunkConfig.get().getWorldForge().getFragmentFuelCost());
        fuelBuilder.put(Services.PLATFORM.worldShardItem(), () -> ChunkByChunkConfig.get().getWorldForge().getFragmentFuelCost() * 4);
        fuelBuilder.put(Services.PLATFORM.worldCrystalItem(), () -> ChunkByChunkConfig.get().getWorldForge().getFragmentFuelCost() * 16);
        // No existing tag groups these, so the strong fuel items are listed directly
        for (Item strongFuel : List.of(Items.ECHO_SHARD, Items.TRIAL_KEY, Items.BREEZE_ROD)) {
            fuelBuilder.put(strongFuel, () -> ChunkByChunkConfig.get().getWorldForge().getStrongFuelValue());
        }

        FUEL = fuelBuilder.build();

        ImmutableMap.Builder<TagKey<Item>, FuelValueSupplier> tagBuilder = ImmutableMap.builder();
        for (String tag : SOIL_FUEL_TAGS) {
            tagBuilder.put(TagKey.create(Registries.ITEM, Identifier.parse(tag)), () -> ChunkByChunkConfig.get().getWorldForge().getSoilFuelValue());
        }
        for (String tag : STONE_FUEL_TAGS) {
            tagBuilder.put(TagKey.create(Registries.ITEM, Identifier.parse(tag)), () -> ChunkByChunkConfig.get().getWorldForge().getStoneFuelValue());
        }
        FUEL_TAGS = tagBuilder.build();
    }

    public WorldForgeBlockEntity(BlockPos pos, BlockState state) {
        super(Services.PLATFORM.worldForgeEntity(), pos, state, NUM_ITEM_SLOTS, SLOT_INPUT, FUEL, FUEL_TAGS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.chunkbychunk.worldforge");
    }

    @Override
    protected AbstractContainerMenu createMenu(int menuId, Inventory inventory) {
        return new WorldForgeMenu(menuId, inventory, this, this.dataAccess);
    }

    public static boolean isWorldForgeFuel(ItemStack itemStack) {
        return FUEL.get(itemStack.getItem()) != null || FUEL_TAGS.keySet().stream().anyMatch(itemStack::is);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, WorldForgeBlockEntity entity) {
        // Consume available fuel
        if (entity.getRemainingFuel() > 0) {
            int consumeAmount = entity.consumeFuel(ChunkByChunkConfig.get().getWorldForge().getProductionRate());
            entity.progress += consumeAmount;
        }

        // Blocked while the output is full, or holds something other than fragments (e.g. from an older version)
        ItemStack outputItems = entity.getItem(SLOT_RESULT);
        if (!outputItems.isEmpty() && (!outputItems.is(PRODUCED_ITEM) || outputItems.getCount() == outputItems.getMaxStackSize())) {
            return;
        }

        boolean changed = entity.checkConsumeFuelItem();

        int itemCost = ChunkByChunkConfig.get().getWorldForge().getFragmentFuelCost();
        entity.goal = itemCost;

        // Produce a fragment if we can
        if (entity.progress >= itemCost) {
            entity.progress -= itemCost;
            changed = true;
            if (outputItems.isEmpty()) {
                entity.setItem(SLOT_RESULT, PRODUCED_ITEM.getDefaultInstance());
            } else {
                outputItems.grow(1);
            }
        }

        if (changed) {
            setChanged(level, blockPos, blockState);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.UP) {
            return SLOTS_FOR_UP;
        }
        return SLOTS_FOR_DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == SLOT_RESULT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack item) {
        if (slot == SLOT_RESULT) {
            return false;
        }
        return super.canPlaceItem(slot, item);
    }

}
