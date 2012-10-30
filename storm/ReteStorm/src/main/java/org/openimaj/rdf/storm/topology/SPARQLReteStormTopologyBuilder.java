/**
 * Copyright (c) 2012, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.rdf.storm.topology;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.thrift7.TException;
import org.openimaj.io.FileUtils;
import org.openimaj.rdf.storm.topology.builder.NTriplesSPARQLReteTopologyBuilder;
import org.openimaj.rdf.storm.topology.builder.ReteTopologyBuilder;
import org.openimaj.rdf.storm.topology.builder.SPARQLReteTopologyBuilder;
import org.openimaj.rdf.storm.utils.CsparqlUtils;
import org.openimaj.rdf.storm.utils.CsparqlUtils.CSparqlComponentHolder;
import org.openimaj.rdf.storm.utils.JenaStormUtils;

import backtype.storm.Config;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.scheduler.Cluster;
import backtype.storm.topology.TopologyBuilder;

import com.hp.hpl.jena.reasoner.rulesys.Rule;

import eu.larkc.csparql.streams.formats.TranslationException;

/**
 * Given a set of rules, construct a RETE topology such that filter (alpha)
 * nodes and join (beta) nodes are filtering bolts
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SPARQLReteStormTopologyBuilder {
	/**
	 * The name of a debug bolt
	 */
	public static final String DEBUG_BOLT = "debugBolt";

	/**
	 * default rules
	 */
	public static final String SELECT_ALL = "REGISTER QUERY ALLQ AS SELECT ?a ?b ?c FROM STREAM <%s> [RANGE 1m STEP 1m] WHERE {?a ?b ?c.}";
	/**
	 * the name of the final bolt
	 */
	public static final String FINAL_TERMINAL = "final_term";
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(SPARQLReteStormTopologyBuilder.class);

	private CSparqlComponentHolder query;

	/**
	 * Construct a Rete topology using the default RDFS rules
	 *
	 * @param conf
	 *            the {@link Config} to be sent to the {@link Cluster}. Only
	 *            used to register serialisers
	 * @throws TranslationException
	 * @throws IOException
	 */
	public SPARQLReteStormTopologyBuilder(Config conf) throws TranslationException, IOException {
		JenaStormUtils.registerSerializers(conf);
//		this.query = QueryFactory.create(SELECT_ALL);
		this.query = CsparqlUtils.parse(SELECT_ALL);
	}

	/**
	 * Construct a Rete topology using the InputStream as a source of rules
	 *
	 * @param conf
	 * @param query
	 *            the SPARQL query
	 * @throws TranslationException
	 * @throws IOException
	 */
	public SPARQLReteStormTopologyBuilder(Config conf, String query) throws TranslationException, IOException {
		JenaStormUtils.registerSerializers(conf);
		this.query = CsparqlUtils.parse(query);

	}

	/**
	 * Using an {@link NTriplesSPARQLReteTopologyBuilder}, load the nTriples
	 * from the given resource and compile a storm topology for the sparql query
	 * used to construct this {@link SPARQLReteStormTopologyBuilder}
	 *
	 * @param nTriples
	 *            A URL containing nTriples
	 *
	 * @return a storm topology
	 */
	public StormTopology buildTopology(String nTriples) {
		final TopologyBuilder builder = new TopologyBuilder();
		final SPARQLReteTopologyBuilder topologyBuilder = new NTriplesSPARQLReteTopologyBuilder();
		topologyBuilder.compile(builder, this.query);
		return builder.createTopology();
	}

	/**
	 * @param topologyBuilder
	 *
	 * @return given a {@link ReteTopologyBuilder} and a list of
	 *         {@link SPARQLReteStormTopologyBuilder} instances construct a
	 *         {@link StormTopology}
	 */
	public StormTopology buildTopology(SPARQLReteTopologyBuilder topologyBuilder) {
		final TopologyBuilder builder = new TopologyBuilder();
		topologyBuilder.compile(builder, this.query);
		final StormTopology top = builder.createTopology();
		return top;
	}

	/**
	 * @param config
	 *            the {@link Config} instance with which the
	 *            {@link StormTopology} will be submitted to the {@link Cluster}
	 *            .
	 * @param topologyBuilder
	 *            the approach to constructing a {@link StormTopology}
	 * @param query
	 *            the query from which to construct the network
	 * @return given a {@link TopologyBuilder} and a source for {@link Rule}
	 *         instances build {@link StormTopology}
	 * @throws TranslationException
	 * @throws IOException
	 */
	public static StormTopology buildTopology(Config config, SPARQLReteTopologyBuilder topologyBuilder, String query) throws TranslationException, IOException {
		final SPARQLReteStormTopologyBuilder topology = new SPARQLReteStormTopologyBuilder(config, query);
		return topology.buildTopology(topologyBuilder);
	}

	private static SPARQLReteStormTopologyBuilder buildDefaultTopology(InputStream resourceAsStream) throws IOException, TranslationException {
		return buildDefaultTopology(FileUtils.readall(resourceAsStream));
	}

	/**
	 * A {@link SPARQLReteStormTopologyBuilder} with a default configuration
	 *
	 * @param query
	 * @return A {@link SPARQLReteStormTopologyBuilder} which can construct storm topologies from queries
	 * @throws TranslationException
	 * @throws IOException
	 */
	public static SPARQLReteStormTopologyBuilder buildDefaultTopology(String query) throws TranslationException, IOException {
		final Config conf = new Config();
		conf.setDebug(false);
		conf.setNumWorkers(2);
		conf.setMaxSpoutPending(1);
		conf.setFallBackOnJavaSerialization(false);
		conf.setSkipMissingKryoRegistrations(false);
		final SPARQLReteStormTopologyBuilder fact = new SPARQLReteStormTopologyBuilder(conf, query);
		return fact;
	}

	/**
	 * run the rete topology
	 *
	 * @param args
	 * @throws InvalidTopologyException
	 * @throws AlreadyAliveException
	 * @throws IOException
	 * @throws TException
	 * @throws TranslationException
	 */
	public static void main(String args[]) throws AlreadyAliveException, InvalidTopologyException, TException,
			IOException, TranslationException
	{
		final String rdfSource = "file:///Users/ss/Development/java/openimaj/trunk/storm/ReteStorm/src/test/resources/test.rdfs";
		String sparqlSource = "/test.csparql";
		final SPARQLReteStormTopologyBuilder fact = SPARQLReteStormTopologyBuilder.buildDefaultTopology(SPARQLReteStormTopologyBuilder.class.getResourceAsStream(sparqlSource));

//		final LocalCluster cluster = new LocalCluster();
		final StormTopology topology = fact.buildTopology(rdfSource);
		System.out.println(topology);
		// cluster.submitTopology("reteTopology", fact.conf, topology);
		// Utils.sleep(10000);
		// cluster.killTopology("reteTopology");
		// cluster.shutdown();

	}



}
