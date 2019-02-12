package com.att.gfp.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.gfp.data.config.AdtranCorrelationConfiguration;
import com.att.gfp.data.ipag.topoModel.IpagAdtranTopoAccess;
import com.att.gfp.data.ipagAlarm.AlarmDelegationType;
import com.att.gfp.data.ipagAlarm.EnrichedAdtranLinkDownAlarm;
import com.hp.uca.common.trace.LogHelper;
import com.hp.uca.expert.alarm.Alarm;
import com.hp.uca.expert.x733alarm.PerceivedSeverity;

/**
 * Implementation class for adtran-pport-alarm() processing
 * 
 * @author st133d
 * 
 */
public class AdtranPportAlarmProcessor {

	private static Logger log = LoggerFactory
			.getLogger(AdtranPportAlarmProcessor.class);

	public void processAdtranPportAlarm(Alarm alarm) {
		EnrichedAdtranLinkDownAlarm enrichedAlarm = null;
		if (alarm instanceof EnrichedAdtranLinkDownAlarm) {
			enrichedAlarm = (EnrichedAdtranLinkDownAlarm) alarm;
			enrichedAlarm.setClci(enrichedAlarm
					.getCustomFieldValue(GFPFields.CLCI));
			enrichedAlarm.setClfi(enrichedAlarm
					.getCustomFieldValue(GFPFields.CLFI));
		} else {  
			enrichedAlarm = new EnrichedAdtranLinkDownAlarm(alarm);
		}
		updateInfo2(enrichedAlarm);
		parseComponentField(enrichedAlarm);
		updateComponentField(enrichedAlarm);
		updateUNIandReason(enrichedAlarm);
		updateCircuitIdandClci(enrichedAlarm);
		populateClfi(enrichedAlarm);
		AdtranCorrelationConfiguration adtCorrelationConfig = new AdtranCorrelationConfiguration();
		if (adtCorrelationConfig
				.getAdtranCorrelationPolicies()
				.getNtdCorrelation()
				.getEventNames()
				.getEventName()
				.contains(
						enrichedAlarm.getCustomFieldValue(GFPFields.EVENT_KEY))) {
			if (enrichedAlarm.getPerceivedSeverity() != PerceivedSeverity.CLEAR) {
				GFPUtil.forwardOrCascadeAlarm(enrichedAlarm,
						AlarmDelegationType.CASCADE, "NTDTICKET_CORRELATION");
			} else {
				GFPUtil.forwardOrCascadeAlarm(enrichedAlarm,
						AlarmDelegationType.FORWARD, null);
			}
		} else {
			GFPUtil.forwardOrCascadeAlarm(enrichedAlarm,
					AlarmDelegationType.FORWARD, null);
		}

	}

