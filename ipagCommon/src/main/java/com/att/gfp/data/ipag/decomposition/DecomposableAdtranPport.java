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

import com.hp.uca.expert.alarm.Alarm;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;

@XmlRootElement
public class DecomposableAdtranPport extends DecomposablePport {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableAdtranPport.class);
	
	public DecomposableAdtranPport() {
		super();
	}
	
	public DecomposableAdtranPport(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableAdtranPport clone() throws CloneNotSupportedException {
		DecomposableAdtranPport newAlarm = (DecomposableAdtranPport) super.clone();				
		return newAlarm;
	}
	
	@Override
	public void decomposition() throws Exception {
		
		String pportInstanceName;
		
		if ("ADTRAN 800 SERIES".equalsIgnoreCase(getDeviceType())) {
			pportInstanceName = getOriginatingManagedEntity().split(" ")[1];
			log.info("pportInstanceName=" + pportInstanceName);
			getEvcsFromPPort(getNodeType(), pportInstanceName);			
		} else if ("ADTRAN 5000 SERIES".equalsIgnoreCase(getDeviceType()) &&
				"JUNIPER MX SERIES".equalsIgnoreCase(getRemoteDeviceType())) {
			if (getCustomFieldValue("EventKey").equals("50001/100/7")) {
				setAlarmDescription("TRUNK_DOWN");
				pportInstanceName = getOriginatingManagedEntity().split(" ")[1];
				log.info("pportInstanceName=" + pportInstanceName);
				getEvcsFromPPort(getNodeType(), pportInstanceName);
			}
		} else if ("ADTRAN 5000 SERIES".equalsIgnoreCase(getDeviceType()) &&
			!("JUNIPER MX SERIES".equalsIgnoreCase(getRemoteDeviceType()))) {
				setDeviceName(getRemoteDeviceName());
				setDeviceType(getRemoteDeviceType());
				setDeviceModel(getRemoteDeviceModel());  
				pportInstanceName = getRemotePportInstanceName();
				if(getRemoteDeviceType() != null) {
					getEvcsFromPPort(getNodeTypeFromDeviceType(getRemoteDeviceType()), 
						pportInstanceName);				  
				}
		}
	}	
}
