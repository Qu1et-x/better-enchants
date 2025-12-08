package net.enchantoutline.modmixinutil;

import net.minecraft.client.model.ModelPart;

public class SodiumHelper {

    private static final ThreadLocal<ModelPart.Quad[]> setQuads = new ThreadLocal<>();

    public static ThreadLocal<ModelPart.Quad[]> getSetQuads() {
        return setQuads;
    }
}
