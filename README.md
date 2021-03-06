# HGraphDB - HBase as a TinkerPop Graph Database

HGraphDB is a client layer for using HBase as a graph database.  It is an implementation of the [Apache TinkerPop 3](http://tinkerpop.apache.org) interfaces.

## Installing

Releases of HGraphDB are deployed to Maven Central.

		<dependency>
		    <groupId>io.hgraphdb</groupId>
		    <artifactId>hgraphdb</artifactId>
		    <version>0.3.2</version>
		</dependency>

## Setup

To initialize HGraphDB, create an `HBaseGraphConfiguration` instance, and then use a static factory method to create an `HBaseGraph` instance.

		...
		Configuration cfg = new HBaseGraphConfiguration()
   			.setInstanceType(InstanceType.Distributed)
  			.setGraphNamespace("mygraph")
  			.setCreateTables(true)
  			.set("hbase.zookeeper.quorum", "127.0.0.1")
  			.set("zookeeper.znode.parent", "/hbase-unsecure");
		HBaseGraph graph = (HBaseGraph) GraphFactory.open(cfg);
		...

As you can see above, HBase-specific configuration parameters can be passed directly.  These will be used when obtaining an HBase connection. 

The resulting graph can be used like any other TinkerPop graph instance.

		...
		Vertex v1 = graph.addVertex(T.id, 1, T.label, "person", "name", "John");
		Vertex v2 = graph.addVertex(T.id, 2, T.label, "person", "name", "Sally");
		v1.addEdge("knows", v2, T.id, "edge1", "since", LocalDate.now());
		...
		
A few things to note from the above example :

- HGraphDB accepts user-supplied IDs, for both vertices and edges.
- The following types can be used for both IDs and property values:
	- boolean
	- String
	- numbers (byte, short, int long, float, double)
	- java.math.BigDecimal
	- java.time.LocalDate
	- java.time.LocalTime
	- java.time.LocalDateTime
	- java.time.Duration
	- java.util.UUID
	- byte arrays
	- Enum instances
	- [Kryo](https://github.com/EsotericSoftware/kryo)-serializable instances
	- Java-serializable instances

## Creating Indices

Two types of indices are supported by HGraphDB:

- Vertices can be indexed by label and property.
- Edges can be indexed by label and property, specific to a vertex.

An index is created as follows:

		graph.createIndex(ElementType.VERTEX, "person", "name");
		...
		graph.createIndex(ElementType.EDGE, "knows", "since");

Indices should be created before the relevant data is populated.  A future [enhancement](#future) will allow for index creation after data population.

Once an index is created and data has been populated, it can be used as follows:

		// get persons named John
		Iterator<Vertex> it = graph.allVertices("person", "name", "John");
		...
		// get persons first known by John between 2007-01-01 (inclusive) and 2008-01-01 (exclusive)
		Iterator<Edge> it = johnV.edges(Direction.OUT, "knows", "since", 
			LocalDate.parse("2007-01-01"), LocalDate.parse("2008-01-01"));
		
Note that the indices support range queries, where the start of the range is inclusive and the end of the range is exclusive.

An index can also be specified as a unique index.  For a vertex index, this means only one vertex can have a particular property name-value for the given vertex label.  For an edge index, this means only one edge of a specific vertex can have a particular property name-value for a given edge label.

		graph.createIndex(ElementType.VERTEX, "person", "name", /* unique */ true);

## Schema Management

By default HGraphDB does not use a schema.  Schema management can be enabled by calling `HBaseGraphConfiguration.useSchema(true)`.  Once schema management is enabled, the schema for vertex and edge labels can be defined.

		graph.createLabel(ElementType.VERTEX, "author", /* id */ ValueType.STRING, "age", ValueType.INT);
		graph.createLabel(ElementType.VERTEX, "book", /* id */ ValueType.STRING, "publisher", ValueType.STRING);
		graph.createLabel(ElementType.EDGE, "writes", /* id */ ValueType.STRING, "since", ValueType.DATE);   
	
Edge labels must be explicitly connected to vertex labels before edges are added to the graph.

		graph.connectLabels("author", "writes", "book"); 

Additional properties can be added to labels at a later time; otherwise labels cannot be changed.

		graph.updateLabel("author", "height", ValueType.DOUBLE);
		
Whenever vertices or edges are added to the graph, they will first be validated against the schema.    

## Using the Gremlin Console

One benefit of having a TinkerPop layer to HBase is that a number of graph-related tools become available, which are all part of the TinkerPop ecosystem.  These tools include the Gremlin DSL and the Gremlin console.  To use HGraphDB in the Gremlin console, run the following commands:

                 \,,,/
                 (o o)
        -----oOOo-(3)-oOOo-----
        plugin activated: tinkerpop.server
        plugin activated: tinkerpop.utilities
        plugin activated: tinkerpop.tinkergraph
        gremlin> :install org.apache.hbase hbase-client 1.2.0
        gremlin> :install org.apache.hbase hbase-common 1.2.0
        gremlin> :install org.apache.hadoop hadoop-common 2.5.1
        gremlin> :install io.hgraphdb hgraphdb 0.3.2
        gremlin> :plugin use io.hgraphdb
                
Then restart the Gremlin console and run the following:

		gremlin> g = HBaseGraph.open("mygraph", "127.0.0.1", "/hbase-unsecure")


## Performance Tuning

### Caching

HGraphDB provides two kinds of caches, global caches and relationship caches.  Global caches contain both vertices and edges. Relationship caches are specific to a vertex and cache the edges that are incident to the vertex.  Both caches can be controlled through `HBaseGraphConfiguration` by specifying a maximum size for each type of cache as well as a TTL for elements after they have been accessed via the cache.  Specifying a maximum size of 0 will disable caching.

### Lazy Loading

By default, vertices and edges are eagerly loaded.  In some failure conditions, it may be possible for indices to point to vertices or edges which have been deleted.  By eagerly loading graph elements, stale data can be filtered out and removed before it reaches the client.  However, this incurs a slight performance penalty.  As an alternative, lazy loading can be enabled.  This can be done by calling `HBaseGraphConfiguration.setLazyLoading(true)`.  However, if there are stale indices in the graph, the client will need to handle the exception that is thrown when an attempt is made to access a non-existent vertex or edge.

### Bulk Loading

HGraphDB also provides an `HBaseBulkLoader` class for more performant loading of vertices and edges.  The bulk loader will not attempt to check if elements with the same ID already exist when adding new elements.

## Implementation Notes

HGraphDB uses a tall table schema.  The schema is created in the namespace specified to the `HBaseGraphConfiguration`.  The tables look as follows:

### Vertex Table

| Row Key | Column: label | Column: createdAt | Column: [property1 key] | Column: [property2 key] | ... |
|---|---|---|---|---|---|
| [vertex ID] | [label value] | [createdAt value] | [property1 value] | [property2 value] |...|

### Edge Table

| Row Key | Column: label | Column: fromVertex | Column: toVertex | Column: createdAt | Column: [property1 key] | Column: [property2 key] | ... |
|---|---|---|---|---|---|---|---|
| [edge ID] | [label value] | [fromVertex ID ] | [toVertex ID] | [createdAt value] | [property1 value] | [property2 value] | ... |

### Vertex Index Table

| Row Key | Column: createdAt | Column: vertexID |
|---|---|---|
| [vertex label, isUnique, property key, property value, vertex ID (if not unique)] | [createdAt value] | [vertex ID (if unique)] |
	
### Edge Index Table

| Row Key | Column: createdAt | Column: vertexID | Column: edgeID |
|---|---|---|---|
| [vertex1 ID, direction, isUnique, property key, edge label, property value, vertex2 ID (if not unique), edge ID (if not unique)] | [createdAt value] | [vertex2 ID (if unique)] | [edge ID (if unique)] |

### Index Metadata Table

| Row Key | Column: createdAt | Column: isUnique | Column: state |
|---|---|---|---|
| [label, property key, element type] | [createdAt value] | [isUnique value] | [state value] |

Note that in the index tables, if the index is a unique index, then the indexed IDs are stored in the column values; otherwise they are stored in the row key.  

If schema management is enabled, two additional tables are used:

### Label Metadata Table

| Row Key | Column: id | Column: createdAt | Column: [property1 key] | Column: [property2 key] | ... |
|---|---|---|---|---|---|
| [label, element type] | [id type] | [createdAt value] | [property1 type] | [property2 type] |...|

### Label Connections Table

| Row Key | Column: createdAt 
|---|---|---|---|---|---|
| [from vertex label, edge label, to vertex label] | [createdAt value] |

HGraphDB was designed to support the features mentioned [here](https://rayokota.wordpress.com/2016/11/10/hgraphdb-hbase-as-a-tinkerpop-graph-database/).

## <a name="future"></a>Future Enhancements 

Possible future enhancements include map-reduce jobs for the following:

- Populating indices after graph elements have already been added.
- Cleaning up stale indices.
- Deleting indices.


