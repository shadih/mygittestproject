package com.att.gfp.data.ipag.decomposition;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.x733alarm.CustomField;
import com.hp.uca.expert.x733alarm.CustomFields;

 
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;


// Just an example of extension of analyzer
public class Decomposer {

	private static Logger log = LoggerFactory.getLogger(Decomposer.class);
	
	public Decomposer() {
	}

	public static List<EnrichedAlarm> decompose(EnrichedAlarm alarm) throws Exception {

		log.info("");
		log.info("***** Alarm.identifier=" + alarm.getIdentifier() + " *****");

		EnrichedAlarm enrichedAlarm = new EnrichedAlarm(alarm);
		enrichedAlarm.setDeviceType(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_TYPE));
		enrichedAlarm.setRemoteDeviceType(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_TYPE));
		enrichedAlarm.setRemoteDeviceName(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_NAME));
		enrichedAlarm.setRemoteDeviceModel(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_DEVICE_MODEL));
		enrichedAlarm.setRemotePportInstanceName(enrichedAlarm.getCustomFieldValue(GFPFields.REMOTE_PPORT_INSTANCE_NAME));
		enrichedAlarm.setDeviceName(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_NAME));
		enrichedAlarm.setDeviceModel(enrichedAlarm.getCustomFieldValue(GFPFields.DEVICE_MODEL));
		
		CustomFields newCustomFields = new CustomFields();
		newCustomFields.getCustomField().addAll(enrichedAlarm.getCustomFields().getCustomField());
		for(CustomField s : newCustomFields.getCustomField()) { 
			if(GFPFields.DEVICE_TYPE.equalsIgnoreCase(s.getName()) ||  
					GFPFields.REMOTE_DEVICE_TYPE.equalsIgnoreCase(s.getName()) || 
					GFPFields.REMOTE_DEVICE_NAME.equalsIgnoreCase(s.getName()) ||
					GFPFields.REMOTE_PPORT_INSTANCE_NAME.equalsIgnoreCase(s.getName()) ||
					GFPFields.DEVICE_NAME.equalsIgnoreCase(s.getName()) ||
					GFPFields.DEVICE_MODEL.equalsIgnoreCase(s.getName()) ||
					GFPFields.REMOTE_DEVICE_MODEL.equalsIgnoreCase(s.getName())) {
	    		log.info("removing the custom fields created for decomposition " +newCustomFields.getCustomField().remove(s));
			}
		}
		enrichedAlarm.setCustomFields(newCustomFields); 
		
		DecomposableAlarm decomposableAlarm = null;
		List<EnrichedAlarm> decomposedAlarms = new ArrayList<EnrichedAlarm>();
		if(alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR) {
			log.trace("decompose: No decmposition because the alarm severity is CLEAR " + alarm.getIdentifier() + "|{" + alarm.getCustomFieldValue(GFPFields.SEQNUMBER) + "}");
			return decomposedAlarms;  
		}    
		
		if (alarm.getCustomFieldValue("EventKey").equals("50001/100/1") ||
			alarm.getCustomFieldValue("EventKey").equals("50001/100/42") ||
			alarm.getCustomFieldValue("EventKey").equals("50001/100/43") ||
			alarm.getCustomFieldValue("EventKey").equals("50001/100/48") ||
			alarm.getCustomFieldValue("EventKey").equals("50001/100/7")) {
			decomposableAlarm = new DecomposableAdtranPport(enrichedAlarm);
		} else if (alarm.getCustomFieldValue("EventKey").equals("50001/100/45")) {
			decomposableAlarm = new DecomposableAdtranLport(enrichedAlarm);
		} else if (alarm.getCustomFieldValue("EventKey").equals("50001/100/20")) {
			decomposableAlarm = new DecomposableAdtranCard(enrichedAlarm);
		} else if (alarm.getCustomFieldValue("EventKey").equals("50001/100/39")) {
			decomposableAlarm = new DecomposableAdtranDevice(enrichedAlarm);
		} else if (alarm.getCustomFieldValue("EventKey").equals("50003/100/1") ||
			alarm.getCustomFieldValue("EventKey").equals("50004/1/10")) {
			decomposableAlarm = new DecomposableJuniperPport(enrichedAlarm);
		} else if (alarm.getCustomFieldValue("EventKey").equals("50003/100/6") ||
			alarm.getCustomFieldValue("EventKey").equals("50003/100/7")) {
			decomposableAlarm = new DecomposableJuniperVpn(enrichedAlarm);
		} else if (alarm.getCustomFieldValue("EventKey").equals("50002/100/1") ||
			alarm.getCustomFieldValue("EventKey").equals("50002/100/19") || 
			alarm.getCustomFieldValue("EventKey").equals("50002/100/21") ||
			alarm.getCustomFieldValue("EventKey").equals("50002/100/22")) {
			decomposableAlarm = new DecomposableCienaPport(enrichedAlarm);			
		} else if (alarm.getCustomFieldValue("EventKey").equals("50002/100/14")) {
			decomposableAlarm = new DecomposableCienaDevice(enrichedAlarm);			
		}
		
		if (decomposableAlarm == null) {
			log.error("No decomposition defined for " + alarm.getCustomFieldValue("EventKey"));
		} else {
			decomposableAlarm.decomposition();
			decomposedAlarms = decomposableAlarm.getDecomposedAlarms();
		}
		
		return decomposedAlarms;
	}

}
