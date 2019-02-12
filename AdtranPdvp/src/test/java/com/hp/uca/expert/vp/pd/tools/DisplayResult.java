/**
 * 
 */
package com.hp.uca.expert.vp.pd.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.att.gfp.helper.GFPFields;
import com.att.gfp.helper.StandardFields;
import com.hp.uca.common.misc.Constants;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.group.Qualifier;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.vp.internal.ActiveValuePack;

/**
 * @author MASSE
 * 
 */
public class DisplayResult {

	private static final String TEST_ALARM_QUEUE_BEAN = "testAlarmQueue";

	Map<String, Alarm> pbAlarms = new HashMap<String, Alarm>();
	Map<String, Alarm> subPbAlarms = new HashMap<String, Alarm>();
	Map<String, Alarm> subAlarms = new HashMap<String, Alarm>();
	Map<String, Alarm> noGroupAlarms = new HashMap<String, Alarm>();
	Scenario scenario;
	StringBuffer buf = new StringBuffer();

	/**
	 * @param scenario
	 */
	public DisplayResult(Scenario scenario, Collection<Alarm> alarms) {
		this.scenario = scenario;

		buildMaps(alarms);

	}

	/**
	 * @param alarms
	 */
	public void buildMaps(Collection<Alarm> alarms) {
		for (Alarm alarm : alarms) {
			String pb = alarm
					.getCustomFieldValue(StandardFields.NAVIGATION_FIELD);
			Qualifier qualifier = Qualifier.Unknown;
			if (pb != null && !pb.isEmpty()) {
				qualifier = Qualifier.valueOf(pb);
			}

			switch (qualifier) {
			case ProblemAlarm:
				pbAlarms.put(alarm.getIdentifier(), alarm);
				break;
			case SubAlarm:
				subAlarms.put(alarm.getIdentifier(), alarm);
				break;
			case SubProblemAlarm:
				subPbAlarms.put(alarm.getIdentifier(), alarm);
				break;
			case Candidate:
			case No:
			case Trigger:
			case Unknown:
				noGroupAlarms.put(alarm.getIdentifier(), alarm);
				break;
			}
		}
	}

	/**
	 * 
	 */
	private void fillAlarmsSortedByQualifier() {

		fillQualifiers(Qualifier.ProblemAlarm, pbAlarms);
		if (!subPbAlarms.isEmpty()) {
			fillQualifiers(Qualifier.SubProblemAlarm, subPbAlarms);
		}
		fillQualifiers(Qualifier.SubAlarm, subAlarms);
		if (!noGroupAlarms.isEmpty()) {
			fillQualifiers(Qualifier.Unknown, noGroupAlarms);
		}

	}

	/**
	 * @param qualifier
	 * @param map
	 */
	private void fillQualifiers(Qualifier qualifier, Map<String, Alarm> map) {

		buf.append(Constants.GROUP_SEPARATOR_LONG.val());
		buf.append(Constants.GROUP_NAME_PREPEND.val());
		buf.append(qualifier.toString());
		if (map.isEmpty()) {
			buf.append("\nNo alarms in this category");
		} else {
			for (Alarm alarm : map.values()) {
				buf.append("\n");
				buf.append(String.format(
						"AlarmID:[%s] id:[%s] netcoolId:[%s] parent:[%s]",
						alarm.getCustomFieldValue("AlarmID"),
						alarm.getIdentifier(),
						alarm.getCustomFieldValue("NetcoolIdentifier"),
						alarm.getCustomFieldValue("ParentIdentifier")));
			}
		}

		buf.append(Constants.GROUP_SEPARATOR_LONG.val());

	}

	/**
	 * 
	 */
	private void fillGroups() {
		buf.append(Constants.GROUP_SEPARATOR_LONG.val());
		buf.append(Constants.GROUP_NAME_PREPEND.val());
		buf.append("GROUPS");
		Collection<Group> groups = scenario.getGroups().getAllGroups();
		for (Group group : groups) {
			buf.append(group.toFormattedString());
		}
	}

	/**
	 * 
	 */
	private void fillAllAlarms() {
		buf.append(Constants.GROUP_SEPARATOR_LONG.val());
		buf.append(Constants.GROUP_NAME_PREPEND.val());
		buf.append("ALARMS");
		Set<Alarm> alarms = scenario.getGroups().getAllAlarms();
		for (Alarm alarm : alarms) {
			buf.append(Constants.GROUP_ALT2_SEPARATOR.val());
			buf.append("\n");
			buf.append(alarm.toFormattedString());
		}
	}

	/**
	 * @param displayAlarms
	 * @return the StringBuffer
	 */
	public StringBuffer displayResult(boolean displayAlarms,
			boolean displayGroups) {

		if (displayGroups) {
			fillGroups();
		}
		if (displayAlarms) {
			fillAllAlarms();
		}
		buf.append("\n");
		buf.append("\n");
		fillAlarmsSortedByQualifier();
		//displayForwardedAlarms();

		return buf;
	}

	private void displayForwardedAlarms() {
		ActiveValuePack activeValuePack = (ActiveValuePack) scenario
				.getValuePack();
		List<Alarm> forwardedAlarms = (List<Alarm>) activeValuePack
				.getApplicationContext().getBean(TEST_ALARM_QUEUE_BEAN);
		if (forwardedAlarms != null) {
			buf.append(Constants.GROUP_ALT2_SEPARATOR.val());
			buf.append(Constants.GROUP_ALT2_NAME_PREPEND.val());
			buf.append("forwardedAlarms\n");
			for (Alarm alarm : forwardedAlarms) {
				buf.append(String.format(
						"[%s][sev:%s][parent:%s][prior:%s]",
						alarm.getIdentifier(),
						alarm.getPerceivedSeverity(),
						alarm.getCustomFieldValue("ParentIdentifier"),
						alarm.getCustomFieldValue("PriorParentIdentifier")));
				buf.append("\n");
			} 
			buf.append(Constants.GROUP_ALT2_SEPARATOR.val());
		}
	}

}
