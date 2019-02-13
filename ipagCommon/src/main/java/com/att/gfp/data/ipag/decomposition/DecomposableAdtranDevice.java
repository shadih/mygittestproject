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
public class DecomposableAdtranDevice extends DecomposableAlarm {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableAdtranDevice.class);

	
	public DecomposableAdtranDevice() {
		super();
	}
	
	public DecomposableAdtranDevice(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableAdtranDevice clone() throws CloneNotSupportedException {
		DecomposableAdtranDevice newAlarm = (DecomposableAdtranDevice) super.clone();				
		return newAlarm;
	}
	
	public void decomposition() throws Exception {
		
		String deviceInstanceName = getOriginatingManagedEntity().split(" ")[1];
		log.info("deviceInstanceName=" + deviceInstanceName);
		if (getDeviceType().equals("ADTRAN 5000 SERIES")) {
			getEvcsFromDeviceAndCard(deviceInstanceName, getNodeType());
		} else {
			getEvcsFromDevice(deviceInstanceName, getNodeType());				
		}
		
	}

	public void getEvcsFromDeviceAndCard(String deviceName, String nodeType) throws Exception {
		
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
				+ "MATCH (device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)"
				+ "-[:Composed_Of]->(lport)-[:Composed_Of]->(evcNode) "
				+ "WHERE (device.class = \"IpagDevice\" and slot.class = \"IpagSlot\" and "
				+	"card.class = \"IpagCard\" and pport.class = \"IpagPPort\" and "
				+	"lport.class = \"IpagLPort\" and evcNode.class = \"IpagEvcNode\") "
				+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
				+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name, "
				+	"evcNode.unickt";
		} else if (nodeType.equals("CE")) {
			query = "START device=node:" + nodeIndex + "(key = \""
					+ deviceName
					+ "\") "
					+ "MATCH (device)-[:Composed_Of]->(slot)-[:Composed_Of]->(card)-[:Composed_Of]->(pport)"
					+ "-[:Composed_Of]->(evcNode) "
					+ "WHERE (device.class = \"IpagDevice\" and slot.class = \"IpagSlot\" and "
					+	"card.class = \"IpagCard\" and pport.class = \"IpagPPort\" and "
					+	"evcNode.class = \"IpagEvcNode\") "
					+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
					+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name, "
					+	"evcNode.unickt";
		}
		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		for ( Map<String, Object> row : result ) {
			setEvcNode(row);
			decomposeAlarm();
		}
	}

	public void getEvcsFromDevice(String deviceName, String nodeType) throws Exception {
		
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
				+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
				+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name, evcNode.unickt,"
				+	"pport.clci";
		} else if (nodeType.equals("CE")) {
			query = "START device=node:" + nodeIndex + "(key = \""
					+ deviceName
					+ "\") "
					+ "MATCH (device)-[:Composed_Of_PPort]->(pport)"
					+ " -[:Composed_Of]->(evcNode) "
					+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
					+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name,evcNode.unickt, "
					+	"pport.clci";  
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
