package net.enchantoutline;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.enchantoutline.config.EnchantmentOutlineConfig;
import net.enchantoutline.events.BufferBuilderModifyReturnValue;
import net.enchantoutline.events.RenderQuads;
import net.enchantoutline.events.WorldRenderer;
import net.enchantoutline.mixin.RenderLayerMultiPhaseAccessor;
import net.enchantoutline.mixin.RenderPhase_TextureAccessor;
import net.enchantoutline.mixin_accessors.*;
import net.enchantoutline.shader.Shaders;
import net.enchantoutline.util.CustomRenderLayers;
import net.enchantoutline.util.QuadHelper;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;

public class EnchantmentGlintOutline implements ModInitializer {
	public static final String MOD_ID = "enchantment-glint-outline";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final CustomRenderLayers GLINT_LAYERS = new CustomRenderLayers();
	public static final CustomRenderLayers COLOR_LAYERS = new CustomRenderLayers();
	public static final CustomRenderLayers ZFIX_LAYERS = new CustomRenderLayers();

	private static EnchantmentOutlineConfig config;

	public static EnchantmentOutlineConfig getConfig()
	{
		return config;
	}

	private static RenderLayer getTargetEnchantGlintLayer(){
		return RenderLayer.getArmorEntityGlint();
	}

	private static RenderLayer getTargetEnchantColorLayer(){
		return TexturedRenderLayers.getEntitySolid();
	}

	private static RenderLayer getTargetEnchantZFixLayer(){
		return RenderLayer.getWaterMask();
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		loadConfig();
		initLayers();

		//called before non-special item is rendered. used for render solid (by having no Z write, while Z test but rendering before the item is Rendered)
		RenderQuads.Normal.Callback.EVENT.register((receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
			if(config.isEnabled()){
				if(config.shouldRenderSolid()) {
					if (glintType != ItemRenderState.Glint.NONE) {
						int[] tint = {config.getOutlineColorAsInt(config.getOutlineColor())};

						List<BakedQuad> thickenedQuads = QuadHelper.thickenQuad(quads, 0.02f);
						orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tint, thickenedQuads, Shaders.COLOR_CUTOUT_LAYER, ItemRenderState.Glint.NONE);
					}
				}
			}
			return ActionResult.PASS;
		});

		//the other half of render solid for special items. They should either both use the renderCommandQueue or both avoid it though, this mix is not making me happy.
		RenderQuads.Model.ModelPart.EVENT.register((receiver, part, matrices, renderLayer, light, overlay, sprite, sheeted, hasGlint, tintedColor, crumblingOverlay, i) -> {
			if(config.isEnabled()){
				if(config.shouldRenderSolid()) {
					int[] tint = {config.getOutlineColorAsInt(config.getOutlineColor())};

					ModelPartAccessor modelPartAccessor = (ModelPartAccessor)(Object)part;
					List<ModelPart.Cuboid> cuboids = modelPartAccessor.enchantOutline$getCuboids();
					ModelPart thickModelPart = new ModelPart(QuadHelper.thickenCuboid(cuboids, 0.02f), modelPartAccessor.enchantOutline$getChildren());
					thickModelPart.setDefaultTransform(part.getDefaultTransform());
					thickModelPart.setTransform(thickModelPart.getTransform());

					OrderedRenderCommandQueueImplAccessor commandQueueAccessor = (OrderedRenderCommandQueueImplAccessor)receiver;
					commandQueueAccessor.enchantOutline$setSkipModelPartCallback(true);

					RenderLayer layer = Shaders.COLOR_CUTOUT_LAYER;
					if(renderLayer instanceof RenderLayer.MultiPhase phase){
						RenderLayer.MultiPhaseParameters params = ((RenderLayerMultiPhaseAccessor)(Object)phase).getPhases();
						if(params != null){
							RenderPhase.TextureBase textureBase = ((MultiPhaseParametersAccessor)(Object)params).enchantOutline$getTexture();
							if(textureBase != null){
								Optional<Identifier> texture = ((RenderPhase_TextureAccessor)textureBase).invokeGetId();
								if(texture.isPresent()){
									RenderLayer newLayer = getOrCreateColorRenderLayer(texture.orElseThrow());
									if(newLayer != null){
										layer = newLayer;
									}
								}
							}
						}
					}
					receiver.submitModelPart(thickModelPart, matrices, layer, 0, 0, sprite, sheeted, hasGlint, tintedColor, crumblingOverlay, i);
					commandQueueAccessor.enchantOutline$setSkipModelPartCallback(false);
				}
			}
			return ActionResult.PASS;
		});

