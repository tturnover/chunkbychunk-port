package xyz.immortius.chunkbychunk.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.CommonEventHandler;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;

/**
 * Payload sent from server to client to synchronise the gameplay config options the client needs to know about
 */
public record ConfigSyncPayload(boolean chunkByChunkWorld, boolean blockPlacementAllowedOutsideSpawnedChunks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "config"));

    public static final StreamCodec<ByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ConfigSyncPayload::chunkByChunkWorld,
            ByteBufCodecs.BOOL, ConfigSyncPayload::blockPlacementAllowedOutsideSpawnedChunks,
            ConfigSyncPayload::new);

    public static ConfigSyncPayload fromConfig() {
        return new ConfigSyncPayload(CommonEventHandler.isChunkByChunkWorld(), ChunkByChunkConfig.get().getGameplayConfig().isBlockPlacementAllowedOutsideSpawnedChunks());
    }

    public void apply() {
        CommonEventHandler.setChunkByChunkWorld(chunkByChunkWorld);
        ChunkByChunkConfig.get().getGameplayConfig().setBlockPlacementAllowedOutsideSpawnedChunks(blockPlacementAllowedOutsideSpawnedChunks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
