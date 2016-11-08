package io.hgraphdb.mutators;

import io.hgraphdb.Constants;
import io.hgraphdb.HBaseEdge;
import io.hgraphdb.HBaseGraph;
import io.hgraphdb.IndexMetadata;
import io.hgraphdb.Serializer;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.Iterator;

public class EdgeIndexWriter implements Mutator {

    private final HBaseGraph graph;
    private final Edge edge;
    private final String key;
    private final Iterator<IndexMetadata> indices;

    public EdgeIndexWriter(HBaseGraph graph, Edge edge, String key) {
        this.graph = graph;
        this.edge = edge;
        this.key = key;
        this.indices = null;
    }

    public EdgeIndexWriter(HBaseGraph graph, Edge edge, Iterator<IndexMetadata> indices) {
        this.graph = graph;
        this.edge = edge;
        this.key = null;
        this.indices = indices;
    }

    @Override
    public Iterator<Mutation> constructMutations() {
        return indices != null ? constructPutsForIndices() : constructPutsForKey();
    }

    private Iterator<Mutation> constructPutsForIndices() {
        return IteratorUtils.flatMap(indices, index -> {
            Put in = constructPut(Direction.IN, index.propertyKey());
            Put out = constructPut(Direction.OUT, index.propertyKey());
            return IteratorUtils.of(in, out);
        });
    }

    private Iterator<Mutation> constructPutsForKey() {
        Put in = constructPut(Direction.IN, key);
        Put out = constructPut(Direction.OUT, key);
        return IteratorUtils.of(in, out);
    }

    private Put constructPut(Direction direction, String key) {
        Put put = new Put(graph.getEdgeIndexModel().serializeForWrite(edge, direction, key));
        put.addColumn(Constants.DEFAULT_FAMILY_BYTES, Constants.CREATED_AT_BYTES,
                Serializer.serialize(((HBaseEdge)edge).createdAt()));
        return put;
    }
}