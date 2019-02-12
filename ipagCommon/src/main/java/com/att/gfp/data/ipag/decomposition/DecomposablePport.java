package com.att.gfp.data.ipag.decomposition;

/**
 * 
 */
import java.util.Iterator;

import javax.xml.bind.annotation.XmlRootElement;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.att.gfp.helper.GFPUtil;


import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
 
@XmlRootElement
public abstract class DecomposablePport extends DecomposableAlarm {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposablePport.class);
	
	public DecomposablePport() {
		super();
	}
	
	public DecomposablePport(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposablePport clone() throws CloneNotSupportedException {
		DecomposablePport newAlarm = (DecomposablePport) super.clone();				
		return newAlarm;
	}
	
	@Override
	public abstract void decomposition() throws Exception;
		
	
	public void getEvcsFromPPort(String nodeType, String pportInstanceName) throws Exception {
		
		String nodeIndex = null;
		if (nodeType.equals("PE")) {
			nodeIndex = "PE_PPort";
		} else {
			nodeIndex = "CE_PPort";
		}
		String query = "";
		if (nodeType.equals("PE")) {
		    query = "START pport=node:" + nodeIndex + "(key = \""
				+ pportInstanceName
				+ "\") "
				+ "MATCH (pport)-[:Composed_Of]-(lport)-[:Composed_Of]->(evcNode) "
				+ "WHERE (lport.class = \"IpagLPort\" and evcNode.class = \"IpagEvcNode\") " 
				+ "RETURN evcNode"; 
		} else if (nodeType.equals("CE")) {
			  query = "START pport=node:" + nodeIndex + "(key = \""
					+ pportInstanceName
					+ "\") "
					+ "MATCH (pport)-[:Composed_Of]->(evcNode) "
					+ "WHERE (evcNode.class = \"IpagEvcNode\") " 
					+ "RETURN evcNode"; 
		}   

		log.info("CYPHER QUERY: " + query); 
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		int count=0;
		Node node = null;
		Iterator<Node> columnRows = result.columnAs("evcNode");
		while (columnRows.hasNext()) {
			node = columnRows.next();
			setEvcNodeFromResult(node);
			decomposeAlarm();

			count++;
		} 			 
		log.info("CYPHER QUERY: count " + count); 
//		
//		for ( Map<String, Object> row : result ) {
//			setEvcNode(row);
//			decomposeAlarm();
//		}
	}
	
	public void getEvcsFromDevicePort(String nodeType, String device) throws Exception {
		
		String nodeIndex = null;
		if (nodeType.equals("PE")) { 
			nodeIndex = "PE_Device";
		} else {
			nodeIndex = "CE_Device";
		}
		String query = "";
		if (nodeType.equals("PE")) { 
		   query = "START device=node:" + nodeIndex + "(key = \""
		+ device
		+ "\") "
		+ "MATCH (device)-[:Composed_Of_PPort]-(pport)"
		+ "-[:Composed_Of]- (lport)-[:Composed_Of]->(evcNode) "
		+ "RETURN DISTINCT evcNode";
		
		} else if (nodeType.equals("CE")) { 
			 query = "START device=node:" + nodeIndex + "(key = \""
			+ device
			+ "\") "
			+ "MATCH (device)-[:Composed_Of_PPort]-(pport)"
			+ " -[:Composed_Of]->(evcNode) "
			+ "RETURN DISTINCT evcNode";
		}
		log.info("CYPHER QUERY: " + query); 
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		int count=0; 
		Node node = null; 
		Iterator<Node> columnRows = result.columnAs("evcNode");
		while (columnRows.hasNext()) {
			node = columnRows.next();
			setEvcNodeFromResult(node);
			decomposeAlarm();

			count++;
		} 			 
		log.info("CYPHER QUERY: count " + count); 
//		
//		for ( Map<String, Object> row : result ) {
//			setEvcNode(row);
//			decomposeAlarm();
//		}
	}
	
}
