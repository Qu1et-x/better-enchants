package net.enchantoutline.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.enchantoutline.EnchantmentGlintOutline;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Taken from webspeak
 */
public class EnchantmentOutlineConfig {
    public static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("enchantment-glint-outline.json");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean render_solid = false;
    public float[] render_solid_outline_color = {0.827f,0.592f,0.973f};

    public Map<String, ItemOverride> overrides;

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    public boolean isEnabled(){
        return enabled;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static EnchantmentOutlineConfig fromJson(String json) {
        return GSON.fromJson(json, EnchantmentOutlineConfig.class);
    }

    public static EnchantmentOutlineConfig fromJson(Reader reader) {
        return GSON.fromJson(reader, EnchantmentOutlineConfig.class);
    }

    /**
     * Asynchronously save the Enchantment Glint Outline config to file.
     * @return A future that completes when the config is saved.
     */
    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(() -> {
            try(BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE)) {
                writer.write(toJson());
            } catch (Exception e) {
                EnchantmentGlintOutline.LOGGER.error("Error saving Enchant Glint Outline config.", e);
                throw new CompletionException(e);
            }
        });
    }
}
