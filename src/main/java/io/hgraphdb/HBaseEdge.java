package io.hgraphdb;

import io.hgraphdb.models.EdgeModel;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.Iterator;
import java.util.Map;

public class HBaseEdge extends HBaseElement implements Edge {

    private Vertex inVertex;
    private Vertex outVertex;

    public HBaseEdge(HBaseGraph graph, Object id) {
        this(graph, id, null, null, null, null, null, null);
    }

    public HBaseEdge(HBaseGraph graph, Object id, String label, Long createdAt, Long updatedAt, Map<String, Object> properties,
                     Vertex inVertex, Vertex outVertex) {
        super(graph, id, label, createdAt, updatedAt, properties);
        this.inVertex = inVertex;
        this.outVertex = outVertex;
    }

    public HBaseEdge(HBaseGraph graph, Object id, String label, Long createdAt, Long updatedAt, Map<String, Object> properties,
                     boolean propertiesFullyLoaded, Vertex inVertex, Vertex outVertex) {
        super(graph, id, label, createdAt, updatedAt, properties, propertiesFullyLoaded);
        this.inVertex = inVertex;
        this.outVertex = outVertex;
    }

    @Override
    public EdgeModel getModel() {
        return graph.getEdgeModel();
    }

    @Override
    public void load() {
        super.load();
    }

    @Override
    public void copyFrom(HBaseElement element) {
        super.copyFrom(element);
        if (element instanceof HBaseEdge) {
            HBaseEdge copy = (HBaseEdge) element;
            if (copy.inVertex != null) this.inVertex = copy.inVertex;
            if (copy.outVertex != null) this.outVertex = copy.outVertex;
        }
    }

    @Override
    public Vertex outVertex() {
        return getVertex(Direction.OUT);
    }

    @Override
    public Vertex inVertex() {
        return getVertex(Direction.IN);
    }

    @Override
    public Iterator<Vertex> vertices(final Direction direction) {
        return direction == Direction.BOTH
                ? IteratorUtils.of(getVertex(Direction.OUT), getVertex(Direction.IN))
                : IteratorUtils.of(getVertex(direction));
    }

    public Vertex getVertex(Direction direction) throws IllegalArgumentException {
        if (!Direction.IN.equals(direction) && !Direction.OUT.equals(direction)) {
            throw new IllegalArgumentException("Invalid direction: " + direction);
        }

        if (inVertex == null || outVertex == null) load();

        return Direction.IN.equals(direction) ? inVertex : outVertex;
    }

    @Override
    public void remove() {
        // Get rid of the endpoints and edge themselves.
        graph.getEdgeIndexModel().deleteEdgeEndpoints(this);
        graph.getEdgeModel().deleteEdge(this);

        setDeleted(true);
        if (!isCached()) {
            HBaseEdge cachedEdge = (HBaseEdge) graph.findEdge(id, false);
            if (cachedEdge != null) cachedEdge.setDeleted(true);
        }
    }

    @Override
    public <V> Iterator<Property<V>> properties(final String... propertyKeys) {
        Iterable<String> keys = getPropertyKeys();
        Iterator<String> filter = IteratorUtils.filter(keys.iterator(),
                key -> ElementHelper.keyExists(key, propertyKeys));
        return IteratorUtils.map(filter,
                key -> new HBaseProperty<>(graph, this, key, getProperty(key)));
    }

    @Override
    public <V> Property<V> property(final String key) {
        V value = getProperty(key);
        return value != null ? new HBaseProperty<>(graph, this, key, value) : Property.empty();
    }

    @Override
    public <V> Property<V> property(final String key, final V value) {
        setProperty(key, value);
        return new HBaseProperty<>(graph, this, key, value);
    }

    @Override
    public String toString() {
        return StringFactory.edgeString(this);
    }
}