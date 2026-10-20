package xyz.immortius.chunkbychunk.server.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;

import java.util.List;
import java.util.Optional;

public final class ChunkGeneratorAccess {

    /**
     * Settings used for generators that are not noise based - mirrors the fallback vanilla uses for the random state of
     * such generators.
     */
    private static final Holder<NoiseGeneratorSettings> DUMMY_SETTINGS = Holder.direct(new NoiseGeneratorSettings(
            NoiseSettings.create(0, 0),
            Blocks.STONE.defaultBlockState(),
            Blocks.AIR.defaultBlockState(),
            NoiseRouterData.none(),
            Holder.direct(new SequenceRule(List.of())),
            List.of(),
            63,
            true,
            Optional.empty(),
            false,
            NoiseGeneratorSettings.DebugFunctions.EMPTY));

    private ChunkGeneratorAccess() {
    }

    public static Holder<NoiseGeneratorSettings> getNoiseGeneratorSettings(ChunkGenerator generator) {
        if (generator instanceof NoiseBasedChunkGenerator noiseParent) {
            return noiseParent.generatorSettings();
        } else {
            return DUMMY_SETTINGS;
        }
    }
}
