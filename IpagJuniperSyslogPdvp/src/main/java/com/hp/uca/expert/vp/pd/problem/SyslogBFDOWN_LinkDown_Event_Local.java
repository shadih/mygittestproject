/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.vp.pd.core.JuniperSyslog_BFDOWN_Default;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class SyslogBFDOWN_LinkDown_Event_Local extends JuniperSyslog_BFDOWN_Default implements
		ProblemInterface {
	

	private Logger log = LoggerFactory
			.getLogger(SyslogBFDOWN_LinkDown_Event_Local.class);

	public SyslogBFDOWN_LinkDown_Event_Local() {
		super();		
		setLog(LoggerFactory.getLogger(SyslogBFDOWN_LinkDown_Event_Local.class));
	}

	
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		//RemoteSavpnSiteID inside the managedObject
		//RemoteIP- from topology
		//LocalIP inside the managedObject
		//LocalSavpnSiteID - from topology
				
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();
		SyslogAlarm alarm = (SyslogAlarm) a;

		String localSiteId = alarm.getLocalDevice_SavpnSiteID();
		problemEntities.add(localSiteId);

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + alarm.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	}
}
	