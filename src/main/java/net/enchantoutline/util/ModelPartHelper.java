package net.enchantoutline.util;

import net.enchantoutline.mixin_accessors.ModelPartAccessor;
import net.enchantoutline.mixin_accessors.ModelPart_CuboidAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

import java.util.*;

public class ModelPartHelper {
    public static ModelPart thickenedModelPart(ModelPart original, float scale){
        //get thick
        ModelPartAccessor modelPartAccessor = (ModelPartAccessor)(Object)original;
        List<ModelPart.Cuboid> cuboids = modelPartAccessor.enchantOutline$getCuboids();

        List<ModelPart.Cuboid> thickCuboids = new ArrayList<>();
        for(var cuboid: cuboids){
            ModelPart.Quad[] cubeQuads = ((ModelPart_CuboidAccessor)cuboid).enchantOutline$getSides();
            List<BakedQuad> bakedQuads = ModelPartHelper.modelPartQuadToBakedQuad(cubeQuads);

            List<BakedQuad> thickQuads = QuadHelper.thickenQuad(bakedQuads, 0.2f);
            List<ModelPart.Cuboid> thickFaceCuboid = ModelPartHelper.bakedQuadToCuboid(thickQuads);
            thickCuboids.addAll(thickFaceCuboid);
        }

        Map<String, ModelPart> oldChildren = modelPartAccessor.enchantOutline$getChildren();
        Map<String, ModelPart> newChildren = new HashMap<>(oldChildren.size());
        for(var set :oldChildren.entrySet()){
            newChildren.put(set.getKey(), thickenedModelPart(set.getValue(), scale));
        }

        ModelPart thickModelPart = new ModelPart(thickCuboids, newChildren);
        thickModelPart.setDefaultTransform(original.getDefaultTransform());
        thickModelPart.setTransform(thickModelPart.getTransform());

        return thickModelPart;
    }

    //instead of doing this make a base quad class that we can do the math to instead, and make everything more reliable
    @Deprecated
    public static List<BakedQuad> modelPartQuadToBakedQuad(ModelPart.Quad[] original){
        List<BakedQuad> newQuads = new ArrayList<>(original.length);
        for(var quad : original){
            var verts = quad.vertices();
            int[] vertexData = new int[verts.length*8];
            for(int i = 0; i < verts.length; i++)
            {
                float[] uvs = {verts[i].u(),verts[i].v()};//BetterEnchants.getConfig().getCustomOrCurrentUV(verts[i].u(), verts[i].v(), isArmor)
                VertexHelper.packVertexData(vertexData, i, new Vector3f(verts[i].x(), verts[i].y(), verts[i].z()), uvs[0], uvs[1]);
            }

            BakedQuad enchantmentQuad = new BakedQuad(vertexData, 0, Direction.fromVector((int)quad.direction().x(), (int)quad.direction().y(), (int)quad.direction().z(), Direction.NORTH), null, false, 100);
            newQuads.add(enchantmentQuad);
        }
        return newQuads;
    }

    @Deprecated
    public static List<ModelPart.Cuboid> bakedQuadToCuboid(List<BakedQuad> original){
        List<ModelPart.Cuboid> newCuboids = new ArrayList<>(original.size());
        for(BakedQuad baked : original){
            VertexHelper.Vertex[] verts = VertexHelper.getVertexData(baked.vertexData());
            if(verts.length == 4){
                ModelPart.Cuboid newCuboid = new ModelPart.Cuboid(0,0, verts[0].pos().x(), verts[0].pos().y(), verts[0].pos().z(), verts[2].pos().x() - verts[0].pos().x(), verts[2].pos().y() - verts[0].pos().y(), verts[2].pos().z() - verts[0].pos().z(), 0 ,0 ,0, false, 0, 0, Set.of());
                ModelPart.Vertex[] newVerts = new ModelPart.Vertex[verts.length];
                for(int i = 0; i < verts.length; i++){
                    VertexHelper.Vertex vert = verts[i];
                     newVerts[i] = new ModelPart.Vertex(vert.pos().x(), vert.pos().y(), vert.pos().z(), vert.u(), vert.v());
                }
                ModelPart.Quad[] quadOut = {new ModelPart.Quad(newVerts, baked.face().getFloatVector())};
                ((ModelPart_CuboidAccessor)newCuboid).enchantOutline$SetSides(quadOut);

                newCuboids.add(newCuboid);
            }
        }
        return newCuboids;
    }
}
