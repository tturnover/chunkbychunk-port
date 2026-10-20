package xyz.immortius.chunkbychunk.server.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.util.ChangeDimensionHelper;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChunkSpawnController extends SavedData {

    private static final int EDGE_PARTICLES = 3;
    private static final int MAX_EDGE_PARTICLE_BURSTS_PER_TICK = 96;

    public static final Codec<ChunkSpawnController> CODEC = RecordCodecBuilder.create(i -> i.group(
            SpawnRequest.CODEC.listOf().optionalFieldOf("requests", List.of()).forGetter(c -> List.copyOf(c.requests)),
            SpawnRequest.CODEC.optionalFieldOf("currentRequest").forGetter(c -> Optional.ofNullable(c.currentSpawnRequest)),
            Codec.STRING.optionalFieldOf("phase", "COPY_BIOMES").forGetter(c -> c.phase == null ? "COPY_BIOMES" : c.phase.name()),
            Codec.BOOL.optionalFieldOf("forcedTargetChunk", false).forGetter(c -> c.forcedTargetChunk),
            Codec.INT.optionalFieldOf("currentLayer", 0).forGetter(c -> c.currentLayer),
            SourceOffset.CODEC.listOf().optionalFieldOf("sourceOffsets", List.of()).forGetter(ChunkSpawnController::getSourceOffsetList)
    ).apply(i, ChunkSpawnController::new));

    public static final SavedDataType<ChunkSpawnController> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "chunkspawncontroller"),
            ChunkSpawnController::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    @Nullable
    private transient MinecraftServer server;

    private final Deque<SpawnRequest> requests = new ArrayDeque<>();

    @Nullable
    private SpawnRequest currentSpawnRequest = null;

    /**
     * For spawned chunks whose content was copied from a different position than their own (unstable spawner chunks,
     * and chunks continuing them), the offset from the chunk to its source chunk, by level and packed chunk position.
     */
    private final Map<ResourceKey<Level>, Map<Long, Long>> sourceOffsets = new HashMap<>();

    @Nullable
    private SpawnPhase phase;
    private boolean forcedTargetChunk;
    private int currentLayer;

    @Nullable
    private transient ServerLevel sourceLevel;
    @Nullable
    private transient ServerLevel targetLevel;
    @Nullable
    private transient CompletableFuture<ChunkResult<ChunkAccess>> sourceChunkFuture;

    public static ChunkSpawnController get(MinecraftServer server) {
        ChunkSpawnController controller = server.getLevel(Level.OVERWORLD).getChunkSource().getDataStorage().computeIfAbsent(TYPE);
        if (controller.server != server) {
            controller.attach(server);
        }
        return controller;
    }

    private ChunkSpawnController() {
    }

    private ChunkSpawnController(List<SpawnRequest> requests, Optional<SpawnRequest> currentRequest, String phase, boolean forcedTargetChunk, int currentLayer, List<SourceOffset> sourceOffsets) {
        this.requests.addAll(requests);
        for (SourceOffset offset : sourceOffsets) {
            this.sourceOffsets.computeIfAbsent(offset.level(), k -> new HashMap<>()).put(offset.chunk(), offset.offset());
        }
        this.currentSpawnRequest = currentRequest.orElse(null);
        if (currentSpawnRequest != null) {
            this.phase = SpawnPhase.valueOf(phase);
            this.forcedTargetChunk = forcedTargetChunk;
            this.currentLayer = currentLayer;
        }
    }

    private void attach(MinecraftServer server) {
        this.server = server;
        if (currentSpawnRequest != null) {
            sourceLevel = server.getLevel(currentSpawnRequest.sourceLevel);
            targetLevel = server.getLevel(currentSpawnRequest.targetLevel);
            sourceChunkFuture = sourceLevel.getChunkSource().getChunkFuture(currentSpawnRequest.sourceChunkPos().x(), currentSpawnRequest.sourceChunkPos().z(), ChunkStatus.FULL, true);
        }
    }

    public void tick() {
        if (currentSpawnRequest != null) {
            if (!sourceChunkFuture.isDone()) {
                return;
            }
            switch (phase) {
                case COPY_BIOMES -> {
                    updateBiomes(sourceLevel,
                            sourceChunkFuture.getNow(ChunkResult.error("Chunk not loaded")).orElseThrow(IllegalStateException::new),
                            targetLevel,
                            targetLevel.getChunk(currentSpawnRequest.targetChunkPos.x(), currentSpawnRequest.targetChunkPos.z()),
                            currentSpawnRequest.targetChunkPos);
                    phase = SpawnPhase.SPAWN_BLOCKS;
                    currentLayer = targetLevel.getMinY();
                    setDirty();
                }
                case SPAWN_BLOCKS -> {
                    int minLayer = currentLayer;
                    int maxLayer = Math.min(currentLayer + ChunkByChunkConfig.get().getGeneration().getChunkLayerSpawnRate(), targetLevel.getMaxY() + 1);
                    copyBlocks(
                            sourceLevel,
                            currentSpawnRequest.sourceChunkPos,
                            targetLevel,
                            currentSpawnRequest.targetChunkPos,
                            minLayer,
                            maxLayer);
                    spawnEdgeParticles(targetLevel, currentSpawnRequest.targetChunkPos, minLayer, maxLayer);
                    if (maxLayer > targetLevel.getMaxY()) {
                        if (ChunkByChunkConfig.get().getGeneration().spawnNewChunkChest() && !ChunkByChunkConfig.get().getGeneration().spawnChestInInitialChunkOnly()) {
                            SpawnChunkHelper.createNextSpawner(targetLevel, currentSpawnRequest.targetChunkPos);
                        }
                        phase = SpawnPhase.SYNCH_CHUNKS;
                    } else {
                        currentLayer = maxLayer;
                    }
                    setDirty();
                }
                case SYNCH_CHUNKS -> {
                    synchChunks();
                    phase = SpawnPhase.SPAWN_ENTITIES;
                    setDirty();
                }
                case SPAWN_ENTITIES -> {
                    if (sourceLevel.areEntitiesLoaded(currentSpawnRequest.sourceChunkPos.pack())) {
                        spawnChunkEntities();
                        completeSpawnRequest();
                        setDirty();
                    }
                }
            }
        } else if (!requests.isEmpty()) {
            currentSpawnRequest = requests.removeFirst();
            targetLevel = server.getLevel(currentSpawnRequest.targetLevel());
            sourceLevel = server.getLevel(currentSpawnRequest.sourceLevel());
            forcedTargetChunk = targetLevel.setChunkForced(currentSpawnRequest.targetChunkPos().x(), currentSpawnRequest.targetChunkPos().z(), true);
            sourceLevel.setChunkForced(currentSpawnRequest.sourceChunkPos().x(), currentSpawnRequest.sourceChunkPos().z(), true);
            sourceChunkFuture = sourceLevel.getChunkSource().getChunkFuture(currentSpawnRequest.sourceChunkPos().x(), currentSpawnRequest.sourceChunkPos().z(), ChunkStatus.FULL, true);

            if (currentSpawnRequest.immediate) {
                phase = SpawnPhase.SYNCH_CHUNKS;
            } else {
                phase = SpawnPhase.COPY_BIOMES;
            }
            ChunkByChunkConstants.LOGGER.info("Spawning chunk " + currentSpawnRequest.targetChunkPos.toString() + " in " + targetLevel.dimension().identifier().toString());
            setDirty();
        }
    }

    private void spawnChunkEntities() {
        List<Entity> entities = sourceLevel.getEntities((Entity) null, new AABB(currentSpawnRequest.sourceChunkPos().getMinBlockX(), sourceLevel.getMinY(), currentSpawnRequest.sourceChunkPos().getMinBlockZ(), currentSpawnRequest.sourceChunkPos().getMaxBlockX(), sourceLevel.getMaxY(), currentSpawnRequest.sourceChunkPos().getMaxBlockZ()), (x) -> true);
        for (Entity e : entities) {
            Vec3 pos = new Vec3(e.getX() + (currentSpawnRequest.targetChunkPos().x() - currentSpawnRequest.sourceChunkPos().x()) * 16, e.getY(), e.getZ() + (currentSpawnRequest.targetChunkPos().z() - currentSpawnRequest.sourceChunkPos().z()) * 16);

            Entity movedEntity = ChangeDimensionHelper.changeDimension(e, targetLevel, pos);
            if (movedEntity != null) {
                movedEntity.setPos(pos);
            }
        }
    }

    private void completeSpawnRequest() {
        if (forcedTargetChunk) {
            targetLevel.setChunkForced(currentSpawnRequest.targetChunkPos().x(), currentSpawnRequest.targetChunkPos().z(), false);
            sourceLevel.setChunkForced(currentSpawnRequest.sourceChunkPos().x(), currentSpawnRequest.sourceChunkPos().z(), false);
            currentSpawnRequest = null;
        }
    }

    /**
     * Crumbles block break particles along the seam where a spawning chunk meets its neighbours: on the outline edges
     * (top, bottom and vertical edges) of the neighbouring blocks' faces that face the new chunk, for the layers that
     * were just spawned.
     */
    private static void spawnEdgeParticles(ServerLevel level, ChunkPos chunkPos, int fromLayer, int toLayer) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        int sent = 0;
        for (Direction outward : Direction.Plane.HORIZONTAL) {
            // The axis running along this side of the chunk, and the plane between the chunk and its neighbour
            Direction along = outward.getClockWise();
            double plane = switch (outward) {
                case WEST -> minX;
                case EAST -> minX + 16;
                case NORTH -> minZ;
                default -> minZ + 16;
            };
            for (int i = 0; i < 16; i++) {
                int x = outward == Direction.WEST ? minX - 1 : outward == Direction.EAST ? minX + 16 : minX + i;
                int z = outward == Direction.NORTH ? minZ - 1 : outward == Direction.SOUTH ? minZ + 16 : minZ + i;
                for (int y = fromLayer; y < toLayer; y++) {
                    neighbour.set(x, y, z);
                    BlockState state = level.getBlockState(neighbour);
                    if (!isSolidForEdge(state)) {
                        continue;
                    }
                    BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
                    // Centre of the face of the neighbour block that faces the new chunk
                    double faceX = outward.getAxis() == Direction.Axis.X ? plane : x + 0.5;
                    double faceZ = outward.getAxis() == Direction.Axis.Z ? plane : z + 0.5;
                    double alongX = along.getStepX() * 0.5;
                    double alongZ = along.getStepZ() * 0.5;
                    double spreadX = outward.getAxis() == Direction.Axis.Z ? 0.25 : 0;
                    double spreadZ = outward.getAxis() == Direction.Axis.X ? 0.25 : 0;
                    if (!isSolidForEdge(level.getBlockState(neighbour.move(Direction.UP)))) {
                        level.sendParticles(particle, false, true, faceX, y + 1, faceZ, EDGE_PARTICLES, spreadX, 0, spreadZ, 0);
                        sent++;
                    }
                    if (!isSolidForEdge(level.getBlockState(neighbour.set(x, y - 1, z)))) {
                        level.sendParticles(particle, false, true, faceX, y, faceZ, EDGE_PARTICLES, spreadX, 0, spreadZ, 0);
                        sent++;
                    }
                    for (int sign : new int[]{-1, 1}) {
                        if (!isSolidForEdge(level.getBlockState(neighbour.set(x + along.getStepX() * sign, y, z + along.getStepZ() * sign)))) {
                            level.sendParticles(particle, false, true, faceX + alongX * sign, y + 0.5, faceZ + alongZ * sign, EDGE_PARTICLES, 0, 0.25, 0, 0);
                            sent++;
                        }
                    }
                    if (sent >= MAX_EDGE_PARTICLE_BURSTS_PER_TICK) {
                        return;
                    }
                }
            }
        }
    }

    private static boolean isSolidForEdge(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static void copyBlocks(ServerLevel sourceLevel, ChunkPos sourceChunkPos, ServerLevel targetLevel, ChunkPos targetChunkPos, int fromLayer, int toLayer) {
        int xOffset = targetChunkPos.getMinBlockX() - sourceChunkPos.getMinBlockX();
        int zOffset = targetChunkPos.getMinBlockZ() - sourceChunkPos.getMinBlockZ();

        Block sealedBlock = Blocks.BEDROCK;
        if (targetLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyChunkGenerator && skyChunkGenerator.getGenerationType() == SkyChunkGenerator.EmptyGenerationType.Sealed) {
            sealedBlock = skyChunkGenerator.getSealBlock();
        }

        BlockPos.MutableBlockPos sourceBlock = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos targetBlock = new BlockPos.MutableBlockPos();
        for (int y = fromLayer; y < toLayer; y++) {
            for (int z = sourceChunkPos.getMinBlockZ(); z <= sourceChunkPos.getMaxBlockZ(); z++) {
                for (int x = sourceChunkPos.getMinBlockX(); x <= sourceChunkPos.getMaxBlockX(); x++) {
                    sourceBlock.set(x, y, z);
                    targetBlock.set(x + xOffset, y, z + zOffset);
                    Block existingBlock = targetLevel.getBlockState(targetBlock).getBlock();
                    if (existingBlock instanceof AirBlock || existingBlock instanceof LiquidBlock || existingBlock == Blocks.BEDROCK || existingBlock == sealedBlock || existingBlock == Blocks.SNOW) {
                        BlockState newBlock = sourceLevel.getBlockState(sourceBlock);
                        if (ChunkByChunkConfig.get().getGameplayConfig().isChunkSpawnLeafDecayDisabled() && newBlock.getBlock() instanceof LeavesBlock) {
                            newBlock = newBlock.setValue(LeavesBlock.PERSISTENT, true);
                        }
                        targetLevel.setBlock(targetBlock, newBlock, Block.UPDATE_ALL);
                        BlockEntity fromBlockEntity = sourceLevel.getBlockEntity(sourceBlock);
                        BlockEntity toBlockEntity = targetLevel.getBlockEntity(targetBlock);
                        if (fromBlockEntity != null && toBlockEntity != null) {
                            toBlockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, targetLevel.registryAccess(), fromBlockEntity.saveWithFullMetadata(sourceLevel.registryAccess())));
                            toBlockEntity.setChanged();
                        }
                    }
                }
            }
        }
    }

    private static void updateBiomes(ServerLevel sourceLevel, ChunkAccess sourceChunk, ServerLevel targetLevel, ChunkAccess targetChunk, ChunkPos targetChunkPos) {
        if (sourceChunk.getSections().length != targetChunk.getSections().length) {
            ChunkByChunkConstants.LOGGER.warn("Section count mismatch between {} and {} - {} vs {}", sourceLevel.dimension(), targetLevel.dimension(), sourceChunk.getSections().length, targetChunk.getSections().length);
        }

        boolean biomesUpdated = false;
        for (int targetIndex = 0; targetIndex < targetChunk.getSections().length; targetIndex++) {
            int sourceIndex = (targetIndex < sourceChunk.getSections().length) ? targetIndex : sourceChunk.getSections().length - 1;
            LevelChunkSection sourceSection = sourceChunk.getSections()[sourceIndex];
            LevelChunkSection targetSection = targetChunk.getSections()[targetIndex];
            byte[] buffer = new byte[sourceSection.getBiomes().getSerializedSize()];
            FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer));
            friendlyByteBuf.writerIndex(0);
            sourceSection.getBiomes().write(friendlyByteBuf);

            byte[] targetBuffer = new byte[targetSection.getBiomes().getSerializedSize()];
            FriendlyByteBuf targetFriendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(targetBuffer));
            targetFriendlyByteBuf.writerIndex(0);
            targetSection.getBiomes().write(targetFriendlyByteBuf);

            if (!Arrays.equals(buffer, targetBuffer)) {
                friendlyByteBuf.readerIndex(0);
                targetSection.readBiomes(friendlyByteBuf);
                targetChunk.markUnsaved();
                biomesUpdated = true;
            }
        }
        if (biomesUpdated) {
            ((ControllableChunkMap) targetLevel.getChunkSource().chunkMap).forceReloadChunk(targetChunkPos);
        }
    }

    private void synchChunks() {
        if (targetLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator) {
            for (ResourceKey<Level> synchLevelId : generator.getSynchedLevels()) {
                ServerLevel synchLevel = server.getLevel(synchLevelId);
                if (synchLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator synchGenerator) {
                    double scale = DimensionType.getTeleportationScale(targetLevel.dimensionType(), synchLevel.dimensionType());
                    BlockPos pos = currentSpawnRequest.targetChunkPos().getMiddleBlockPosition(0);
                    ChunkPos synchChunk = ChunkPos.containing(BlockPos.containing(pos.getX() * scale, 0, pos.getZ() * scale));
                    request(synchChunk, synchLevelId, synchChunk, synchGenerator.getGenerationLevel(), false);
                }
            }
        }
    }

    public boolean isValidForLevel(ServerLevel level, String biomeTheme, boolean random) {
        if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator) {
            if (!biomeTheme.isEmpty()) {
                return generator.getBiomeDimension(biomeTheme) != null;
            } else if (random) {
                return generator.isRandomChunkSpawnerAllowed();
            } else {
                return generator.isChunkSpawnerAllowed();
            }
        }
        return false;
    }

    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos) {
        return request(level, biomeTheme, random, blockPos, false);
    }

    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos, boolean immediate) {
        return request(level, biomeTheme, random, blockPos, immediate, null);
    }

    /**
     * Requests a chunk spawn. A non-random spawn (plain or themed) next to a chunk that was copied from elsewhere -
     * such as an unstable spawner chunk - continues that chunk's terrain rather than spawning the world's own chunk.
     * Themed spawns continue it in their biome theme's dimension.
     *
     * @param level          The level to spawn the chunk in
     * @param biomeTheme     The biome theme of the spawner, or empty
     * @param random         Whether this is a random (unstable) spawn
     * @param blockPos       A position in the chunk to spawn
     * @param immediate      Whether to spawn the whole chunk at once
     * @param referenceChunk The spawned chunk a non-random spawn continues from, or null to continue from any adjacent chunk
     * @return Whether the spawn was requested
     */
    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos, boolean immediate, @Nullable ChunkPos referenceChunk) {
        ChunkPos targetChunkPos = ChunkPos.containing(blockPos);
        if (isValidForLevel(level, biomeTheme, random) && SpawnChunkHelper.isEmptyChunk(level, targetChunkPos) && level.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator) {
            ChunkPos sourceChunkPos;
            if (random) {
                Random rng = new Random(blockPos.asLong());
                sourceChunkPos = new ChunkPos(rng.nextInt(Short.MIN_VALUE, Short.MAX_VALUE), rng.nextInt(Short.MIN_VALUE, Short.MAX_VALUE));
            } else {
                ChunkPos offset = getContinuationOffset(level.dimension(), targetChunkPos, referenceChunk);
                sourceChunkPos = new ChunkPos(targetChunkPos.x() + offset.x(), targetChunkPos.z() + offset.z());
            }
            ResourceKey<Level> sourceLevel;
            if (biomeTheme.isEmpty()) {
                sourceLevel = generator.getGenerationLevel();
            } else {
                sourceLevel = generator.getBiomeDimension(biomeTheme);
            }
            if (request(targetChunkPos, level.dimension(), sourceChunkPos, sourceLevel, immediate)) {
                recordSourceOffset(level.dimension(), targetChunkPos, sourceChunkPos);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean request(ChunkPos targetChunkPos, ResourceKey<Level> targetLevel, ChunkPos sourceChunkPos, ResourceKey<Level> sourceLevel, boolean immediate) {
        SpawnRequest spawnRequest = new SpawnRequest(targetChunkPos, targetLevel, sourceChunkPos, sourceLevel, immediate);
        if (!spawnRequest.equals(currentSpawnRequest) && !requests.contains(spawnRequest)) {
            if (immediate) {
                ServerLevel toLevel = server.getLevel(targetLevel);
                ServerLevel fromLevel = server.getLevel(sourceLevel);
                LevelChunk toChunk = toLevel.getChunk(targetChunkPos.x(), targetChunkPos.z());
                LevelChunk fromChunk = fromLevel.getChunk(sourceChunkPos.x(), sourceChunkPos.z());
                updateBiomes(fromLevel, fromChunk, toLevel, toChunk, targetChunkPos);
                copyBlocks(fromLevel, spawnRequest.sourceChunkPos, toLevel, spawnRequest.targetChunkPos, toLevel.getMinY(), toLevel.getMaxY() + 1);
                requests.addFirst(spawnRequest);
            } else {
                requests.add(spawnRequest);
            }
            setDirty();
            return true;
        }
        return false;
    }

    /**
     * @param level The level of the chunk
     * @param chunk A spawned chunk
     * @return The offset from the chunk to the chunk its content was copied from (0, 0 for the world's own chunk)
     */
    public ChunkPos getSourceOffset(ResourceKey<Level> level, ChunkPos chunk) {
        return ChunkPos.unpack(sourceOffsets.getOrDefault(level, Map.of()).getOrDefault(chunk.pack(), 0L));
    }

    private ChunkPos getContinuationOffset(ResourceKey<Level> level, ChunkPos targetChunkPos, @Nullable ChunkPos referenceChunk) {
        Map<Long, Long> levelOffsets = sourceOffsets.getOrDefault(level, Map.of());
        if (referenceChunk != null) {
            return ChunkPos.unpack(levelOffsets.getOrDefault(referenceChunk.pack(), 0L));
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Long offset = levelOffsets.get(new ChunkPos(targetChunkPos.x() + direction.getStepX(), targetChunkPos.z() + direction.getStepZ()).pack());
            if (offset != null) {
                return ChunkPos.unpack(offset);
            }
        }
        return new ChunkPos(0, 0);
    }

    private void recordSourceOffset(ResourceKey<Level> level, ChunkPos targetChunkPos, ChunkPos sourceChunkPos) {
        int dx = sourceChunkPos.x() - targetChunkPos.x();
        int dz = sourceChunkPos.z() - targetChunkPos.z();
        Map<Long, Long> levelOffsets = sourceOffsets.computeIfAbsent(level, k -> new HashMap<>());
        if (dx == 0 && dz == 0) {
            levelOffsets.remove(targetChunkPos.pack());
        } else {
            levelOffsets.put(targetChunkPos.pack(), new ChunkPos(dx, dz).pack());
        }
        setDirty();
    }

    private List<SourceOffset> getSourceOffsetList() {
        List<SourceOffset> result = new ArrayList<>();
        sourceOffsets.forEach((level, offsets) -> offsets.forEach((chunk, offset) -> result.add(new SourceOffset(level, chunk, offset))));
        return result;
    }

    private record SourceOffset(ResourceKey<Level> level, long chunk, long offset) {
        public static final Codec<SourceOffset> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("level").forGetter(SourceOffset::level),
                Codec.LONG.fieldOf("chunk").forGetter(SourceOffset::chunk),
                Codec.LONG.fieldOf("offset").forGetter(SourceOffset::offset)
        ).apply(i, SourceOffset::new));
    }

    public boolean isBusy() {
        return currentSpawnRequest != null || !requests.isEmpty();
    }

    private record SpawnRequest(ChunkPos targetChunkPos, ResourceKey<Level> targetLevel, ChunkPos sourceChunkPos, ResourceKey<Level> sourceLevel, boolean immediate) {

        public static final Codec<SpawnRequest> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("targetPos").forGetter(r -> r.targetChunkPos.pack()),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("targetLevel").forGetter(SpawnRequest::targetLevel),
                Codec.LONG.fieldOf("sourcePos").forGetter(r -> r.sourceChunkPos.pack()),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("sourceLevel").forGetter(SpawnRequest::sourceLevel),
                Codec.BOOL.optionalFieldOf("immediate", false).forGetter(SpawnRequest::immediate)
        ).apply(i, (targetPos, targetLevel, sourcePos, sourceLevel, immediate) -> new SpawnRequest(ChunkPos.unpack(targetPos), targetLevel, ChunkPos.unpack(sourcePos), sourceLevel, immediate)));

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SpawnRequest that = (SpawnRequest) o;

            if (!targetChunkPos.equals(that.targetChunkPos)) return false;
            return targetLevel.equals(that.targetLevel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetChunkPos, targetLevel);
        }
    }

    private enum SpawnPhase {
        COPY_BIOMES,
        SPAWN_BLOCKS,
        SYNCH_CHUNKS,
        SPAWN_ENTITIES
    }
}
