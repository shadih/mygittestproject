package com.att.gfp.decomposed.decomposedScenario;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.ipagAlarm.AlarmState;
import com.att.gfp.data.ipagAlarm.EnrichedAlarm;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

@XmlRootElement
public class DecomposedAlarm extends EnrichedAlarm 
{
	public DecomposedAlarm(Alarm alarm) throws Exception {
		super(alarm);
		setAlarmState(AlarmState.pending);
	}

	public void setSeverity(int severity) {
		setCustomFieldValue("severity", Integer.toString(severity));
		switch(severity) {
	    case 0:
		setPerceivedSeverity(PerceivedSeverity.CRITICAL);
	    	break;
	    case 1:
		setPerceivedSeverity(PerceivedSeverity.MAJOR);
	    	break;
	    case 2:
		setPerceivedSeverity(PerceivedSeverity.MINOR);
	    	break;
	    case 3:
		setPerceivedSeverity(PerceivedSeverity.WARNING);
	    	break;
	    case 5:
		setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
	    	break;
	    case 4:
		setPerceivedSeverity(PerceivedSeverity.CLEAR);
	    	break;
	    default:
		setPerceivedSeverity(PerceivedSeverity.INDETERMINATE);
	    	break;
		}	
	}
}
