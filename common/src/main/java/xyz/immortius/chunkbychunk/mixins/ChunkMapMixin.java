package xyz.immortius.chunkbychunk.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;
import xyz.immortius.chunkbychunk.server.world.ChunkSpawnController;
import xyz.immortius.chunkbychunk.server.world.ControllableChunkMap;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ControllableChunkMap {

    @Final
    @Shadow
    ServerLevel level;

    @Shadow
    private void markChunkPendingToSend(ServerPlayer player, ChunkPos chunkPos) {
    }

    public void forceReloadChunk(ChunkPos chunk) {
        ChunkMap thisMap = (ChunkMap) (Object) this;
        for (ServerPlayer player : thisMap.getPlayers(chunk, false)) {
            markChunkPendingToSend(player, chunk);
        }
    }

    @Inject(method = "onFullChunkStatusChange", at = @At("HEAD"))
    public void onFullStatusChange(ChunkPos pos, FullChunkStatus status, CallbackInfo ci) {
        if (ChunkByChunkConfig.get().getGeneration().isSpawnChunkStrip() && status.isOrAfter(FullChunkStatus.ENTITY_TICKING) && level.dimension().equals(Level.OVERWORLD) && ChunkPos.containing(level.getRespawnData().pos()).x() == pos.x()) {
            BlockPos blockPos = pos.getMiddleBlockPosition(level.getMaxY());
            ChunkSpawnController.get(level.getServer()).request(level, "", false, blockPos);
        }
    }
}
