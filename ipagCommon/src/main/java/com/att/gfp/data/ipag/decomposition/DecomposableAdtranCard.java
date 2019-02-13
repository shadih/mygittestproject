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
public class DecomposableAdtranCard extends DecomposableAlarm {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableAdtranCard.class);

	
	public DecomposableAdtranCard() {
		super();
	}
	
	public DecomposableAdtranCard(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableAdtranCard clone() throws CloneNotSupportedException {
		DecomposableAdtranCard newAlarm = (DecomposableAdtranCard) super.clone();				
		return newAlarm;
	}
	
	public void decomposition() throws Exception {
		
		setAlarmDescription("CARD_DOWN");
		String cardInstanceName = getOriginatingManagedEntity().split(" ")[1];
		log.info("cardInstanceName=" + cardInstanceName);
		getEvcsFromCard(cardInstanceName, getNodeType());			
		
	}

	
	public void getEvcsFromCard(String cardName, String nodeType) throws Exception {
		
		String nodeIndex = null;
		if (nodeType.equals("PE")) {
			nodeIndex = "PE_Card";
		} else {
			nodeIndex = "CE_Card";
		}
		
		String query = "START card=node:" + nodeIndex + "(key = \""
				+ cardName
				+ "\") "
				+ "MATCH (card)-[:Composed_Of]->(local_pport)-[:PLink]->(remote_pport)"
				+ "-[:Composed_Of]->(evcNode) "
				+ "WHERE (card.class = \"IpagCard\" and local_pport.class = \"IpagPPort\" and "
				+	"remote_pport.class = \"IpagPPort\" and "
				+	"evcNode.class = \"IpagEvcNode\" and "
				+	"(card.card_type = \"ESHDSL_EFM_32\" or card.card_type = \"CHANDS3_3\") and "
				+	"local_pport.remote_device_type = \"ADTRAN 800 SERIES\") "
				+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
				+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name, "
				+	"evcNode.unickt, remote_pport.key, remote_pport.device_type";

		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		for ( Map<String, Object> row : result ) {
			getDeviceInfoFromPPort((String) row.get("remote_pport.key"), 
				(String) row.get("remote_pport.device_type"));
			setEvcNode(row);
			decomposeAlarm();
		}
	}

	public void getDeviceInfoFromPPort(String pport, String deviceType) {
		
		String nodeIndex = null;
		if (getNodeTypeFromDeviceType(deviceType).equals("CE")) {
			nodeIndex = "CE_PPort";
		} else {
			nodeIndex = "PE_PPort";
		}
		
		String query = "START pport=node:" + nodeIndex + "(key = \"" + pport + "\") "
				+ "MATCH (pport)<-[:Composed_Of_PPort]-(device) "
				+ "RETURN device";
		log.info("CYPHER QUERY: " + query);

		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);

		Iterator<Node> columnRows = result.columnAs("device");
		while (columnRows.hasNext()) {
			Node device = columnRows.next();
			setDeviceName((String) device.getProperty("device_name"));
			setDeviceType((String) device.getProperty("device_type"));
			setDeviceModel((String) device.getProperty("device_model"));
			break;
		}
	}	
}
