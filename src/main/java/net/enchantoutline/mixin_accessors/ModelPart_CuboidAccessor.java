package net.enchantoutline.mixin_accessors;

import net.minecraft.client.model.ModelPart;

public interface ModelPart_CuboidAccessor {
    public ModelPart.Quad[] enchantOutline$getSides();
    public void enchantOutline$SetSides(ModelPart.Quad[] newSides);
}
