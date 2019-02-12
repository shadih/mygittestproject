package com.att.gfp.decomposed.decomposedScenario;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hp.uca.common.callback.Callback;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.scenario.Scenario;

public class PurgeOldAlarms {
	
	private static final int ARGUMENT_1 = 0;
	private static final int ARGUMENT_2 = 1;


	private static final Logger LOG = LoggerFactory.getLogger(PurgeOldAlarms.class);
	
	private static final int NB_CALLBACK_ARGUMENTS = 2;

	public static void startPurgePeriod(Scenario scenario, Alarm alarm) {
		if (LOG.isTraceEnabled())
			LOG.trace("STARTING purge interval for alarm: " + alarm.getIdentifier());
		alarm.getVar().put("PurgingOnGoing", new Boolean(true));
		Callback callback = null;
		try {
			callback = buildpurgeCallback(scenario, alarm);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		scenario.addCallbackWatchdogItem(
				86400000,
				callback, false, "Purging alarm xxx", true,
				alarm);

	}
	
	
	public static Callback buildpurgeCallback(Scenario scenario,
			Alarm alarm)
			throws NoSuchMethodException {

		Class<?> partypes[] = new Class[NB_CALLBACK_ARGUMENTS];
		partypes[ARGUMENT_1] = Scenario.class;
		partypes[ARGUMENT_2] = Alarm.class;

		Object arglist[] = new Object[NB_CALLBACK_ARGUMENTS];
		arglist[ARGUMENT_1] = scenario;
		arglist[ARGUMENT_2] = alarm;
		Method method = PurgeOldAlarms.class.getMethod(
				"purgeCallback", partypes);

		Callback callback = new Callback(method, null, arglist);

		return callback;
	}
	 
	
	public static void purgeCallback(Scenario scenario, Alarm alarm) {
		if (LOG.isTraceEnabled())  
			LOG.trace("End of  purge interval for alarm :" + alarm.getIdentifier());    
	    scenario.getSession().retract(alarm); 
	}
}
