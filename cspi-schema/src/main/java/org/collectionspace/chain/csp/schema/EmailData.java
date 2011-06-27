/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author caret
 * all email specific data from the cspace-config.xml file that is parsed when the server starts up
 * will hold static data e.g. content of emails, from addresses
 * 
 */
public class EmailData {

	String baseurl,fromaddress,toaddress,loginurl ;
	String smtphost,smtpport,smtppass,smtpuser;
	Boolean smtpdebug,smtpauth;
	String pswdmsg, pswdsubj, tokenvalid;
	
/* 	<email>
		<baseurl>hendecasyllabic.local:8180</baseurl>
		<from>csm22@caret.cam.ac.uk</from>
		<to></to><!-- if specified then all emails will send to this address - used for debugging -->
		<smtp>
			<host>localhost</host>
			<port>25</port>
			<debug>false</debug>
			<auth enabled="false"> <!-- set to true if wish to use auth -->
				<username></username>
				<password></password>
			</auth>
		</smtp>
		<passwordreset>
			<subject>CollectionSpace Password reset request</subject>
			<message>A password reset has been requested from this email. If you wish to reset your password please click on this link {{link}}.</message>
		</passwordreset>
	</email> 

*/
	
	public EmailData(Spec spec, ReadOnlySection section) {
		baseurl=(String)section.getValue("/baseurl");
		fromaddress=(String)section.getValue("/from");
		toaddress=(String)section.getValue("/to");
		smtphost = (String)section.getValue("/smtp/host");
		smtpport = (String)section.getValue("/smtp/port");
		smtpdebug = Util.getBooleanOrDefault(section,"/smtp/debug",false);
		smtpauth = Util.getBooleanOrDefault(section,"/smtp/auth/@enabled",false);
		smtppass = (String)section.getValue("/smtp/auth/password");
		smtpuser = (String)section.getValue("/smtp/auth/username");
		pswdmsg = (String)section.getValue("/passwordreset/message");
		pswdsubj = (String)section.getValue("/passwordreset/subject");
		loginurl = (String)section.getValue("/passwordreset/loginpage");
		tokenvalid = Util.getStringOrDefault(section, "/passwordreset/token/daysvalid", "7");
	}
	

	public String getLoginUrl() {return loginurl; }
	public String getBaseURL() { return baseurl; }
	public String getFromAddress() { return fromaddress; }
	public String getToAddress() { return toaddress; }

	public String getSMTPPort() { return smtpport; }
	public String getSMTPHost() { return smtphost; }
	public Boolean doSMTPDebug() { return smtpdebug; }

	public String getPasswordResetMessage() { return pswdmsg; }
	public String getPasswordResetSubject() { return pswdsubj; }
	public Integer getTokenValidForLength() { return Integer.parseInt(tokenvalid); }
	
	public Boolean doSMTPAuth() { return smtpauth; }
	public String getSMTPAuthPassword() { if(smtpauth){ return smtppass;} else {return null;} }
	public String getSMTPAuthUsername() { if(smtpauth){ return smtpuser;} else {return null;} }
	
	public EmailData getEmailData() { return this; }

	void dumpJson(JSONObject out) throws JSONException {
		JSONObject record = new JSONObject();
		record.put("baseurl", baseurl);
		record.put("getFromAddress", fromaddress);
		record.put("getToAddress", toaddress);
		record.put("getPasswordResetMessage", pswdmsg);
		record.put("getPasswordResetSubject", pswdsubj);
		record.put("getTokenValidForLength", tokenvalid);
		out.put("EmailData", record);
	}
}
