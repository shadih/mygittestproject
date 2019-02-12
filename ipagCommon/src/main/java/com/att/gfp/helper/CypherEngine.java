package com.att.gfp.helper;

import javax.annotation.PostConstruct;

import org.neo4j.cypher.javacompat.ExecutionEngine;



import com.hp.uca.expert.topology.TopoAccess;

public class CypherEngine {
	private ExecutionEngine engine;
	
	@PostConstruct
	private void init() {
		engine = new ExecutionEngine(TopoAccess.getGraphDB()); 
	}

	public ExecutionEngine getEngine() {
		return engine;
	}

	public void setEngine(ExecutionEngine engine) {
		this.engine = engine;
	}
	 

}
