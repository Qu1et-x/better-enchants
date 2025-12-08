package net.enchantoutline.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.enchantoutline.events.sodium.ModelCuboidInitBeforeReturnCallback;
import net.enchantoutline.mixin_accessors.sodium.ModelCuboidAccessor;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ModelCuboid.class)

public class ModelCuboidMixin implements ModelCuboidAccessor {
    @Shadow
    @Final
    @Mutable
    public long[] textures;

    @Inject(method = "<init>", at = @At("RETURN"))
    void enchantOutline$setUvOnInit(int u, int v, float x1, float y1, float z1, float sizeX, float sizeY, float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth, float textureHeight, Set<Direction> renderDirections, CallbackInfo ci){
        ModelCuboidInitBeforeReturnCallback.EVENT.invoker().callback((ModelCuboid)(Object)this, u, v, x1, y1, z1, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, renderDirections);
    }


    @Override
    public void enchantOutline$setUvs(long[] uvs) {
        textures = uvs;
    }
}
