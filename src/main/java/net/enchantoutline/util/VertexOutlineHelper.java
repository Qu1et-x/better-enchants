package net.enchantoutline.util;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VertexOutlineHelper {
    public static final Logger LOGGER = LoggerFactory.getLogger(VertexOutlineHelper.class);

    /**
     * 统一的顶点偏移轮廓生成方法
     * 适用于所有类型的几何体，包括简单和复杂模型
     */
    public static List<BakedQuad> generateVertexBasedOutline(List<BakedQuad> original, float thickness) {
        LOGGER.debug("Generating vertex-based outline for {} quads", original.size());
        
        // 收集所有顶点和面的连接关系
        Map<Vector3f, Set<Integer>> vertexToFaceMap = new HashMap<>();
        List<Vector3f[]> faces = new ArrayList<>();
        List<Direction> originalFaceDirections = new ArrayList<>();

        // 收集顶点信息
        for (int quadIdx = 0; quadIdx < original.size(); quadIdx++) {
            BakedQuad quad = original.get(quadIdx);
            Vector3f[] vertices = { 
                new Vector3f(quad.position0()), 
                new Vector3f(quad.position1()),
                new Vector3f(quad.position2()), 
                new Vector3f(quad.position3()) 
            };

            faces.add(vertices);
            originalFaceDirections.add(quad.face());

            for (Vector3f vertex : vertices) {
                Vector3f matchedVertex = findOrAddVertex(vertexToFaceMap, vertex);
                Set<Integer> connectedFaces = vertexToFaceMap.computeIfAbsent(matchedVertex, k -> new HashSet<>());
                connectedFaces.add(quadIdx);
            }
        }

        // 计算每个顶点的平均法线
        Map<Vector3f, Vector3f> vertexNormals = calculateAverageVertexNormals(faces, vertexToFaceMap);

        // 生成新的轮廓面
        List<BakedQuad> newQuads = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            BakedQuad originalQuad = original.get(i);
            Vector3f[] originalVertices = faces.get(i);
            Direction originalFaceDir = originalFaceDirections.get(i);
            Vec3i originalFaceNormal = originalFaceDir.getVector();

            Vector3f[] offsetVertices = new Vector3f[4];
            for (int j = 0; j < 4; j++) {
                Vector3f originalVertex = originalVertices[j];
                Vector3f normal = vertexNormals.get(originalVertex);
                
                if (normal == null) {
                    // 回退到面法线
                    normal = new Vector3f(originalFaceNormal.getX(), originalFaceNormal.getY(), originalFaceNormal.getZ());
                } else {
                    // 确保法线方向与原始面法线一致
                    Vector3f faceNormalVec = new Vector3f(originalFaceNormal.getX(), originalFaceNormal.getY(), originalFaceNormal.getZ());
                    float dotProduct = normal.dot(faceNormalVec);
                    
                    // 如果法线方向相反，则翻转
                    if (dotProduct < 0) {
                        normal = new Vector3f(-normal.x, -normal.y, -normal.z);
                    }
                }

                offsetVertices[j] = new Vector3f(originalVertex);
                offsetVertices[j].add(normal.x * thickness, normal.y * thickness, normal.z * thickness);
            }

            // 使用正确的构造函数参数顺序
            BakedQuad offsetQuad = new BakedQuad(
                    offsetVertices[3], offsetVertices[2], offsetVertices[1], offsetVertices[0],
                    originalQuad.packedUV3(), originalQuad.packedUV2(),
                    originalQuad.packedUV1(), originalQuad.packedUV0(),
                    0, originalQuad.face().getOpposite(), null, false, 100);

            newQuads.add(offsetQuad);
        }

        return newQuads;
    }

    private static Vector3f findOrAddVertex(Map<Vector3f, Set<Integer>> vertexMap, Vector3f vertex) {
        final float tolerance = 0.001f;

        for (Vector3f existing : vertexMap.keySet()) {
            if (existing.distance(vertex) < tolerance) {
                return existing;
            }
        }

        return new Vector3f(vertex.x, vertex.y, vertex.z);
    }

    /**
     * 计算顶点的平均法线（统一用于所有几何体类型）
     */
    private static Map<Vector3f, Vector3f> calculateAverageVertexNormals(List<Vector3f[]> faces,
            Map<Vector3f, Set<Integer>> vertexToFaceMap) {
        Map<Vector3f, Vector3f> vertexNormals = new HashMap<>();

        for (Vector3f vertex : vertexToFaceMap.keySet()) {
            Set<Integer> connectedFaceIndices = vertexToFaceMap.get(vertex);
            Vector3f normalSum = new Vector3f(0, 0, 0);
            int count = 0;

            for (Integer faceIndex : connectedFaceIndices) {
                if (faceIndex >= 0 && faceIndex < faces.size()) {
                    Vector3f[] faceVertices = faces.get(faceIndex);

                    // 计算面法线
                    Vector3f edge1 = new Vector3f(faceVertices[1]);
                    edge1.sub(faceVertices[0]);
                    Vector3f edge2 = new Vector3f(faceVertices[2]);
                    edge2.sub(faceVertices[0]);
                    
                    Vector3f faceNormal = new Vector3f();
                    edge1.cross(edge2, faceNormal);
                    faceNormal.normalize();

                    normalSum.add(faceNormal);
                    count++;
                }
            }

            if (count > 0) {
                normalSum.div(count);
                normalSum.normalize();
                vertexNormals.put(vertex, normalSum);
            }
        }

        return vertexNormals;
    }
}
