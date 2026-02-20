package net.enchantoutline.util;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VertexOutlineHelper {
    public static final Logger LOGGER = LoggerFactory.getLogger(VertexOutlineHelper.class);

    public static List<BakedQuad> generateVertexBasedOutline(List<BakedQuad> original, float thickness) {

        Map<Vector3f, Set<Integer>> vertexToFaceMap = new HashMap<>();
        List<Vector3f[]> faces = new ArrayList<>();

        for (int quadIdx = 0; quadIdx < original.size(); quadIdx++) {
            BakedQuad quad = original.get(quadIdx);
            Vector3f[] vertices = { new Vector3f(quad.position0()), new Vector3f(quad.position1()),
                    new Vector3f(quad.position2()), new Vector3f(quad.position3()) };

            faces.add(vertices);

            for (Vector3f vertex : vertices) {
                Vector3f matchedVertex = findOrAddVertex(vertexToFaceMap, vertex);

                Set<Integer> connectedFaces = vertexToFaceMap.computeIfAbsent(matchedVertex, k -> new HashSet<>());
                connectedFaces.add(quadIdx);
            }
        }

        Map<Vector3f, Vector3f> vertexNormals = calculateVertexNormals(faces, vertexToFaceMap);

        List<BakedQuad> newQuads = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            BakedQuad originalQuad = original.get(i);
            Vector3f[] originalVertices = faces.get(i);

            Vector3f[] offsetVertices = new Vector3f[4];
            for (int j = 0; j < 4; j++) {
                Vector3f originalVertex = originalVertices[j];
                Vector3f normal = vertexNormals.get(originalVertex);
                if (normal == null) {
                    // If vertex normals are not found, use face normals
                    Vec3i faceNormal = originalQuad.face().getVector();
                    normal = new Vector3f(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
                }

                offsetVertices[j] = new Vector3f(originalVertex);
                offsetVertices[j].add(normal.x() * thickness, normal.y() * thickness, normal.z() * thickness);
            }

            BakedQuad offsetQuad = new BakedQuad(
                    offsetVertices[0], offsetVertices[1],
                    offsetVertices[2], offsetVertices[3],
                    originalQuad.packedUV3(), originalQuad.packedUV2(),
                    originalQuad.packedUV1(), originalQuad.packedUV0(),
                    0, originalQuad.face(), null, false, 100);

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


        return vertex;
    }

    private static Map<Vector3f, Vector3f> calculateVertexNormals(List<Vector3f[]> faces,
            Map<Vector3f, Set<Integer>> vertexToFaceMap) {
        Map<Vector3f, Vector3f> vertexNormals = new HashMap<>();

        for (Vector3f vertex : vertexToFaceMap.keySet()) {
            Set<Integer> connectedFaceIndices = vertexToFaceMap.get(vertex);

            Vector3f normalSum = new Vector3f(0, 0, 0);

            for (Integer faceIndex : connectedFaceIndices) {
                if (faceIndex >= 0 && faceIndex < faces.size()) {
                    Vector3f[] faceVertices = faces.get(faceIndex);

                    // Calculate surface normal
                    Vector3f edge1 = new Vector3f(faceVertices[1]);
                    edge1.sub(faceVertices[0]);

                    Vector3f edge2 = new Vector3f(faceVertices[2]);
                    edge2.sub(faceVertices[0]);

                    Vector3f faceNormal = new Vector3f();
                    edge1.cross(edge2, faceNormal);
                    faceNormal.normalize();

                    normalSum.add(faceNormal);
                }
            }

            normalSum.normalize();
            vertexNormals.put(vertex, normalSum);
        }

        return vertexNormals;
    }
}
