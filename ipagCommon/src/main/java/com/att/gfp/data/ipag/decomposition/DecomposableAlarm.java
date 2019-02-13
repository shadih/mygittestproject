package com.att.gfp.data.ipag.decomposition;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.x733alarm.AlarmType;
import com.hp.uca.expert.x733alarm.ClassInstance;
import com.hp.uca.expert.x733alarm.OriginatingManagedEntityStructure;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.expert.topology.TopoAccess;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DecomposableAlarm extends EnrichedAlarm {
	
	private static Logger log = LoggerFactory.getLogger(DecomposableAlarm.class);
	//GraphDatabaseService topo = TopoAccess.getGraphDB();
	List<EnrichedAlarm> decomposedAlarms = new ArrayList<EnrichedAlarm>();
	
	private EvcNode evcNode = new EvcNode();
	private String alarmDescription = "PORT_DOWN";

	public DecomposableAlarm() {
	}

	public DecomposableAlarm(Alarm alarm) throws Exception {
		super(alarm);
	}

	public DecomposableAlarm(EnrichedAlarm alarm) throws Exception {
		super(alarm);
	}
		
	public EvcNode getEvcNode() {
		return evcNode;
	}
	
	public void setEvcNode(EvcNode evcNode) {
		this.evcNode = evcNode;
	}

	public void setEvcNode(Map<String, Object> row) {
		
		evcNode = new EvcNode();
		
		evcNode.setInstanceName((String) row.get("evcNode.key"));
		evcNode.setClassification((String) row.get("evcNode.alarm_classification"));
		evcNode.setDomain((String) row.get("evcNode.alarm_domain"));
		evcNode.setSource((String) row.get("evcNode.data_source"));
		evcNode.setAcnaban((String) row.get("evcNode.acnaban"));
		evcNode.setVrfName((String) row.get("evcNode.vrf_name"));
		evcNode.setEvcName((String) row.get("evcNode.evc_name"));
		evcNode.setUnickt((String) row.get("evcNode.unickt"));
 
		log.info("setEvcNode(): " + evcNode.toString());
	}

	public void setEvcNodeFromResult(Node node) {
		log.info("setEvcNodeFromResult(): ");
		
		evcNode = new EvcNode();
		if(node.hasProperty("key")) {
			evcNode.setInstanceName((String) node.getProperty("key"));  
		} 
		if(node.hasProperty("alarm_classification")) {
			evcNode.setClassification((String) node.getProperty("alarm_classification"));  
		} 
		if(node.hasProperty("alarm_domain")) {
			evcNode.setDomain((String) node.getProperty("alarm_domain"));  
		} 
		if(node.hasProperty("data_source")) {
			evcNode.setSource((String) node.getProperty("data_source"));  
		} 
		if(node.hasProperty("acnaban")) {
			evcNode.setAcnaban((String) node.getProperty("acnaban"));  
		} 
		if(node.hasProperty("vrf_name")) {
			evcNode.setVrfName((String) node.getProperty("vrf_name"));  
		} 
		if(node.hasProperty("evc_name")) {
			evcNode.setEvcName((String) node.getProperty("evc_name"));  
		} 
		if(node.hasProperty("unickt")) {
			evcNode.setUnickt((String) node.getProperty("unickt"));  
		}  

		log.info("setEvcNodeFromResult(): " + evcNode.toString());
	}
	
	
	public List<EnrichedAlarm> getDecomposedAlarms() {
		return decomposedAlarms;
	}

	
	public String getAlarmDescription() {
		return alarmDescription;
	}

	public void setAlarmDescription(String alarmDescription) {
		this.alarmDescription = alarmDescription;
	}

	public String parseLabeledText(String textStr, String label) {
		
		String parsedText = "";
		label += "=<";
		int i = textStr.indexOf(label);
		if (i > 0) {
			i += label.length();
			parsedText = textStr.substring(i);
			i = parsedText.indexOf(">");
			if (i > 0) {
				parsedText = parsedText.substring(0, i);
			}
		}
		return parsedText;
	}
	
	public String getNodeTypeFromDeviceType(String deviceType) {
		
		String nodeType = null;
		
		if (deviceType.contains("ADTRAN") ||
			deviceType.contains("CIENA ")) {
			nodeType = "CE";
		} else if (deviceType.contains("JUNIPER") ||
			deviceType.contains("CISCO") ||
			deviceType.contains("NV1") ||
			deviceType.contains("NV2") ||
			deviceType.contains("VR1") )
		{
			nodeType = "PE";
		}
		
		return nodeType;
	}
	
	public void decomposeAlarm() throws Exception {
		
		long now = System.currentTimeMillis();
		String nowStr = Long.toString(now / 1000);
		
		try {
			EnrichedAlarm ea = new EnrichedAlarm(this);
			ea.setIdentifier(getCustomFieldValue("EventKey") + 
					"-" + evcNode.getInstanceName());
			ea.setOriginatingManagedEntity("EVC " + evcNode.getInstanceName());
			OriginatingManagedEntityStructure orig = new OriginatingManagedEntityStructure();
			ClassInstance cl = new ClassInstance();
			cl.setClazz("EVC");
			cl.setInstance(evcNode.getInstanceName());
			orig.getClassInstance().add(cl);
			ea.setOriginatingManagedEntityStructure(orig);			
			GregorianCalendar gCalendar = new GregorianCalendar();
	        gCalendar.setTime(new java.util.Date(now));	 
			XMLGregorianCalendar alarmraisedtime = 
				DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
			ea.setAlarmRaisedTime(alarmraisedtime);
			if (this.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
				ea.setCustomFieldValue("reason", "CLEAR");
				ea.setCustomFieldValue("last-clear-time", nowStr);
				
			} else {
				ea.setCustomFieldValue("reason", alarmDescription);
				ea.setCustomFieldValue("last-clear-time", "0.0");
			}
			ea.setCustomFieldValue("fe-alarm-time", nowStr);
			ea.setCustomFieldValue("be-alarm-time", nowStr);   
			ea.setCustomFieldValue("last-update", nowStr);  
			ea.setCustomFieldValue("EventKey", getCustomFieldValue("EventKey"));    
			ea.setCustomFieldValue("destination", "CFM");
			ea.setSourceIdentifier(evcNode.getSource());
			ea.setCustomFieldValue("component", "deviceType=<" + getDeviceType() + 
				"> deviceModel=<" + getDeviceModel() + 
				"> EVC " + evcNode.getInstanceName());
			ea.setCustomFieldValue("alert-id", ea.getIdentifier());
			ea.setCustomFieldValue("domain", evcNode.getDomain());
			ea.setCustomFieldValue("sm-source-domain", evcNode.getSource());
			ea.setCustomFieldValue("managed-object-class", "EVC");
			ea.setCustomFieldValue("managed-object-instance", evcNode.getInstanceName());
			ea.setCustomFieldValue("sm-class", "EVC");
			ea.setCustomFieldValue("sm-instance", evcNode.getInstanceName());
			ea.setCustomFieldValue("classification", evcNode.getClassification());
			ea.setCustomFieldValue("source", evcNode.getSource());
			ea.setCustomFieldValue("acnaban", evcNode.getAcnaban());
			ea.setCustomFieldValue("vrf-name", evcNode.getVrfName());
			ea.setCustomFieldValue("evc-name", evcNode.getEvcName());
			ea.setCustomFieldValue("clci", evcNode.getUnickt());
			ea.setCustomFieldValue("node-name", getDeviceName());
			ea.setCustomFieldValue("secondary-alert-id", "");
			ea.setCustomFieldValue(GFPFields.IS_GENERATED_BY_DECOMPOSITION, "true");  
			log.info("decompose() ==> Decomposed Alarm: " + ea.toString());
			decomposedAlarms.add(ea);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public abstract void decomposition() throws Exception;

}
