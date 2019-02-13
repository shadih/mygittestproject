package com.att.gfp.data.ipag.decomposition;

/**
 * 
 */
import java.util.*;

import javax.xml.bind.annotation.XmlRootElement;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.att.gfp.helper.GFPUtil;

import com.hp.uca.expert.alarm.Alarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;

@XmlRootElement
public class DecomposableAdtranLport extends DecomposableAdtranPport {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableAdtranLport.class);
	
	
	public DecomposableAdtranLport() {
		super();
	}
	
	public DecomposableAdtranLport(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableAdtranLport clone() throws CloneNotSupportedException {
		DecomposableAdtranLport newAlarm = (DecomposableAdtranLport) super.clone();				
		return newAlarm;
	}
	
	public void decomposition() throws Exception {
		
		String lportInstanceName = getOriginatingManagedEntity().split(" ")[1];
		log.info("lportInstanceName=" + lportInstanceName);
		getPPortFromLPort(lportInstanceName, getNodeType());
		super.decomposition();
	}

	
	public void getPPortFromLPort(String lportInstanceName, String nodeType) {
		
		String nodeIndex = null;
		if (nodeType.equals("PE")) {
			nodeIndex = "PE_LPort";
		} else {
			nodeIndex = "CE_LPort";
		}
		
		String query = "START lport=node:" + nodeIndex + "(key = \""
				+ lportInstanceName
				+ "\") "
				+ "MATCH (lport)<-[:Composed_Of]-(local_pport)-[:PLink]->(remote_pport) "
				+ "WHERE (lport.class = \"IpagLPort\" and local_pport.class = \"IpagPPort\" "
				+	"and remote_pport.class = \"IpagPPort\") "
				+ "RETURN local_pport.key, local_pport.remote_device_type, remote_pport.key";
		
		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		for ( Map<String, Object> row : result ) {
			setOriginatingManagedEntity("PPORT " + (String) row.get("local_pport.key"));
			setRemotePportInstanceName((String) row.get("remote_pport.key"));
			String remoteDeviceType = (String) row.get("local_pport.remote_device_type");
			String remoteNodeType = getNodeTypeFromDeviceType(remoteDeviceType);
			if (remoteDeviceType.equals("JUNIPER MX SERIES")) {
				getDeviceInfoFromPportAndCard(
					(String) row.get("remote_pport.key"), remoteNodeType);
			} else {
				getDeviceInfoFromPport((String) row.get("remote_pport.key"), remoteNodeType);
			}
			break;
		}
	}

	
	public void getDeviceInfoFromPport(String pport, String nodeType) {
		
		String nodeIndex = null;
		if (nodeType.equals("PE")) {
			nodeIndex = "PE_PPort";
		} else {
			nodeIndex = "CE_PPort";
		}
		
		String query = "START pport=node:" + nodeIndex + "(key = \""
				+ pport
				+ "\") "
				+ "MATCH (pport)<-[:Composed_Of_PPort]-(device) "
				+ "WHERE (device.class = \"IpagDevice\") "
				+ "RETURN device.device_name, device.device_model, device.device_type";
		
		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		for ( Map<String, Object> row : result ) {
			setRemoteDeviceName((String) row.get("device.device_name"));
			setRemoteDeviceType((String) row.get("device.device_type"));
			setRemoteDeviceModel((String) row.get("device.device_model"));
			break;
		}
	}

	
	public void getDeviceInfoFromPportAndCard(String pport, String nodeType) {
		
		String nodeIndex = null;
		if (nodeType.equals("PE")) {
			nodeIndex = "PE_PPort";
		} else {
			nodeIndex = "CE_PPort";
		}
		
		String query = "START pport=node:" + nodeIndex + "(key = \""
				+ pport
				+ "\") "
				+ "MATCH (pport)<-[:Composed_Of]-(card)<-[:Composed_Of]-(slot)"
				+	"<-[:Composed_Of]-(device) "
				+ "WHERE (card.class = \"IpagCard\" and slot.class = \"IpagSlot\" "
				+	"and device.class = \"IpagDevice\") "
				+ "RETURN device.device_name, device.device_model, device.device_type";
		
		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		for ( Map<String, Object> row : result ) {
			setRemoteDeviceName((String) row.get("device.device_name"));
			setRemoteDeviceType((String) row.get("device.device_type"));
			setRemoteDeviceModel((String) row.get("device.device_model"));
			break;
		}
	}
}
