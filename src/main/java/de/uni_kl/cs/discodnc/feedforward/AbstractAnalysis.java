/*
 * This file is part of the Disco Deterministic Network Calculator.
 *
 * Copyright (C) 2008 - 2010 Andreas Kiefer
 * Copyright (C) 2011 - 2018 Steffen Bondorf
 * Copyright (C) 2017+ The DiscoDNC contributors
 *
 * Distributed Computer Systems (DISCO) Lab
 * University of Kaiserslautern, Germany
 *
 * http://discodnc.cs.uni-kl.de
 *
 *
 * The Disco Deterministic Network Calculator (DiscoDNC) is free software;
 * you can redistribute it and/or modify it under the terms of the 
 * GNU Lesser General Public License as published by the Free Software Foundation; 
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package de.uni_kl.cs.discodnc.feedforward;

import de.uni_kl.cs.discodnc.curves.ArrivalCurve;
import de.uni_kl.cs.discodnc.numbers.Num;
import de.uni_kl.cs.discodnc.server_graph.ServerGraph;
import de.uni_kl.cs.discodnc.server_graph.Server;

import java.util.Map;
import java.util.Set;

public abstract class AbstractAnalysis {
    protected ServerGraph server_graph;
    protected AnalysisConfig configuration;
    protected AnalysisResults result;

    public ServerGraph getNetwork() {
        return server_graph;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    // ----------------------------------------------------------------------------------------------------
    // Convenience functions to access the results object.
    // ----------------------------------------------------------------------------------------------------

    /**
     * Returns the delay bound of the analysis.
     *
     * @return the delay bound
     */
    public Num getDelayBound() {
        return result.getDelayBound();
    }

    /**
     * Returns the backlog bound of the analysis.
     *
     * @return the backlog bound
     */
    public Num getBacklogBound() {
        return result.getBacklogBound();
    }

    /**
     * For TFA this is the whole traffic at a server because you do not separate the
     * flow of interest during analysis.
     * <p>
     * For SFA and PMOO you will get the arrival bounds of the cross-traffic at
     * every server.
     *
     * @return Mapping from the server to the server's arrival bound
     */
    public Map<Server, Set<ArrivalCurve>> getServerAlphasMap() {
        return result.map__server__alphas;
    }

    public String getServerAlphasMapString() {
        return result.getServerAlphasMapString();
    }
}