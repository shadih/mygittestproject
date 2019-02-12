package com.hp.uca.expert.vp.pd.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.vp.pd.config.Booleans;
import com.hp.uca.expert.vp.pd.config.Longs;
import com.hp.uca.expert.vp.pd.config.MainPolicy;
import com.hp.uca.expert.vp.pd.config.ProblemPolicy;
import com.hp.uca.expert.vp.pd.config.Strings;
import com.hp.uca.expert.vp.pd.config.TimeWindow;
import com.hp.uca.expert.vp.pd.config.TimeWindowMode;
import com.hp.uca.expert.vp.pd.interfaces.GeneralBehaviorInterface;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;
import com.hp.uca.expert.vp.pd.interfaces.SupportedActions;
import com.hp.uca.expert.vp.pd.interfaces.SupportedTroubleTicketActions;
import com.hp.uca.expert.vp.pd.services.PD_Service_Action;
import com.hp.uca.expert.vp.pd.services.PD_Service_Lifecycle;
import com.hp.uca.expert.vp.pd.services.PD_Service_Navigation;
import com.hp.uca.expert.vp.pd.services.PD_Service_ProblemAlarm;
import com.hp.uca.expert.vp.pd.services.PD_Service_TroubleTicket;
import com.hp.uca.expert.vp.pd.services.PD_Service_Util;
import com.hp.uca.expert.x733alarm.AlarmType;
import com.hp.uca.expert.x733alarm.AttributeChange;
import com.hp.uca.expert.x733alarm.NetworkState;
import com.hp.uca.expert.x733alarm.OperatorState;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;
import com.hp.uca.expert.x733alarm.ProblemState;
import com.hp.uca.mediation.action.client.Action;

/**
 * Class used to define Default behavior of Customized Problems. It is not
 * needed to override methods in Customization if the default behavior
 * represented by this class is matching the needs.
 * 
 * 
 * @author MASSE
 * 
 */
@SuppressWarnings("all")
public class ProblemDefault implements ProblemInterface {

	/**
	 * Default Operator Note value
	 */
	protected static final String PROBLEM_DEFAULT_OPERATOR_NOTE_TEXT = "Problem Default OperatorNote text...";

	/**
	 * Default Additional Text value
	 */
	protected static final String PROBLEM_DEFAULT_ADDITIONAL_TEXT = "Problem Default additional text...";

	/**
	 * Logger used to trace this component
	 */
	private Logger log = LoggerFactory.getLogger(ProblemDefault.class);

	/**
	 * The {@linkplain ProblemPolicy} instance used to configure the Problem
	 * Customization
	 */
	private ProblemPolicy problemPolicy;

	/**
	 * 
	 */
	private MainPolicy mainPolicy;

	/**
	 * 
	 */
	private Map<String, SupportedActions> supportedActionsMap;

	/**
	 * 
	 */
	private Map<String, SupportedTroubleTicketActions> supportedTroubleTicketActionsMap;

	/**
	 * Reference to the {@linkplain Scenario} running this Problem
	 */
	private Scenario scenario;

	/**
	 * 
	 */
	private ProblemContext problemContext;

	private Booleans booleans;
	private Longs longs;
	private Strings strings;

	/**
	 * Default constructor. Mainly build the {@linkplain #problemPolicy}.
	 */
	public ProblemDefault() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#getProblemPolicy()
	 */
	@Override
	public final ProblemPolicy getProblemPolicy() {
		return problemPolicy;
	}

