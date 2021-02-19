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

import org.jgrapht.alg.util.Pair;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedGraph;

import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.LazyIntIterators;

public class SparseGraphTest {

	public static final class PairList extends AbstractObjectList<Pair<Integer, Integer>> {
		private final ImmutableGraph graph;
		private final int n;
		private final int m;

		public PairList(final ImmutableGraph graph) {
			this.graph = graph;
			this.m = (int)graph.numArcs();
			this.n = graph.numNodes();
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
						if (s != -1) {
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
		final int m = (int)graph.numArcs();

		final ArrayListMutableGraph mg = new ArrayListMutableGraph(n);

		// Check that PairList is doing is job correctly
		final AbstractObjectList<Pair<Integer, Integer>> pairList = new PairList(graph);
		for (final var p : pairList) {
			mg.addArc(p.getFirst(), p.getSecond());
		}

		if (!graph.equals(mg.immutableView())) throw new AssertionError();

		SparseIntDirectedGraph sparse;

		sparse = new SparseIntDirectedGraph(graph.numNodes(), new PairList(graph));

		for (int x = 0; x < n; x++) {
			if (graph.outdegree(x) != sparse.outDegreeOf(x)) System.out.println(x);
		}
	}
}
