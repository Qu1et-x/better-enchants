package net.enchantoutline;

import net.enchantoutline.config.EnchantmentOutlineConfig;
import net.enchantoutline.events.RenderItem;
import net.enchantoutline.shader.Shaders;
import net.enchantoutline.util.QuadHelper;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EnchantmentGlintOutline implements ModInitializer {
	public static final String MOD_ID = "enchantment-glint-outline";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static EnchantmentOutlineConfig config;

	public static EnchantmentOutlineConfig getConfig()
	{
		return config;
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		loadConfig();

		//called before non-special item is rendered. used for render solid (by having no Z write, while Z test but rendering before the item is Rendered)
		RenderItem.Normal.Callback.EVENT.register((receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
			if(config.isEnabled()){

			}
			return ActionResult.PASS;
		});

		//called right after special item is rendered. used for render glint (by having Z write and Z test, but no color write after rendering the item, in other words write to the depth buffer)
		RenderItem.Normal.Post.EVENT.register((receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
			if(config.isEnabled()){
				if(glintType != ItemRenderState.Glint.NONE){
					List<BakedQuad> thickenedQuads = QuadHelper.thicken(quads, 0.02f);
					orderedRenderCommandQueue.submitItem(matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, thickenedQuads, Shaders.GLINT_CUTOUT_LAYER, glintType);
				}
			}
			return ActionResult.PASS;
		});
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