	/**
	 * 
	 * @param problemPolicy
	 *            {@linkplain com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#getProblemPolicy()}
	 */
	public void setProblemPolicy(ProblemPolicy problemPolicy) {
		this.problemPolicy = problemPolicy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#getMainPolicy()
	 */
	@Override
	public final MainPolicy getMainPolicy() {
		return this.mainPolicy;
	}

	/**
	 * @param mainPolicy
	 */
	public final void setMainPolicy(MainPolicy mainPolicy) {
		this.mainPolicy = mainPolicy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#getProblemContext()
	 */
	@Override
	public ProblemContext getProblemContext() {
		return problemContext;
	}

	/**
	 * @param problemContext
	 *            the problemContext to set
	 */
	public void setProblemContext(ProblemContext problemContext) {
		this.problemContext = problemContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#getActionsFactory()
	 */
	@Override
	public final Map<String, SupportedActions> getSupportedActions() {
		return this.supportedActionsMap;
	}

	/**
	 * @param actionsFactory
	 *            the actionsFactory to set
	 */
	public final void setSupportedActions(
			Map<String, SupportedActions> supportedActions) {
		this.supportedActionsMap = supportedActions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#
	 * getSupportedTroubleTicketActions()
	 */
	@Override
	public final Map<String, SupportedTroubleTicketActions> getSupportedTroubleTicketActions() {
		return this.supportedTroubleTicketActionsMap;
	}

	/**
	 * @param actionsFactory
	 *            the actionsFactory to set
	 */
	public final void setSupportedTroubleTicketActions(
			Map<String, SupportedTroubleTicketActions> supportedTroubleTicketActions) {
		this.supportedTroubleTicketActionsMap = supportedTroubleTicketActions;
	}

	/*
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#getScenario()
	 */
	@Override
	public final Scenario getScenario() {
		return scenario;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.problem.BasicProblemInterface#setScenario(com.hp.uca
	 * .expert.scenario.Scenario)
	 */
	@Override
	public final void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#onComputeProblemEntity
	 * (com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {
		/*
		 * Default behavior: get first level of managing entity
		 */
		String pbEntity = null;
		List<String> problemEntities = new ArrayList<String>();

		// // TODO remove this in an example of Custom
		// if (a.getOriginatingManagedEntityStructure() != null) {
		// ClassInstance firstLevel = a.getOriginatingManagedEntityStructure()
		// .getClassInstance().get(FIRST_LEVEL);
		// if (firstLevel != null) {
		// pbEntity = firstLevel.getClazz() + " "
		// + firstLevel.getInstance();
		// }
		// }

		pbEntity = a.getOriginatingManagedEntity();

		problemEntities.add(pbEntity);

		if (log.isTraceEnabled()) {
			LogHelper.method(log, "computeProblemEntity()",
					problemEntities.toString());
		}
		return problemEntities;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#compareProblemEntity
	 * (com.hp.uca.expert.alarm.Alarm, com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean compareProblemEntity(Alarm a, Group group,
			String newAlarmProblemEntity) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "compareProblemEntity()",
					group.getProblemEntity() + "/" + newAlarmProblemEntity);
		}

		boolean ret = false;
		String groupProblemEntity;

		if (!getProblemPolicy().isSameGroupForAllProblemEntities()) {

			groupProblemEntity = group.getProblemEntity();
			if (newAlarmProblemEntity.contains(groupProblemEntity)
					|| groupProblemEntity.contains(newAlarmProblemEntity)) {
				ret = true;
			}
		} else {

			for (String fullProblemKey : group.getFullProblemKeys()) {
				groupProblemEntity = PD_Service_Util.extractSubString(
						fullProblemKey, PD_AlarmRecognition.KEY_BEGIN,
						PD_AlarmRecognition.KEY_END);

				if (newAlarmProblemEntity.contains(groupProblemEntity)
						|| groupProblemEntity.contains(newAlarmProblemEntity)) {
					ret = true;
					break;
				}

			}

		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "compareProblemEntity()", String.valueOf(ret));
		}
		return ret;
	}

	/**
	 * @param a
	 * @param group
	 * @return
	 * @throws Exception
	 */
	public final boolean compareProblemEntities(Alarm a, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "compareProblemEntities()", a.getIdentifier()
					+ "/" + group.getName());
		}

		boolean ret = false;

		List<String> newAlarmProblemEntities = computeProblemEntity(a);

		if (newAlarmProblemEntities != null
				&& newAlarmProblemEntities.isEmpty()) {
			newAlarmProblemEntities.add(a.getOriginatingManagedEntity());
		}

		if (newAlarmProblemEntities != null
				&& !newAlarmProblemEntities.isEmpty()) {

			for (String newAlarmProblemEntity : newAlarmProblemEntities) {
				newAlarmProblemEntity = newAlarmProblemEntity.toLowerCase();

				if (compareProblemEntity(a, group, newAlarmProblemEntity)) {
					ret = true;
					break;
				}

			}
		}
		if (log.isTraceEnabled()) {
			LogHelper
					.exit(log, "compareProblemEntities()", String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#computeProblemKey
	 * (com.hp.uca.expert.vp.pd.core.Problem, com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public String computeProblemKey(Alarm a, String problemEntity)
			throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.method(log, "computeProblemKey()", problemEntity);
		}
		return problemEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#
	 * isMatchingTriggerAlarmCriteria(com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public boolean isMatchingTriggerAlarmCriteria(Alarm a) throws Exception {
		boolean ret = true;

		if (log.isTraceEnabled()) {
			LogHelper.method(log, "isTriggerAlarm()", String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#
	 * isMatchingProblemAlarmCriteria(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean isMatchingProblemAlarmCriteria(Alarm a, Group group)
			throws Exception {
		boolean ret = true;

		if (log.isTraceEnabled()) {
			LogHelper.method(log, "isProblemAlarm()", String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#
	 * isMatchingCandidateAlarmCriteria(com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public boolean isMatchingCandidateAlarmCriteria(Alarm a) throws Exception {
		boolean ret = true;

		if (log.isTraceEnabled()) {
			LogHelper.method(log, "isCandidateAlarm()", String.valueOf(ret));
		}

		/*
		 * All alarms that are matching the problem (through filter) are
		 * considered as candidate by default
		 */
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmRecognition#
	 * isMatchingSubAlarmCriteria(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean isMatchingSubAlarmCriteria(Alarm a, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isMatchingSubAlarmCriteria()",
					a.getIdentifier());
		}

		boolean ret = true;

		Alarm pbAlarm = group.getProblemAlarm();
		if ((pbAlarm != null)
				&& (pbAlarm.getOperatorState() == OperatorState.TERMINATED || pbAlarm
						.getNetworkState() == NetworkState.CLEARED)) {
			/*
			 * Accept this alarm only when the ProblemAlarm is Not-Terminated
			 * AND Not-Cleared
			 */
			ret = false;

		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isMatchingSubAlarmCriteria()",
					String.valueOf(ret));
		}

		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.problem.BasicProblemInterface#getLog()
	 */
	@Override
	public Logger getLog() {
		return log;
	}

	/**
	 * @param log
	 *            {@linkplain #log}
	 */
	public final void setLog(Logger log) {
		this.log = log;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * isAllCriteriaForProblemAlarmCreation(com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean isAllCriteriaForProblemAlarmCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isAllCriteriaForProblemAlarmCreation()",
					group.getName());
		}

		boolean ret = PD_Service_ProblemAlarm
				.isItTimeForProblemAlarmCreation(group);

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForProblemAlarmCreation()",
					String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateReferenceAlarm(com.hp.uca.expert.group.Group)
	 */
	@Override
	public Alarm calculateReferenceAlarm(Group group) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateReferenceAlarm()", group.getName());
		}

		Alarm ret = group.getTrigger();

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateReferenceAlarm()", ret.toString());
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmAdditionalText()
	 */
	@Override
	public String calculateProblemAlarmAdditionalText(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmAdditionalText()",
					group.getName());
		}

		String ret = PROBLEM_DEFAULT_ADDITIONAL_TEXT;

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmAdditionalText()", ret);
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmManagedEntity(com.hp.uca.expert.group.Group)
	 */
	@Override
	public String calculateProblemAlarmManagedEntity(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmManagedEntity()",
					group.getName());
		}

		String ret = group.getProblemEntity();

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmManagedEntity()", ret);
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmOperatorNote(com.hp.uca.expert.group.Group)
	 */
	@Override
	public String calculateProblemAlarmOperatorNote(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmOperatorNote()",
					group.getName());
		}

		String ret = PROBLEM_DEFAULT_OPERATOR_NOTE_TEXT;

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmOperatorNote()", ret);
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmUserText(com.hp.uca.expert.group.Group)
	 */
	@Override
	public String calculateProblemAlarmUserText(Group group, Action action)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmUserText()",
					group.getName());
		}

		String ret = String.format(PD_Service_ProblemAlarm.PB_USER_TEXT_FORMAT,
				action.getActionId(), group.getTrigger().getIdentifier(),
				group.getName());

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmUserText()", ret);
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmAlarmType(com.hp.uca.expert.group.Group)
	 */
	@Override
	public AlarmType calculateProblemAlarmAlarmType(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmAlarmType()",
					group.getName());
		}

		AlarmType ret = group.getTrigger().getAlarmType();

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmAlarmType()",
					String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmSeverity(com.hp.uca.expert.group.Group)
	 */
	@Override
	public PerceivedSeverity calculateProblemAlarmSeverity(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmSeverity()",
					group.getName());
		}

		PerceivedSeverity ret = group.computeHighestSeverity();

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmSeverity()",
					String.valueOf(ret));
		}

		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmProbableCause(com.hp.uca.expert.group.Group)
	 */
	@Override
	public String calculateProblemAlarmProbableCause(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmProbableCause()",
					group.getName());
		}

		String ret = group.getTrigger().getProbableCause();

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmProbableCause()", ret);
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmEventTime(com.hp.uca.expert.group.Group)
	 */
	@Override
	public Long calculateProblemAlarmEventTime(Group group) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateProblemAlarmEventTime()",
					group.getName());
		}

