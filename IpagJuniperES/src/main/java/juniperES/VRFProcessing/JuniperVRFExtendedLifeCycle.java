package juniperES.VRFProcessing;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.juniperES.helper.SendAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.alarm.AlarmUpdater;
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.lifecycle.LifeCycleAnalysis;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

public class JuniperVRFExtendedLifeCycle extends LifeCycleAnalysis{

	public JuniperVRFExtendedLifeCycle(Scenario scenario) {
		super(scenario);
		// TODO Auto-generated constructor stub
	}

	//private static final Object PPORT = "PPORT";
	private static Logger log = LoggerFactory.getLogger(JuniperVRFExtendedLifeCycle.class);


	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {
		LogHelper.enter(log, "onAlarmCreationProcess()");

		EnrichedAlarm enrichedAlarm = null; // for junit testing
				
		// this entire method is needed because we can't inject an enriched alarm for junit
		if (!(alarm  instanceof EnrichedAlarm)) {
			// for junit testing
			try {
				enrichedAlarm = new EnrichedAlarm(alarm);
				
				if(alarm.getCustomFieldValue("sm-event-text").equals("noupdate"))
					enrichedAlarm.setRemoteDeviceType("JUNIPER MX SERIES");
				else
					enrichedAlarm.setRemoteDeviceType("thisandthat");
				
				enrichedAlarm.setAlarmTargetExists(true);

				LogHelper.exit(log, "onAlarmCreationProcess() - updated for JUnit");

				return enrichedAlarm;

			} catch (Exception UnknowAlarmFieldException) {
				LogHelper.method(log, "onAlarmCreationProcess()", "Unable to create a enrichedJuniperAlarm for:" + alarm.getIdentifier());
			}
			
		} else {
			enrichedAlarm = (EnrichedAlarm) alarm;
			LogHelper.exit(log, "onAlarmCreationProcess() - not updated for JUnit");
		}
		
	    if(!enrichedAlarm.getRemoteDeviceType().equals("JUNIPER MX SERIES") && 
	    		!enrichedAlarm.getRemoteDeviceType().equals("ADTRAN 5000 SERIES") &&
	    		!enrichedAlarm.getRemoteDeviceType().equals("CIENA EMUX")) {
	    		    
	    	String pportInstance = alarm.getOriginatingManagedEntity().split(" ")[1];
    
	    	String status = "true";
    
	    	if(alarm.getPerceivedSeverity().equals(PerceivedSeverity.CLEAR))
	    		status = "false";
    	
	    	UpdateVRFStatus.getInstance().updateJuniperLinkDownStatusByPport(pportInstance, null, status);

	    	SendAlarm.send(enrichedAlarm, SendAlarm.VFR);
	    } 
	    
	    // no need to save this alarm locally
	    SendAlarm.send(enrichedAlarm, SendAlarm.VFR);
		return null;
		
	}

	/**
	 * We do not receive AVC (attribute value chang) & SC (state change) events. So we need to 
	 * simulate the
	 * generation of this information by manually updates the Attributes Changes
	 * of the alarm in the Working Memory.
	 * <p>
	 * <u>List of attributes managed:</u>
	 * <li>perceivedSeverity (impacting the networkState severity is CLEAR)</li>
	 * <li>serverSerial</li>
	 * </p>
	 * <hr>
	 * 
	 * @see com.hp.uca.expert.lifecycle.CommonLifeCycle#onUpdateSpecificFieldsFromAlarm
	 *      (com.hp.uca.expert.alarm.Alarm, com.hp.uca.expert.alarm.AlarmCommon)
	 */
	@Override
	public boolean onUpdateSpecificFieldsFromAlarm(Alarm newAlarm,
			AlarmCommon alarmInWorkingMemory) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "onUpdateSpecificFieldsFromAlarm()", newAlarm.getIdentifier());
		}
					
		// for now just handling the severity
		boolean ret = false;
		if (alarmInWorkingMemory instanceof Alarm) {
			Alarm alarmInWM = (Alarm) alarmInWorkingMemory;
			
			List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>();
			List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
			AttributeChange attributeChange = null;

			/*
			 * Updating the Perceived Severity of the alarm in Working memory
			 * only if the Alarm received is different.
			 */
			if (newAlarm.getPerceivedSeverity() != alarmInWM
					.getPerceivedSeverity()) {

				if (newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR
						&& alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) {

					if (log.isTraceEnabled()) {
						LogHelper.method(log, "onUpdateSpecificFieldsFromAlarm() - This alarm is cleared now:", newAlarm.getIdentifier());
					}
					
					// set the EVC because this alarm is cleared.
			        String pportInstance = alarmInWM.getOriginatingManagedEntity().split(" ")[1];			        
			 		UpdateVRFStatus.getInstance().updateJuniperLinkDownStatusByPport(pportInstance, null, "false");
			 
					// forward the clear onto the next stage
			 		if(newAlarm instanceof EnrichedAlarm)
			 			SendAlarm.send((EnrichedAlarm) newAlarm, SendAlarm.VFR);
			 		else {
			 			EnrichedAlarm enrichedAlarm = null;
						try {
							enrichedAlarm = new EnrichedAlarm(newAlarm);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			 			SendAlarm.send(enrichedAlarm, SendAlarm.VFR);
			 		}
					
					/*
					 * 
					 */
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange
							.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(alarmInWM.getNetworkState()
							.toString());
					attributeChangesSC.add(attributeChange);

					/*
					 * 
					 */
	
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(PerceivedSeverity.CLEAR
							.toString());
					attributeChange.setOldValue(alarmInWM
							.getPerceivedSeverity().toString());
					attributeChangesAVC.add(attributeChange);

					// change the clearence time of the alarm.
					attributeChange = new AttributeChange();
					attributeChange.setName(GFPFields.LAST_CLEAR_TIME);
					attributeChange.setNewValue(String.valueOf(System.currentTimeMillis()/1000));
					attributeChange.setOldValue(alarmInWM.getCustomFieldValue(GFPFields.LAST_CLEAR_TIME));
					attributeChangesAVC.add(attributeChange);
				} else {

					/*
					 * 
					 */
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(newAlarm.getPerceivedSeverity()
							.toString());
					attributeChange.setOldValue(alarmInWM
							.getPerceivedSeverity().toString());
					attributeChangesAVC.add(attributeChange);
				}
			}

	
			/*
			 * 
			 */
			if (!attributeChangesSC.isEmpty()) {
				
				if (log.isTraceEnabled()) {
					LogHelper.method(log, "onUpdateSpecificFieldsFromAlarm() - Sending a Status change:", newAlarm.getIdentifier());
				}

				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM,
						UsualVar.StateChange, attributeChangesSC,
						System.currentTimeMillis());
				alarmInWM.setHasStateChanged(true);
				ret = true;
			}

			/*
			 * 
			 */
			if (!attributeChangesAVC.isEmpty()) {
				
				if (log.isTraceEnabled()) {
					LogHelper.method(log, "onUpdateSpecificFieldsFromAlarm() - Sending an attribute change:", newAlarm.getIdentifier());
				}

				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM,
						UsualVar.AVCChange, attributeChangesAVC,
						System.currentTimeMillis());
				alarmInWM.setHasAVCChanged(true);
				ret = true;
			}

		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onUpdateSpecificFieldsFromAlarm()",
					String.valueOf(ret));
		}
		return ret;
	}


	
	
	
	
}
