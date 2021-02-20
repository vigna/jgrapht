/**
 * Immutable graphs stored using <a href="http://sux4j.di.unimi.it/">Sux4J</a>'s quasi-succinct data
 * structures.
 *
 * <p>
 * This package contains implementations of immutable graphs based on the
 * {@linkplain it.unimi.dsi.sux4j.util.EliasFanoIndexedMonotoneLongBigList Elias&ndash;Fano
 * quasi-succinct representation of monotone sequences}. The entries of the adjacency matrix of a
 * graph with <var>n</var> vertices are represented as a monotone sequence of natural numbers, where
 * an arc <var>x</var>&nbsp;&rarr;&nbsp;<var>y</var> is represented by
 * <var>x</var><var>n</var>&nbsp;+&nbsp;<var>y</var>.
 *
 * <p>
 * The memory footprint of these implementation is close to the information-theoretical lower bound;
 * the actual space used can be easily measured as all implementations are serializable, and their
 * in-memory footprint is very closed to the on-disk footprint. Usually the size is a few times
 * smaller than that of a {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedGraph
 * SparseIntDirectedGraph}/ {@link org.jgrapht.opt.graph.sparse.SparseIntUndirectedGraph
 * SparseIntUndirectedGraph}.
 *
 * <p>
 * We provide two classes mimicking {@link org.jgrapht.opt.graph.sparse.SparseIntDirectedGraph
 * SparseIntDirectedGraph} and {@link org.jgrapht.opt.graph.sparse.SparseIntUndirectedGraph
 * SparseIntUndirectedGraph}, in the sense that both vertices and edges are integers (and they are
 * numbered contiguously).
 *
 * <ul>
 * <li>{@link org.jgrapht.sux4j.SuccinctIntDirectedGraph} is an implementation for directed graphs.
 * {@linkplain org.jgrapht.GraphIterables#outgoingEdgesOf(Object) Enumeration of outgoing edges} is
 * quite fast, but {@linkplain org.jgrapht.GraphIterables#incomingEdgesOf(Object) enumeration of
 * incoming edges} is very slow. {@link org.jgrapht.Graph#containsEdge(Object) Adjacency tests} are
 * very fast and happen in almost constant time.
 * <li>{@link org.jgrapht.sux4j.SuccinctIntUndirectedGraph} is an implementation for undirected
 * graphs. {@linkplain org.jgrapht.GraphIterables#edgesOf(Object) Enumeration of edges} is quite
 * slow. {@link org.jgrapht.Graph#containsEdge(Object) Adjacency tests} are very fast and happen in
 * almost constant time.
 * </ul>
 *
 * <p>
 * The sometimes slow behavior of the previous classes is due to a clash between JGraphT's design
 * and the need of representing an edge with an {@link java.lang.Integer Integer}: there is no
 * information that can be carried by the object representing the edge. This limitation forces the
 * two classes above to compute two expensive functions that are one the inverse of the other.
 *
 * <p>
 * As an alternative, we provide classes {@link org.jgrapht.sux4j.SuccinctDirectedGraph
 * SuccinctDirectedGraph} and {@link org.jgrapht.sux4j.SuccinctUndirectedGraph
 * SuccinctUndirectedGraph} using the same amount of space, but having edges represented by pairs of
 * integers stored in an {@link it.unimi.dsi.fastutil.ints.IntIntPair IntIntPair} (for directed
 * graphs) or an {@link it.unimi.dsi.fastutil.ints.IntIntSortedPair IntIntSortedPair} (for
 * undirected graphs). Storing the edges explicitly avoids the cumbersome back-and-forth
 * computations of the previous classes. All accessors are extremely fast.
 *
 * <p>
 * Both classes provide methods {@link org.jgrapht.sux4j.SuccinctDirectedGraph#getEdgeFromIndex(int)
 * getEdgeFromIndex()} and
 * {@link org.jgrapht.sux4j.SuccinctDirectedGraph#getIndexFromEdge(it.unimi.dsi.fastutil.ints.IntIntPair)
 * getIndexFromEdge()} that map bijectively the edge set into a contiguous set of integers. In this
 * way the user can choose when and how to use the feature (e.g., to store compactly data associated
 * to edges).
 *
 * <p>
 * Finally, note that the best performance and compression can be obtained by representing the graph
 * using <a href="http://webgraph.di.unimi.it/">WebGraph</a>'s {@link it.unimi.dsi.webgraph.EFGraph
 * EFGraph} format and then accessing the graph using the suitable {@linkplain org.jgrapht.webgraph
 * adapter}; in particular, one can represent graphs with more than {@link Integer#MAX_VALUE}
 * vertices. However, the adapters to not provide methods mapping bijectively edges into a
 * contiguous set of integers.
 */
package org.jgrapht.sux4j;
