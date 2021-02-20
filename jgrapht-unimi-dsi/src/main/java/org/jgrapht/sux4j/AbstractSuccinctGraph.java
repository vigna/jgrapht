package org.jgrapht.sux4j;

import java.util.function.Supplier;

import org.jgrapht.graph.AbstractGraph;

import it.unimi.dsi.bits.Fast;

public abstract class AbstractSuccinctGraph<E>
    extends
    AbstractGraph<Integer, E>
{
    protected static final String UNMODIFIABLE = "this graph is unmodifiable";

    /** The number of vertices in the graph. */
    protected final int n;
    /** The number of edges in the graph. */
    protected final int m;
    /** The shift used to read sources in the successor list. */
    protected final int sourceShift;
    /** The mask used to read targets in the successor list (lowest {@link #sourceShift} bits). */
    protected final long targetMask;

    public AbstractSuccinctGraph(final int n, final int m)
    {
        super();
        this.n = n;
        this.m = m;
        sourceShift = Fast.ceilLog2(n);
        targetMask = (1L << sourceShift) - 1;
    }

    @Override
    public Supplier<Integer> getVertexSupplier()
    {
        return null;
    }

    @Override
    public Supplier<E> getEdgeSupplier()
    {
        return null;
    }

    @Override
    public E addEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean addEdge(final Integer sourceVertex, final Integer targetVertex, final E e)
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
    public E removeEdge(final Integer sourceVertex, final Integer targetVertex)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeEdge(final E e)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeVertex(final Integer v)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public double getEdgeWeight(final E e)
    {
        return 1.0;
    }

    @Override
    public void setEdgeWeight(final E e, final double weight)
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

}
