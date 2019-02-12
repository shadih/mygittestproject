package juniperES.NTDTicketCorrelation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipag.JunipertopoModel.IpagJuniperESTopoAccess;
import com.att.gfp.data.ipagJuniperAlarm.EnrichedNTDAlarm;
import com.att.gfp.data.ipagJuniperAlarm.JuniperESEnrichedAlarm;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.GFPUtil;
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
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.att.gfp.data.juniperES.helper.SendAlarm;

// Just an example of extension of analyzer
public class JuniperNTDExtendedLifeCycle extends LifeCycleAnalysis {

	//private static final Object PPORT = "PPORT";
	private static Logger log = LoggerFactory.getLogger(JuniperNTDExtendedLifeCycle.class);

	public JuniperNTDExtendedLifeCycle(Scenario scenario) {
		super(scenario);

		/*
		 * If needed more configuration, use the context.xml to define any beans
		 * that will be available here using scenario.getGlobals()
		 */
		// scenario.getGlobals()
	}

	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {
	
		if (log.isTraceEnabled())
			LogHelper.enter(log, "onAlarmCreationProcess()");  

		if (log.isDebugEnabled()) 
			log.debug("Incoming alarm: "+ alarm.getIdentifier());
		
		if(alarm.getPerceivedSeverity().equals(PerceivedSeverity.CLEAR)) {
			if (log.isDebugEnabled()) 
				log.debug("Orphan clear recieved, ignored... "+ alarm.getIdentifier());
			return null;
		}
		
		//EnrichedAlarm enrichedAlarm = null; // for junit testing
		EnrichedNTDAlarm enrichedNTDAlarm = null;
  
		try {
			enrichedNTDAlarm = new EnrichedNTDAlarm(alarm);
		} catch (Exception UnknowAlarmFieldException) {
			LogHelper.method(log, "onAlarmCreationProcess()", "Unable to create a enrichedJuniperAlarm for:" + alarm.getIdentifier());
		}			

		if(enrichedNTDAlarm != null){
			// for junit testing
			if (!(alarm  instanceof EnrichedAlarm)) {
				enrichedNTDAlarm.setDeviceType("PE");
				// increase aging to over 3 minutes for aging test
				//enrichedNTDAlarm.SetAgingAccumulativeTime(200000);
			}
			
			if(enrichedNTDAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA) == null || 
					enrichedNTDAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA).isEmpty())
				enrichedNTDAlarm.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, "NO");


			//String managedObjectClass = alarm.getOriginatingManagedEntity().split(" ")[0];
			//String managedObjectInstance = alarm.getOriginatingManagedEntity().split(" ")[1];

			// the filter only excepts PPORT alarms
			//if(managedObjectClass.equals(PPORT))
			// make the query into topology for local port information
			enrichedNTDAlarm.setPortCLFI(enrichedNTDAlarm.getCustomFieldValue(GFPFields.CLFI));
			//IpagJuniperESTopoAccess.getInstance().FetchLocalPPortLevelInformation( managedObjectInstance,enrichedNTDAlarm);

			//(alarm.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1"))
			if(enrichedNTDAlarm.getPortCLFI() != null && !enrichedNTDAlarm.getPortCLFI().isEmpty())
				IpagJuniperESTopoAccess.getInstance().FetchCLFIinfo(enrichedNTDAlarm);

			enrichedNTDAlarm.SetTimeIn();
		}
		
		
		if (log.isTraceEnabled()) 
			LogHelper.exit(log, "onAlarmCreationProcess()");

		return enrichedNTDAlarm;
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

				if (log.isTraceEnabled()) {
					log.trace("Severity of alarm in WM " + alarmInWM.getPerceivedSeverity() +
							" severity of new alarm" + newAlarm.getPerceivedSeverity());
				}

				if (newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR
						&& alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) {
					/*
					 * 
					 */

					if (log.isTraceEnabled()) {
						log.trace("Processing clear alarm.");
					}

					EnrichedAlarm a = (EnrichedAlarm) alarmInWorkingMemory;

					a.setSeverity(PerceivedSeverity.CLEAR);
					//a.setSeverity(4);
					a.setPerceivedSeverity(PerceivedSeverity.CLEAR);

					a.setCustomFieldValue(GFPFields.LAST_CLEAR_TIME, ""+System.currentTimeMillis()/1000);

					// we also have to set the sequence number to that of the clear alarm
					a.setCustomFieldValue(GFPFields.SEQNUMBER, newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER));
					a.setCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA, newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA) ); 

					if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED)))
						a.setCustomFieldValue(GFPFields. IS_PURGE_INTERVAL_EXPIRED, "YES"); 
					
					GFPUtil.populateEnrichedAlarmObj(a);

					// send clear to AM
					SendAlarm.send(a, SendAlarm.NTD);

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
				}
			}


			/*
			 * 
			 */
			if (!attributeChangesSC.isEmpty()) {
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
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM,
						UsualVar.AVCChange, attributeChangesAVC,
						System.currentTimeMillis());
				alarmInWM.setHasAVCChanged(true);
				ret = true;
			}

			if(ret)
				getScenario().getSession().retract(alarmInWorkingMemory);
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "onUpdateSpecificFieldsFromAlarm()",
					String.valueOf(ret));
		}
		return ret;
	}
	
}
