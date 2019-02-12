package com.att.gfp.ciena.cienaPD;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
// import com.att.gfp.ciena.cienaScenario.util.service_util;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;

public class CandidateAlarmProc implements Runnable {
	private static Logger log = LoggerFactory.getLogger(CandidateAlarmProc.class);
	public static ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(3);
	// public static Scenario scenario = ScenarioThreadLocal.getScenario();
        private Scenario scenario;

	private CienaAlarm alarm;
	private AlarmDelegationType delegation;
	private String targetName;
	private boolean isDecomposed = false;

	public CandidateAlarmProc(Scenario scenario, CienaAlarm alarm, AlarmDelegationType delegation, String targetName, boolean isDecomposed) 
	{
		this.scenario = scenario;
		this.alarm = alarm;
		this.delegation = delegation;
		this.targetName = targetName;
		this.isDecomposed = isDecomposed;
	}
	public void run()
	{
		log.info("CandidateAlarmProc() runs.  alarm = " + alarm.getIdentifier());
		// below is needed to create a thread in UCA
		ScenarioThreadLocal.setScenario(this.scenario);

		if (!Util.isTriggerAlarm(scenario, alarm))
			Util.sendAlarm(alarm,  delegation, targetName, isDecomposed);
	}
}
