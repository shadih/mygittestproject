package com.hp.uca.expert.vp.pd.problem;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import com.hp.uca.expert.vp.pd.services.PD_Service_Group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.SyslogAlarm;
// import com.att.gfp.ciena.cienaScenario.util.service_util;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.scenario.ScenarioThreadLocal;

public class CandidateAlarmProc implements Runnable {
	private static Logger log = LoggerFactory.getLogger(CandidateAlarmProc.class);
	public static ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(3);
	//public static Scenario scenario = ScenarioThreadLocal.getScenario();
	private Scenario scenario;
	private SyslogAlarm alarm;
	private AlarmDelegationType delegation;
	private String targetName;
	private boolean isDecomposed = false;

	public CandidateAlarmProc(SyslogAlarm alarm, AlarmDelegationType delegation, String targetName, boolean isDecomposed,
			Scenario scenario) 
	{
		this.alarm = alarm;
		this.delegation = delegation;
		this.targetName = targetName;
		this.isDecomposed = isDecomposed;
		this.scenario = scenario;
	}

	public void run()
	{
        ScenarioThreadLocal.setScenario(this.scenario);
        
        if (log.isTraceEnabled())
        	log.trace("CandidateAlarmProc() runs.  alarm = " + alarm.getIdentifier());

        if (!Util.isTriggerAlarm(scenario, alarm))
			Util.whereToSendThenSend(alarm, isDecomposed);
	}
}
