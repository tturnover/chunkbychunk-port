package xyz.immortius.chunkbychunk.common.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Shared block properties for Chunk By Chunk blocks.
 */
public final class ChunkByChunkBlockProperties {

    private ChunkByChunkBlockProperties() {
    }

    /**
     * Stone-like properties that drop when broken by hand. The pickaxe is the preferred tool via the
     * minecraft:mineable/pickaxe tag.
     * @return New block properties
     */
    public static BlockBehaviour.Properties stoneLike() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(1.5F, 6.0F);
    }
}