		Long ret = group.getTrigger().getTimeInMilliseconds();

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateProblemAlarmEventTime()",
					String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.GroupLifecycle#
	 * whatToDoWhenSubAlarmIsAttachedToGroup(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsAttachedToGroup(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenSubAlarmIsAttachedToGroup()",
					alarm.getIdentifier());
		}

		if (group.getProblemAlarm() != null) {
			/*
			 * Operator State Propagation
			 */
			if (group.getProblemAlarm().getOperatorState() == OperatorState.ACKNOWLEDGED
					&& alarm.getOperatorState() == OperatorState.NOT_ACKNOWLEDGED) {
				PD_Service_Action.acknowledgeAlarm(scenario, alarm, this);
			} else if (group.getProblemAlarm().getOperatorState() == OperatorState.TERMINATED
					&& alarm.getOperatorState() != OperatorState.TERMINATED) {
				PD_Service_Action.terminateAlarm(scenario, alarm, this);
			}

			/*
			 * Trouble Ticket Propagation... if policy permits
			 */
			if (getProblemPolicy().getTroubleTicket()
					.isPropagateTroubleTicketToSubAlarms()
					&& group.getProblemAlarm().getProblemState() == ProblemState.HANDLED
					&& !PD_Service_TroubleTicket
							.isAttachingTroubleTicket(alarm)
					&& group.getTroubleTicketIdentifier() != null
					&& alarm.getProblemState() == ProblemState.NOT_HANDLED) {

				List<Alarm> alarms = new ArrayList<Alarm>();
				alarms.add(alarm);
				if (!alarms.isEmpty()) {
					PD_Service_TroubleTicket.attachingTroubleTicket(alarm);
					PD_Service_Action.associateTroubleTicket(scenario, group,
							this, alarms, group.getTroubleTicketIdentifier());
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.GroupLifecycle#
	 * whatToDoWhenProblemAlarmIsAttachedToGroup( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsAttachedToGroup(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log,
					"whatToDoWhenProblemAlarmIsAttachedToGroup()", group
							.getProblemAlarm().getIdentifier());
		}

		if (group.getProblemAlarm() != null) {

			if (group.getProblemAlarm().getOperatorState() == OperatorState.ACKNOWLEDGED) {
				PD_Service_Lifecycle.acknowledgeAllAlarmsInGroup(scenario,
						group);
			} else if (group.getProblemAlarm().getOperatorState() == OperatorState.TERMINATED) {

				synchronized (group.getAlarmsMap()) {

					for (Alarm alarm : group.getAlarmList()) {
						if (alarm.getOperatorState() != OperatorState.TERMINATED) {
							PD_Service_Action
									.terminateAlarm(scenario, alarm,
											((ProblemContext) group
													.getProblemContext())
													.getProblem());
						}

					}
				}
			}

			if (getProblemPolicy().getTroubleTicket()
					.isPropagateTroubleTicketToSubAlarms()
					&& group.getProblemAlarm().getProblemState() == ProblemState.HANDLED
					&& group.getTroubleTicketIdentifier() != null) {
				List<Alarm> alarms = new ArrayList<Alarm>();

				synchronized (group.getAlarmsMap()) {
					for (Alarm alarm : group.getAlarmList()) {
						if (alarm.getProblemState() == ProblemState.NOT_HANDLED
								&& !PD_Service_TroubleTicket
										.isAttachingTroubleTicket(alarm)) {

							PD_Service_TroubleTicket
									.attachingTroubleTicket(alarm);

							alarms.add(alarm);

						}
					}
				}

				if (!alarms.isEmpty()) {
					PD_Service_Action.associateTroubleTicket(scenario, group,
							this, alarms, group.getTroubleTicketIdentifier());
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.GroupLifecycle#
	 * whatToDoPeriodicallyForAGroup(com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean whatToDoPeriodicallyForAGroup(Group group) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoPeriodicallyForAGroup()",
					group.getName());
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenProblemAlarmIsAcknowledged( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsAcknowledged(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenProblemAlarmIsAcknowledged()",
					group.getProblemAlarm().getIdentifier());
		}

		PD_Service_Lifecycle.acknowledgeAllAlarmsInGroup(scenario, group);

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenProblemAlarmIsAcknowledged()");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenProblemAlarmIsTerminated( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsTerminated(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenProblemAlarmIsTerminated()",
					group.getProblemAlarm().getIdentifier());
		}

		synchronized (group.getAlarmsMap()) {

			// TODO do this only if these alarm are only part of this group ?
			for (Alarm alarm : group.getAlarmList()) {
				switch (alarm.getNetworkState()) {
				case CLEARED:
					PD_Service_Action.terminateAlarm(scenario, alarm, this);
					break;
				case NOT_CLEARED:
					if (alarm.getOperatorState() == OperatorState.ACKNOWLEDGED) {
						PD_Service_Action.unacknowledgeAlarm(scenario, alarm,
								this);
					}

					/*
					 * This alarm will need future Navigation Update, so tag it
					 * to trigger the Navigation Rule
					 */
					PD_Service_Navigation.needNavigationUpdate(scenario, alarm);
					break;
				}
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenProblemAlarmIsTerminated()");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenProblemAlarmIsCleared( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsCleared(Group group) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenProblemAlarmIsCleared()", group
					.getProblemAlarm().getIdentifier());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * calculateIfProblemAlarmhasToBeCleared(com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean calculateIfProblemAlarmhasToBeCleared(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "calculateIfProblemAlarmhasToBeCleared()",
					group.getName());
		}

		boolean ret = true;

		synchronized (group.getAlarmsMap()) {
			for (Alarm alarm : group.getAlarmList()) {
				if (alarm.getNetworkState() == NetworkState.NOT_CLEARED) {
					ret = false;
					break;
				}
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "calculateIfProblemAlarmhasToBeCleared()",
					String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenProblemAlarmIsUnacknowledged( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsUnacknowledged(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenProblemAlarmIsUnacknowledged()",
					group.getProblemAlarm().getIdentifier());
		}

		PD_Service_Lifecycle.unacknowledgeAllAlarmsInGroup(scenario, group);

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenProblemAlarmIsUnacknowledged()",
					group.getProblemAlarm().getIdentifier());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenProblemAlarmSeverityHasChanged(com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmSeverityHasChanged(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log,
					"whatToDoWhenProblemAlarmSeverityHasChanged()", group
							.getProblemAlarm().getIdentifier());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.TroubleTicketLifecycle#
	 * whatToDoWhenProblemAlarmIsClosed( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsClosed(Group group) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenProblemAlarmIsClosed()", group
					.getProblemAlarm().getIdentifier());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.TroubleTicketLifecycle#
	 * whatToDoWhenProblemAlarmIsHandled( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsHandled(Group group) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenProblemAlarmIsHandled()", group
					.getProblemAlarm().getIdentifier());
		}

		if (getProblemPolicy().getTroubleTicket()
				.isPropagateTroubleTicketToSubAlarms()) {

			List<Alarm> alarmsToAssociate = new ArrayList<Alarm>();

			synchronized (group.getAlarmsMap()) {
				for (Alarm alarm : group.getAlarmList()) {
					if (alarm.getProblemState() == ProblemState.NOT_HANDLED
							&& !PD_Service_TroubleTicket
									.isAttachingTroubleTicket(alarm)) {
						PD_Service_TroubleTicket.attachingTroubleTicket(alarm);
						alarmsToAssociate.add(alarm);
					}
				}
			}

			if (!alarmsToAssociate.isEmpty()) {
				PD_Service_Action.associateTroubleTicket(scenario, group, this,
						alarmsToAssociate, group.getTroubleTicketIdentifier());
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.TroubleTicketLifecycle#
	 * whatToDoWhenProblemAlarmIsReleased( com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsReleased(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenProblemAlarmIsReleased()", group
					.getProblemAlarm().getIdentifier());
		}

		if (getProblemPolicy().getTroubleTicket()
				.isPropagateTroubleTicketToSubAlarms()) {

			List<Alarm> alarmsToDissociate = new ArrayList<Alarm>();

			group.getProblemAlarm().getProblemInformation();
			synchronized (group.getAlarmsMap()) {
				for (Alarm alarm : group.getAlarmList()) {
					if (alarm.getProblemState() == ProblemState.HANDLED) {
						alarmsToDissociate.add(alarm);
					}
				}
			}

			if (!alarmsToDissociate.isEmpty()) {
				PD_Service_Action.dissociateTroubleTicket(scenario, group,
						this, alarmsToDissociate,
						group.getTroubleTicketIdentifier());
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenSubAlarmIsAcknowledged(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsAcknowledged(Alarm alarm, Group group)
			throws Exception {
		if (log.isDebugEnabled()) {
			LogHelper.method(log, "whatToDoWhenSubAlarmIsAcknowledged()",
					alarm.getIdentifier());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenSubAlarmIsCleared(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsCleared(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsCleared()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsCleared()",
					alarm.getIdentifier());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenSubAlarmIsTerminated(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsTerminated(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenSubAlarmIsTerminated()",
					alarm.getIdentifier());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenSubAlarmIsUnacknowledged(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsUnacknowledged(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenSubAlarmIsUnacknowledged()",
					alarm.getIdentifier());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * whatToDoWhenSubAlarmSeverityHasChanged(com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmSeverityHasChanged(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmSeverityHasChanged()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmSeverityHasChanged()",
					alarm.getIdentifier());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.TroubleTicketLifecycle#
	 * whatToDoWhenSubAlarmIsClosed(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsClosed(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenSubAlarmIsClosed()",
					alarm.getIdentifier());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.TroubleTicketLifecycle#
	 * whatToDoWhenSubAlarmIsHandled(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsHandled(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenSubAlarmIsHandled()",
					alarm.getIdentifier());
		}

		if (getProblemPolicy().getTroubleTicket()
				.isPropagateTroubleTicketToProblemAlarm()
				&& group.getProblemAlarm() != null
				&& group.getProblemAlarm().getProblemState() != ProblemState.HANDLED
				&& !PD_Service_TroubleTicket.isAttachingTroubleTicket(group
						.getProblemAlarm())) {

			List<Alarm> alarmsToAssociate = new ArrayList<Alarm>();
			alarmsToAssociate.add(group.getProblemAlarm());
			PD_Service_TroubleTicket.attachingTroubleTicket(group
					.getProblemAlarm());

			if (!alarmsToAssociate.isEmpty()) {
				PD_Service_Action
						.associateTroubleTicket(
								scenario,
								group,
								this,
								alarmsToAssociate,
								PD_Service_TroubleTicket
										.extractFirstTTIdentifierFromProblemInformation(alarm
												.getProblemInformation()));
			}

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.hp.uca.expert.vp.pd.interfaces.TroubleTicketLifecycle#
	 * whatToDoWhenSubAlarmIsReleased(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsReleased(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenSubAlarmIsReleased()",
					alarm.getIdentifier());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmLifecycle#
	 * isInformationNeededAvailable(com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public boolean isInformationNeededAvailable(Alarm alarm) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isInformationNeededAvailable()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isInformationNeededAvailable()", "true");
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.ProblemAlarmCreation#
	 * calculateProblemAlarmOtherAttribute
	 * (com.hp.uca.mediation.action.client.Action)
	 */
	@Override
	public void calculateProblemAlarmOtherAttribute(Group group, Action action)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "calculateProblemAlarmOtherAttribute()",
					String.valueOf(action.getActionId()));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.TroubleTicketLifecycle#
	 * isAllCriteriaForTroubleTicketCreation(com.hp.uca.expert.group.Group)
	 */
	@Override
	public boolean isAllCriteriaForTroubleTicketCreation(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "isAllCriteriaForTroubleTicketCreation()",
					group.getName());
		}

		boolean ret = PD_Service_TroubleTicket
				.isItTimeForTroubleTicketCreation(group);

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "isAllCriteriaForTroubleTicketCreation()",
					String.valueOf(ret));
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#chooseSupportedActions
	 * (com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public SupportedActions chooseSupportedActions(Alarm alarm,
			ProblemInterface problem) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "chooseSupportedActions()",
					alarm.getIdentifier());
		}

		SupportedActions supportedActions = getSupportedActions().get(
				alarm.getSourceIdentifier());

		if (log.isTraceEnabled()) {
			LogHelper.exit(
					log,
					"chooseSupportedActions()",
					supportedActions == null ? null : supportedActions
							.getName());
		}
		return supportedActions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.CoreConfiguration#
	 * chooseSupportedTroubleTicketActions(com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public SupportedTroubleTicketActions chooseSupportedTroubleTicketActions(
			Alarm alarm, ProblemInterface problem) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "chooseSupportedTroubleTicketActions()",
					alarm.getIdentifier());
		}

		SupportedTroubleTicketActions supportedTroubleTicketActions = null;

		Set<String> tags = alarm.getPassingFiltersTags().get(
				problem.getProblemContext().getName());
		if (tags != null) {
			for (String tTActionsName : getSupportedTroubleTicketActions()
					.keySet()) {
				if (tags.contains(tTActionsName)) {
					supportedTroubleTicketActions = getSupportedTroubleTicketActions()
							.get(tTActionsName);
				}
			}
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "chooseSupportedTroubleTicketActions()",
					supportedTroubleTicketActions == null ? null
							: supportedTroubleTicketActions.getName());
		}
		return supportedTroubleTicketActions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmEligibilityUpdate#
	 * whatToDoWhenProblemAlarmIsNoMoreEligible(com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsNoMoreEligible(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenProblemAlarmIsNoMoreEligible()",
					group.getProblemAlarm().getIdentifier());
		}

		synchronized (group.getAlarmsMap()) {

			// TODO do this only if these alarm are only part of this group ?
			for (Alarm alarm : group.getAlarmList()) {
				switch (alarm.getNetworkState()) {
				case CLEARED:
					PD_Service_Action.terminateAlarm(scenario, alarm, this);
					break;
				case NOT_CLEARED:
					if (alarm.getOperatorState() == OperatorState.ACKNOWLEDGED) {
						PD_Service_Action.unacknowledgeAlarm(scenario, alarm,
								this);
					}

					/*
					 * This alarm will need future Navigation Update, so tag it
					 * to trigger the Navigation Rule
					 */
					PD_Service_Navigation.needNavigationUpdate(scenario, alarm);
					break;
				}
			}
		}

		scenario.getSession().retract(group);

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenProblemAlarmIsNoMoreEligible()");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AlarmEligibilityUpdate#
	 * whatToDoWhenSubAlarmIsNoMoreEligible(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsNoMoreEligible(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsNoMoreEligible()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsNoMoreEligible()");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AttributeUpdate#
	 * whatToDoWhenProblemAlarmAttributeHasChanged
	 * (com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmAttributeHasChanged(Group group,
			AttributeChange attributeChange) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log,
					"whatToDoWhenProblemAlarmAttributeHasChanged()", group
							.getProblemAlarm().getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper
					.exit(log, "whatToDoWhenProblemAlarmAttributeHasChanged()");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.AttributeUpdate#
	 * whatToDoWhenSubAlarmAttributeHasChanged(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmAttributeHasChanged(Alarm alarm,
			Group group, AttributeChange attributeChange) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmAttributeHasChanged()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmAttributeHasChanged()");
		}

	}

	/**
	 * @return the booleans
	 */
	@Override
	public final Booleans getBooleans() {
		return booleans;
	}

	/**
	 * @param booleans
	 *            the booleans to set
	 */
	public final void setBooleans(Booleans booleans) {
		this.booleans = booleans;
	}

	/**
	 * @return the longs
	 */
	@Override
	public final Longs getLongs() {
		return longs;
	}

	/**
	 * @param longs
	 *            the longs to set
	 */
	public final void setLongs(Longs longs) {
		this.longs = longs;
	}

	/**
	 * @return the strings
	 */
	@Override
	public final Strings getStrings() {
		return strings;
	}

	/**
	 * @param strings
	 *            the strings to set
	 */
	public final void setStrings(Strings strings) {
		this.strings = strings;
	}

	/**
	 * 
	 */
	@Override
	public void computeLongs() {

	}

	/**
	 * 
	 */
	@Override
	public void computeBooleans() {

	}

	/**
	 * 
	 */
	@Override
	public void computeStrings() {

	}

	/**
	 * @param alarm
	 * @return
	 */
	@Override
	public TimeWindow computeTimeWindow(Alarm alarm) {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeTimeWindow()", String.format(
					"of alarm [%s] in the context of ProblemContext [%s]",
					alarm.getIdentifier(), getProblemContext().getName()));
		}

		ProblemPolicy problemPolicies = problemContext.getProblem()
				.getProblemPolicy();

		if (log.isTraceEnabled()) {
			String output;
			if (problemPolicies.getTimeWindow().getTimeWindowMode() == TimeWindowMode.NONE) {
				output = String.format(" [TimeWindowMode=%s]", problemPolicies
						.getTimeWindow().getTimeWindowMode());
			} else {
				output = String
						.format(" [TimeWindowMode=%s][TimeWindowAfterTrigger=%s][TimeWindowBeforeTrigger=%s]",
								problemPolicies.getTimeWindow()
										.getTimeWindowMode(), problemPolicies
										.getTimeWindow()
										.getTimeWindowAfterTrigger(),
								problemPolicies.getTimeWindow()
										.getTimeWindowBeforeTrigger());

			}
			LogHelper.exit(log, "computeTimeWindow()", output);
		}

		return problemPolicies.getTimeWindow();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.NetworkStateUpdate#
	 * whatToDoWhenProblemAlarmIsUncleared(com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenProblemAlarmIsUncleared(Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.method(log, "whatToDoWhenProblemAlarmIsUncleared()",
					group.getProblemAlarm().getIdentifier());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hp.uca.expert.vp.pd.interfaces.NetworkStateUpdate#
	 * whatToDoWhenSubAlarmIsUncleared(com.hp.uca.expert.alarm.Alarm,
	 * com.hp.uca.expert.group.Group)
	 */
	@Override
	public void whatToDoWhenSubAlarmIsUncleared(Alarm alarm, Group group)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenSubAlarmIsUncleared()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenSubAlarmIsUncleared()",
					alarm.getIdentifier());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hp.uca.expert.vp.pd.interfaces.CommonEntityCheck#computeGroupPriority
	 * (com.hp.uca.expert.alarm.Alarm)
	 */
	@Override
	public Long computeGroupPriority(Alarm alarm) {
		return null;
	}

	@Override
	public void whatToDoWhenOrphanAlarmSeverityHasChanged(Alarm alarm)
			throws Exception {
		
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmSeverityHasChanged()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmSeverityHasChanged()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmAttributeHasChanged(Alarm alarm,
			AttributeChange attributeChange) throws Exception {
		
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmAttributeHasChanged()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmAttributeHasChanged()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsCleared(Alarm alarm) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsCleared()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsCleared()");
		}
		
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsUncleared(Alarm alarm)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsUncleared()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsUncleared()");
		}
		
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsTerminated(Alarm alarm)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsTerminated()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsTerminated()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsAcknowledged(Alarm alarm)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsAcknowledged()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsAcknowledged()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsUnacknowledged(Alarm alarm)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsUnacknowledged()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsUnacknowledged()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsHandled(Alarm alarm) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsHandled()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsHandled()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsReleased(Alarm alarm) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsReleased()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsReleased()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsClosed(Alarm alarm) throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsClosed()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsClosed()");
		}
	}

	@Override
	public void whatToDoWhenOrphanAlarmIsNoMoreEligible(Alarm alarm)
			throws Exception {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "whatToDoWhenOrphanAlarmIsNoMoreEligible()",
					alarm.getIdentifier());
		}

		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "whatToDoWhenOrphanAlarmIsNoMoreEligible()");
		}
	}

	@Override
	public Long computeDelayForProblemAlarmClearance(Alarm arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long computeDelayForProblemAlarmCreation(Alarm arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long computeDelayForTroubleTicketCreation(Alarm arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
