package net.enchantoutline.mixin;

import net.enchantoutline.mixin_accessors.VertexConsumerProvider_ImmediateAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;

@Mixin(VertexConsumerProvider.Immediate.class)
public class VertexConsumerProvider_ImmediateMixin implements VertexConsumerProvider_ImmediateAccessor {

    @Shadow
    @Final
    protected SequencedMap<RenderLayer, BufferAllocator> layerBuffers;

    @Unique
    Map<Object, Integer> dirty = new HashMap<>();

    @Override
    public SequencedMap<RenderLayer, BufferAllocator> enchantOutline$getLayerBuffers() {
        return layerBuffers;
    }

    @Override
    public Map<Object, Integer> enchantOutline$getDirtyMap() {
        return dirty;
    }

    @Override
    @Nullable
    public Integer enchantOutline$getDirty(Object o) {
        return dirty.get(o);
    }

    @Override
    public void enchantOutline$setDirty(Object o, int newDirty) {
        dirty.put(o, newDirty);
    }
}
