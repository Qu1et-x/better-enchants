package net.enchantoutline.util;

import net.enchantoutline.mixin_accessors.ModelPart_CuboidAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class QuadHelper {
    public static final Logger LOGGER = LoggerFactory.getLogger(QuadHelper.class);

    public static List<BakedQuad> thickenQuad(List<BakedQuad> original, float percentSize){
        List<BakedQuad> newQuads = new ArrayList<>(original.size()*4);
        for (BakedQuad quad : original) {
            int[] vertexData = quad.vertexData().clone();
            Vector3f[] defaultVerts = VertexHelper.getVertexPos(vertexData);

            Vec3i intVec = quad.face().getVector();
            Vector3f faceVec = new Vector3f(intVec.getX(), intVec.getY(), intVec.getZ());
            faceVec.mul(percentSize);

            Vector3f[] cardinalDirs = VertexHelper.getFaceCardinalDirs(defaultVerts, percentSize);
            if(cardinalDirs != null){
                for (Vector3f dir : cardinalDirs) {
                    Vector3f[] vertPoses = VertexHelper.growFace(defaultVerts, dir, faceVec);

                    VertexHelper.setVertexData(vertexData, vertPoses);

                    BakedQuad enchantmentQuad = new BakedQuad(VertexHelper.flip(vertexData), 0, quad.face().getOpposite(), null, false, 100);

                    newQuads.add(enchantmentQuad);
                }
            }
            else{
                LOGGER.warn("Quad did not have 4 vertices");
            }
        }
        return newQuads;
    }

    /*public static List<ModelPart.Cuboid> thickenCuboid(List<ModelPart.Cuboid> original, float percentSize){
        List<ModelPart.Cuboid> newCuboids = new ArrayList<>(original.size()*8);
        for (ModelPart.Cuboid cuboid : original) {
            ModelPart_CuboidAccessor cuboidAccessor = (ModelPart_CuboidAccessor)cuboid;
            ModelPart.Quad[] quads = cuboidAccessor.enchantOutline$getSides();
            if(quads.length > 0){
                ModelPart.Quad quad = quads[0];

                ModelPart.Vertex[] verts = quad.vertices();
                Vector3f[] defaultVerts = new Vector3f[verts.length];
                for(int i = 0; i < defaultVerts.length; i++)
                {
                    defaultVerts[i] = new Vector3f(verts[i].x(), verts[i].y(), verts[i].z());
                }

                Vector3f[] cardinalDirs = VertexHelper.getFaceCardinalDirs(defaultVerts, percentSize);
                if(cardinalDirs != null){
                    Vector3f faceVec = new Vector3f(quad.direction());
                    faceVec.normalize();
                    faceVec.mul(percentSize);

                    Vector3f[] doubleSides = {faceVec, new Vector3f(faceVec)};

                    for(var curFaceVec : doubleSides){
                        for(Vector3f dir : cardinalDirs){
                            Vector3f normalizedNormal = new Vector3f(curFaceVec);
                            normalizedNormal.normalize();
                            normalizedNormal.mul(0.0001f);
                            curFaceVec.add(normalizedNormal);



                            ModelPart.Cuboid newCuboid = new ModelPart.Cuboid(0,0, cuboid.minX + dir.x() + curFaceVec.x(), cuboid.minY + dir.y() + curFaceVec.y(), cuboid.minZ + dir.z() + curFaceVec.z(), cuboid.maxX - cuboid.minX + dir.x() + curFaceVec.x(), cuboid.maxY - cuboid.minY + dir.y() + curFaceVec.y(), cuboid.maxZ - cuboid.minZ + dir.z() + curFaceVec.z(), 0,0,0, false, 0, 0, Collections.emptySet());
                            //ModelPart_CuboidAccessor newAccessor = (ModelPart_CuboidAccessor)newCuboid;

                            for(ModelPart.Quad curQuad : quads){
                                int length = curQuad.vertices().length;
                                for(int i = 0; i < length; i++){
                                    ModelPart.Vertex vertex = quad.vertices()[i];
                                    curQuad.vertices()[length - 1 - i] = new ModelPart.Vertex(vertex.x() + dir.x() + curFaceVec.x(), vertex.y() + dir.y() + curFaceVec.y(), vertex.z() + dir.z() + curFaceVec.z(), vertex.u(), vertex.v());
                                }
                            }

                            newCuboids.add(newCuboid);
                        }
                    }
                }
                else {
                    LOGGER.warn("Model Quad did not have 4 vertices");
                }
            }
        }
        return newCuboids;
    }*/

    //instead of doing this make a base class that we can do the math to instead, and make everything more reliable
    @Deprecated
    public static List<BakedQuad> modelPartQuadToBakedQuad(ModelPart.Quad[] original){
        List<BakedQuad> newQuads = new ArrayList<>(original.length);
        for(var quad : original){
            var verts = quad.vertices();
            //LOGGER.info("try 2: {}", verts);
            //Vector3f[] convertedVerts = new Vector3f[verts.length];
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
