package net.enchantoutline.mixin;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(RenderPhase.Texture.class)
public interface RenderPhase_TextureAccessor {
    @Invoker("getId")
    Optional<Identifier> invokeGetId();
}
