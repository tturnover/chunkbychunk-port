package xyz.immortius.chunkbychunk.neoforge;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.TriState;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import xyz.immortius.chunkbychunk.common.SpawnerDisassembly;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.CommonEventHandler;
import xyz.immortius.chunkbychunk.common.blockEntities.BedrockChestBlockEntity;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldForgeBlockEntity;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldMenderBlockEntity;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldScannerBlockEntity;
import xyz.immortius.chunkbychunk.common.blocks.ChunkByChunkBlockProperties;
import xyz.immortius.chunkbychunk.common.blocks.*;
import xyz.immortius.chunkbychunk.common.menus.BedrockChestMenu;
import xyz.immortius.chunkbychunk.common.menus.WorldForgeMenu;
import xyz.immortius.chunkbychunk.common.menus.WorldMenderMenu;
import xyz.immortius.chunkbychunk.common.menus.WorldScannerMenu;
import xyz.immortius.chunkbychunk.common.network.ConfigSyncPayload;
import xyz.immortius.chunkbychunk.common.network.GlitchMessagePayload;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;
import xyz.immortius.chunkbychunk.config.system.ConfigSystem;
import xyz.immortius.chunkbychunk.server.ServerEventHandler;
import xyz.immortius.chunkbychunk.server.commands.SpawnChunkCommand;
import xyz.immortius.chunkbychunk.server.world.SkyChunkGenerator;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The NeoForge mod, registers all mod elements for NeoForge
 */
@Mod(ChunkByChunkConstants.MOD_ID)
public class ChunkByChunkMod {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(BuiltInRegistries.MENU, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, ChunkByChunkConstants.MOD_ID);

    public static final DeferredBlock<SpawnChunkBlock> SPAWN_CHUNK_BLOCK = BLOCKS.registerBlock("chunkspawner", p -> new SpawnChunkBlock("", false, p), () -> ChunkByChunkBlockProperties.stoneLike());
    public static final DeferredBlock<SpawnChunkBlock> UNSTABLE_SPAWN_CHUNK_BLOCK = BLOCKS.registerBlock("unstablechunkspawner", p -> new SpawnChunkBlock("", true, p), () -> ChunkByChunkBlockProperties.stoneLike());
    public static final DeferredBlock<BedrockChestBlock> BEDROCK_CHEST_BLOCK = BLOCKS.registerBlock("bedrockchest", BedrockChestBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(-1, 3600000.0F).noLootTable().isValidSpawn(((state, getter, pos, type) -> false)));
    public static final DeferredBlock<Block> WORLD_CORE_BLOCK = BLOCKS.registerBlock("worldcore", Block::new, () -> ChunkByChunkBlockProperties.stoneLike().strength(3.0F).lightLevel((state) -> 7));
    public static final DeferredBlock<WorldForgeBlock> WORLD_FORGE_BLOCK = BLOCKS.registerBlock("worldforge", WorldForgeBlock::new, () -> ChunkByChunkBlockProperties.stoneLike().strength(3.5F).lightLevel((state) -> 7));
    public static final DeferredBlock<WorldScannerBlock> WORLD_SCANNER_BLOCK = BLOCKS.registerBlock("worldscanner", WorldScannerBlock::new, () -> ChunkByChunkBlockProperties.stoneLike().strength(3.5F).lightLevel((state) -> 4));
    public static final DeferredBlock<WorldMenderBlock> WORLD_MENDER_BLOCK = BLOCKS.registerBlock("worldmender", WorldMenderBlock::new, () -> ChunkByChunkBlockProperties.stoneLike().strength(3.5F).lightLevel((state) -> 4));

    public static final DeferredItem<BlockItem> SPAWN_CHUNK_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(SPAWN_CHUNK_BLOCK);
    public static final DeferredItem<BlockItem> UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(UNSTABLE_SPAWN_CHUNK_BLOCK);
    public static final DeferredItem<BlockItem> BEDROCK_CHEST_ITEM = ITEMS.registerSimpleBlockItem(BEDROCK_CHEST_BLOCK);
    public static final DeferredItem<BlockItem> WORLD_CORE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(WORLD_CORE_BLOCK);
    public static final DeferredItem<BlockItem> WORLD_FORGE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(WORLD_FORGE_BLOCK);
    public static final DeferredItem<BlockItem> WORLD_SCANNER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(WORLD_SCANNER_BLOCK);
    public static final DeferredItem<BlockItem> WORLD_MENDER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(WORLD_MENDER_BLOCK);

