package net.enchantoutline.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.enchantoutline.mixin.RenderLayerMultiPhaseAccessor;
import net.enchantoutline.mixin.RenderPhase_TextureAccessor;
import net.enchantoutline.mixin_accessors.MultiPhaseParametersAccessor;
import net.enchantoutline.mixin_accessors.RenderPipelineAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class RenderLayerHelper {
    public static boolean isRenderLayerDoubleSided(RenderLayer renderLayer){
        if(renderLayer instanceof RenderLayer.MultiPhase phase){
            RenderPipeline pipeline = ((RenderLayerMultiPhaseAccessor)(Object)phase).getPipeline();
            if(pipeline != null){
                return !((RenderPipelineAccessor)pipeline).enchantOutline$getCull();
            }
        }
        return false;
    }

    @Nullable
    public static Identifier getIdentifierFromRenderLayer(RenderLayer renderLayer){
        if(renderLayer instanceof RenderLayer.MultiPhase phase) {
            RenderLayer.MultiPhaseParameters params = ((RenderLayerMultiPhaseAccessor) (Object) phase).getPhases();
            if (params != null) {
                RenderPhase.TextureBase textureBase = ((MultiPhaseParametersAccessor) (Object) params).enchantOutline$getTexture();
                if (textureBase != null) {
                    Optional<Identifier> texture = ((RenderPhase_TextureAccessor) textureBase).invokeGetId();
                    if (texture.isPresent()) {
                        return texture.orElseThrow();
                    }
                }
            }
        }
        return null;
    }

    public static RenderLayer renderLayerFromRenderLayerDoubleSided(RenderLayer renderLayer, CustomRenderLayers customRenderLayers, Function<Identifier, RenderLayer> doubleSidedFactory, Function<Identifier, RenderLayer> singleSidedFactory, RenderLayer fallback, boolean isDoubleSided){
        return renderLayerFromIdentifierDoubleSided(getIdentifierFromRenderLayer(renderLayer), customRenderLayers, doubleSidedFactory, singleSidedFactory, fallback, isDoubleSided);
    }

    public static RenderLayer renderLayerFromIdentifierDoubleSided(Identifier identifier, CustomRenderLayers customRenderLayers, Function<Identifier, RenderLayer> doubleSidedFactory, Function<Identifier, RenderLayer> singleSidedFactory, RenderLayer fallback, boolean isDoubleSided){
        if(isDoubleSided){
            return renderLayerFromIdentifierWithFallback(identifier, (id) -> getOrCreateRenderLayer(customRenderLayers, doubleSidedFactory, identifier, identifier.toString() + "_db"), fallback);
        }
        return renderLayerFromIdentifierWithFallback(identifier, (id) -> getOrCreateRenderLayer(customRenderLayers, singleSidedFactory, identifier), fallback);
    }

    public static RenderLayer renderLayerFromIdentifierWithFallback(@Nullable Identifier identifier, Function<Identifier, RenderLayer> layerFactory, RenderLayer fallback){
        if(identifier != null){
            RenderLayer newLayer = layerFactory.apply(identifier);
            if(newLayer != null){
                return newLayer;
            }
        }
        return fallback;
    }

    @Nullable
    public static RenderLayer getOrCreateRenderLayer(CustomRenderLayers customRenderLayers, Function<Identifier, RenderLayer> layerFactory, Identifier identifier, String storagePath){
        RenderLayer output = customRenderLayers.getCustomRenderLayer(identifier.toString());
        if(output != null)
        {
            return output;
        }
        return customRenderLayers.addCustomRenderLayer(identifier.toString(), layerFactory.apply(identifier));
    }

    @Nullable
    public static RenderLayer getOrCreateRenderLayer(CustomRenderLayers customRenderLayers, Function<Identifier, RenderLayer> layerCreationFunction, Identifier identifier){
        return getOrCreateRenderLayer(customRenderLayers, layerCreationFunction, identifier, identifier.toString());
    }
}
