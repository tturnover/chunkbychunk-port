package xyz.immortius.chunkbychunk.common.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import xyz.immortius.chunkbychunk.common.network.GlitchMessagePayload;
import xyz.immortius.chunkbychunk.interop.Services;
import xyz.immortius.chunkbychunk.server.world.ChunkSpawnController;
import xyz.immortius.chunkbychunk.server.world.SpawnChunkHelper;

/**
 * This is the base for blocks that can be used to trigger spawning an "empty" chunk. Empty in this case is signified by the chunk not having bedrock at the base level.
 *
 * When used these blocks will spawn:
 * <ul>
 *     <li>The chunk they are in, if that chunk is empty</li>
 *     <li>Otherwise the chunk adjacent to theirs on the far side of the block from the player using it, wherever in the chunk the block is placed</li>
 * </ul>
 * The target depends only on the using player, so each player in multiplayer aims independently. Players looking at a
 * chunk spawner see an arrow pointing at the chunk that would spawn for them.
 */
public class SpawnChunkBlock extends Block {

    private final String biomeTheme;
    private final boolean random;

    public SpawnChunkBlock(String biomeTheme, boolean random, Properties blockProperties) {
        super(blockProperties);
        this.biomeTheme = biomeTheme;
        this.random = random;
    }

    /**
     * @param pos    The position of the spawner
     * @param player The player using the spawner
     * @return The horizontal direction from the player to the spawner, or the player's facing if standing on top of it
     */
    public static Direction getAimDirection(BlockPos pos, Player player) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dz = pos.getZ() + 0.5 - player.getZ();
        if (dx * dx + dz * dz < 0.25) {
            return player.getDirection();
        }
        return Direction.getApproximateNearest(dx, 0, dz);
    }

    /**
     * @param level  The level containing the spawner
     * @param pos    The position of the spawner
     * @param player The player using the spawner
     * @return The chunk the spawner would spawn when used by the player
     */
    public static ChunkPos getTargetChunk(LevelAccessor level, BlockPos pos, Player player) {
        ChunkPos ownChunk = ChunkPos.containing(pos);
        if (SpawnChunkHelper.isEmptyChunk(level, ownChunk)) {
            return ownChunk;
        }
        Direction direction = getAimDirection(pos, player);
        return new ChunkPos(ownChunk.x() + direction.getStepX(), ownChunk.z() + direction.getStepZ());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ChunkSpawnController chunkSpawnController = ChunkSpawnController.get(serverLevel.getServer());
            if (!chunkSpawnController.isValidForLevel(serverLevel, biomeTheme, random)) {
                player.sendOverlayMessage(Component.translatable("ui.chunkbychunk.chunkspawner.invalidlevel").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            ChunkPos targetChunk = getTargetChunk(level, pos, player);
            if (!SpawnChunkHelper.isEmptyChunk(level, targetChunk)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    Services.PLATFORM.sendToPlayer(serverPlayer, new GlitchMessagePayload("ui.chunkbychunk.chunkspawner.occupied"));
                }
                return InteractionResult.FAIL;
            }
            // Offset by whole chunks so the position stays unique to this spawner (it seeds the unstable spawner)
            ChunkPos ownChunk = ChunkPos.containing(pos);
            BlockPos targetPos = pos.atY(level.getMaxY()).offset((targetChunk.x() - ownChunk.x()) * 16, 0, (targetChunk.z() - ownChunk.z()) * 16);
            // Continue the terrain of the chunk the spawner stands in (e.g. a chunk from an unstable spawner)
            ChunkPos referenceChunk = targetChunk.equals(ownChunk) ? null : ownChunk;
            if (chunkSpawnController.request(serverLevel, biomeTheme, random, targetPos, false, referenceChunk)) {
                level.playSound(null, pos, Services.PLATFORM.spawnChunkSoundEffect(), SoundSource.BLOCKS, 1.0f, 1.0f);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return InteractionResult.SUCCESS;
            }
            player.sendOverlayMessage(Component.translatable("ui.chunkbychunk.chunkspawner.alreadyspawning").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public String getBiomeTheme() {
        return biomeTheme;
    }

    public boolean isRandom() {
        return random;
    }
}
