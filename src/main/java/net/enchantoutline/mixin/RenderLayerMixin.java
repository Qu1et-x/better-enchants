package net.enchantoutline.mixin;

import net.enchantoutline.mixin_accessors.RenderLayerAccessor;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderLayer.class)
public class RenderLayerMixin implements RenderLayerAccessor {

    @Unique
    boolean shouldUseLayerBuffer = true;

    @Override
    public boolean enchantOutline$shouldUseLayerBuffer() {
        return shouldUseLayerBuffer;
    }

    @Override
    public void enchantOutline$setShouldUseLayerBuffer(boolean newUseLayerBuffer) {
        shouldUseLayerBuffer = newUseLayerBuffer;
    }
}
