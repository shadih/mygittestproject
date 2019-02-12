package com.att.gfp.data.ipag.decomposition;

/**
 * 
 */
import java.util.*;

import javax.xml.bind.annotation.XmlRootElement;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.att.gfp.helper.GFPUtil;

import com.hp.uca.expert.alarm.Alarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;

@XmlRootElement
public class DecomposableCienaDevice extends DecomposableAlarm {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableCienaDevice.class);

	
	public DecomposableCienaDevice() {
		super();
	}
	
	public DecomposableCienaDevice(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableCienaDevice clone() throws CloneNotSupportedException {
		DecomposableCienaDevice newAlarm = (DecomposableCienaDevice) super.clone();				
		return newAlarm;
	}
	
	public void decomposition() throws Exception {
		
		String deviceInstanceName = getOriginatingManagedEntity().split(" ")[1];
		log.info("deviceInstanceName=" + deviceInstanceName);
		
		if (getDeviceType().equals("CIENA EMUX")) {
			setAlarmDescription("CARD_DOWN");
			getEvcsFromCienaEmuxDevice(deviceInstanceName, getNodeType());				
		} else if (getDeviceType().equals("CIENA NTE")) {
			getEvcsFromCienaNteDevice(deviceInstanceName, getNodeType());
		}
	}


	public void getEvcsFromCienaEmuxDevice(String deviceName, String nodeType) 
		throws Exception {
		
		String nodeIndex = null;
		if (nodeType.equals("PE")) {
			nodeIndex = "PE_Device";
		} else {
			nodeIndex = "CE_Device";
		}
		String query ="";
		if (nodeType.equals("PE")) {
		  query = "START device=node:" + nodeIndex + "(key = \""
				+ deviceName
				+ "\") "
				+ "MATCH (device)-[:Composed_Of_PPort]->(pport)"
				+ "-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) "
				+ "WHERE (pport.class = \"IpagPPort\" and lport.class = \"IpagLPort\" and "
				+	"evcNode.class = \"IpagEvcNode\" and "
				+	"pport.remote_device_type <> \"JUNIPER MX SERIES\") "
				+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
				+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, "
				+	"evcNode.evc_name, pport.clci";
		} else if (nodeType.equals("CE")) {
				  query = "START device=node:" + nodeIndex + "(key = \""
						+ deviceName
						+ "\") "
						+ "MATCH (device)-[:Composed_Of_PPort]->(pport)"
						+ " -[:Composed_Of]->(evcNode) "
						+ "WHERE (pport.class = \"IpagPPort\" and "
						+	"evcNode.class = \"IpagEvcNode\" and "
						+	"pport.remote_device_type <> \"JUNIPER MX SERIES\") "
						+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
						+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, "
						+	"evcNode.evc_name, pport.clci";
		} 

		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query); 
		for ( Map<String, Object> row : result ) {
			setEvcNode(row);
			decomposeAlarm();
		}
	}	

	
	public void getEvcsFromCienaNteDevice(String deviceName, String nodeType) 
			throws Exception {
			
		String nodeIndex = null;
		if (nodeType.equals("PE")) {
			nodeIndex = "PE_Device";
		} else {
			nodeIndex = "CE_Device";
		}
		String query = "";
		if (nodeType.equals("PE")) {
		 query = "START device=node:" + nodeIndex + "(key = \""
				+ deviceName
				+ "\") "
				+ "MATCH (device)-[:Composed_Of_PPort]->(pport)"
				+ "-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) "
				+ "WHERE (pport.class = \"IpagPPort\" and lport.class = \"IpagLPort\" and "
				+	"evcNode.class = \"IpagEvcNode\" and "
				+	"pport.remote_device_type IN [\"JUNIPER MX SERIES\", "
				+	"\"ADTRAN 5000 SERIES\", \"CIENA EMUX\"]) "
				+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
				+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, "
				+	"evcNode.evc_name, pport.clci";
		} else if (nodeType.equals("CE")) {
				 query = "START device=node:" + nodeIndex + "(key = \""
						+ deviceName
						+ "\") "
						+ "MATCH (device)-[:Composed_Of_PPort]->(pport)"
						+ " -[:Composed_Of]->(evcNode) "
						+ "WHERE (pport.class = \"IpagPPort\" and "
						+	"evcNode.class = \"IpagEvcNode\" and "
						+	"pport.remote_device_type IN [\"JUNIPER MX SERIES\", " 
						+	"\"ADTRAN 5000 SERIES\", \"CIENA EMUX\"]) "
						+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
						+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, "
						+	"evcNode.evc_name, pport.clci";
		}

		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		for ( Map<String, Object> row : result ) { 
			setEvcNode(row);
			decomposeAlarm();
		}
	}	
}
