package com.att.gfp.decomposed.decomposedScenario;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.alarm.AlarmCommon;
import com.hp.uca.expert.alarm.AlarmUpdater;
import com.hp.uca.expert.alarm.AlarmUpdater.UsualVar;
import com.hp.uca.expert.lifecycle.LifeCycleAnalysis;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;


public class DecomposedExtendedLifeCycle extends LifeCycleAnalysis {
	Scenario scenario;
	private static Logger log = LoggerFactory.getLogger(DecomposedExtendedLifeCycle.class);
	private static ApplicationContext context = null;
	public DecomposedExtendedLifeCycle(Scenario passedscenario) {
		super(passedscenario);
		this.scenario = passedscenario;
		if (context == null)
		{
			if (log.isTraceEnabled())
				log.trace("Get Application Context.");
			context = scenario.getVPApplicationContext();
		}

		scenario.getGlobals();
	}

	@Override
	public AlarmCommon onAlarmCreationProcess(Alarm alarm) {
		try {
			if (log.isTraceEnabled()) {
				String axml = alarm.toXMLString();
				axml = axml.replaceAll("\\n", " ");
				log.trace("Incoming alarm: "+axml);
			}
			if (alarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR)
			{
				log.info("Dropping the clear alarm as there is no corresponding active alarm.");
				return null;
			}
			DecomposedAlarm decompAlarm = new DecomposedAlarm(alarm);
			return decompAlarm;
		} catch (Exception e) {
			log.info("Dropped this alarm = " + alarm.getIdentifier() + ",  as enrichment failed. ", e);
			return null;
		}
	}

	@Override
	public boolean onUpdateSpecificFieldsFromAlarm(Alarm newAlarm, AlarmCommon alarmInWorkingMemory) {

		if (log.isTraceEnabled()) {
			String axml = newAlarm.toXMLString();
			axml = axml.replaceAll("\\n", " ");
			log.trace("Incoming updated alarm: "+axml);
		}
		DecomposedAlarm decomposedAlarm = null;
		try {
			decomposedAlarm = new DecomposedAlarm(newAlarm);
		} catch (Exception e) {
			log.info("failed to create DecomposedAlarm: "+ e);
			return true;
		}

		int severity = decomposedAlarm.getSeverity();
		// for now just handling the severity
		boolean ret = false;
		Alarm alarmInWM = (Alarm) alarmInWorkingMemory;

		List<AttributeChange> attributeChangesSC = new ArrayList<AttributeChange>();
		List<AttributeChange> attributeChangesAVC = new ArrayList<AttributeChange>();
		AttributeChange attributeChange = null;

		if (alarmInWorkingMemory instanceof Alarm) {
			if (newAlarm.getPerceivedSeverity() != alarmInWM.getPerceivedSeverity()) {
				if (newAlarm.getPerceivedSeverity() == PerceivedSeverity.CLEAR && alarmInWM.getNetworkState() == NetworkState.NOT_CLEARED) {
					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.NETWORK_STATE);
					attributeChange.setNewValue(NetworkState.CLEARED.toString());
					attributeChange.setOldValue(alarmInWM.getNetworkState().toString());
					attributeChangesSC.add(attributeChange);

					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(PerceivedSeverity.CLEAR.toString());
					attributeChange.setOldValue(alarmInWM.getPerceivedSeverity().toString());
					attributeChangesAVC.add(attributeChange);
					attributeChange = new AttributeChange();
					attributeChange.setName(GFPFields.LAST_CLEAR_TIME);
					attributeChange.setNewValue(String.valueOf(System.currentTimeMillis()/1000));
					attributeChange.setOldValue(alarmInWM.getCustomFieldValue(GFPFields.LAST_CLEAR_TIME));
					attributeChangesAVC.add(attributeChange);
				} else {

					attributeChange = new AttributeChange();
					attributeChange.setName(StandardFields.PERCEIVED_SEVERITY);
					attributeChange.setNewValue(newAlarm.getPerceivedSeverity().toString());
					attributeChange.setOldValue(alarmInWM.getPerceivedSeverity().toString());
					attributeChangesAVC.add(attributeChange);
				}
			}

			if (!attributeChangesSC.isEmpty()) {
				ret = true;
			}

			if (!attributeChangesAVC.isEmpty()) {
				ret = true;
			}
		}

		if (severity == 4)
		{
			DecomposedAlarm a = (DecomposedAlarm) alarmInWorkingMemory;
			boolean isGenByUCA = false;
			boolean isPurgeExp = false;
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_GENERATED_BY_UCA)))
				isGenByUCA = true;
			if("YES".equalsIgnoreCase(newAlarm.getCustomFieldValue(GFPFields.IS_PURGE_INTERVAL_EXPIRED)))
				isPurgeExp = true;
			AlarmState state = a.getAlarmState();
			if (ret == true && state == AlarmState.sent)
			{
				if (log.isTraceEnabled())
					log.trace("Send the clear alarm.");
				// no need to call populateEnrichedAlarmObj()
				Util.sendClear(a.getIdentifier(), newAlarm.getCustomFieldValue(GFPFields.SEQNUMBER), isGenByUCA, isPurgeExp);
			}
			if (!attributeChangesSC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM, UsualVar.StateChange, attributeChangesSC, System.currentTimeMillis());
				alarmInWM.setHasStateChanged(true);
				ret = true;
			}

			if (!attributeChangesAVC.isEmpty()) {
				AlarmUpdater.updateAlarmFromAttributesChanges(alarmInWM, UsualVar.AVCChange, attributeChangesAVC, System.currentTimeMillis());
				alarmInWM.setHasAVCChanged(true);
				ret = true;
			}
			if(ret)
			{
				if (log.isTraceEnabled())
					log.trace("retract alarm in WM: " + alarmInWorkingMemory.getIdentifier());
				getScenario().getSession().retract(alarmInWorkingMemory);
			}
		}
		return ret;
	}
}
