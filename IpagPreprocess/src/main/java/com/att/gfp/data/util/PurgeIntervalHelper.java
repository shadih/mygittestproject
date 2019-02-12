package com.att.gfp.data.util;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagPreprocess.preprocess.EnrichedPreprocessAlarm;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.scenario.Scenario;

public class PurgeIntervalHelper {
	
	private static final String PROBLEM_WHEN_REQUESTING_ENRICHMENT_ACTION = "Problem when requesting Enrichment Action";
	private static final int ARGUMENT_1 = 0;
	private static final int ARGUMENT_2 = 1;
	private static final int ARGUMENT_3 = 2;
	private static final int ARGUMENT_4 = 3;

	private static final Logger LOG = LoggerFactory.getLogger(PurgeIntervalHelper.class);
	
	private static final int NB_CALLBACK_ARGUMENTS = 2;

	public static void startPurgePeriod(Scenario scenario, EnrichedPreprocessAlarm alarm) {

		LOG.info("STARTING purge interval for alarm: " + alarm.getIdentifier());
		alarm.getVar().put("PurgingOnGoing", new Boolean(true));
		Callback callback = null;
		try {
			callback = buildenrichmentCallback(scenario, alarm);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		if (alarm.getIdentifier().contains("Chronic")){
			LOG.trace("Setting purge interval to 6 min");
			scenario.addCallbackWatchdogItem(360000,
					callback, false, "Purging alarm xxx", true,
					alarm);
		}
		else {
			
		scenario.addCallbackWatchdogItem(
				Long.parseLong(alarm
						.getCustomFieldValue(com.att.gfp.data.util.NetcoolFields.PURGE_INTERVAL)),
				callback, false, "Purging alarm xxx", true,
				alarm);
		}

	}
	
	
	public static Callback buildenrichmentCallback(Scenario scenario,
			EnrichedPreprocessAlarm alarm)
			throws NoSuchMethodException {

		Class<?> partypes[] = new Class[NB_CALLBACK_ARGUMENTS];
		partypes[ARGUMENT_1] = Scenario.class;
		partypes[ARGUMENT_2] = EnrichedPreprocessAlarm.class;

		Object arglist[] = new Object[NB_CALLBACK_ARGUMENTS];
		arglist[ARGUMENT_1] = scenario;
		arglist[ARGUMENT_2] = alarm;
		Method method = PurgeIntervalHelper.class.getMethod(
				"enrichmentCallback", partypes);

		Callback callback = new Callback(method, null, arglist);

		return callback;
	}
	 
	
	public static void enrichmentCallback(Scenario scenario, EnrichedPreprocessAlarm alarm) {
		 LOG.info("End of  purge interval for alarm :" + alarm.getIdentifier()); 
	     service_util.sendClearAlarmToCascadingVPs(scenario, alarm);
	     alarm.setAlarmState(AlarmState.sent);     
	     scenario.getSession().retract(alarm); 
	}
}
