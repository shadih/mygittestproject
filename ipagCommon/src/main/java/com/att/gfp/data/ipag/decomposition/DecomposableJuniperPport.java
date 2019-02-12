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
public class DecomposableJuniperPport extends DecomposablePport {

	private static final long serialVersionUID = -5235137849600943223L;

	private static Logger log = LoggerFactory.getLogger(DecomposableJuniperPport.class);
	
	
	public DecomposableJuniperPport() {
		super();
	}
	
	public DecomposableJuniperPport(EnrichedAlarm alarm) throws Exception {
		
		super(alarm);
		
	}

	@Override
	public DecomposableJuniperPport clone() throws CloneNotSupportedException {
		DecomposableJuniperPport newAlarm = (DecomposableJuniperPport) super.clone();				
		return newAlarm;
	}
	
	public void decomposition() throws Exception {
		
		setDeviceName(getRemoteDeviceName());
		setDeviceType(getRemoteDeviceType());   
		setDeviceModel(getRemoteDeviceModel());
		if(getRemoteDeviceType() != null && !getRemoteDeviceType().isEmpty() 
				&& getRemotePportInstanceName() != null && !getRemotePportInstanceName().isEmpty() ) {
			 getEvcsFromDevicePort(getNodeTypeFromDeviceType(getRemoteDeviceType()), getRemotePportInstanceName().split("/")[0]);	   
		}
	}	
}
