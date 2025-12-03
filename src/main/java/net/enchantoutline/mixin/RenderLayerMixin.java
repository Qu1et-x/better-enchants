package net.enchantoutline.mixin;

import net.enchantoutline.mixin_accessors.RenderLayerAccessor;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderLayer.class)
public class RenderLayerMixin implements RenderLayerAccessor {

    @Unique
    boolean shouldUseLayerBuffer = true;

    @Unique
    //It's my mod I can do what I want.
    byte drawBeforeAfterCustom = 0;

    @Override
    public boolean enchantOutline$shouldUseLayerBuffer() {
        return shouldUseLayerBuffer;
    }

    @Override
    public void enchantOutline$setShouldUseLayerBuffer(boolean newUseLayerBuffer) {
        shouldUseLayerBuffer = newUseLayerBuffer;
    }

    @Override
    public boolean enchantOutline$shouldDrawBeforeCustom() {
        return drawBeforeAfterCustom == -1;
    }

    @Override
    public boolean enchantOutline$shouldDrawAfterCustom() {
        return drawBeforeAfterCustom == 1;
    }

    @Override
    public void enchantOutline$setDrawBeforeCustom(boolean drawBeforeCustom) {
        if(drawBeforeCustom){
            this.drawBeforeAfterCustom = -1;
            return;
        }
        this.drawBeforeAfterCustom = 0;
    }

    @Override
    public void enchantOutline$setDrawAfterCustom(boolean drawAfterCustom) {
        if(drawAfterCustom){
            this.drawBeforeAfterCustom = 1;
            return;
        }
        this.drawBeforeAfterCustom = 0;
    }
}
