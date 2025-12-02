package net.enchantoutline.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.enchantoutline.EnchantmentGlintOutline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

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

    public static final RenderPipeline CUTOUT_PIPELINE_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/cutout"))
                    .withDepthWrite(true)
                    .withColorWrite(false)
                    .withCull(true)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .build()
    );

    public static final RenderPipeline SOLID_PIPELINE_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withDepthWrite(true)
                    .withColorWrite(false)
                    .withCull(true)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/solid"))
                    .build()
    );

    public static final RenderPipeline CUTOUT_PIPELINE_COLOR = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/cutout"))
                    .withDepthWrite(false)
                    .withColorWrite(true)
                    .withCull(true)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .build()
    );

    public static final RenderPipeline SOLID_PIPELINE_COLOR = RenderPipelines.register(
            RenderPipeline.builder(OUTLINE_SNIPPET)
                    .withDepthWrite(false)
                    .withColorWrite(true)
                    .withCull(true)
                    .withLocation(Identifier.of(MOD_ID, "pipeline/solid"))
                    .build()
    );

    public static final RenderLayer GLINT_CUTOUT_LAYER = RenderLayer.of(
            "custom_enchants_cutout",
            786432,
            true,
            false,
            CUTOUT_PIPELINE_DEPTH,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    .texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static final RenderLayer GLINT_SOLID_LAYER = RenderLayer.of(
            "custom_enchants_solid",
            786432,
            true,
            false,
            SOLID_PIPELINE_DEPTH,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    //.texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static final RenderLayer COLOR_CUTOUT_LAYER = RenderLayer.of(
            "custom_enchants_cutout",
            786432,
            true,
            false,
            CUTOUT_PIPELINE_COLOR,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    .texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static final RenderLayer COLOR_SOLID_LAYER = RenderLayer.of(
            "custom_enchants_solid",
            786432,
            true,
            false,
            SOLID_PIPELINE_COLOR,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    .overlay(RenderLayer.DISABLE_OVERLAY_COLOR)
                    .build(true)
    );

    public static final RenderLayer ZFIX_CUTOUT_LAYER = RenderLayer.of(
            "custom_enchants_cutout",
            786432,
            true,
            false,
            CUTOUT_PIPELINE_DEPTH,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    .texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static final RenderLayer ZFIX_SOLID_LAYER = RenderLayer.of(
            "custom_enchants_solid",
            786432,
            true,
            false,
            SOLID_PIPELINE_DEPTH,
            RenderLayer.MultiPhaseParameters.builder()
                    .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                    //.texture(RenderLayer.BLOCK_ATLAS_TEXTURE)
                    .build(true)
    );

    public static RenderLayer createColorRenderLayer(Identifier texture) {
        return RenderLayer.of(
                "custom_enchants_model",
                786432,
                true,
                false,
                CUTOUT_PIPELINE_COLOR,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .lightmap(RenderLayer.ENABLE_LIGHTMAP)
                        .overlay(RenderLayer.DISABLE_OVERLAY_COLOR)
                        .build(true));
    }
}
