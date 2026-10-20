package xyz.immortius.chunkbychunk.common.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Implementation for changing the dimension of entities without a portal
 */
public final class ChangeDimensionHelper {

    private ChangeDimensionHelper() {

    }

    public static @Nullable Entity changeDimension(Entity entity, ServerLevel level, Vec3 pos) {
        return entity.teleport(new TeleportTransition(level, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING));
    }
}
