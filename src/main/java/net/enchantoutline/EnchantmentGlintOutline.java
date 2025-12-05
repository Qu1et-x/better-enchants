package net.enchantoutline;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.enchantoutline.config.EnchantmentOutlineConfig;
import net.enchantoutline.events.*;
import net.enchantoutline.mixin.RenderLayerMultiPhaseAccessor;
import net.enchantoutline.mixin.RenderPhase_TextureAccessor;
import net.enchantoutline.mixin_accessors.*;
import net.enchantoutline.model.HijackedModel;
import net.enchantoutline.shader.Shaders;
import net.enchantoutline.util.CustomRenderLayers;
import net.enchantoutline.util.ModelHelper;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class EnchantmentGlintOutline implements ModInitializer {
	public static final String MOD_ID = "enchantment-glint-outline";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static int getSolidBatchingQueue(){
		return -9124657;
	}

	public static int getZFixBatchingQueue(){
		return 1;
	}

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
						orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tintLayers, thickenedQuads, Shaders.ZFIX_CUTOUT_LAYER, ItemRenderState.Glint.NONE);
					}
				}
			}
			return ActionResult.PASS;
		});

		//the other half of render solid for special items. They should either both use the renderCommandQueue or both avoid it though, this mix is not making me happy.
		RenderQuads.Model.ModelPart.EVENT.register((receiver, part, matrices, renderLayer, light, overlay, sprite, sheeted, hasGlint, tintedColor, crumblingOverlay, i) -> {
			if(config.isEnabled()){
				if(hasGlint){
					if(config.shouldRenderSolid()) {
						int tint = config.getOutlineColorAsInt(config.getOutlineColor());

						//get render layer
						RenderLayer colorLayer = getOrCreateRenderLayerRenderLayerWithTexture(renderLayer, (identifier) -> getOrCreateRenderLayer(COLOR_LAYERS, Shaders::createColorRenderLayer, identifier), Shaders.COLOR_CUTOUT_LAYER);
						RenderLayer zFixLayer = getOrCreateRenderLayerRenderLayerWithTexture(renderLayer, (identifier) -> getOrCreateRenderLayer(ZFIX_LAYERS, Shaders::createZFixRenderLayer, identifier), Shaders.ZFIX_CUTOUT_LAYER);

						ModelPart thickModelPart = ModelHelper.thickenedModelPart(part, 0.02f);

						//render call
						OrderedRenderCommandQueueImplAccessor commandQueueAccessor = (OrderedRenderCommandQueueImplAccessor)receiver;
						commandQueueAccessor.enchantOutline$setSkipModelPartCallback(true);
						receiver.getBatchingQueue(getSolidBatchingQueue()).submitModelPart(thickModelPart, matrices, colorLayer, Integer.MAX_VALUE, 0, sprite, sheeted, false, tint, crumblingOverlay, i);
						receiver.getBatchingQueue(getZFixBatchingQueue()).submitModelPart(thickModelPart, matrices, zFixLayer, Integer.MAX_VALUE, 0, sprite, sheeted, false, tint, crumblingOverlay, i);
						commandQueueAccessor.enchantOutline$setSkipModelPartCallback(false);
					}
				}
			}
			return ActionResult.PASS;
		});

		//called right after normal item is rendered. used for render glint (by having Z write and Z test, but no color write after rendering the item, in other words write to the depth buffer)
		RenderQuads.Normal.Post.EVENT.register((receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
			if(config.isEnabled()){
				if(glintType != ItemRenderState.Glint.NONE){
					if(!config.shouldRenderSolid()){
						List<BakedQuad> thickenedQuads = QuadHelper.thickenQuad(quads, 0.02f);
						orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tintLayers, thickenedQuads, Shaders.GLINT_CUTOUT_LAYER, glintType);
					}
				}
			}
			return ActionResult.PASS;
		});

		EquipmentRendererQueueEnchantedCallback.EVENT.register(((receiver, queueHolder, texture, model, s, matrixStack, renderLayer, light, overlay, tintColor, sprite, outlineColor, crumblingOverlayCommand) -> {
			//I can build this using the current renderLayer the model class is surprisingly simple. It just is made of a model part which I already am able to render an outline for. just build a new model every frame and we should be set
			if(config.isEnabled()){
				int tint = config.getOutlineColorAsInt(config.getOutlineColor());

				Function<Identifier, RenderLayer> colorLayerFactory = (identifier) -> {
					RenderLayer layer = Shaders.COLOR_CUTOUT_LAYER;
					RenderLayer generatedColorLayer = getOrCreateRenderLayer(COLOR_LAYERS, Shaders::createColorRenderLayer, identifier);
					if(generatedColorLayer != null){
						return generatedColorLayer;
					}
					return layer;
				};
				RenderLayer colorLayer = colorLayerFactory.apply(texture);

				model.setAngles(s);
				HijackedModel thickColorModel = ModelHelper.getThickenedModel(model, colorLayerFactory, 0.02f);

				queueHolder.getBatchingQueue(getSolidBatchingQueue()).submitModel(thickColorModel, s, matrixStack, colorLayer, Integer.MAX_VALUE, 0, tint, sprite, outlineColor, crumblingOverlayCommand);
			}

			return ActionResult.PASS;
		}));

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

		ImmediateRenderCurrentLayer.Before.EVENT.register((receiver, layer) -> {
			for (RenderLayer renderLayer : ((VertexConsumerProvider_ImmediateAccessor)receiver).enchantOutline$getLayerBuffers().keySet()) {
				if(((RenderLayerAccessor)renderLayer).enchantOutline$shouldDrawBeforeCustom()){
					receiver.draw(renderLayer);
				}
			}

			return ActionResult.PASS;
		});

		ImmediateRenderCurrentLayer.After.EVENT.register((receiver, layer) -> {
			for (RenderLayer renderLayer : ((VertexConsumerProvider_ImmediateAccessor)receiver).enchantOutline$getLayerBuffers().keySet()) {
				if(((RenderLayerAccessor)renderLayer).enchantOutline$shouldDrawAfterCustom()){
					receiver.draw(renderLayer);
				}
			}

			return ActionResult.PASS;
		});

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
								if(((RenderLayerAccessor)layer).enchantOutline$shouldUseLayerBuffer()) {
									buffers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
								}
							}
						}
						if(set.getKey() == enchantZFixLayer){
							for(RenderLayer layer : ZFIX_LAYERS.renderLayers())
							{
								if(((RenderLayerAccessor)layer).enchantOutline$shouldUseLayerBuffer()) {
									buffers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
								}
							}
						}
						buffers.put(set.getKey(), set.getValue());
					}
				}
			}

			return null;
		});
	}

	public static RenderLayer getOrCreateRenderLayerRenderLayerWithTexture(RenderLayer fromLayer, Function<Identifier, RenderLayer> layerCreateFunction, RenderLayer fallback){
		RenderLayer layer = fallback;
		if(fromLayer instanceof RenderLayer.MultiPhase phase){
			RenderLayer.MultiPhaseParameters params = ((RenderLayerMultiPhaseAccessor)(Object)phase).getPhases();
			if(params != null){
				RenderPhase.TextureBase textureBase = ((MultiPhaseParametersAccessor)(Object)params).enchantOutline$getTexture();
				if(textureBase != null){
					Optional<Identifier> texture = ((RenderPhase_TextureAccessor)textureBase).invokeGetId();
					if(texture.isPresent()){
						RenderLayer newLayer = layerCreateFunction.apply(texture.orElseThrow());
						if(newLayer != null){
							layer = newLayer;
						}
					}
				}
			}
		}
		return layer;
	}

	private static void initLayers(){
		GLINT_LAYERS.addCustomRenderLayer(Identifier.of(MOD_ID,"cutoutlayer"), Shaders.GLINT_CUTOUT_LAYER);
		COLOR_LAYERS.addCustomRenderLayer(Identifier.of(MOD_ID,"cutoutlayer"), Shaders.COLOR_CUTOUT_LAYER);
		ZFIX_LAYERS.addCustomRenderLayer(Identifier.of(MOD_ID, "cutoutlayer"), Shaders.ZFIX_CUTOUT_LAYER);
	}

	/*public static void renderModelWithGlint(RenderCommandQueue receiver, Model model, Object s, MatrixStack matrixStack, RenderLayer renderLayer, int light, int overlay, int tintColor, @Nullable Sprite sprite, int outlineColor, @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand){


	}*/

	@Nullable
	public static RenderLayer getOrCreateRenderLayer(CustomRenderLayers customRenderLayers, Function<Identifier, RenderLayer> layerCreationFunction, Identifier identifier){
		RenderLayer output = customRenderLayers.getCustomRenderLayer(identifier);
		if(output != null)
		{
			return output;
		}
		return customRenderLayers.addCustomRenderLayer(identifier, layerCreationFunction.apply(identifier));
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