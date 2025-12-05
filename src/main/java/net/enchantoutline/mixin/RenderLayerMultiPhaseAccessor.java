package net.enchantoutline.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderLayer.MultiPhase.class)
public interface RenderLayerMultiPhaseAccessor {
    @Accessor("phases")
    RenderLayer.MultiPhaseParameters getPhases();
    @Accessor("pipeline")
    RenderPipeline getPipeline();
}
