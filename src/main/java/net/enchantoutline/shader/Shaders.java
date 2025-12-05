package net.enchantoutline.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.enchantoutline.EnchantmentGlintOutline;
import net.enchantoutline.mixin_accessors.RenderLayerAccessor;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.Identifier;

public class Shaders {
    private static final String MOD_ID = EnchantmentGlintOutline.MOD_ID;

    //still don't know what I'm doing so just gonna do this
    public static final RenderPipeline.Snippet OUTLINE_SNIPPET = RenderPipeline.builder(RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET)
            .withVertexShader(Identifier.of(MOD_ID, "core/outline"))
            .withFragmentShader(Identifier.of(MOD_ID, "core/outline"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
            .buildSnippet();

    public static final RenderPipeline CUTOUT_PIPELINE_DEPTH_CULL = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/cutout"))
                    .withDepthWrite(true)
                    .withColorWrite(false)
                    .withCull(true)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .build()
    );

    public static final RenderPipeline CUTOUT_PIPELINE_COLOR_CULL = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/cutout"))
                    .withDepthWrite(false)
                    .withColorWrite(true)
                    .withCull(true)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .build()
    );

    public static final RenderPipeline CUTOUT_PIPELINE_DEPTH_NOCULL = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/cutout"))
                    .withDepthWrite(true)
                    .withColorWrite(false)
                    .withCull(false)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .build()
    );

    public static final RenderPipeline CUTOUT_PIPELINE_COLOR_NOCULL = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/cutout"))
                    .withDepthWrite(false)
                    .withColorWrite(true)
                    .withCull(false)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .build()
    );

    public static final RenderLayer GLINT_CUTOUT_LAYER = RenderLayer.of(
            "enchout_glint_normal",
            786432,
            true,
            false,
            CUTOUT_PIPELINE_DEPTH_CULL,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    .texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static final RenderLayer COLOR_CUTOUT_LAYER = RenderLayer.of(
            "enchnout_color_normal",
            786432,
            true,
            false,
            CUTOUT_PIPELINE_COLOR_CULL,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    .texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static final RenderLayer ZFIX_CUTOUT_LAYER = RenderLayer.of(
            "enchout_zfix_normal",
            786432,
            true,
            false,
            CUTOUT_PIPELINE_DEPTH_CULL,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    .texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static final RenderLayer ARMOR_ENTITY_GLINT_FIX = RenderLayer.of(
            "enchantoutline_armor_glint",
            1536,
            RenderPipelines.GLINT,
            RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(ItemRenderer.ENTITY_ENCHANTMENT_GLINT, false))
                    .texturing(RenderLayer.ARMOR_ENTITY_GLINT_TEXTURING)
                    .build(true)
    );

    public static RenderLayer createGlintRenderLayerCull(Identifier texture) {
        return RenderLayer.of(
                "enchout_glint_model",
                786432,
                true,
                false,
                CUTOUT_PIPELINE_DEPTH_CULL,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                        .build(true));

    }

    public static RenderLayer createGlintRenderLayerNoCull(Identifier texture) {
        return RenderLayer.of(
                "enchout_glint_model",
                786432,
                true,
                false,
                CUTOUT_PIPELINE_DEPTH_NOCULL,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                        .build(true));

    }

    public static RenderLayer createColorRenderLayerCull(Identifier texture) {
        RenderLayer layer = RenderLayer.of(
                "enchout_color_model",
                786432,
                true,
                false,
                CUTOUT_PIPELINE_COLOR_CULL,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                        .overlay(RenderLayer.DISABLE_OVERLAY_COLOR)
                        .build(true));
        RenderLayerAccessor accessor = (RenderLayerAccessor)layer;
        accessor.enchantOutline$setDrawBeforeCustom(true);
        accessor.enchantOutline$setShouldUseLayerBuffer(false);
        return layer;
    }

    public static RenderLayer createColorRenderLayerNoCull(Identifier texture) {
        RenderLayer layer = RenderLayer.of(
                "enchout_color_model",
                786432,
                true,
                false,
                CUTOUT_PIPELINE_COLOR_NOCULL,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                        .overlay(RenderLayer.DISABLE_OVERLAY_COLOR)
                        .build(true));
        RenderLayerAccessor accessor = (RenderLayerAccessor)layer;
        accessor.enchantOutline$setDrawBeforeCustom(true);
        accessor.enchantOutline$setShouldUseLayerBuffer(false);
        return layer;
    }

    public static RenderLayer createZFixRenderLayerCull(Identifier texture) {
        return RenderLayer.of(
                "enchout_zfix_model",
                786432,
                true,
                false,
                CUTOUT_PIPELINE_DEPTH_CULL,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                        .build(true));
    }

    public static RenderLayer createZFixRenderLayerNoCull(Identifier texture) {
        return RenderLayer.of(
                "enchout_zfix_model",
                786432,
                true,
                false,
                CUTOUT_PIPELINE_DEPTH_NOCULL,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                        .build(true));
    }
}
