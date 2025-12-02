package net.enchantoutline.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.enchantoutline.EnchantmentGlintOutline;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.ColorHelper;

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

    public void setRenderSolid(boolean renderSolid){
        this.render_solid = renderSolid;
    }

    public boolean shouldRenderSolid(){
        return render_solid;
    }

    public float[] getOutlineColor()
    {
        return render_solid_outline_color;
    }

    public void setBaseSolidOutlineColorAsInt(int color)
    {
        color = ColorHelper.withAlpha(255, color);
        float[] newOutlineColor = new float[3];
        newOutlineColor[0] = ColorHelper.getRedFloat(color);
        newOutlineColor[1] = ColorHelper.getGreenFloat(color);
        newOutlineColor[2] = ColorHelper.getBlueFloat(color);
        render_solid_outline_color = newOutlineColor;
    }

    public int getOutlineColorAsInt(float[] outlineColorFloat)
    {
        if(outlineColorFloat.length < 3){
            return -1;
        }
        return ColorHelper.withAlpha(255,ColorHelper.getArgb((int)(outlineColorFloat[0]*255), (int)(outlineColorFloat[1]*255), (int)(outlineColorFloat[2]*255)));
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
