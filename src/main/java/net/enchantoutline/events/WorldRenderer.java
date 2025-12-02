package net.enchantoutline.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.ActionResult;

import java.util.List;

public interface WorldRenderer {
    public static interface RenderLayer{
        public static interface Callback{
            Event<Post> EVENT = EventFactory.createArrayBacked(Post.class,
                    (listeners) -> (receiver, layer) -> {
                        for (Post listener : listeners) {
                            ActionResult result = listener.renderLayer(receiver, layer);

                            if (result != ActionResult.PASS) {
                                return result;
                            }
                        }

                        return ActionResult.PASS;
                    });

            ActionResult renderLayer(VertexConsumerProvider.Immediate receiver, net.minecraft.client.render.RenderLayer layer);
        }
        public static interface Post{
                Event<Post> EVENT = EventFactory.createArrayBacked(Post.class,
                        (listeners) -> (receiver, layer) -> {
                            for (Post listener : listeners) {
                                ActionResult result = listener.renderLayer(receiver, layer);

                                if (result != ActionResult.PASS) {
                                    return result;
                                }
                            }

                            return ActionResult.PASS;
                        });

                ActionResult renderLayer(VertexConsumerProvider.Immediate receiver, net.minecraft.client.render.RenderLayer layer);
        }
    }
}
