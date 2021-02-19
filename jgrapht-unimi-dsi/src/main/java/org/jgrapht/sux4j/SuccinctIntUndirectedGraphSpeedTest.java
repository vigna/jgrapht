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

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.jgrapht.GraphIterables;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.opt.graph.sparse.SparseIntUndirectedGraph;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.LazyIntIterators;
import it.unimi.dsi.webgraph.NodeIterator;

public class SuccinctIntUndirectedGraphSpeedTest {

	private static final class SortedPairList extends AbstractObjectList<Pair<Integer, Integer>> {
		private final ImmutableGraph graph;
		private final int m;
		private final int n;

		private SortedPairList(final ImmutableGraph graph, final int m) {
			this.graph = graph;
			this.n = graph.numNodes();
			this.m = m;
		}

		@Override
		public Pair<Integer, Integer> get(final int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectListIterator<Pair<Integer, Integer>> iterator() {
			return new ObjectListIterator<>() {
				private int c = 0;
				private int x = -1;
				LazyIntIterator i = LazyIntIterators.EMPTY_ITERATOR;

				@Override
				public boolean hasNext() {
					return c < m;
				}

				@Override
				public boolean hasPrevious() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Pair<Integer, Integer> next() {
					if (!hasNext()) throw new NoSuchElementException();
					for (;;) {
						final int s = i.nextInt();
						if (s != -1 && s <= x) {
							c++;
							return Pair.of(x, s);
						}
						if (x < n - 1) i = graph.successors(++x);
					}
				}

				@Override
				public int nextIndex() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Pair<Integer, Integer> previous() {
					throw new UnsupportedOperationException();
				}

				@Override
				public int previousIndex() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public int size() {
			return m;
		}
	}

	public static void main(final String args[]) throws IOException, ClassNotFoundException {
		final ImmutableGraph graph = ImmutableGraph.load(args[0]);
		final String basename = new File(args[0]).getName();

		final int n = graph.numNodes();
		int t = 0;
		for (final NodeIterator iterator = graph.nodeIterator(); iterator.hasNext();) {
			final int x = iterator.nextInt();
			final LazyIntIterator successors = iterator.successors();
			for (int s; (s = successors.nextInt()) != -1;) if (s <= x) t++;
		}

		final int m = t;

		SparseIntUndirectedGraph sparse;
		final File sparseFile = new File(basename + ".sparse");

		if (sparseFile.exists()) sparse = (SparseIntUndirectedGraph)BinIO.loadObject(sparseFile);
		else {
			final AbstractObjectList<Pair<Integer, Integer>> edges = new SortedPairList(graph, m);
			sparse = new SparseIntUndirectedGraph(graph.numNodes(), edges);

			BinIO.storeObject(sparse, sparseFile);
		}

		SuccinctIntUndirectedGraph succinct;
		final File succinctFile = new File(basename + ".succinct");

		if (succinctFile.exists()) succinct = (SuccinctIntUndirectedGraph)BinIO.loadObject(succinctFile);
		else {
			succinct = new SuccinctIntUndirectedGraph(sparse);
			BinIO.storeObject(succinct, succinctFile);
		}

		int u = 0;
		final ProgressLogger pl = new ProgressLogger();
		XoRoShiRo128PlusRandom r;
		final GraphIterables<Integer, Integer> sparseIterables = sparse.iterables();
		final GraphIterables<Integer, Integer> succinctIterables = succinct.iterables();

		for (int x = 0; x < n; x++) {
			final IntOpenHashSet sparseSucc = new IntOpenHashSet();
			for (final var e : sparse.outgoingEdgesOf(x)) {
				if (sparse.getAllEdges(sparse.getEdgeSource(e), sparse.getEdgeTarget(e)).isEmpty()) throw new AssertionError("Inconsistent information for edge " + e + " (" + sparse.getEdgeSource(e) + " -> " + sparse.getEdgeTarget(e) + ")");
				sparseSucc.add(Graphs.getOppositeVertex(sparse, e, x));
			}
			final IntOpenHashSet succinctSucc = new IntOpenHashSet();
			for (final var e : succinct.outgoingEdgesOf(x)) succinctSucc.add(Graphs.getOppositeVertex(succinct, e, x));
			if (!sparseSucc.equals(succinctSucc)) throw new AssertionError("Inconsistent information for node " + x);
		}

		for (int k = 10; k-- != 0;) {
			pl.start("Enumerating edges on sparse representation...");
			for (final Integer e : sparseIterables.edges()) {
				u += sparse.getEdgeSource(e);
				u += sparse.getEdgeTarget(e);
			}
			pl.done(m);

			pl.start("Enumerating edges on succinct representation...");
			for (final Integer e : succinctIterables.edges()) {
				u += succinct.getEdgeSource(e);
				u += succinct.getEdgeTarget(e);
			}
			pl.done(m);

			pl.start("Sampling successors on sparse representation...");
			r = new XoRoShiRo128PlusRandom(0);
			for (int i = 1000000; i-- != 0;) {
				for (final Integer e : sparseIterables.outgoingEdgesOf(r.nextInt(n))) {
					u += sparse.getEdgeSource(e);
					u += sparse.getEdgeTarget(e);
				}
			}
			pl.done(m);

			pl.start("Sampling successors on succinct representation...");
			r = new XoRoShiRo128PlusRandom(0);
			for (int i = 1000000; i-- != 0;) {
				for (final Integer e : succinctIterables.outgoingEdgesOf(r.nextInt(n))) {
					u += succinct.getEdgeSource(e);
					u += succinct.getEdgeTarget(e);
				}
			}
			pl.done(m);

			pl.start("Sampling adjacency on sparse representation...");
			r = new XoRoShiRo128PlusRandom(0);
			for (int i = 1000000; i-- != 0;) {
				for (final Integer e : sparse.getAllEdges(r.nextInt(n), r.nextInt(n))) {
					u += sparse.getEdgeSource(e);
					u += sparse.getEdgeTarget(e);
				}
			}
			pl.done(m);

			pl.start("Sampling adjacency on succinct representation...");
			r = new XoRoShiRo128PlusRandom(0);
			for (int i = 1000000; i-- != 0;) {
				for (final Integer e : succinct.getAllEdges(r.nextInt(n), r.nextInt(n))) {
					u += succinct.getEdgeSource(e);
					u += succinct.getEdgeTarget(e);
				}
			}
			pl.done(m);
		}

		if (u == 0) System.out.println();
	}
}
