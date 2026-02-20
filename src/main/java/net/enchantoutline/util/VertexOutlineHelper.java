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

    public static List<BakedQuad> generateVertexBasedOutline(List<BakedQuad> original, float thickness) {
        // 更严格的几何体检测
        if (isStrictlySimpleGeometry(original)) {
            LOGGER.debug("Detected strictly simple geometry, using enhanced face-based approach");
            return generateConnectedFaceBasedOutline(original, thickness);
        } else {
            LOGGER.debug("Detected complex geometry, using vertex-based approach");
            return generateSmoothVertexBasedOutline(original, thickness);
        }
    }

    /**
     * 更严格的简单几何体检测
     * 只有真正的立方体或类立方体才被认为是简单几何体
     */
    private static boolean isStrictlySimpleGeometry(List<BakedQuad> original) {
        if (original.isEmpty() || original.size() > 12) return false; // 太多面肯定不是简单几何体
        
        // 统计面法线分布
        Map<Direction, Integer> faceNormalCount = new HashMap<>();
        Set<Vec3i> uniqueNormals = new HashSet<>();
        
        for (BakedQuad quad : original) {
            Direction faceDir = quad.face();
            faceNormalCount.merge(faceDir, 1, Integer::sum);
            uniqueNormals.add(faceDir.getVector());
        }
        
        // 检查是否只有标准的6个方向
        if (uniqueNormals.size() != 6 && uniqueNormals.size() != 3) { // 6面体或特殊简化情况
            return false;
        }
        
        // 检查每个方向的面数是否合理（每个方向最多2个面）
        for (int count : faceNormalCount.values()) {
            if (count > 2) return false;
        }
        
        // 检查法线是否都是标准轴向
        for (Vec3i normal : uniqueNormals) {
            int nonZeroComponents = Math.abs(normal.getX()) + Math.abs(normal.getY()) + Math.abs(normal.getZ());
            if (nonZeroComponents != 1) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 针对复杂模型的顶点偏移方法，保持相邻面的连续性
     */
    private static List<BakedQuad> generateSmoothVertexBasedOutline(List<BakedQuad> original, float thickness) {
        Map<Vector3f, Set<Integer>> vertexToFaceMap = new HashMap<>();
        List<Vector3f[]> faces = new ArrayList<>();
        List<Direction> originalFaceDirections = new ArrayList<>();

        // 收集顶点和面信息
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

        // 计算顶点法线（使用角度加权平均）
        Map<Vector3f, Vector3f> vertexNormals = calculateWeightedVertexNormals(faces, vertexToFaceMap);

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

    /**
     * 改进的简单几何体处理方法，确保轮廓连接
     */
    private static List<BakedQuad> generateConnectedFaceBasedOutline(List<BakedQuad> original, float thickness) {
        List<BakedQuad> newQuads = new ArrayList<>();
        
        // 首先收集所有顶点以便计算共享顶点的偏移
        Map<Vector3f, Set<Integer>> vertexToFaceMap = new HashMap<>();
        List<Vector3f[]> faces = new ArrayList<>();
        
        for (int quadIdx = 0; quadIdx < original.size(); quadIdx++) {
            BakedQuad quad = original.get(quadIdx);
            Vector3f[] vertices = {
                new Vector3f(quad.position0()), 
                new Vector3f(quad.position1()), 
                new Vector3f(quad.position2()), 
                new Vector3f(quad.position3())
            };
            
            faces.add(vertices);
            
            for (Vector3f vertex : vertices) {
                Vector3f matchedVertex = findOrAddVertex(vertexToFaceMap, vertex);
                Set<Integer> connectedFaces = vertexToFaceMap.computeIfAbsent(matchedVertex, k -> new HashSet<>());
                connectedFaces.add(quadIdx);
            }
        }
        
        // 为每个顶点计算平均法线方向
        Map<Vector3f, Vector3f> vertexNormals = calculateAverageVertexNormals(faces, vertexToFaceMap);
        
        // 为每个面生成偏移后的顶点
        List<Vector3f[]> offsetFaceVertices = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            BakedQuad quad = original.get(i);
            Vector3f[] originalVertices = faces.get(i);
            Vec3i faceNormal = quad.face().getVector();
            
            Vector3f[] offsetVertices = new Vector3f[4];
            for (int j = 0; j < 4; j++) {
                Vector3f originalVertex = originalVertices[j];
                Vector3f avgNormal = vertexNormals.get(originalVertex);
                
                if (avgNormal != null) {
                    // 使用共享顶点的平均法线
                    offsetVertices[j] = new Vector3f(originalVertex);
                    offsetVertices[j].add(avgNormal.x * thickness, avgNormal.y * thickness, avgNormal.z * thickness);
                } else {
                    // 回退到面法线
                    Vector3f faceNormalVec = new Vector3f(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
                    faceNormalVec.normalize();
                    offsetVertices[j] = new Vector3f(originalVertex);
                    offsetVertices[j].add(faceNormalVec.x * thickness, faceNormalVec.y * thickness, faceNormalVec.z * thickness);
                }
            }
            offsetFaceVertices.add(offsetVertices);
        }
        
        // 创建偏移后的面
        for (int i = 0; i < original.size(); i++) {
            BakedQuad originalQuad = original.get(i);
            Vector3f[] offsetVertices = offsetFaceVertices.get(i);
            
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
     * 计算顶点的平均法线（用于简单几何体）
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

    /**
     * 使用角度加权的顶点法线计算，提高锐角/钝角处的准确性
     */
    private static Map<Vector3f, Vector3f> calculateWeightedVertexNormals(List<Vector3f[]> faces,
            Map<Vector3f, Set<Integer>> vertexToFaceMap) {
        Map<Vector3f, Vector3f> vertexNormals = new HashMap<>();

        for (Vector3f vertex : vertexToFaceMap.keySet()) {
            Set<Integer> connectedFaceIndices = vertexToFaceMap.get(vertex);
            Vector3f weightedNormalSum = new Vector3f(0, 0, 0);
            float totalWeight = 0;

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

                    // 计算角度权重（使用顶点在该面上的角度）
                    float angleWeight = calculateVertexAngleInFace(vertex, faceVertices);
                    weightedNormalSum.fma(angleWeight, faceNormal); // fused multiply-add
                    totalWeight += angleWeight;
                }
            }

            if (totalWeight > 0) {
                weightedNormalSum.div(totalWeight);
                weightedNormalSum.normalize();
            }
            
            vertexNormals.put(vertex, weightedNormalSum);
        }

        return vertexNormals;
    }

    /**
     * 计算顶点在面中的角度权重
     */
    private static float calculateVertexAngleInFace(Vector3f vertex, Vector3f[] faceVertices) {
        // 找到顶点在面中的位置
        int vertexIndex = -1;
        for (int i = 0; i < faceVertices.length; i++) {
            if (faceVertices[i].distance(vertex) < 0.001f) {
                vertexIndex = i;
                break;
            }
        }
        
        if (vertexIndex == -1) return 1.0f; // 默认权重
        
        // 计算相邻边的夹角
        Vector3f prevEdge = new Vector3f(faceVertices[(vertexIndex + 3) % 4]);
        prevEdge.sub(vertex);
        Vector3f nextEdge = new Vector3f(faceVertices[(vertexIndex + 1) % 4]);
        nextEdge.sub(vertex);
        
        prevEdge.normalize();
        nextEdge.normalize();
        
        float dotProduct = prevEdge.dot(nextEdge);
        // 限制在[-1, 1]范围内以防浮点误差
        dotProduct = Math.max(-1.0f, Math.min(1.0f, dotProduct));
        
        // 角度越大，权重越小（钝角权重小，锐角权重大）
        float angle = (float) Math.acos(dotProduct);
        return angle; // 直接使用角度作为权重
    }
}