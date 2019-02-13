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
public class DecomposableJuniperVpn extends DecomposableAlarm {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableJuniperVpn.class);

	
	public DecomposableJuniperVpn() {
		super();
	}
	
	public DecomposableJuniperVpn(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableJuniperVpn clone() throws CloneNotSupportedException {
		DecomposableJuniperVpn newAlarm = (DecomposableJuniperVpn) super.clone();				
		return newAlarm;
	}
	
	public void decomposition() throws Exception {

		String ipAddr = getOriginatingManagedEntity().split(" ")[1];
		String vrfName = getCustomFieldValue("vrf-name");
		if (vrfName == null) {
			vrfName = parseLabeledText(getCustomFieldValue("reason"), "VRF");
		}
		log.info("vrfName=" + vrfName);
		getEvcsFromVrf(vrfName, ipAddr);
		
	}

	
	public void getEvcsFromVrf(String vrfName, String ipAddr) throws Exception {
		
		String query = "START evc=node:EVC(key = \"" + vrfName + "\") "
				+ "MATCH (evc)-[:Associated_To]->(evcNode)<-[:Composed_Of]-(lport)"
				+ "<-[:Composed_Of]-(pport)<-[:Composed_Of_PPort]-(device) "
				+ "WHERE (evc.class = \"IpagEvc\" and evcNode.class = \"IpagEvcNode\" and "
				+     "lport.class = \"IpagLPort\" and pport.class = \"IpagPPort\" and "
				+     "device.class = \"IpagDevice\" and "
				+     "pport.device_type = \"CIENA NTE\" and "
				+     "pport.remote_deviceloopbackip = \"" + ipAddr + "\") " 
				+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
				+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name, "
				+	"evcNode.unickt, device.device_name, device.device_type, device.device_model";

		log.info("CYPHER QUERY: " + query);
		ExecutionEngine engine = GFPUtil.getCypherEngine();
		ExecutionResult result = engine.execute(query);
		for ( Map<String, Object> row : result ) {
			setDeviceName((String) row.get("device.device_name"));
			setDeviceType((String) row.get("device.device_type"));
			setDeviceModel((String) row.get("device.device_model"));
			setEvcNode(row);
			decomposeAlarm();
		}
	}	
}
