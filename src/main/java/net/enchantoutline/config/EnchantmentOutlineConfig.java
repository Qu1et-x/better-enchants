package net.enchantoutline.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.enchantoutline.EnchantmentGlintOutline;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
    public int[] render_solid_outline_color_rgb = {210,150,248};
    public boolean render_armor = true;
    public boolean render_armor_solid = false;
    public Map<String, ItemOverride> item_overrides = new HashMap<>();
    public Map<String, ItemOverride> armor_overrides = new HashMap<>();

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

    public int[] getOutlineColor()
    {
        return render_solid_outline_color_rgb;
    }

    public void setBaseSolidOutlineColorAsInt(int color)
    {
        color = ColorHelper.withAlpha(255, color);
        int[] newOutlineColor = new int[3];
        newOutlineColor[0] = ColorHelper.getRed(color);
        newOutlineColor[1] = ColorHelper.getGreen(color);
        newOutlineColor[2] = ColorHelper.getBlue(color);
        render_solid_outline_color_rgb = newOutlineColor;
    }

    public int getOutlineColorAsInt(int[] outlineColorInt)
    {
        if(outlineColorInt.length < 3){
            return -1;
        }
        return ColorHelper.withAlpha(255,ColorHelper.getArgb((outlineColorInt[0]), (outlineColorInt[1]), (outlineColorInt[2])));
    }

    public void setRenderArmor(boolean renderArmor){
        this.render_armor = renderArmor;
    }

    public boolean shouldRenderArmor(){
        return render_armor;
    }

    public void setRenderArmorSolid(boolean renderArmorSolid){
        this.render_armor_solid = renderArmorSolid;
    }

    public boolean shouldRenderArmorSolid(){
        return render_armor_solid;
    }

    public void setItemOverrides(Map<String, ItemOverride> item_overrides){
        this.item_overrides = item_overrides;
    }

    public Map<String, ItemOverride> getItemOverrides(){
        return item_overrides;
    }

    public void setArmorOverrides(Map<String, ItemOverride> armor_overrides){
        this.armor_overrides = armor_overrides;
    }

    public Map<String, ItemOverride> getArmorOverrides(){
        return armor_overrides;
    }

    @Nullable
    public ItemOverride getItemOverride(String item){
        return item_overrides.get(item);
    }

    @Nullable
    public ItemOverride getArmorOverride(String item){
        return armor_overrides.get(item);
    }

    public boolean getRenderSolidOverrideOrDefault(ItemOverride override){
        if(override != null){
            if(override.shouldOverrideRenderSolid()){
                return override.shouldRenderSolid();
            }
        }
        return shouldRenderSolid();
    }

    public int[] getOutlineColorOverrideOrDefault(ItemOverride override){
        if(override != null){
            if(override.shouldOverrideRenderSolidOutlineColor()){
                return override.getRenderSolidOutlineColor();
            }
        }
        return getOutlineColor();
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
