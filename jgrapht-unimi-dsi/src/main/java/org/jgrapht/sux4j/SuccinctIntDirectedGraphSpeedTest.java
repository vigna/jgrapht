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

import java.io.IOException;
import java.util.NoSuchElementException;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.opt.graph.sparse.SparseIntDirectedGraph;

import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.LazyIntIterators;

public class SuccinctIntDirectedGraphSpeedTest {

	public static void main(final String args[]) throws IOException {
		final ImmutableGraph graph = ImmutableGraph.load(args[0]);
		final int m = (int)graph.numArcs();
		final var sparse = new SparseIntDirectedGraph(graph.numNodes(), new AbstractObjectList<Pair<Integer, Integer>>() {

			@Override
			public Pair<Integer, Integer> get(final int index) {
				throw new UnsupportedOperationException();
			}

			@Override
			public ObjectListIterator<Pair<Integer, Integer>> iterator() {
				return new ObjectListIterator<>() {
					private int c = 0;
					private int x = 0;
					LazyIntIterator i = LazyIntIterators.EMPTY_ITERATOR;
					@Override
					public boolean hasNext() {
						return c < m;
					}

					@Override
					public Pair<Integer, Integer> next() {
						if (! hasNext()) throw new NoSuchElementException();
						for(;;) {
							final int s = i.nextInt();
							if (s != -1) {
								c++;
								return Pair.of(x, s);
							}
							i = graph.successors(x++);
						}
					}
				};
			}

			@Override
			public int size() {
				return m;
			}
		});

		int u = 0;
		final ProgressLogger pl = new ProgressLogger();

		pl.start("Enumerating edges on sparse representation...");
		for (final Integer e : sparse.iterables().edges()) {
			u += sparse.getEdgeSource(e);
			u += sparse.getEdgeTarget(e);
		}
		pl.done(m);

		final var succinct = new SuccinctIntDirectedGraph(sparse);

		pl.start("Enumerating edges on succinct representation...");

		for (final Integer e : succinct.iterables().edges()) {
			u += succinct.getEdgeSource(e);
			u += succinct.getEdgeTarget(e);
		}
		pl.done(m);
	}
}
