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
public class DecomposableCienaPport extends DecomposablePport {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableCienaPport.class);
	
	public DecomposableCienaPport() {
		super();
	}
	
	public DecomposableCienaPport(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableCienaPport clone() throws CloneNotSupportedException {
		DecomposableCienaPport newAlarm = (DecomposableCienaPport) super.clone();				
		return newAlarm;
	}
	
	@Override
	public void decomposition() throws Exception {
		
		String pportInstanceName = "";
		
		if ("CIENA EMUX".equalsIgnoreCase(getDeviceType())) {
			if ("JUNIPER MX SERIES".equalsIgnoreCase(getRemoteDeviceType())) {
				if (!getCustomFieldValue("EventKey").equals("50002/100/22")) {
					setAlarmDescription("TRUNK_DOWN");
					pportInstanceName = getOriginatingManagedEntity().split(" ")[1];
					log.info("pportInstanceName=" + pportInstanceName);
					 getEvcsFromDevicePort(getNodeType(), pportInstanceName.split("/")[0]);
				}
			} else {
				setDeviceName(getRemoteDeviceName());
				setDeviceType(getRemoteDeviceType());
				setDeviceModel(getRemoteDeviceModel()); 
				if(getRemotePportInstanceName() != null) { 
					pportInstanceName = getRemotePportInstanceName();
				}
				if(getRemoteDeviceType() != null && ! getRemoteDeviceType().isEmpty() 
						&&  pportInstanceName != null && !pportInstanceName.isEmpty() ) {
					 getEvcsFromDevicePort(getNodeTypeFromDeviceType(getRemoteDeviceType()),  pportInstanceName.split("/")[0]);		
				} 
			}
		} else if ("CIENA NTE".equalsIgnoreCase(getDeviceType())) {
			if (("JUNIPER MX SERIES".equalsIgnoreCase(getRemoteDeviceType())) ||
					"CIENA EMUX".equalsIgnoreCase(getRemoteDeviceType())) {
				pportInstanceName = getOriginatingManagedEntity().split(" ")[1]; 
				log.info("pportInstanceName=" + pportInstanceName);
				 getEvcsFromDevicePort(getNodeType(), pportInstanceName.split("/")[0]);
			} else {
				pportInstanceName = getOriginatingManagedEntity().split(" ")[1]; 
				log.info("pportInstanceName=" + pportInstanceName);
				getEvcsFromPPortMatchingClci(getNodeType(), pportInstanceName);
			}
		}
	}  	

	
	public void getEvcsFromPPortMatchingClci(String nodeType, String pportInstanceName) 
		throws Exception {
		
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
					+ "WHERE (lport.class = \"IpagLPort\" and "
					+	"evcNode.class = \"IpagEvcNode\" and pport.clci = evcNode.unickt) " 
					+ "RETURN evcNode.key, evcNode.alarm_classification, evcNode.alarm_domain, "
					+ 	"evcNode.data_source, evcNode.acnaban, evcNode.vrf_name, evcNode.evc_name, "
					+	"evcNode.unickt";
		} else	if (nodeType.equals("CE")) {
				query = "START pport=node:" + nodeIndex + "(key = \""
						+ pportInstanceName
						+ "\") "
						+ "MATCH (pport)- [:Composed_Of]->(evcNode) "
						+ "WHERE ("
						+	"evcNode.class = \"IpagEvcNode\" and pport.clci = evcNode.unickt) " 
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

}
