package org.jgrapht.sux4j;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultGraphType;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.LongIterator;

public abstract class AbstractSuccinctUndirectedGraph<E>
    extends
    AbstractSuccinctGraph<E>
{

    public AbstractSuccinctUndirectedGraph(final int n, final int m)
    {
        super(n, m);
    }

    /**
     * Turns all lists of successors into a single monotone sequence by representing an arc
     * <var>x</var>&nbsp;&rarr;&nbsp;<var>y</var> as
     * <var>x</var>2<sup>&lceil;log&nbsp;<var>n</var>&rceil;</sup> + <var>y</var>, and all lists of
     * predecessors into a single monotone sequence by representing an arc
     * <var>x</var>&nbsp;&rarr;&nbsp;<var>y</var> as <var>x</var><var>n</var> + <var>y</var> -
     * <var>e</var>, where <var>e</var> is the index of the arc in lexicographical order.
     *
     * @param <E> the graph edge type
     */

    protected final static class CumulativeSuccessors<E>
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
    protected final static class CumulativeDegrees<E>
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

    @Override
    public GraphType getType()
    {
        return new DefaultGraphType.Builder()
            .directed().weighted(false).modifiable(false).allowMultipleEdges(false)
            .allowSelfLoops(true).build();
    }

}
