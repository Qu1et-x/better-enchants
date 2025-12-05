package net.enchantoutline.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.enchantoutline.mixin_accessors.RenderPipelineAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderPipeline.class)
public class RenderPipelineMixin implements RenderPipelineAccessor {

    @Shadow
    @Final
    private boolean cull;

    @Override
    public boolean enchantOutline$getCull() {
        return cull;
    }
}
