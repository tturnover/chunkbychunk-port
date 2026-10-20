package xyz.immortius.chunkbychunk.mixins;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Provides access to unfreeze a registry and to the value lookups needed to replace a registered value
 */
@Mixin(MappedRegistry.class)
public interface DefrostedRegistry<T> {
    @Accessor("frozen")
    void setFrozen(boolean newFrozen);

    @Accessor("byValue")
    Map<T, Holder.Reference<T>> getByValue();

    @Accessor("toId")
    Reference2IntMap<T> getToId();
}
