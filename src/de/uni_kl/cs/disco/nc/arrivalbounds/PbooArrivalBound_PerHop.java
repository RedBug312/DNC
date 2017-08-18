/*
 * This file is part of the Disco Deterministic Network Calculator v2.4.0beta3 "Chimera".
 *
 * Copyright (C) 2014 - 2017 Steffen Bondorf
 * Copyright (C) 2017 The DiscoDNC contributors
 *
 * Distributed Computer Systems (DISCO) Lab
 * University of Kaiserslautern, Germany
 *
 * http://disco.cs.uni-kl.de/index.php/projects/disco-dnc
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

package de.uni_kl.cs.disco.nc.arrivalbounds;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.uni_kl.cs.disco.curves.ArrivalCurve;
import de.uni_kl.cs.disco.curves.CurvePwAffineFactoryDispatch;
import de.uni_kl.cs.disco.curves.CurvePwAffineUtilsDispatch;
import de.uni_kl.cs.disco.curves.ServiceCurve;
import de.uni_kl.cs.disco.misc.SetUtils;
import de.uni_kl.cs.disco.nc.AbstractArrivalBound;
import de.uni_kl.cs.disco.nc.AnalysisConfig;
import de.uni_kl.cs.disco.nc.ArrivalBound;
import de.uni_kl.cs.disco.nc.ArrivalBoundDispatch;
import de.uni_kl.cs.disco.nc.analyses.TotalFlowAnalysis;
import de.uni_kl.cs.disco.nc.operations.LeftOverService;
import de.uni_kl.cs.disco.nc.operations.OutputBound;
import de.uni_kl.cs.disco.network.Flow;
import de.uni_kl.cs.disco.network.Link;
import de.uni_kl.cs.disco.network.Network;
import de.uni_kl.cs.disco.network.Path;
import de.uni_kl.cs.disco.network.Server;
import de.uni_kl.cs.disco.numbers.Num;
import de.uni_kl.cs.disco.numbers.NumFactory;

public class PbooArrivalBound_PerHop extends AbstractArrivalBound implements ArrivalBound {

	@SuppressWarnings("unused")
	private PbooArrivalBound_PerHop() {
	}

	public PbooArrivalBound_PerHop(Network network, AnalysisConfig configuration) {
		this.network = network;
		this.configuration = configuration;
	}

	public Set<ArrivalCurve> computeArrivalBound(Link link, Flow flow_of_interest) throws Exception {
		return computeArrivalBound(link, network.getFlows(link), flow_of_interest);
	}

	public Set<ArrivalCurve> computeArrivalBound(Link link, Set<Flow> f_xfcaller, Flow flow_of_interest)
			throws Exception {
		Set<ArrivalCurve> alphas_xfcaller = new HashSet<ArrivalCurve>(
				Collections.singleton(CurvePwAffineFactoryDispatch.createZeroArrivals()));
		if (f_xfcaller == null || f_xfcaller.isEmpty()) {
			return alphas_xfcaller;
		}

		// Get the servers on common sub-path of f_xfcaller flows crossing link
		// loi == location of interference
		Server loi = link.getDest();
		Set<Flow> f_loi = network.getFlows(loi);
		Set<Flow> f_xfcaller_loi = SetUtils.getIntersection(f_loi, f_xfcaller);
		f_xfcaller_loi.remove(flow_of_interest);
		if (f_xfcaller_loi.size() == 0) {
			return alphas_xfcaller;
		}

		// The shortcut found in PmooArrivalBound for the a common_subpath of length 1
		// will not be implemented here.
		// There's not a big potential to increase performance as the PBOO arrival bound
		// implicitly handles this situation by only iterating over one server in the
		// for loop.
		Server common_subpath_src = network.findSplittingServer(loi, f_xfcaller_loi);
		Server common_subpath_dest = link.getSource();
		Flow f_representative = f_xfcaller_loi.iterator().next();
		Path common_subpath = f_representative.getSubPath(common_subpath_src, common_subpath_dest);

		alphas_xfcaller = ArrivalBoundDispatch.computeArrivalBounds(network, configuration, common_subpath_src,
				f_xfcaller, flow_of_interest);

		// Calculate the left-over service curves for ever server on the sub-path and
		// convolve the cross-traffics arrival with it
		Link link_from_prev_s;
		Path foi_path = flow_of_interest.getPath();
		for (Server server : common_subpath.getServers()) {
			try {
				link_from_prev_s = network.findLink(foi_path.getPrecedingServer(server), server);
			} catch (Exception e) { // Reached the path's first server
				link_from_prev_s = null; // reset to null
			}

			Set<ServiceCurve> betas_lo_s;

			Set<Flow> f_xxfcaller_server = network.getFlows(server);
			f_xxfcaller_server.removeAll(f_xfcaller);
			f_xxfcaller_server.remove(flow_of_interest);

			Set<Flow> f_xxfcaller_server_path = SetUtils.getIntersection(f_xxfcaller_server,
					network.getFlows(link_from_prev_s));

			// Convert f_xfoi_server to f_xfoi_server_offpath
			f_xxfcaller_server.removeAll(f_xxfcaller_server_path);

			// If we are off the path of interest, flow_of_interest is Flow.NULL_FLOW
			// already.
			Set<ArrivalCurve> alpha_xxfcaller_path = ArrivalBoundDispatch.computeArrivalBounds(network, configuration,
					server, f_xxfcaller_server_path, flow_of_interest);
			Set<ArrivalCurve> alpha_xxfcaller_offpath = ArrivalBoundDispatch.computeArrivalBounds(network,
					configuration, server, f_xxfcaller_server, Flow.NULL_FLOW);

			Set<ArrivalCurve> alphas_xxfcaller_s = new HashSet<ArrivalCurve>();
			for (ArrivalCurve arrival_curve_path : alpha_xxfcaller_path) {
				for (ArrivalCurve arrival_curve_offpath : alpha_xxfcaller_offpath) {
					alphas_xxfcaller_s.add(CurvePwAffineUtilsDispatch.add(arrival_curve_path, arrival_curve_offpath));
				}
			}

			// Calculate the left-over service curve for this single server
			betas_lo_s = LeftOverService.compute(configuration, server, alphas_xxfcaller_s);

			// Check if there's any service left on this path. If not, the set only contains
			// a null-service curve.
			if (betas_lo_s.size() == 1
					&& betas_lo_s.iterator().next().equals(CurvePwAffineFactoryDispatch.createZeroService())) {
				System.out.println("No service left over during PBOO arrival bounding!");
				alphas_xfcaller.clear();
				alphas_xfcaller.add((ArrivalCurve) CurvePwAffineFactoryDispatch.createZeroDelayInfiniteBurst());
				return alphas_xfcaller;
			}

			// The deconvolution of the two sets, arrival curves and service curves,
			// respectively, takes care of all the possible combinations
			alphas_xfcaller = OutputBound.compute(configuration, alphas_xfcaller, server, betas_lo_s);
		}

		if (configuration.abConsiderTFANodeBacklog()) {
			Server last_hop_xtx = link.getSource();
			TotalFlowAnalysis tfa = new TotalFlowAnalysis(network, configuration);
			tfa.deriveBoundsAtServer(last_hop_xtx);

			Set<Num> tfa_backlog_bounds = tfa.getServerBacklogBoundMap().get(last_hop_xtx);
			Num tfa_backlog_bound_min = NumFactory.getNumFactory().getPositiveInfinity();

			for (Num tfa_backlog_bound : tfa_backlog_bounds) {
				if (tfa_backlog_bound.leq(tfa_backlog_bound_min)) {
					tfa_backlog_bound_min = tfa_backlog_bound;
				}
			}

			// Reduce the burst
			for (ArrivalCurve alpha_xfcaller : alphas_xfcaller) {
				if (alpha_xfcaller.getBurst().gt(tfa_backlog_bound_min)) {
					alpha_xfcaller.getSegment(1).setY(tfa_backlog_bound_min); // if the burst is >0 then there are at
																				// least two segments and the second
																				// holds the burst as its y-axis value
				}
			}
		}

		return alphas_xfcaller;
	}
}