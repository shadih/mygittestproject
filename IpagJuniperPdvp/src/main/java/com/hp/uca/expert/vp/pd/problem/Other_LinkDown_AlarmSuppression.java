/**
 * This Problem is empty and ready to define methods to customize this problem
 */
package com.hp.uca.expert.vp.pd.problem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.EnrichedJuniperAlarm;
import com.att.gfp.helper.GFPFields;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.group.Group;
import com.hp.uca.expert.vp.pd.core.JuniperLinkDown_ProblemDefault;
import com.hp.uca.expert.vp.pd.interfaces.ProblemInterface;

/**
 * @author df
 * 
 */
public final class Other_LinkDown_AlarmSuppression extends JuniperLinkDown_ProblemDefault implements
		ProblemInterface {

	private Logger log = LoggerFactory.getLogger(Other_LinkDown_AlarmSuppression.class);

	public Other_LinkDown_AlarmSuppression() {
		super();

	}


	 // this is sets 8 and 9 in the requirements doc for link down
	@Override
	public List<String> computeProblemEntity(Alarm a) throws Exception {

		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "computeProblemEntity()");
		}

		List<String> problemEntities = new ArrayList<String>();

		String problemEntity =  null;

		// if this is the trigger link down alarm
		if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50003/100/1"))  {
			// here we place the reason code in the problem entity for correlation with
			// 50003/100/7
			
			// GFPC14150 - this is being removed because unrelated LD alarms are being correlated due to matching ifindexes
			// the ifindex is unique only on a device not across devices...
			
//			if(a.getCustomFieldValue(GFPFields.REASON_CODE) != null)
//				problemEntities.add(a.getCustomFieldValue(GFPFields.REASON_CODE));
			
			//  here we use the remote port so we can correlate with an 50002/100/19
			// alarm on that port
			if(a.getOriginatingManagedEntity().contains("PPORT") && 
					((EnrichedJuniperAlarm)a).getRemotePortKey() != null &&
					!((EnrichedJuniperAlarm) a).getRemotePortKey().isEmpty() )
				problemEntities.add(((EnrichedJuniperAlarm)a).getRemotePortKey());
				
		} else {
			// if we will correlate with 50002/100/19 then its by link down remote port
			// which is this alarm's ME instance
			if(a.getCustomFieldValue(GFPFields.EVENT_KEY).equals("50002/100/19"))
					problemEntities.add(a.getOriginatingManagedEntity().split(" ")[1]);
			else
				// if we correlate with 50003/100/7 then its with the reason code for 
				// both alarms
				if(a.getCustomFieldValue(GFPFields.REASON_CODE) != null)
					problemEntities.add(a.getCustomFieldValue(GFPFields.REASON_CODE));
		}

		if (log.isTraceEnabled())
			LogHelper.exit(log, "alarm = " + a.getIdentifier() + ", computeProblemEntity() --- problemEntities=" + problemEntities);

		return problemEntities;
	} 
}
