package net.enchantoutline.events.sodium;

import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;

import java.util.Set;

public interface ModelCuboidInitBeforeReturnCallback {
    Event<ModelCuboidInitBeforeReturnCallback> EVENT = EventFactory.createArrayBacked(ModelCuboidInitBeforeReturnCallback.class,
            (listeners) -> (receiver, u, v, x1, y1, z1, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, renderDirections) -> {
                for (ModelCuboidInitBeforeReturnCallback listener : listeners) {
                    ActionResult result = listener.callback(receiver, u, v, x1, y1, z1, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, renderDirections);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    ActionResult callback(ModelCuboid receiver, int u, int v, float x1, float y1, float z1, float sizeX, float sizeY, float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth, float textureHeight, Set<Direction> renderDirections);
}
