/**
 * 
 */
package com.att.gfp.helper;

/**
 * Utility Class to define the Alarm standard fields or Constants used for the
 * Mobility Customization
 * 
 * @author MASSE
 */
public class StandardFields {

	/**
	 * Standard Alarm field. Value is {@value #IDENTIFIER}
	 */
	public static final String IDENTIFIER = "Identifier";

	/**
	 * Standard Alarm field. Value is {@value #NETWORK_STATE}
	 */
	public static final String NETWORK_STATE = "networkState";

	/**
	 * Standard Alarm field. Value is {@value #OPERATOR_STATE}
	 */
	public static final String OPERATOR_STATE = "operatorState";

	/**
	 * Standard Alarm field. Value is {@value #PERCEIVED_SEVERITY}
	 */
	public static final String PERCEIVED_SEVERITY = "perceivedSeverity";

	/**
	 * Standard Alarm field. Value is {@value #NAVIGATION_FIELD}
	 */
	public static final String NAVIGATION_FIELD = "NavigationField";

	/**
	 * Constant used to retrieve the ActionsFactory. Value is
	 * {@value #NETCOOL_ACTIONSFACTORY}
	 */
	public static final String NETCOOL_ACTIONSFACTORY = "Netcool";
	
	/**
	 * Constant used to retrieve the Alarm Forwarder Bean. Value is
	 * {@value #ALARM_FORWARDER_BEAN_NAME}
	 */
	public static final String ALARM_FORWARDER_BEAN_NAME = "alarmForwarder";
	
	/**
	 * Constant used to retrieve the Alarm List used for Test. Value is
	 * {@value #TEST_ALARM_LIST}
	 */
	public static final String TEST_ALARM_LIST = "testAlarmQueue";
	
	
	/**
	 * Constant used to reinit a Custom Field. Value is
	 * {@value #EMPTY_CUSTOM_FIELD}
	 */
	public static final String EMPTY_CUSTOM_FIELD = "";

	/**
	 * Constant used to initialize ServerSerial Custom Field. Value is
	 * {@value #DEFAULT_SERVER_SERIAL}
	 */
	public static final String DEFAULT_SERVER_SERIAL = "0";
	
	/**
	 * Constant used for SyntheticAlarm. Value is
	 * {@value #CORRELATED}
	 */
	public static final String CORRELATED = "CORRELATED:";
	
	/**
	 * Constant used to initialize SubscriptionInfo Custom Field. Value is
	 * {@value #DEFAULT_SUBSCRIPTION_INFO}
	 */
	public static final String DEFAULT_SUBSCRIPTION_INFO = "CorrelatedBy=PD";
	
	/**
	 * Constant used to initialize SubscriptionInfo Custom Field. Value is
	 * {@value #DEFAULT_SUBSCRIPTION_INFO}
	 */
	public static final String ALERT_GROUP_SYNTHETIC_APPEND = "GC:";
	
	
}
