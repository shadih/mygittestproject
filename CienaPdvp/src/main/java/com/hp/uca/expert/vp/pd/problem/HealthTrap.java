/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.ciena.cienaPD.CienaAlarm;
import com.att.gfp.ciena.cienaPD.ExtendedLifeCycle;
import com.att.gfp.ciena.cienaPD.Util;
import com.att.gfp.ciena.cienaPD.topoModel.NodeManager;
import com.hp.uca.common.trace.LogHelper;

import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author MASSE
 * 
 */
public final class HealthTrap extends ProblemDefault implements
		ProblemInterface {
	private static Logger log = LoggerFactory.getLogger(HealthTrap.class);
	private static NodeManager nmgr;

	static {
		nmgr = new NodeManager();
	}

	public HealthTrap() {
		super();
	}
}