	private void populateClfi(EnrichedAdtranLinkDownAlarm enrichedAlarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "populateClfi()");
		}
		String classfication = enrichedAlarm
				.getCustomFieldValue(GFPFields.CLASSIFICATION);
		log.trace("populateClfi() : classification = " + classfication);
		if (classfication != null) {
			if (AlarmClassificationTypes.NFOMOBILITY.getClassifcationType()
					.equalsIgnoreCase(classfication)
					|| AlarmClassificationTypes.NFOTA5000
							.getClassifcationType().equalsIgnoreCase(
									classfication)
					|| AlarmClassificationTypes.NFOEMUX.getClassifcationType()
							.equalsIgnoreCase(classfication)) {
				log.trace("populateClfi() : classification = " + classfication);
				String manangedClass = enrichedAlarm
						.getCustomFieldValue(GFPFields.SM_CLASS);
				log.trace("populateClfi() : mangedclasss = " + manangedClass);
				if (ManagedClassTypes.PPORT.name().equals(manangedClass)) {
					log.trace("populateClfi() : classification = "
							+ classfication);
					enrichedAlarm.setCustomFieldValue(GFPFields.CLFI,
							enrichedAlarm.getClfi());
				} else if (ManagedClassTypes.LPort.name().equals(manangedClass)) {
					enrichedAlarm
							.setCustomFieldValue(
									GFPFields.CLFI,   
									StringUtils
											.defaultString(getClfiFromTology(GFPUtil
													.getManagedInstanceFromMangaedEntity(enrichedAlarm
															.getOriginatingManagedEntity()))));
				} else {
					enrichedAlarm.setCustomFieldValue(GFPFields.CLFI,
							GFPFields.CLFIUNKNOWN);
				}
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "populateClfi()");
		}
	}

	private void updateCircuitIdandClci(
			EnrichedAdtranLinkDownAlarm enrichedAlarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "updateCircuitIdandClci()");
		}
		enrichedAlarm.setCustomFieldValue(GFPFields.CIRCUIT_ID,
				enrichedAlarm.getClci());
		enrichedAlarm.setCustomFieldValue(GFPFields.CLCI,
				enrichedAlarm.getClci());
		log.trace("updateCircuitIdandClci() : Circuit ID after update = "
				+ enrichedAlarm.getCustomFieldValue(GFPFields.CIRCUIT_ID));
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "updateCircuitIdandClci()");
		}
	}

	private void updateUNIandReason(EnrichedAdtranLinkDownAlarm enrichedAlarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "updateUNIandReason()");
		}
		String reason = enrichedAlarm.getCustomFieldValue(GFPFields.REASON);
		String ipAddress = enrichedAlarm.getOriginatingManagedEntity().split(
				" ")[1];
		ipAddress = StringUtils.defaultString(ipAddress);
		reason = StringUtils.defaultString(reason);
		log.trace("updateUNIandReason() : reason from input alarms = " + reason);
		log.trace("updateUNIandReason() : ipAddress from input alarms = "
				+ ipAddress);
		enrichedAlarm.setIpAddress(ipAddress);
		if (GFPFields.UNI.equalsIgnoreCase(enrichedAlarm.getUninniType()) &&
				enrichedAlarm.getClci() != null
				&& enrichedAlarm.getClci().isEmpty()) {
			// TODO: Change the hard-coded String
			enrichedAlarm.setCustomFieldValue(GFPFields.REASON, reason + " "
					+ "Region=" + enrichedAlarm.getPportLegacyOrgInd()
					+ GFPFields.IP_ADDRESS + "=" + ipAddress);
			log.trace("NULL_CLCI: " + "" + "| Port: " + ipAddress 
					+ " | Reason: "  
					+ enrichedAlarm.getCustomFieldValue(GFPFields.REASON));
		}
		if (GFPFields.UNI.equalsIgnoreCase(enrichedAlarm.getUninniType()) &&
				enrichedAlarm.getClci() != null 
				&& !enrichedAlarm.getClci().isEmpty()) {
			log.trace("CLCI: " + enrichedAlarm.getClci() + "| Port: "
					+ ipAddress + " | Reason: " + reason + " "
					+ GFPFields.IP_ADDRESS + "=" + ipAddress);
		}
		if (!GFPFields.UNI.equalsIgnoreCase(enrichedAlarm.getUninniType())) {
			// Dont add Region or Port IP to Reason"
			log.trace("Port: " + ipAddress + " | NOT UNI | "
					+ enrichedAlarm.getUninniType() + " | Reason: " + reason);
		}
		log.trace("updateUNIandReason() : REASON after update = "
				+ enrichedAlarm.getCustomFieldValue(GFPFields.REASON));
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "updateUNIandReason()");
		}
	}

	private void updateComponentField(EnrichedAdtranLinkDownAlarm enrichedAlarm) {
		if ("50001/100/2".equals(enrichedAlarm.getTunable())) {
			enrichedAlarm.setCustomFieldValue(GFPFields.COMPONENT,
					removeSlotNamefromComponent(enrichedAlarm
							.getCustomFieldValue(GFPFields.COMPONENT)));
		}
		log.trace("updateComponentField(alarm) componet after update = "
				+ enrichedAlarm.getCustomFieldValue(GFPFields.COMPONENT));
	}

	private String removeSlotNamefromComponent(String component) {
		Pattern p = Pattern.compile("slotname=[\\w]* ");
		Matcher m = p.matcher(component);
		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			log.trace(" found the match for matcher = " + m.group());
			m.appendReplacement(sb, "");
		}

		return m.appendTail(sb).toString();

	}

	private void parseComponentField(EnrichedAdtranLinkDownAlarm alarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "parseComponentField()");
		}
		String[] fields = alarm.getCustomFieldValue(GFPFields.COMPONENT).split(
				" ");
		for (String field : fields) {
			log.trace("parseComponentField(alarm): Found fields seperated by comma");
			if (field.contains("=")) {
				String[] keyValues = field.split("\\=");
				if (GFPFields.SLOTNAME.equalsIgnoreCase(keyValues[0])) {
					if (keyValues.length > 1) {
						log.trace("parseComponentField(alarm): Found slotname field seperated by = " + keyValues[1]);
						alarm.setSlotName(keyValues[1]);
					}
				}
				if (GFPFields.SLOT.equalsIgnoreCase(keyValues[0])) {
					log.trace("parseComponentField(alarm): Found slot field seperated by = " + keyValues[1]);
					alarm.setSlot(keyValues[1]);
				}
				if (GFPFields.PORT.equalsIgnoreCase(keyValues[0])) {
					log.trace("parseComponentField(alarm): Found port field seperated by = " + keyValues[1]);
					alarm.setPort(keyValues[1]);
				} 
			}
		}
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "parseComponentField()");
		}
	}

	private void updateInfo2(EnrichedAdtranLinkDownAlarm enrichedAlarm) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "updateInfo2()");
		}
		String info2 = StringUtils.defaultString(enrichedAlarm
				.getCustomFieldValue(GFPFields.INFO2));
		log.trace("updateInfo2(alarm) info2 = " + info2);
		if (DeviceTypes.ADTRAN5000SERIES.getDeviceType().equals(
				enrichedAlarm.getDeviceType())
				&& !(DeviceTypes.JUNIPERMXSERIES
						.getDeviceType().equals(
								enrichedAlarm.getRemoteDeviceType()))) {
			enrichedAlarm.setCustomFieldValue(
					GFPFields.INFO2,
					info2 + GFPFields.REMOTENTECLLI + "=<"
							+ enrichedAlarm.getRemoteDeviceName() + "> "
							+ GFPFields.REMOTEPORTAID + "=<"
							+ enrichedAlarm.getRemotePortAid() + ">"); 	 
		}
		log.trace("updateInfo2(alarm) info2 after update = "
				+ enrichedAlarm.getCustomFieldValue(GFPFields.INFO2));
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "updateInfo2()");
		}
	}

	private String getClfiFromTology(String lportInstance) {
		if (log.isTraceEnabled()) {
			LogHelper.enter(log, "getClfiFromTology()");
		}
		String clfi = null;
		IpagAdtranTopoAccess topoAccess = IpagAdtranTopoAccess.getInstance();
		clfi = topoAccess.fetchClfiOfPportFromLportInstance(lportInstance);
		if (log.isTraceEnabled()) {
			LogHelper.exit(log, "getClfiFromTology()");
		}
		return clfi;
	}

}
