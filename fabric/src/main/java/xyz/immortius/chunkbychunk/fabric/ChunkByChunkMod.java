package xyz.immortius.chunkbychunk.fabric;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionHand;
import xyz.immortius.chunkbychunk.common.SpawnerDisassembly;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.function.Function;

/**
 * Common mod initialization
 */
public class ChunkByChunkMod implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger(ChunkByChunkConstants.MOD_ID);

    public static SpawnChunkBlock SPAWN_CHUNK_BLOCK;
    public static Block UNSTABLE_SPAWN_CHUNK_BLOCK;
    public static Block BEDROCK_CHEST_BLOCK;
    public static Block WORLD_CORE_BLOCK;
    public static Block WORLD_FORGE_BLOCK;
    public static Block WORLD_SCANNER_BLOCK;
    public static Block WORLD_MENDER_BLOCK;

    public static Item SPAWN_CHUNK_BLOCK_ITEM;
    public static Item UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM;

    public static Item BEDROCK_CHEST_BLOCK_ITEM;
    public static Item WORLD_CORE_BLOCK_ITEM;
    public static Item WORLD_FORGE_BLOCK_ITEM;
    public static Item WORLD_SCANNER_BLOCK_ITEM;
    public static Item WORLD_MENDER_BLOCK_ITEM;

    public static Item WORLD_FRAGMENT_ITEM;
    public static Item WORLD_SHARD_ITEM;
    public static Item WORLD_CRYSTAL_ITEM;

    public static BlockEntityType<BedrockChestBlockEntity> BEDROCK_CHEST_BLOCK_ENTITY;
    public static BlockEntityType<WorldForgeBlockEntity> WORLD_FORGE_BLOCK_ENTITY;
    public static BlockEntityType<WorldScannerBlockEntity> WORLD_SCANNER_BLOCK_ENTITY;
    public static BlockEntityType<WorldMenderBlockEntity> WORLD_MENDER_BLOCK_ENTITY;

    public static MenuType<BedrockChestMenu> BEDROCK_CHEST_MENU;
    public static MenuType<WorldForgeMenu> WORLD_FORGE_MENU;
    public static MenuType<WorldScannerMenu> WORLD_SCANNER_MENU;
    public static MenuType<WorldMenderMenu> WORLD_MENDER_MENU;

    public static SoundEvent SPAWN_CHUNK_SOUND_EVENT;
    public static SimpleParticleType SPAWNER_ARROW_PARTICLE;

    public static List<Item> biomeThemedBlockItems;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing");

        SPAWN_CHUNK_BLOCK = registerBlock("chunkspawner", p -> new SpawnChunkBlock("", false, p), ChunkByChunkBlockProperties.stoneLike());
        UNSTABLE_SPAWN_CHUNK_BLOCK = registerBlock("unstablechunkspawner", p -> new SpawnChunkBlock("", true, p), ChunkByChunkBlockProperties.stoneLike());

        BEDROCK_CHEST_BLOCK = registerBlock("bedrockchest", BedrockChestBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(-1, 3600000.0F).noLootTable().isValidSpawn(((state, getter, pos, arg) -> false)));
        WORLD_CORE_BLOCK = registerBlock("worldcore", Block::new, ChunkByChunkBlockProperties.stoneLike().strength(3.0F).lightLevel((state) -> 7));
        WORLD_FORGE_BLOCK = registerBlock("worldforge", WorldForgeBlock::new, ChunkByChunkBlockProperties.stoneLike().strength(3.5F).lightLevel((state) -> 7));
        WORLD_SCANNER_BLOCK = registerBlock("worldscanner", WorldScannerBlock::new, ChunkByChunkBlockProperties.stoneLike().strength(3.5F).lightLevel((state) -> 4));
        WORLD_MENDER_BLOCK = registerBlock("worldmender", WorldMenderBlock::new, ChunkByChunkBlockProperties.stoneLike().strength(3.5F).lightLevel((state) -> 4));

        SPAWN_CHUNK_BLOCK_ITEM = registerBlockItem("chunkspawner", SPAWN_CHUNK_BLOCK);
        UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM = registerBlockItem("unstablechunkspawner", UNSTABLE_SPAWN_CHUNK_BLOCK);

        List<Item> themeSpawnBlockItems = new ArrayList<>();
        for (String biomeTheme : ChunkByChunkConstants.BIOME_THEMES) {
            Block spawnBlock = registerBlock(biomeTheme + ChunkByChunkConstants.BIOME_CHUNK_BLOCK_SUFFIX, p -> new SpawnChunkBlock(biomeTheme, false, p), ChunkByChunkBlockProperties.stoneLike());
            Item item = registerBlockItem(biomeTheme + ChunkByChunkConstants.BIOME_CHUNK_BLOCK_ITEM_SUFFIX, spawnBlock);
            themeSpawnBlockItems.add(item);
        }
        biomeThemedBlockItems = ImmutableList.copyOf(themeSpawnBlockItems);

        BEDROCK_CHEST_BLOCK_ITEM = registerBlockItem("bedrockchest", BEDROCK_CHEST_BLOCK);
        WORLD_CORE_BLOCK_ITEM = registerBlockItem("worldcore", WORLD_CORE_BLOCK);
        WORLD_FORGE_BLOCK_ITEM = registerBlockItem("worldforge", WORLD_FORGE_BLOCK);
        WORLD_SCANNER_BLOCK_ITEM = registerBlockItem("worldscanner", WORLD_SCANNER_BLOCK);
        WORLD_MENDER_BLOCK_ITEM = registerBlockItem("worldmender", WORLD_MENDER_BLOCK);

        WORLD_FRAGMENT_ITEM = registerItem("worldfragment");
        WORLD_SHARD_ITEM = registerItem("worldshard");
        WORLD_CRYSTAL_ITEM = registerItem("worldcrystal");

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.insertAfter(Items.MUSIC_DISC_PIGSTEP, WORLD_FRAGMENT_ITEM, WORLD_SHARD_ITEM, WORLD_CRYSTAL_ITEM, WORLD_CORE_BLOCK_ITEM, WORLD_FORGE_BLOCK_ITEM, WORLD_SCANNER_BLOCK_ITEM, WORLD_MENDER_BLOCK_ITEM, SPAWN_CHUNK_BLOCK_ITEM, UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM);
            content.insertAfter(UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM, themeSpawnBlockItems.toArray(new Item[]{}));
        });

        BEDROCK_CHEST_BLOCK_ENTITY = registerBlockEntity("bedrockchestentity", BedrockChestBlockEntity::new, BEDROCK_CHEST_BLOCK);
        WORLD_FORGE_BLOCK_ENTITY = registerBlockEntity("worldforgeentity", WorldForgeBlockEntity::new, WORLD_FORGE_BLOCK);
        WORLD_SCANNER_BLOCK_ENTITY = registerBlockEntity("worldscannerentity", WorldScannerBlockEntity::new, WORLD_SCANNER_BLOCK);
        WORLD_MENDER_BLOCK_ENTITY = registerBlockEntity("worldmenderentity", WorldMenderBlockEntity::new, WORLD_MENDER_BLOCK);

        BEDROCK_CHEST_MENU = registerMenu("bedrockchestmenu", BedrockChestMenu::new);
        WORLD_FORGE_MENU = registerMenu("worldforgemenu", WorldForgeMenu::new);
        WORLD_SCANNER_MENU = registerMenu("worldscannermenu", WorldScannerMenu::new);
        WORLD_MENDER_MENU = registerMenu("worldmendermenu", WorldMenderMenu::new);

        SPAWN_CHUNK_SOUND_EVENT = Registry.register(BuiltInRegistries.SOUND_EVENT, createId("spawnchunkevent"), SoundEvent.createVariableRangeEvent(createId("chunk_spawn_sound")));
        SPAWNER_ARROW_PARTICLE = Registry.register(BuiltInRegistries.PARTICLE_TYPE, createId("spawner_arrow"), FabricParticleTypes.simple(true));

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, createId("skychunkgenerator"), SkyChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, createId("netherchunkgenerator"), SkyChunkGenerator.OLD_NETHER_CODEC);

        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(createId("server_data"), (ResourceManagerReloadListener) ServerEventHandler::onResourceManagerReload);

        ServerLifecycleEvents.SERVER_STARTED.register(ServerEventHandler::onServerStarted);
        ServerLifecycleEvents.SERVER_STARTING.register(ServerEventHandler::onServerStarting);
        ServerTickEvents.END_SERVER_TICK.register(ServerEventHandler::onLevelTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SpawnChunkCommand.register(dispatcher);
        });

        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GlitchMessagePayload.TYPE, GlitchMessagePayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayNetworking.send(handler.getPlayer(), ConfigSyncPayload.fromConfig());
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                hand == InteractionHand.MAIN_HAND && SpawnerDisassembly.onLeftClickBlock(player, world, pos) ? InteractionResult.SUCCESS : InteractionResult.PASS);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockPos placePos = pos.relative(hitResult.getDirection());
            if (!CommonEventHandler.isBlockPlacementAllowed(placePos, player, world)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        });

        setupConfig();
    }

    private void setupConfig() {
        new ConfigSystem().synchConfig(Paths.get(ChunkByChunkConstants.DEFAULT_CONFIG_PATH, ChunkByChunkConstants.CONFIG_FILE), ChunkByChunkConfig.get());
    }

    private static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, createId(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item registerBlockItem(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, createId(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    private static Item registerItem(String name) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, createId(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
    }

    private static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, createId(name), new BlockEntityType<>(factory, Set.of(block)));
    }

    private static <T extends AbstractContainerMenu> MenuType<T> registerMenu(String name, MenuType.MenuSupplier<T> factory) {
        return Registry.register(BuiltInRegistries.MENU, createId(name), new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }

    private static Identifier createId(String id) {
        return Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, id);
    }

}
