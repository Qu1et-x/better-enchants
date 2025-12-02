package net.enchantoutline.mixin_accessors;

//TODO: make it so that instead of a shouldDrawBeforeCustom each renderLayer can store the layer it should draw before, and use that. This should fix ALL strange rendering artifacts
public interface RenderLayerAccessor {
    /**
     * If we shouldn't render this layer in the layer buffer
     * @return
     */
    public boolean enchantOutline$shouldUseLayerBuffer();
    public void enchantOutline$setShouldUseLayerBuffer(boolean newUseLayerBuffer);
}
