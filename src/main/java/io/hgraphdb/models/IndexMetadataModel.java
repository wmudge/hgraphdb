package io.hgraphdb.models;

import io.hgraphdb.*;
import io.hgraphdb.IndexMetadata.State;
import io.hgraphdb.mutators.Creator;
import io.hgraphdb.mutators.IndexMetadataRemover;
import io.hgraphdb.mutators.IndexMetadataWriter;
import io.hgraphdb.mutators.Mutator;
import io.hgraphdb.mutators.Mutators;
import io.hgraphdb.readers.IndexMetadataReader;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Order;
import org.apache.hadoop.hbase.util.OrderedBytes;
import org.apache.hadoop.hbase.util.PositionedByteRange;
import org.apache.hadoop.hbase.util.SimplePositionedByteRange;
import org.apache.hadoop.hbase.util.SimplePositionedMutableByteRange;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.io.IOException;
import java.util.Iterator;

public class IndexMetadataModel extends BaseModel {

    public IndexMetadataModel(HBaseGraph graph, Table table) {
        super(graph, table);
    }

    public void createIndexMetadata(IndexMetadata index) {
        Creator creator = new IndexMetadataWriter(graph, index);
        Mutators.create(table, creator);
    }

    public void writeIndexMetadata(IndexMetadata index) {
        Mutator writer = new IndexMetadataWriter(graph, index);
        Mutators.write(table, writer);
    }

    public void deleteIndexMetadata(IndexMetadata index) {
        Mutator writer = new IndexMetadataRemover(graph, index);
        Mutators.write(table, writer);
    }

    public IndexMetadata index(IndexType type, String label, String propertyKey) {
        final IndexMetadataReader parser = new IndexMetadataReader(graph);
        Get get = new Get(serialize(new IndexMetadata.Key(type, label, propertyKey)));
        try {
            Result result = table.get(get);
            return parser.parse(result);
        } catch (IOException e) {
            throw new HBaseGraphException(e);
        }
    }

    public Iterator<IndexMetadata> indices() {
        final IndexMetadataReader parser = new IndexMetadataReader(graph);
        ResultScanner scanner = null;
        try {
            scanner = table.getScanner(new Scan());
            return IteratorUtils.<Result, IndexMetadata>map(scanner.iterator(), parser::parse);
        } catch (IOException e) {
            throw new HBaseGraphException(e);
        }
    }

    public byte[] serialize(IndexMetadata.Key index) {
        PositionedByteRange buffer = new SimplePositionedMutableByteRange(4096);
        OrderedBytes.encodeString(buffer, index.label(), Order.ASCENDING);
        OrderedBytes.encodeString(buffer, index.propertyKey(), Order.ASCENDING);
        OrderedBytes.encodeInt8(buffer, index.type() == IndexType.VERTEX ? (byte) 1 : (byte) 0, Order.ASCENDING);
        buffer.setLength(buffer.getPosition());
        buffer.setPosition(0);
        byte[] bytes = new byte[buffer.getRemaining()];
        buffer.get(bytes);
        return bytes;
    }

    public IndexMetadata deserialize(Result result) {
        byte[] bytes = result.getRow();
        PositionedByteRange buffer = new SimplePositionedByteRange(bytes);
        String label = OrderedBytes.decodeString(buffer);
        String propertyKey = OrderedBytes.decodeString(buffer);
        IndexType type = OrderedBytes.decodeInt8(buffer) == 1 ? IndexType.VERTEX : IndexType.EDGE;
        Cell stateCell = result.getColumnLatestCell(Constants.DEFAULT_FAMILY_BYTES, Constants.INDEX_STATE_BYTES);
        State state = State.valueOf(Serializer.deserialize(CellUtil.cloneValue(stateCell)));
        Cell createdAtCell = result.getColumnLatestCell(Constants.DEFAULT_FAMILY_BYTES, Constants.CREATED_AT_BYTES);
        Long createdAt = Serializer.deserialize(CellUtil.cloneValue(createdAtCell));
        Cell updatedAtCell = result.getColumnLatestCell(Constants.DEFAULT_FAMILY_BYTES, Constants.UPDATED_AT_BYTES);
        Long updatedAt = Serializer.deserialize(CellUtil.cloneValue(updatedAtCell));
        return new IndexMetadata(type, label, propertyKey, state, createdAt, updatedAt);
    }
}