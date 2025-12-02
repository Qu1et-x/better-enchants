package net.enchantoutline.mixin;

import net.enchantoutline.events.RenderQuads;
import net.enchantoutline.mixin_accessors.ModelPart_CuboidAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cuboid.class)
public class ModelPart_CuboidMixin implements ModelPart_CuboidAccessor {
    @Shadow
    @Final
    @Mutable
    public ModelPart.Quad[] sides;

    @Override
    public ModelPart.Quad[] enchantOutline$getSides() {
        return sides;
    }

    @Override
    public void enchantOutline$SetSides(ModelPart.Quad[] newSides) {
        sides = newSides;
    }
}
