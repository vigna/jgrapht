/*
 * (C) Copyright 2020-2021, by Sebastiano Vigna and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */

package org.jgrapht.sux4j;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.GraphIterables;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedGraph;

import com.google.common.collect.Iterables;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntIntSortedPair;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.longs.LongBigListIterator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import it.unimi.dsi.sux4j.util.EliasFanoIndexedMonotoneLongBigList;
import it.unimi.dsi.sux4j.util.EliasFanoIndexedMonotoneLongBigList.EliasFanoIndexedMonotoneLongBigListIterator;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;

/**
 * An immutable undirected graph represented using quasi-succinct data structures.
 *
 * <p>
 * This class is the undirected counterpart of {@link SuccintIntDirectedGraph}: the same comments
 * apply.
 *
 * @author Sebastiano Vigna
 */

public class SuccinctUndirectedGraph
    extends
    AbstractGraph<Integer, IntIntSortedPair>
    implements
    Serializable
{
    private static final long serialVersionUID = 0L;
    protected static final String UNMODIFIABLE = "this graph is unmodifiable";

    /**
     * Turns all lists of adjacent nodes into a single monotone sequence by representing the edge
     * <var>x</var>&nbsp;&mdash;&nbsp;<var>y</var> as <var>x</var><var>n</var> + <var>y</var>.
     * Depending on the value of {@code sorted}, only edges with source less than or equal to the
     * target (or vice versa) are included.
     *
     * @param <E> the graph edge type
     */
    private final static class CumulativeSuccessors<E>
        implements
        LongIterator
    {
        private final Graph<Integer, E> graph;
        private final long n;
        private final int sourceShift;
        private final Function<Integer, Iterable<E>> succ;
        private final boolean sorted;

        private int x = -1, d, i, e;
        private long next = -1;
        private int[] s = IntArrays.EMPTY_ARRAY;

        public CumulativeSuccessors(
            final Graph<Integer, E> graph, final boolean sorted,
            final Function<Integer, Iterable<E>> succ)
        {
            this.n = (int) graph.iterables().vertexCount();
            this.sourceShift = Fast.ceilLog2(n);
            this.graph = graph;
            this.sorted = sorted;
            this.succ = succ;
        }

        @Override
        public boolean hasNext()
        {
            if (next != -1)
                return true;
            if (x == n)
                return false;
            while (i == d) {
                if (++x == n)
                    return false;
                int d = 0;
                for (final E e : succ.apply(x)) {
                    final int y = Graphs.getOppositeVertex(graph, e, x);
                    if (sorted) {
                        if (x <= y) {
                            s = IntArrays.grow(s, d + 1);
                            s[d++] = y;
                        }
                    } else {
                        if (x >= y) {
                            s = IntArrays.grow(s, d + 1);
                            s[d++] = y;
                        }
                    }
                }
                Arrays.sort(s, 0, d);
                this.d = d;
                i = 0;
            }
            // The predecessor list will not be indexed, so we can gain a few bits of space by
            // subtracting the edge position in the list
            next = sorted ? s[i] + ((long) x << sourceShift) : s[i] + x * n - e++;
            i++;
            return true;
        }

        @Override
        public long nextLong()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            final long result = next;
            next = -1;
            return result;
        }
    }

    /**
     * Iterates over the cumulative degrees (starts with a zero). Depending on the value of
     * {@code sorted}, only edges with source less than or equal to the target (or vice versa) are
     * included.
     *
     * @param <E> the graph edge type
     */
    private final static class CumulativeDegrees<E>
        implements
        LongIterator
    {
        private final int n;
        private int x = -1;
        private long cumul = 0;
        private final Function<Integer, Iterable<E>> succ;
        private final boolean sorted;
        private final Graph<Integer, E> graph;

        public CumulativeDegrees(
            final Graph<Integer, E> graph, final boolean sorted,
            final Function<Integer, Iterable<E>> succ)
        {
            this.n = (int) graph.iterables().vertexCount();
            this.graph = graph;
            this.succ = succ;
            this.sorted = sorted;
        }

        @Override
        public boolean hasNext()
        {
            return x < n;
        }

        @Override
        public long nextLong()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            if (x == -1)
                return ++x;
            int d = 0;
            if (sorted) {
                for (final E e : succ.apply(x))
                    if (x <= Graphs.getOppositeVertex(graph, e, x))
                        d++;
            } else {
                for (final E e : succ.apply(x))
                    if (x >= Graphs.getOppositeVertex(graph, e, x))
                        d++;
            }
            x++;
            return cumul += d;
        }
    }

    /** The number of vertices in the graph. */
    private final int n;
    /** The number of edges in the graph. */
    private final int m;
    /** The cumulative list of outdegrees (number of edges in sorted order, including loops). */
    private final EliasFanoIndexedMonotoneLongBigList cumulativeOutdegrees;
    /** The cumulative list of indegrees (number of edges in reversed order, including loops). */
    private final EliasFanoMonotoneLongBigList cumulativeIndegrees;
    /** The cumulative list of successor (edges in sorted order, including loops) lists. */
    private final EliasFanoIndexedMonotoneLongBigList successors;
    /** The cumulative list of predecessor (edges in reversed order, including loops) lists. */
    private final EliasFanoMonotoneLongBigList predecessors;
    /** The shift used to read sources. */
    private final int sourceShift;
    /** The mask used to read targets (lowest {@link #sourceShift} bits). */
    private final long targetMask;

    /**
     * Creates a new immutable succinct undirected graph from a given undirected graph.
     *
     * @param graph an undirected graph: for good results, vertices should be numbered consecutively
     *        starting from 0.
     * @param <E> the graph edge type
     */
    public <E> SuccinctUndirectedGraph(final Graph<Integer, E> graph)
    {
        if (graph.getType().isDirected())
            throw new IllegalArgumentException("This class supports directed graphs only");
        assert graph.getType().isUndirected();
        final GraphIterables<Integer, E> iterables = graph.iterables();
        if (iterables.vertexCount() > Integer.MAX_VALUE)
            throw new IllegalArgumentException(
                "The number of nodes (" + iterables.vertexCount() + ") is greater than "
                    + Integer.MAX_VALUE);
        if (iterables.edgeCount() > Integer.MAX_VALUE)
            throw new IllegalArgumentException(
                "The number of edges (" + iterables.edgeCount() + ") is greater than "
                    + Integer.MAX_VALUE);

        n = (int) iterables.vertexCount();
        m = (int) iterables.edgeCount();

        cumulativeOutdegrees = new EliasFanoIndexedMonotoneLongBigList(
            n + 1, m, new CumulativeDegrees<>(graph, true, iterables::edgesOf));
        cumulativeIndegrees = new EliasFanoMonotoneLongBigList(
            n + 1, m, new CumulativeDegrees<>(graph, false, iterables::edgesOf));
        assert cumulativeOutdegrees.getLong(cumulativeOutdegrees.size64() - 1) == m;
        assert cumulativeIndegrees.getLong(cumulativeIndegrees.size64() - 1) == m;

        sourceShift = Fast.ceilLog2(n);
        targetMask = (1L << sourceShift) - 1;

        successors = new EliasFanoIndexedMonotoneLongBigList(
            m, (long) n << sourceShift,
            new CumulativeSuccessors<>(graph, true, iterables::outgoingEdgesOf));
        predecessors = new EliasFanoIndexedMonotoneLongBigList(
            m, (long) n * n - m,
            new CumulativeSuccessors<>(graph, false, iterables::incomingEdgesOf));
    }

    /**
     * Creates a new immutable succinct undirected graph from an edge list.
     *
     * <p>
     * This constructor just builds a {@link SparseIntUndirectedGraph} and delegates to the
     * {@linkplain #SuccinctIntUndirectedGraph(Graph) main constructor}.
     *
     * @param numVertices the number of vertices.
     * @param edges the edge list.
     * @see #SuccinctIntUndirectedGraph(Graph)
     */

    public SuccinctUndirectedGraph(
        final int numVertices, final List<Pair<Integer, Integer>> edges)
    {
        this(new SparseIntUndirectedGraph(numVertices, edges));
    }

    @Override
    public Supplier<Integer> getVertexSupplier()
    {
        return null;
    }

    @Override
    public Supplier<IntIntSortedPair> getEdgeSupplier()
    {
        return null;
    }

    @Override
    public IntIntSortedPair addEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean addEdge(
        final Integer sourceVertex, final Integer targetVertex, final IntIntSortedPair e)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public Integer addVertex()
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean addVertex(final Integer v)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean containsEdge(final IntIntSortedPair e)
    {
        return successors.indexOfUnsafe(((long) e.firstInt() << sourceShift) + e.secondInt()) != -1;
    }

    @Override
    public boolean containsVertex(final Integer v)
    {
        return v >= 0 && v < n;
    }

    @Override
    public Set<IntIntSortedPair> edgeSet()
    {
        return new ObjectOpenHashSet<>(iterables().edges().iterator());
    }

    @Override
    public int degreeOf(final Integer vertex)
    {
        return (int) cumulativeIndegrees.getDelta(vertex)
            + (int) cumulativeOutdegrees.getDelta(vertex);
    }

    @Override
    public Set<IntIntSortedPair> edgesOf(final Integer vertex)
    {
        assertVertexExist(vertex);
        final long[] result = new long[2];
        cumulativeOutdegrees.get(vertex, result);
        final Set<IntIntSortedPair> s = new ObjectOpenHashSet<>();

        for (int e = (int) result[0]; e < (int) result[1]; e++) {
            final long t = successors.getLong(e);
            s.add(IntIntSortedPair.of((int) (t >>> sourceShift), (int) (t & targetMask)));
        }

        for (final IntIntSortedPair e : ITERABLES.reverseSortedEdgesOfNoLoops(vertex))
            s.add(e);

        return s;
    }

    @Override
    public int inDegreeOf(final Integer vertex)
    {
        return degreeOf(vertex);
    }

    @Override
    public Set<IntIntSortedPair> incomingEdgesOf(final Integer vertex)
    {
        return edgesOf(vertex);
    }

    @Override
    public int outDegreeOf(final Integer vertex)
    {
        return degreeOf(vertex);
    }

    @Override
    public Set<IntIntSortedPair> outgoingEdgesOf(final Integer vertex)
    {
        return edgesOf(vertex);
    }

    @Override
    public IntIntSortedPair removeEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeEdge(final IntIntSortedPair e)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeVertex(final Integer v)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public Set<Integer> vertexSet()
    {
        return IntSets.fromTo(0, n);
    }

    @Override
    public Integer getEdgeSource(final IntIntSortedPair e)
    {
        return e.firstInt();
    }

    @Override
    public Integer getEdgeTarget(final IntIntSortedPair e)
    {
        return e.secondInt();
    }

    /**
     * Returns the index associated with the given edge.
     *
     * @param e an edge of the graph.
     * @return the index associated with the edge, or &minus;1 if the edge is not part of the graph.
     * @see #getEdgeFromIndex(int)
     */
    public int getIndexFromEdge(final IntIntSortedPair e)
    {
        final int source = e.firstInt();
        final int target = e.secondInt();
        if (source < 0 || source >= n || target < 0 || target >= n)
            throw new IllegalArgumentException();
        return (int) successors.indexOfUnsafe(((long) source << sourceShift) + target);
    }

    /**
     * Returns the edge with given index.
     *
     * @param i an index between 0 (included) and the number of edges (excluded).
     * @return the pair with index {@code i}.
     * @see #getIndexFromEdge(IntIntPair)
     */
    public IntIntSortedPair getEdgeFromIndex(final int i)
    {
        if (i < 0 || i >= m)
            throw new IllegalArgumentException();
        final long t = successors.getLong(i);
        return IntIntSortedPair.of((int) (t >>> sourceShift), (int) (t & targetMask));
    }

    @Override
    public GraphType getType()
    {
        return new DefaultGraphType.Builder()
            .directed().weighted(false).modifiable(false).allowMultipleEdges(false)
            .allowSelfLoops(true).build();
    }

    @Override
    public double getEdgeWeight(final IntIntSortedPair e)
    {
        return 1.0;
    }

    @Override
    public void setEdgeWeight(final IntIntSortedPair e, final double weight)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public IntIntSortedPair getEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        int x = sourceVertex;
        int y = targetVertex;
        if (x > y) {
            final int t = x;
            x = y;
            y = t;
        }
        final long index = successors.indexOfUnsafe(((long) x << sourceShift) + y);
        return index != -1 ? IntIntSortedPair.of(x, y) : null;
    }

    @Override
    public boolean containsEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        int x = sourceVertex;
        int y = targetVertex;
        if (x > y) {
            final int t = x;
            x = y;
            y = t;
        }
        return successors.indexOfUnsafe(((long) x << sourceShift) + y) != -1;
    }

    @Override
    public Set<IntIntSortedPair> getAllEdges(final Integer sourceVertex, final Integer targetVertex)
    {
        final IntIntSortedPair edge = getEdge(sourceVertex, targetVertex);
        return edge == null ? ObjectSets.emptySet() : ObjectSets.singleton(edge);
    }

    /**
     * Ensures that the specified vertex exists in this graph, or else throws exception.
     *
     * @param v vertex
     * @return <code>true</code> if this assertion holds.
     * @throws IllegalArgumentException if specified vertex does not exist in this graph.
     */
    @Override
    protected boolean assertVertexExist(final Integer v)
    {
        if (v < 0 || v >= n)
            throw new IllegalArgumentException();
        return true;
    }

    /**
     * Ensures that the specified edge exists in this graph, or else throws exception.
     *
     * @param e edge
     * @return <code>true</code> if this assertion holds.
     * @throws IllegalArgumentException if specified edge does not exist in this graph.
     */
    protected boolean assertEdgeExist(final Integer e)
    {
        if (e < 0 || e >= m)
            throw new IllegalArgumentException();
        return true;

    }

    private final static class SuccinctGraphIterables
        implements
        GraphIterables<Integer, IntIntSortedPair>,
        Serializable
    {
        private static final long serialVersionUID = 0L;
        private final SuccinctUndirectedGraph graph;

        private SuccinctGraphIterables()
        {
            graph = null;
        }

        private SuccinctGraphIterables(final SuccinctUndirectedGraph graph)
        {
            this.graph = graph;
        }

        @Override
        public Graph<Integer, IntIntSortedPair> getGraph()
        {
            return graph;
        }

        @Override
        public long vertexCount()
        {
            return graph.n;
        }

        @Override
        public long edgeCount()
        {
            return graph.m;
        }

        @Override
        public Iterable<IntIntSortedPair> edges()
        {
            final int sourceShift = graph.sourceShift;
            final long targetMask = graph.targetMask;

            return () -> new Iterator<>()
            {
                private final EliasFanoIndexedMonotoneLongBigListIterator iterator =
                    graph.successors.iterator();
                private final int n = graph.n;

                @Override
                public boolean hasNext()
                {
                    return iterator.hasNext();
                }

                @Override
                public IntIntSortedPair next()
                {
                    final long t = iterator.nextLong();
                    return IntIntSortedPair.of((int) (t >>> sourceShift), (int) (t & targetMask));
                }

            };
        }

        @Override
        public Iterable<IntIntSortedPair> edgesOf(final Integer source)
        {
            return Iterables.concat(sortedEdges(source), reverseSortedEdgesOfNoLoops(source));
        }

        private Iterable<IntIntSortedPair> sortedEdges(final int source)
        {
            final int sourceShift = graph.sourceShift;
            final long targetMask = graph.targetMask;
            final long[] result = new long[2];
            graph.cumulativeOutdegrees.get(source, result);
            final var successors = graph.successors;
            final int n = graph.n;

            return () -> new Iterator<>()
            {
                private int e = (int) result[0];

                @Override
                public boolean hasNext()
                {
                    return e < result[1];
                }

                @Override
                public IntIntSortedPair next()
                {
                    final long t = successors.getLong(e++);
                    return IntIntSortedPair.of((int) (t >>> sourceShift), (int) (t & targetMask));
                }
            };
        }

        private Iterable<IntIntSortedPair> reverseSortedEdgesOfNoLoops(final int target)
        {
            final long[] result = new long[2];
            graph.cumulativeIndegrees.get(target, result);
            final int d = (int) (result[1] - result[0]);
            final LongBigListIterator iterator = graph.predecessors.listIterator(result[0]);

            return () -> new Iterator<>()
            {
                int i = d;
                IntIntSortedPair edge = null;
                long n = graph.n;
                long base = n * target - result[0];

                @Override
                public boolean hasNext()
                {
                    if (edge == null && i > 0) {
                        i--;
                        final long source = iterator.nextLong() - base--;
                        if (source == target && i-- == 0)
                            return false;
                        edge = IntIntSortedPair.of((int) source, target);
                    }
                    return edge != null;
                }

                @Override
                public IntIntSortedPair next()
                {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    final IntIntSortedPair result = edge;
                    edge = null;
                    return result;
                }
            };
        }

        @Override
        public Iterable<IntIntSortedPair> incomingEdgesOf(final Integer vertex)
        {
            return edgesOf(vertex);
        }

        @Override
        public Iterable<IntIntSortedPair> outgoingEdgesOf(final Integer vertex)
        {
            return edgesOf(vertex);
        }
    }

    private final SuccinctGraphIterables ITERABLES = new SuccinctGraphIterables(this);

    @Override
    public GraphIterables<Integer, IntIntSortedPair> iterables()
    {
        return ITERABLES;
    }
}