    public static final DeferredItem<Item> WORLD_FRAGMENT_ITEM = ITEMS.registerSimpleItem("worldfragment");
    public static final DeferredItem<Item> WORLD_SHARD_ITEM = ITEMS.registerSimpleItem("worldshard");
    public static final DeferredItem<Item> WORLD_CRYSTAL_ITEM = ITEMS.registerSimpleItem("worldcrystal");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BedrockChestBlockEntity>> BEDROCK_CHEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("bedrockchestentity", () -> new BlockEntityType<>(BedrockChestBlockEntity::new, Set.of(BEDROCK_CHEST_BLOCK.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldForgeBlockEntity>> WORLD_FORGE_BLOCK_ENTITY = BLOCK_ENTITIES.register("worldforgeentity", () -> new BlockEntityType<>(WorldForgeBlockEntity::new, Set.of(WORLD_FORGE_BLOCK.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldScannerBlockEntity>> WORLD_SCANNER_BLOCK_ENTITY = BLOCK_ENTITIES.register("worldscannerentity", () -> new BlockEntityType<>(WorldScannerBlockEntity::new, Set.of(WORLD_SCANNER_BLOCK.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldMenderBlockEntity>> WORLD_MENDER_BLOCK_ENTITY = BLOCK_ENTITIES.register("worldmenderentity", () -> new BlockEntityType<>(WorldMenderBlockEntity::new, Set.of(WORLD_MENDER_BLOCK.get())));

    public static final DeferredHolder<MenuType<?>, MenuType<BedrockChestMenu>> BEDROCK_CHEST_MENU = CONTAINERS.register("bedrockchestmenu", () -> new MenuType<>(BedrockChestMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<WorldForgeMenu>> WORLD_FORGE_MENU = CONTAINERS.register("worldforgemenu", () -> new MenuType<>(WorldForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<WorldScannerMenu>> WORLD_SCANNER_MENU = CONTAINERS.register("worldscannermenu", () -> new MenuType<>(WorldScannerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<WorldMenderMenu>> WORLD_MENDER_MENU = CONTAINERS.register("worldmendermenu", () -> new MenuType<>(WorldMenderMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPAWNER_ARROW_PARTICLE = PARTICLE_TYPES.register("spawner_arrow", () -> new SimpleParticleType(true));
    public static final DeferredHolder<SoundEvent, SoundEvent> SPAWN_CHUNK_SOUND_EVENT = SOUNDS.register("spawnchunkevent", () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "chunk_spawn_sound")));

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<SkyChunkGenerator>> SKY_CHUNK_GENERATOR = CHUNK_GENERATORS.register("skychunkgenerator", () -> SkyChunkGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<SkyChunkGenerator>> OLD_NETHER_CHUNK_GENERATOR = CHUNK_GENERATORS.register("netherchunkgenerator", () -> SkyChunkGenerator.OLD_NETHER_CODEC);

    public static final List<Supplier<ItemStack>> THEMED_SPAWN_CHUNK_ITEMS = new ArrayList<>();

    private static final String PROTOCOL_VERSION = "1";

    static {
        for (String biomeTheme : ChunkByChunkConstants.BIOME_THEMES) {
            DeferredBlock<SpawnChunkBlock> spawnBlock = BLOCKS.registerBlock(biomeTheme + ChunkByChunkConstants.BIOME_CHUNK_BLOCK_SUFFIX, p -> new SpawnChunkBlock(biomeTheme, false, p), () -> ChunkByChunkBlockProperties.stoneLike());
            DeferredItem<BlockItem> spawnBlockItem = ITEMS.registerSimpleBlockItem(biomeTheme + ChunkByChunkConstants.BIOME_CHUNK_BLOCK_ITEM_SUFFIX, spawnBlock);
            THEMED_SPAWN_CHUNK_ITEMS.add(() -> spawnBlockItem.get().getDefaultInstance());
        }
    }

    public ChunkByChunkMod(IEventBus eventBus, ModContainer container) {
        new ConfigSystem().synchConfig(Paths.get(ChunkByChunkConstants.DEFAULT_CONFIG_PATH, ChunkByChunkConstants.CONFIG_FILE), ChunkByChunkConfig.get());

        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);

        BLOCK_ENTITIES.register(eventBus);
        CONTAINERS.register(eventBus);
        SOUNDS.register(eventBus);
        PARTICLE_TYPES.register(eventBus);
        CHUNK_GENERATORS.register(eventBus);

        eventBus.addListener(this::updateCreativeTabs);
        eventBus.addListener(this::registerPayloadHandler);
        NeoForge.EVENT_BUS.addListener(this::registerResourceReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlaceItem);
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && event.getHand() == InteractionHand.MAIN_HAND
                    && SpawnerDisassembly.onLeftClickBlock(event.getEntity(), event.getLevel(), event.getPos())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        if (FMLEnvironment.getDist().isClient()) {
            ChunkByChunkClientMod.init(eventBus, container);
        }
    }

    public void updateCreativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            e.accept(WORLD_FRAGMENT_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(WORLD_SHARD_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(WORLD_CRYSTAL_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(WORLD_CORE_BLOCK_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(WORLD_FORGE_BLOCK_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(WORLD_SCANNER_BLOCK_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(WORLD_MENDER_BLOCK_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(SPAWN_CHUNK_BLOCK_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            e.accept(UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            for (Supplier<ItemStack> biomeThemeSpawner : THEMED_SPAWN_CHUNK_ITEMS) {
                e.accept(biomeThemeSpawner.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    public void registerPayloadHandler(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC, (payload, context) -> payload.apply());
        registrar.playToClient(GlitchMessagePayload.TYPE, GlitchMessagePayload.STREAM_CODEC, (payload, context) -> payload.apply());
    }

    public void registerResourceReloadListeners(AddServerReloadListenersEvent e) {
        e.addListener(Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "server_data"), (ResourceManagerReloadListener) ServerEventHandler::onResourceManagerReload);
    }

    public void registerCommands(RegisterCommandsEvent event) {
        SpawnChunkCommand.register(event.getDispatcher());
    }

    public void onPlaceItem(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        BlockPos placePos = pos.relative(event.getFace());
        if (!CommonEventHandler.isBlockPlacementAllowed(placePos, event.getEntity(), event.getLevel())) {
            event.setUseItem(TriState.FALSE);
        }
    }

    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, ConfigSyncPayload.fromConfig());
        }
    }

    public void onServerStarted(ServerStartedEvent event) {
        ServerEventHandler.onServerStarted(event.getServer());
    }

    public void onServerStarting(ServerAboutToStartEvent event) {
        ServerEventHandler.onServerStarting(event.getServer());
    }

    public void onServerTick(ServerTickEvent.Post tickEvent) {
        ServerEventHandler.onLevelTick(tickEvent.getServer());
    }

}
