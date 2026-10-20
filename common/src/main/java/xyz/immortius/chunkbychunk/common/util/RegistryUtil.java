package xyz.immortius.chunkbychunk.common.util;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import xyz.immortius.chunkbychunk.mixins.DefrostedRegistry;
import xyz.immortius.chunkbychunk.mixins.HolderReferenceAccessor;

import java.util.Optional;

/**
 * Utility methods for modifying registries during server start
 */
public final class RegistryUtil {
    private RegistryUtil() {
    }

    /**
     * Registers a value to a key, replacing any existing value while retaining its id and holder.
     * The registry must have been unfrozen.
     */
    @SuppressWarnings("unchecked")
    public static <T> void registerOrReplace(MappedRegistry<T> registry, ResourceKey<T> key, T value) {
        Optional<Holder.Reference<T>> existing = registry.get(key);
        if (existing.isEmpty()) {
            registry.register(key, value, RegistrationInfo.BUILT_IN);
            return;
        }
        Holder.Reference<T> holder = existing.get();
        T oldValue = holder.value();
        DefrostedRegistry<T> access = (DefrostedRegistry<T>) registry;
        int id = access.getToId().removeInt(oldValue);
        access.getToId().put(value, id);
        access.getByValue().remove(oldValue);
        access.getByValue().put(value, holder);
        ((HolderReferenceAccessor<T>) holder).callBindValue(value);
    }
}
