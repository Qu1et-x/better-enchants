package net.enchantoutline.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.ActionResult;

import java.util.List;

public interface RenderItem {
    public static class Normal{
        public static interface Callback {
            Event<RenderItem.Normal.Callback> EVENT = EventFactory.createArrayBacked(net.enchantoutline.events.RenderItem.Normal.Callback.class,
                    (listeners) -> (receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
                        for (net.enchantoutline.events.RenderItem.Normal.Callback listener : listeners) {
                            ActionResult result = listener.renderItem(receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType);

                            if (result != ActionResult.PASS) {
                                return result;
                            }
                        }

                        return ActionResult.PASS;
                    });

            ActionResult renderItem(ItemRenderState.LayerRenderState receiver, OrderedRenderCommandQueue orderedRenderCommandQueue, MatrixStack matrixStack, ItemDisplayContext itemDisplayContext, int light, int overlay, int outlineColors, int[] tintLayers, List<BakedQuad> quads, RenderLayer renderLayer, ItemRenderState.Glint glintType);
        }

        public static interface Post {
            Event<net.enchantoutline.events.RenderItem.Normal.Post> EVENT = EventFactory.createArrayBacked(net.enchantoutline.events.RenderItem.Normal.Post.class,
                    (listeners) -> (receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType) -> {
                        for (net.enchantoutline.events.RenderItem.Normal.Post listener : listeners) {
                            ActionResult result = listener.renderItem(receiver, orderedRenderCommandQueue, matrixStack, itemDisplayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, glintType);

                            if (result != ActionResult.PASS) {
                                return result;
                            }
                        }

                        return ActionResult.PASS;
                    });
            ActionResult renderItem(ItemRenderState.LayerRenderState receiver, OrderedRenderCommandQueue orderedRenderCommandQueue, MatrixStack matrixStack, ItemDisplayContext itemDisplayContext, int light, int overlay, int outlineColors, int[] tintLayers, List<BakedQuad> quads, RenderLayer renderLayer, ItemRenderState.Glint glintType);
        }
    }
}
