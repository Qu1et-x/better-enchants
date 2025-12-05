package net.enchantoutline;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.enchantoutline.config.EnchantmentOutlineConfig;
import net.enchantoutline.config.ItemOverride;
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
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
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

	public static int getColorBatchingQueue(){
		return -9124657;
	}

	public static int getZFixBatchingQueue(){
		return 9124657;
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

		//---------- Renderer Calls ----------

		//called before non-special item is rendered.
		RenderQuads.Normal.Callback.EVENT.register((receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
			if(config.isEnabled()){
				if (glintType != ItemRenderState.Glint.NONE) {
					@Nullable ItemOverride override = getItemOverrideFromLayerRenderState(receiver);
					if(override == null || override.shouldRender()){
						if(config.getRenderSolidOverrideOrDefault(override)) {
							//render solid (by having no Z write, while Z test but rendering before the item is Rendered)

							int[] tint = {config.getOutlineColorAsInt(config.getOutlineColorOverrideOrDefault(override))};

							List<BakedQuad> thickenedQuads = QuadHelper.thickenQuad(quads, 0.02f);
							orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tint, thickenedQuads, Shaders.COLOR_CUTOUT_LAYER, ItemRenderState.Glint.NONE);
							orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tintLayers, thickenedQuads, Shaders.ZFIX_CUTOUT_LAYER, ItemRenderState.Glint.NONE);

						}
						else{
							//render glint (by having Z write and Z test, but no color write after rendering the item, in other words write to the depth buffer)
							List<BakedQuad> thickenedQuads = QuadHelper.thickenQuad(quads, 0.02f);
							orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, 0, 0, outlineColors, tintLayers, thickenedQuads, Shaders.GLINT_CUTOUT_LAYER, glintType);
						}
					}
				}
			}
			return ActionResult.PASS;
		});

		//the other half of render solid for special items. They should either both use the renderCommandQueue or both avoid it though, this mix is not making me happy.
		RenderQuads.Model.ModelPart.EVENT.register((receiver, part, matrices, renderLayer, light, overlay, sprite, sheeted, hasGlint, tintedColor, crumblingOverlay, i) -> {
			if(config.isEnabled()){
				if(hasGlint){
					boolean isDoubleSided = isRenderLayerDoubleSided(renderLayer);
					ModelPart thickModelPart = ModelHelper.thickenedModelPart(part, isDoubleSided, 0.02f);
					if(config.shouldRenderSolid()) {
						int tint = config.getOutlineColorAsInt(config.getOutlineColor());

						//get render layer
						RenderLayer colorLayer = getOrCreateRenderLayerRenderLayerWithTexture(renderLayer, (identifier) -> getOrCreateRenderLayer(COLOR_LAYERS, Shaders::createColorRenderLayer, identifier), Shaders.COLOR_CUTOUT_LAYER);
						RenderLayer zFixLayer = getOrCreateRenderLayerRenderLayerWithTexture(renderLayer, (identifier) -> getOrCreateRenderLayer(ZFIX_LAYERS, Shaders::createZFixRenderLayer, identifier), Shaders.ZFIX_CUTOUT_LAYER);

						//render call
						OrderedRenderCommandQueueImplAccessor commandQueueAccessor = (OrderedRenderCommandQueueImplAccessor)receiver;
						commandQueueAccessor.enchantOutline$setSkipModelPartCallback(true);
						receiver.getBatchingQueue(getColorBatchingQueue()).submitModelPart(thickModelPart, matrices, colorLayer, Integer.MAX_VALUE, 0, sprite, sheeted, false, tint, crumblingOverlay, i);
						receiver.getBatchingQueue(getZFixBatchingQueue()).submitModelPart(thickModelPart, matrices, zFixLayer, Integer.MAX_VALUE, 0, sprite, sheeted, false, tint, crumblingOverlay, i);
						commandQueueAccessor.enchantOutline$setSkipModelPartCallback(false);
					}
					else{

						//get render layer
						RenderLayer glintLayer = getOrCreateRenderLayerRenderLayerWithTexture(renderLayer, (identifier) -> getOrCreateRenderLayer(GLINT_LAYERS, Shaders::createGlintRenderLayer, identifier), Shaders.GLINT_CUTOUT_LAYER);

						//render call
						OrderedRenderCommandQueueImplAccessor commandQueueAccessor = (OrderedRenderCommandQueueImplAccessor)receiver;
						commandQueueAccessor.enchantOutline$setSkipModelPartCallback(true);
						receiver.getBatchingQueue(getZFixBatchingQueue()).submitModelPart(thickModelPart, matrices, glintLayer, Integer.MAX_VALUE, 0, sprite, sheeted, true, tintedColor, crumblingOverlay, i);
						commandQueueAccessor.enchantOutline$setSkipModelPartCallback(false);
					}
				}
			}
			return ActionResult.PASS;
		});

		EquipmentRendererQueueEnchantedCallback.EVENT.register(((receiver, queueHolder, texture, model, s, matrixStack, renderLayer, light, overlay, tintColor, sprite, outlineColor, crumblingOverlayCommand) -> {
			//I can build this using the current renderLayer the model class is surprisingly simple. It just is made of a model part which I already am able to render an outline for. just build a new model every frame and we should be set
			if(config.isEnabled()){
				model.setAngles(s);
				if(config.shouldRenderSolid()){
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

					boolean isDoubleSided = isRenderLayerDoubleSided(renderLayer);
					HijackedModel thickColorModel = ModelHelper.getThickenedModel(model, colorLayerFactory, isDoubleSided, 0.02f);

					queueHolder.getBatchingQueue(getColorBatchingQueue()).submitModel(thickColorModel, s, matrixStack, colorLayer, Integer.MAX_VALUE, 0, tint, sprite, outlineColor, crumblingOverlayCommand);
				}
				else{
					Function<Identifier, RenderLayer> glintZLayerFactory = (identifier) -> {
						RenderLayer layer = Shaders.GLINT_CUTOUT_LAYER;
						RenderLayer generatedGlintLayer = getOrCreateRenderLayer(GLINT_LAYERS, Shaders::createGlintRenderLayer, identifier);
						if(generatedGlintLayer != null){
							return generatedGlintLayer;
						}
						return layer;
					};
					RenderLayer glintZLayer = glintZLayerFactory.apply(texture);

					boolean isDoubleSided = isRenderLayerDoubleSided(renderLayer);
					HijackedModel thickGlintZModel = ModelHelper.getThickenedModel(model, glintZLayerFactory, isDoubleSided, 0.02f);

					queueHolder.getBatchingQueue(getZFixBatchingQueue()).submitModel(thickGlintZModel, s, matrixStack, glintZLayer, light, overlay, tintColor, sprite, outlineColor, crumblingOverlayCommand);
					queueHolder.getBatchingQueue(getZFixBatchingQueue()+1).submitModel(thickGlintZModel, s, matrixStack, Shaders.ARMOR_ENTITY_GLINT_FIX, light, overlay, tintColor, sprite, outlineColor, crumblingOverlayCommand);
				}
			}

			return ActionResult.PASS;
		}));

		//---------- End Render Calls ----------

		//---------- Render Layer Order ----------

		//solid ordering in WorldRenderer
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

		//glint ordering in world renderer
		WorldRenderer.RenderLayer.Callback.EVENT.register((receiver, renderLayer) -> {

			if(renderLayer.equals(getTargetEnchantGlintLayer())){
				for(var customLayer : GLINT_LAYERS.renderLayers())
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

						//this block is stupid, but we need to make sure our armor layer goes where we want it to. This is how
						if(set.getKey() == Shaders.ARMOR_ENTITY_GLINT_FIX){
							enchantGlintLayer = Shaders.ARMOR_ENTITY_GLINT_FIX;
						}
						if(set.getKey() == enchantGlintLayer) {
							for(RenderLayer layer : GLINT_LAYERS.renderLayers())
							{
								if(((RenderLayerAccessor)layer).enchantOutline$shouldUseLayerBuffer()) {
									buffers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
								}
							}
						}
						if(set.getKey() == getTargetEnchantGlintLayer()){
							buffers.put(Shaders.ARMOR_ENTITY_GLINT_FIX, new BufferAllocator(Shaders.ARMOR_ENTITY_GLINT_FIX.getExpectedBufferSize()));
						}
						//end dumb block

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
		//--------- End Render Layer Order ----------

		//---------- Item Type Storage ----------
		ItemModelManagerUpdateModelCallback.EVENT.register(((receiver, model, itemRenderState, itemStack, itemModelManager, itemDisplayContext, clientWorld, heldItemContext, seed) -> {
			((ItemRenderStateAccessor)itemRenderState).enchantOutline$setItemRendered(itemStack.getItem());

			return ActionResult.PASS;
		}));

		ItemRenderStateRenderLayerCallback.EVENT.register(((receiver, layerRenderState, matrices, orderedRenderCommandQueue, light, overlay, i) -> {
			((ItemRenderState_LayerRenderStateAccessor)layerRenderState).enchantOutline$setOwningItemRenderState(receiver);

			return ActionResult.PASS;
		}));
		//---------- End Item Type Storage ----------
	}

	@Nullable ItemOverride getItemOverrideFromLayerRenderState(ItemRenderState.LayerRenderState layerRenderState){
		@Nullable ItemRenderState owningState = ((ItemRenderState_LayerRenderStateAccessor)layerRenderState).enchantOutline$getOwningRenderState();
		if(owningState != null){
			@Nullable Item renderedItem = ((ItemRenderStateAccessor)owningState).enchantOutline$getItemRendered();
			if(renderedItem != null){
				Identifier itemId = Registries.ITEM.getId(renderedItem);
				if(itemId != null){
					return config.getItemOverride(itemId.toString());
				}
			}
		}
		return null;
	}

	public static boolean isRenderLayerDoubleSided(RenderLayer renderLayer){
		if(renderLayer instanceof RenderLayer.MultiPhase phase){
			RenderPipeline pipeline = ((RenderLayerMultiPhaseAccessor)(Object)phase).getPipeline();
			if(pipeline != null){
				return !((RenderPipelineAccessor)pipeline).enchantOutline$getCull();
			}
		}
		return false;
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