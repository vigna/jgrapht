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

import org.jgrapht.GraphIterables;
import org.jgrapht.webgraph.ImmutableDirectedGraphAdapter;
import org.jgrapht.webgraph.ImmutableGraphAdapter;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import it.unimi.dsi.webgraph.ImmutableGraph;

public class ImmutableGraphSpeedTest {

	public static void main(final String args[]) throws IOException, ClassNotFoundException {
        final ImmutableGraphAdapter<IntIntPair> graph = new ImmutableDirectedGraphAdapter(
            ImmutableGraph.load(args[0]), ImmutableGraph.load(args[1]));

        final int n = (int) graph.iterables().vertexCount();
        final long m = graph.iterables().edgeCount();

		int u = 0;
		final ProgressLogger pl = new ProgressLogger();
		XoRoShiRo128PlusRandom r;
        final GraphIterables<Integer, IntIntPair> succinctIterables = graph.iterables();

		// Source and target for adjacency tests
		final int source[] = new int[1000000];
		final int target[] = new int[1000000];

		// Sample about 500000 edges
		r = new XoRoShiRo128PlusRandom(0);
		final double prob = 500000. / succinctIterables.edgeCount();
		int p = 0, c;

        for (final IntIntPair e : succinctIterables.edges()) {
			if (r.nextDouble() <= prob) {
                source[p] = graph.getEdgeSource(e);
                target[p] = graph.getEdgeTarget(e);
				p++;
			}
		}

		for (; p < 1000000; p++) {
			source[p] = r.nextInt(n);
			target[p] = r.nextInt(n);
		}

		IntArrays.shuffle(source, new XoRoShiRo128PlusRandom(0));
		IntArrays.shuffle(target, new XoRoShiRo128PlusRandom(0));

		for (int k = 10; k-- != 0;) {
            pl.start("Enumerating edges...");

            for (final IntIntPair e : succinctIterables.edges()) {
                u += graph.getEdgeSource(e);
                u += graph.getEdgeTarget(e);
			}
			pl.done(m);

            pl.start("Sampling successors...");
			r = new XoRoShiRo128PlusRandom(0);
			c = 0;
			for (int i = 1000000; i-- != 0;) {
                for (final IntIntPair e : succinctIterables.outgoingEdgesOf(r.nextInt(n))) {
                    u += graph.getEdgeSource(e);
                    u += graph.getEdgeTarget(e);
					c++;
				}
			}
			pl.done(c);

            pl.start("Sampling predecessors...");
			r = new XoRoShiRo128PlusRandom(0);
            c = 0;
			for (int i = 1000000; i-- != 0;) {
                for (final IntIntPair e : succinctIterables.incomingEdgesOf(r.nextInt(n))) {
                    u += graph.getEdgeSource(e);
                    u += graph.getEdgeTarget(e);
                    c++;
				}
			}
            pl.done(c);

            pl.start("Sampling adjacency...");
            for (int i = 1000000; i-- != 0;) {
                u += graph.containsEdge(source[i], target[i]) ? 0 : 1;
            }
            pl.done(1000000);
		}

		if (u == 0) System.out.println();
	}
}