		//called right after special item is rendered. used for render glint (by having Z write and Z test, but no color write after rendering the item, in other words write to the depth buffer)
		RenderQuads.Normal.Post.EVENT.register((receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
			if(config.isEnabled()){
				if(glintType != ItemRenderState.Glint.NONE){
					List<BakedQuad> thickenedQuads = QuadHelper.thickenQuad(quads, 0.02f);
					if(!config.shouldRenderSolid()){
						orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tintLayers, thickenedQuads, Shaders.GLINT_CUTOUT_LAYER, glintType);
					}
					else{
						//finally fixes the things rendering behind other things when they shouldn't be. Used for RenderSolid
						orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tintLayers, thickenedQuads, Shaders.ZFIX_CUTOUT_LAYER, ItemRenderState.Glint.NONE);
					}
				}
			}
			return ActionResult.PASS;
		});

		WorldRenderer.RenderLayer.Callback.EVENT.register((receiver, renderLayer) -> {

			if(renderLayer.equals(getTargetEnchantColorLayer())){
				for(var customLayer : COLOR_LAYERS.renderLayers())
				{
					if(((RenderLayerAccessor)customLayer).enchantOutline$shouldUseLayerBuffer()) {
						receiver.draw(customLayer);
					}
				}
			}
			return ActionResult.PASS;
		});

		WorldRenderer.RenderLayer.Post.EVENT.register(((receiver, renderLayer) -> {
			if(renderLayer.equals(getTargetEnchantColorLayer())){

			}
			return ActionResult.PASS;
		}));

		//VertexConsumerProvider contains a setDirty method used to track if we need to update it's return value or not.
		BufferBuilderModifyReturnValue.EVENT.register((original) -> {
			VertexConsumerProvider_ImmediateAccessor accessor = (VertexConsumerProvider_ImmediateAccessor)original;

			var enchantGlintLayer = getTargetEnchantGlintLayer();
			var enchantColorLayer = getTargetEnchantColorLayer();
			var enchantZFixLayer = getTargetEnchantZFixLayer();

			var buffers = accessor.enchantOutline$getLayerBuffers();
			if(!Objects.equals(accessor.enchantOutline$getDirty(GLINT_LAYERS), GLINT_LAYERS.getDirty()) && buffers.containsKey(enchantGlintLayer) || !Objects.equals(accessor.enchantOutline$getDirty(COLOR_LAYERS), COLOR_LAYERS.getDirty()) && buffers.containsKey(enchantColorLayer) || !Objects.equals(accessor.enchantOutline$getDirty(ZFIX_LAYERS), ZFIX_LAYERS.getDirty()) && buffers.containsKey(enchantZFixLayer)){
				accessor.enchantOutline$setDirty(GLINT_LAYERS, GLINT_LAYERS.getDirty());
				accessor.enchantOutline$setDirty(COLOR_LAYERS, COLOR_LAYERS.getDirty());
				accessor.enchantOutline$setDirty(ZFIX_LAYERS, ZFIX_LAYERS.getDirty());

				SequencedMap<RenderLayer, BufferAllocator> clonedBuffer = new Object2ObjectLinkedOpenHashMap<>(buffers);
				buffers.clear();
				for(var set : clonedBuffer.entrySet()) {
					if(!GLINT_LAYERS.containsRenderLayer(set.getKey()) && !COLOR_LAYERS.containsRenderLayer(set.getKey()) && !ZFIX_LAYERS.containsRenderLayer(set.getKey())) {
						if(set.getKey() == enchantColorLayer) {
							for(RenderLayer layer : COLOR_LAYERS.renderLayers()) {
								if(((RenderLayerAccessor)layer).enchantOutline$shouldUseLayerBuffer()){
									buffers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
								}
							}
						}
						if(set.getKey() == enchantGlintLayer) {
							for(RenderLayer layer : GLINT_LAYERS.renderLayers())
							{
								buffers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
							}
						}
						if(set.getKey() == enchantZFixLayer){
							for(RenderLayer layer : ZFIX_LAYERS.renderLayers())
							{
								buffers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
							}
						}
						buffers.put(set.getKey(), set.getValue());
					}
				}
			}

			return null;
		});
	}

	private static void initLayers(){
		GLINT_LAYERS.addCustomRenderLayer(Identifier.of(MOD_ID,"cutoutlayer"), Shaders.GLINT_CUTOUT_LAYER);
		COLOR_LAYERS.addCustomRenderLayer(Identifier.of(MOD_ID,"cutoutlayer"), Shaders.COLOR_CUTOUT_LAYER);
		ZFIX_LAYERS.addCustomRenderLayer(Identifier.of(MOD_ID, "cutoutlayer"), Shaders.ZFIX_CUTOUT_LAYER);
	}

	public static RenderLayer getOrCreateColorRenderLayer(Identifier identifier)
	{
		RenderLayer output = COLOR_LAYERS.getCustomRenderLayer(identifier);
		if(output != null)
		{
			return output;
		}
		return COLOR_LAYERS.addCustomRenderLayer(identifier, Shaders.createColorRenderLayer(identifier));
	}

	private static void loadConfig() {
		Path configFile = EnchantmentOutlineConfig.CONFIG_FILE;
		if (Files.exists(configFile)) {
			try(BufferedReader reader = Files.newBufferedReader(configFile)) {
				config = EnchantmentOutlineConfig.fromJson(reader);
			} catch (Exception e) {
				LOGGER.error("Error loading Enchantment Glint Outline config file. Default values will be used for this session.", e);
				config = new EnchantmentOutlineConfig();
			}
		} else {
			config = new EnchantmentOutlineConfig();
		}

		// Immediately save config to file to update any fields that may have changed.
		config.saveAsync();
	}
}