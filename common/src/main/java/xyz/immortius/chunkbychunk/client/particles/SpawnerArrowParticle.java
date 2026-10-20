package xyz.immortius.chunkbychunk.client.particles;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import xyz.immortius.chunkbychunk.common.blocks.SpawnChunkBlock;
import xyz.immortius.chunkbychunk.interop.Services;
import xyz.immortius.chunkbychunk.server.world.SpawnChunkHelper;

/**
 * Camera facing arrow shown, like the barrier marker, over the chunk spawner the local player is looking at - above it
 * when their head is above the block, below it otherwise. It points at the chunk the spawner would spawn if the local player used it from where they stand, picking the
 * closest of 8 pre-drawn on-screen directions. Colour shows the target state: green empty, red already spawned,
 * blue spawns the spawner's own chunk, orange not loaded.
 */
public class SpawnerArrowParticle extends SingleQuadParticle {

    private static @Nullable SpawnerArrowParticle active;

    // Sprite order in particles/spawner_arrow.json: 4 colours of 8 directions, clockwise from screen up
    private static final int DIRECTIONS = 8;
    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    private static final int ORANGE = 3;
    private static final int SCREEN_DOWN = 4;
    private static final double ABOVE_OFFSET = 1.6;
    private static final double BELOW_OFFSET = -0.6;

    private final BlockPos spawnerPos;
    private final SpriteSet sprites;
    private int colour = GREEN;
    private @Nullable Direction aim;

    private SpawnerArrowParticle(ClientLevel level, BlockPos spawnerPos, SpriteSet sprites) {
        super(level, spawnerPos.getX() + 0.5, spawnerPos.getY() + ABOVE_OFFSET, spawnerPos.getZ() + 0.5, sprites.first());
        this.spawnerPos = spawnerPos;
        this.sprites = sprites;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 80;
        this.quadSize = 0.5F;
        updateTarget();
    }

    /**
     * Called at the end of each client tick: shows the arrow over the chunk spawner the player is looking at.
     */
    public static void clientTick(Minecraft minecraft) {
        BlockPos lookedAt = getLookedAtSpawner(minecraft);
        if (lookedAt == null || minecraft.level == null) {
            return;
        }
        if (active == null || !active.isAlive() || active.level != minecraft.level || !active.spawnerPos.equals(lookedAt)) {
            if (active != null) {
                active.remove();
            }
            minecraft.particleEngine.createParticle(Services.PLATFORM.spawnerArrowParticle(), lookedAt.getX(), lookedAt.getY(), lookedAt.getZ(), 0, 0, 0);
        }
    }

    private static @Nullable BlockPos getLookedAtSpawner(Minecraft minecraft) {
        if (minecraft.level != null && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && minecraft.level.getBlockState(hit.getBlockPos()).getBlock() instanceof SpawnChunkBlock) {
            return hit.getBlockPos();
        }
        return null;
    }

    private boolean updateTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !spawnerPos.equals(getLookedAtSpawner(minecraft))) {
            return false;
        }
        // Above the block when the player's head is above it, below otherwise
        y = yo = spawnerPos.getY() + (player.getEyeY() > spawnerPos.getY() + 0.5 ? ABOVE_OFFSET : BELOW_OFFSET);
        ChunkPos target = SpawnChunkBlock.getTargetChunk(level, spawnerPos, player);
        if (target.equals(ChunkPos.containing(spawnerPos))) {
            aim = null;
            colour = BLUE;
        } else {
            aim = SpawnChunkBlock.getAimDirection(spawnerPos, player);
            if (!level.hasChunk(target.x(), target.z())) {
                colour = ORANGE;
            } else if (SpawnChunkHelper.isEmptyChunk(level, target)) {
                colour = GREEN;
            } else {
                colour = RED;
            }
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) {
            if (updateTarget()) {
                // Stay alive for as long as the arrow is wanted
                age = 0;
            } else {
                remove();
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        int screenDirection = SCREEN_DOWN;
        if (aim != null) {
            // Direction to the target in the camera's view space: x is right, y is up
            Vector3f view = new Vector3f(aim.getStepX(), 0, aim.getStepZ()).rotate(new Quaternionf(camera.rotation()).conjugate());
            if (view.x * view.x + view.y * view.y > 1.0E-4f) {
                double clockwiseFromUp = Math.toDegrees(Math.atan2(view.x, view.y));
                screenDirection = Math.floorMod(Math.round(clockwiseFromUp / (360.0 / DIRECTIONS)), DIRECTIONS);
            } else {
                screenDirection = 0;
            }
        }
        setSprite(sprites.get(colour * DIRECTIONS + screenDirection, 4 * DIRECTIONS - 1));
        super.extract(particleTypeRenderState, camera, partialTickTime);
    }

    @Override
    protected Layer getLayer() {
        return Layer.bySprite(sprite);
    }

    @Override
    protected int getLightCoords(float a) {
        return 0xF000F0;
    }

    /**
     * Creates the arrow for the spawner at the given block position (passed as x, y, z).
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SpawnerArrowParticle particle = new SpawnerArrowParticle(level, BlockPos.containing(x, y, z), sprites);
            active = particle;
            return particle;
        }
    }
}
