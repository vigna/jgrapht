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

public abstract class AbstractSuccinctDirectedGraph<E>
    extends
    AbstractSuccinctGraph<E>
{

    public AbstractSuccinctDirectedGraph(final int n, final int m)
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
        private final boolean strict;

        private int x = -1, d, i, e;
        private long next = -1;
        private int[] s = IntArrays.EMPTY_ARRAY;

        public CumulativeSuccessors(
            final Graph<Integer, E> graph, final Function<Integer, Iterable<E>> succ,
            final boolean strict)
        {
            this.n = graph.iterables().vertexCount();
            this.sourceShift = Fast.ceilLog2(n);
            this.graph = graph;
            this.succ = succ;
            this.strict = strict;
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
                    s = IntArrays.grow(s, d + 1);
                    s[d++] = Graphs.getOppositeVertex(graph, e, x);
                }
                Arrays.sort(s, 0, d);
                this.d = d;
                i = 0;
            }
            // The predecessor list will not be indexed, so we can gain a few bits of space by
            // subtracting the edge position in the list
            next = strict ? s[i] + ((long) x << sourceShift) : s[i] + x * n - e++;
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
     * Iterates over the cumulative degrees (starts with a zero).
     */
    protected final static class CumulativeDegrees
        implements
        LongIterator
    {
        private final Function<Integer, Integer> degreeOf;
        private final int n;
        private int i = -1;
        private long cumul = 0;

        public CumulativeDegrees(final int n, final Function<Integer, Integer> degreeOf)
        {
            this.n = n;
            this.degreeOf = degreeOf;
        }

        @Override
        public boolean hasNext()
        {
            return i < n;
        }

        @Override
        public long nextLong()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            if (i == -1)
                return ++i;
            return cumul += degreeOf.apply(i++);
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
