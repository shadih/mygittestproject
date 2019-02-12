/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

//import org.slf4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.SyslogAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.vp.pd.core.JuniperSyslog_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;

/**
 * @author df
 * 
 */
public final class SyslogLinkdown_Event extends JuniperSyslog_ProblemDefault implements
		ProblemInterface {
	//private Logger log = LoggerFactory.getLogger(SyslogLinkdown_Event.class);
	private Logger log = LoggerFactory.getLogger(SyslogLinkdown_Event.class);

	public SyslogLinkdown_Event() {
		super();
		setLog(LoggerFactory.getLogger(SyslogLinkdown_Event.class));
	}
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingCandidateAlarmCriteria() runs.");
		return true;
	}

	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a,Group group) throws Exception {
		boolean ret = true;
		SyslogAlarm ea = (SyslogAlarm) a;
		String eventKey = ea.getCustomFieldValue(GFPFields.EVENT_KEY);
		String targetName = null;
		Util.WDPool(ea, targetName, false, 0, getScenario());
		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", isMatchingSubAlarmCriteria() " + "[" + ret + "], group name = " + group.getName());
		return ret;
	}
}
