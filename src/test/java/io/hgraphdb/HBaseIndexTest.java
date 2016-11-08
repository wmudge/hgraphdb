package io.hgraphdb;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.Iterator;

import static org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils.count;
import static org.junit.Assert.assertEquals;

public class HBaseIndexTest extends HBaseGraphTest {

    @Test
    public void testVertexIndex() {
        assertEquals(0, count(graph.vertices()));
        graph.addVertex(T.id, id(0), T.label, "a", "key1", 1);
        graph.addVertex(T.id, id(1), T.label, "a", "key1", 2);
        graph.addVertex(T.id, id(2), T.label, "a", "key2", 2);
        graph.addVertex(T.id, id(3), T.label, "a", "key1", 1);
        graph.addVertex(T.id, id(4), T.label, "b", "key1", 1);

        Iterator<Vertex> it = graph.allVertices("a", "key1", 1);
        assertEquals(2, count(it));

        graph.createIndex(IndexType.VERTEX, "a", "key1");
        graph.addVertex(T.id, id(10), T.label, "a", "key1", 11);
        graph.addVertex(T.id, id(11), T.label, "a", "key1", 12);
        graph.addVertex(T.id, id(12), T.label, "a", "key2", 12);
        graph.addVertex(T.id, id(13), T.label, "a", "key1", 11);
        graph.addVertex(T.id, id(14), T.label, "b", "key1", 11);

        it = graph.allVertices("a", "key1", 11);
        assertEquals(2, count(it));
    }

    @Test
    public void testVertexIndexRange() {
        assertEquals(0, count(graph.vertices()));
        graph.addVertex(T.id, id(0), T.label, "a", "key1", 0);
        graph.addVertex(T.id, id(1), T.label, "a", "key1", 1);
        graph.addVertex(T.id, id(2), T.label, "a", "key1", 2);
        graph.addVertex(T.id, id(3), T.label, "a", "key1", 3);
        graph.addVertex(T.id, id(4), T.label, "a", "key1", 4);
        graph.addVertex(T.id, id(5), T.label, "a", "key1", 5);

        Iterator<Vertex> it = graph.allVertices("a", "key1", 1, 4);
        assertEquals(3, count(it));

        graph.createIndex(IndexType.VERTEX, "a", "key1");
        graph.addVertex(T.id, id(10), T.label, "a", "key1", 10);
        graph.addVertex(T.id, id(11), T.label, "a", "key1", 11);
        graph.addVertex(T.id, id(12), T.label, "a", "key1", 12);
        graph.addVertex(T.id, id(13), T.label, "a", "key1", 13);
        graph.addVertex(T.id, id(14), T.label, "a", "key1", 14);
        graph.addVertex(T.id, id(15), T.label, "a", "key1", 15);

        it = graph.allVertices("a", "key1", 11, 14);
        assertEquals(3, count(it));
    }

    @Test
    public void testEdgeIndex() {
        assertEquals(0, count(graph.vertices()));
        Vertex v0 = graph.addVertex(T.id, id(0));
        Vertex v1 = graph.addVertex(T.id, id(1));
        Vertex v2 = graph.addVertex(T.id, id(2));
        Vertex v3 = graph.addVertex(T.id, id(3));
        v0.addEdge("b", v1, "key1", 1);
        v0.addEdge("b", v2, "key1", 1);
        v0.addEdge("b", v3, "key1", 2);

        Iterator<Edge> it = ((HBaseVertex) v0).edges(Direction.OUT, "b", "key1", 1);
        assertEquals(2, count(it));

        graph.createIndex(IndexType.EDGE, "b", "key1");
        Vertex v10 = graph.addVertex(T.id, id(10));
        Vertex v11 = graph.addVertex(T.id, id(11));
        Vertex v12 = graph.addVertex(T.id, id(12));
        Vertex v13 = graph.addVertex(T.id, id(13));
        v10.addEdge("b", v11, "key1", 11);
        v10.addEdge("b", v12, "key1", 11);
        v10.addEdge("b", v13, "key1", 12);

        it = ((HBaseVertex) v10).edges(Direction.OUT, "b", "key1", 11);
        assertEquals(2, count(it));
    }

    @Test
    public void testEdgeIndexRange() {
        assertEquals(0, count(graph.vertices()));
        Vertex v0 = graph.addVertex(T.id, id(0));
        Vertex v1 = graph.addVertex(T.id, id(1));
        Vertex v2 = graph.addVertex(T.id, id(2));
        Vertex v3 = graph.addVertex(T.id, id(3));
        Vertex v4 = graph.addVertex(T.id, id(4));
        Vertex v5 = graph.addVertex(T.id, id(5));
        Vertex v6 = graph.addVertex(T.id, id(6));
        v0.addEdge("b", v1, "key1", 1);
        v0.addEdge("b", v2, "key1", 2);
        v0.addEdge("b", v3, "key1", 3);
        v0.addEdge("b", v4, "key1", 4);
        v0.addEdge("b", v5, "key1", 5);
        v0.addEdge("b", v6, "key1", 6);

        Iterator<Edge> it = ((HBaseVertex) v0).edges(Direction.OUT, "b", "key1", 2, 6);
        assertEquals(4, count(it));

        graph.createIndex(IndexType.EDGE, "b", "key1");
        Vertex v10 = graph.addVertex(T.id, id(10));
        Vertex v11 = graph.addVertex(T.id, id(11));
        Vertex v12 = graph.addVertex(T.id, id(12));
        Vertex v13 = graph.addVertex(T.id, id(13));
        Vertex v14 = graph.addVertex(T.id, id(14));
        Vertex v15 = graph.addVertex(T.id, id(15));
        Vertex v16 = graph.addVertex(T.id, id(16));
        v10.addEdge("b", v11, "key1", 11);
        v10.addEdge("b", v12, "key1", 12);
        v10.addEdge("b", v13, "key1", 13);
        v10.addEdge("b", v14, "key1", 14);
        v10.addEdge("b", v15, "key1", 15);
        v10.addEdge("b", v16, "key1", 16);

        it = ((HBaseVertex) v10).edges(Direction.OUT, "b", "key1", 12, 16);
        assertEquals(4, count(it));
    }

    @Test
    public void testGremlinVertexIndex() {
        assertEquals(0, count(graph.vertices()));

        graph.createIndex(IndexType.VERTEX, "a", "key1");
        graph.addVertex(T.id, id(0), T.label, "a", "key1", 0);
        graph.addVertex(T.id, id(1), T.label, "a", "key1", 1);
        graph.addVertex(T.id, id(2), T.label, "a", "key1", 2);
        graph.addVertex(T.id, id(3), T.label, "a", "key1", 3);
        graph.addVertex(T.id, id(4), T.label, "a", "key1", 4);
        graph.addVertex(T.id, id(5), T.label, "a", "key1", 5);

        GraphTraversalSource g = graph.traversal();
        Iterator<Vertex> it = g.V().has("a", "key1", 0);
        assertEquals(1, count(it));
    }
}