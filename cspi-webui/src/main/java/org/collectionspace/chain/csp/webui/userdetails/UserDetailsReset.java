/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.userdetails;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.AdminData;
import org.collectionspace.chain.csp.schema.EmailData;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * receive JSON: { email: "fred@bloggs.com" }
 * test if valid user
 * if valid user: create token and email to user
 * if not send back exception
 * 
 * user gets email: goes to url in email and sets new password
 * we check that the email and token submitted with the password match
 * if so change password
 * if not send back exception
 * @author csm22
 *
 */
public class UserDetailsReset implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(UserDetailsReset.class);
	boolean setnewpassword = false;
	String base = "users";
	Spec spec;
	private static String tokensalt = "74102328UserDetailsReset";
	public UserDetailsReset(Boolean setnewpassword, Spec spec) { 
		this.setnewpassword = setnewpassword;
		this.spec = spec;
	}
	

	
	private Boolean doEmail(String csid, String emailparam, Request in, JSONObject userdetails) throws UIException, JSONException {
		
		String token = createToken(csid);
		EmailData ed = spec.getEmailData();
		String[] recipients = new String[1];

		/* ABSTRACT EMAIL STUFF : WHERE do we get the content of emails from? cspace-config.xml */
		String messagebase = ed.getPasswordResetMessage();
		String link = ed.getBaseURL() + ed.getLoginUrl() + "?token="+token+"&email="+ emailparam;
		String message = messagebase.replaceAll("\\{\\{link\\}\\}", link);
		String greeting = userdetails.getJSONObject("fields").getString("screenName");
		message = message.replaceAll("\\{\\{greeting\\}\\}", greeting);
		message = message.replaceAll("\\\\n", "\\\n");
		message = message.replaceAll("\\\\r", "\\\r");
		
	    String SMTP_HOST_NAME = ed.getSMTPHost();
	    String SMTP_PORT = ed.getSMTPPort();
	    String subject = ed.getPasswordResetSubject();
	    String from = ed.getFromAddress();
	    if(ed.getToAddress().isEmpty()){
	    	recipients[0] = emailparam;
	    }
	    else{
	    	recipients[0] = ed.getToAddress();
	    }
	    Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
	    boolean debug = false;
       
	    Properties props = new Properties();
	    props.put("mail.smtp.host", SMTP_HOST_NAME);
	    props.put("mail.smtp.auth", ed.doSMTPAuth());
       	props.put("mail.debug", ed.doSMTPDebug());
       	props.put("mail.smtp.port", SMTP_PORT);

       	Session session = Session.getDefaultInstance(props);
       	// XXX fix to allow authpassword /username

       	session.setDebug(debug);

       	Message msg = new MimeMessage(session);
       	InternetAddress addressFrom;
		try {
			addressFrom = new InternetAddress(from);
			msg.setFrom(addressFrom);

			InternetAddress[] addressTo = new InternetAddress[recipients.length];
			for (int i = 0; i < recipients.length; i++) {
				addressTo[i] = new InternetAddress(recipients[i]);
			}
			msg.setRecipients(Message.RecipientType.TO, addressTo);

			// Setting the Subject and Content Type
			msg.setSubject(subject);
			msg.setText(message);

			Transport.send(msg);
		} catch (AddressException e) {
			throw new UIException("AddressException: "+e.getMessage());
		} catch (MessagingException e) {
			throw new UIException("MessagingException: "+e.getMessage());
		}
		
		return true;
	}

	private String createHash(String csid) throws UIException {
		try {
			byte[] buffer = csid.getBytes();
			byte[] result = null;
			StringBuffer buf = null;
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			result = new byte[md5.getDigestLength()];
			md5.reset();
			md5.update(buffer);
			result = md5.digest(tokensalt.getBytes());

			//create hex string from the 16-byte hash 
			buf = new StringBuffer(result.length * 2);
				for (int i = 0; i < result.length; i++) {
					int intVal = result[i] & 0xff;
					if (intVal < 0x10) {
						buf.append("0");
					}
				buf.append(Integer.toHexString(intVal).toUpperCase());
			}
			return buf.toString().substring(0,5);
		} catch (NoSuchAlgorithmException e) {
			throw new UIException("There were problems with the algorithum");
		}
	}
	
	/* create a token that holds date information */
	private String createToken(String csid) throws UIException {
		String hash = createHash(csid); 
		Long token = Long.parseLong(hash,16) * (System.currentTimeMillis() / 100000); 
		return token.toString();
	}
	
	private long daysBetween(Date startDate, Date endDate) throws UIException {

		long daysBetween = 0; 
		while (startDate.before(endDate)) { 
			startDate.setTime(startDate.getTime() + (24*60*60*1000)); 
			daysBetween++; 
		} 
		return daysBetween;
	}
	
	private boolean tokenExpired(String token, String match) throws UIException{ 
		
		EmailData ed = spec.getEmailData();
		Integer lengthTokenIsValidFor = ed.getTokenValidForLength();
		//token is what we got from the user, match is what we created 
		//Get the date from when the token was created by dividing token by match 
		Long dateFromToken = (Long.parseLong(token)*100000) / Long.parseLong(match, 16); 
		Date oldDate = new Date(dateFromToken); 
		
		//Date oldDate = new Date(dateFromToken - (8*24*60*60*1000)); 
		//String old = dateFormat.format(oldDate); 
		//Get the current date 
		Date date = new Date();  
		//lengthTokenIsValidFor defaults to 7 days 
		if(daysBetween(oldDate, date) > lengthTokenIsValidFor) 
		return true; 
		else 
		return false;
	} 
	
	private Boolean testToken(String csid, String token) throws UIException {
		String match = createHash(csid);
		if(!tokenExpired(token, match)){
			log.debug("token matches");
            return true;
		}
		log.debug("token expired");
        return false;
    }

	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(base+"/"+path,fields, new JSONObject());
		} //else {
			// Create SHOULDN"T EVER HAPPEN
			//if(fields!=null)
			//	path=storage.autocreateJSON(base,fields);
		//}
		return path;
	}
	
	private JSONObject getcsID(Storage storage,String emailparam) throws UIException {
		JSONObject restriction=new JSONObject();
		JSONObject failedJSON = new JSONObject();
		try {
			failedJSON.put("isError",true);
			if(emailparam!=null && emailparam!="") {
				restriction.put("email",emailparam);	
				restriction.put("pageSize","40");	

				int resultsize =1;
				int pagenum = 0;
				String checkpagination = "";
				while(resultsize >0){
					restriction.put("pageNum",pagenum);	
					/* XXX need to force it to only do an exact match */
					JSONObject data = storage.getPathsJSON(base,restriction);
					String[] paths = (String[]) data.get("listItems");
					pagenum++;
					
					if(paths.length==0 || checkpagination.equals(paths[0])){
						resultsize=0;
						//testing whether we have actually returned the same page or the next page - all csid returned should be unique
					}
					else{
						checkpagination = paths[0];
						/* make sure it is an exact match */
						for(int i=0;i<paths.length;i++) {
							//GET full details
							JSONObject fields = storage.retrieveJSON(base+"/"+paths[i], new JSONObject());
						
							String emailtest = fields.getString("email");
							if(emailtest.equals(emailparam)){
								JSONObject outputJSON = new JSONObject();
								outputJSON.put("fields",fields);
								outputJSON.put("isError",false);
								outputJSON.put("csid",paths[i]);
								return outputJSON;
							}
						}
					}
				}
				failedJSON.put("message","Could not find a user with email " + emailparam);
			}
			else{
				failedJSON.put("message","No email specified ");
			}
			return failedJSON;
		} catch (JSONException e) {
			throw new UIException("JSONException during search on email address",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during search on email address",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during search on email address",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			return uiexception.getJSON();
		}
	}

	private boolean testSuccess(Storage storage) {
		for(Record r : this.spec.getAllRecords()) {
			if((r.isType("record") == false) || (r.isRealRecord() == false)) {
				continue;
                        } else {
                            try {
                                    storage.getPathsJSON(r.getID(),null);
                                    return true;
                            } catch (Exception e) {
                                    return false;
                            }
                        }
		}
		return false;
	}
	
	
	/* find csid for email, create token, email token to the user */
	private void send_reset_email(Storage storage,UIRequest request, Request in) throws UIException {

		JSONObject data = null;	
		data=request.getJSONBody();

		//mock login else service layer gets upset = not working
		// XXX ARGH
		AdminData ad = spec.getAdminData();
		request.getSession().setValue(UISession.USERID,ad.getAuthUser());
		request.getSession().setValue(UISession.PASSWORD,ad.getAuthPass());
		in.reset();
		JSONObject outputJSON = new JSONObject();
		if(testSuccess(in.getStorage())) {
			String emailparam = "";
		
			/* get csid of email address */
			try {
				emailparam = data.getString("email");
				JSONObject userdetails = getcsID(storage,emailparam);
				if(!userdetails.getBoolean("isError")){
					String csid = userdetails.getString("csid");

					/* for debug purposes */
					if(data.has("debug") && data.getBoolean("debug")){ //only send email if debug is false/null see unit test TestGeneral testPasswordReset
						outputJSON.put("token",createToken(csid));
						outputJSON.put("email", emailparam);
					}
					else{
						doEmail(csid,emailparam,in,userdetails);
					}
			
					outputJSON.put("isError",false);

					JSONObject messages = new JSONObject();
					messages.put("message", "Password reset sent to " + emailparam);
					messages.put("severity", "info");
					JSONArray arr = new JSONArray();
					arr.put(messages);
					outputJSON.put("messages", arr);
				}
				else {
					outputJSON = userdetails;
				}
				request.getSession().setValue(UISession.USERID,"");
				request.getSession().setValue(UISession.PASSWORD,"");
				in.reset();
			} catch (UIException e) {
				//throw new UIException("Failed to send email",e);
				try {
					outputJSON.put("isError", true);
					JSONObject messages = new JSONObject();
					messages.put("message", "Failed to send email: "+e.getMessage());
					messages.put("severity", "error");
					JSONArray arr = new JSONArray();
					arr.put(messages);
					outputJSON.put("messages", arr);
				} catch (JSONException e1) {
					throw new UIException("JSONException during error messaging",e);
				}
			} catch (JSONException e) {
				throw new UIException("JSONException during search on email address",e);
			}	
		}
		else{
			try {
				outputJSON.put("isError", true);
				JSONObject messages = new JSONObject();
				messages.put("message", "The admin details in cspace-config.xml failed");
				messages.put("severity", "error");
				JSONArray arr = new JSONArray();
				arr.put(messages);
				outputJSON.put("messages", arr);
			} catch (JSONException x) {
				throw new UIException("Failed to parse json: ",x);
			}
			
		}
		request.sendJSONResponse(outputJSON);
		request.setOperationPerformed(Operation.CREATE);
	}
	
	/* check token and if matches csid then reset password 
	 * */
	private void reset_password(Storage storage,UIRequest request, Request in) throws UIException {

		//mock login else service layer gets upset
		// XXX ARGH

		AdminData ad = spec.getAdminData();
		request.getSession().setValue(UISession.USERID,ad.getAuthUser());
		request.getSession().setValue(UISession.PASSWORD,ad.getAuthPass());
		in.reset();
		JSONObject outputJSON = new JSONObject();
		
		if(testSuccess(in.getStorage())) {
			JSONObject data = null;
			data=request.getJSONBody();
		
			String token;
			try {
				token = data.getString("token");
				String password=data.getString("password");
				String email=data.getString("email");
				JSONObject userdetails = getcsID(storage,email);
				if(!userdetails.getBoolean("isError")){
					String csid = userdetails.getString("csid");
					if(testToken(csid,token)){
						/* update userdetails */
						String path = csid;
						JSONObject fields = userdetails.getJSONObject("fields");
						try {
							JSONObject changedata = new JSONObject();
							JSONObject updatefields = fields;
							updatefields.put("password",password);
							changedata.put("fields",updatefields);
							changedata.put("csid",csid);
							
							sendJSON(storage,path,changedata);
							
							outputJSON.put("isError",false);
							JSONObject messages = new JSONObject();
							messages.put("message", "Your Password has been succesfully changed, Please login");
							messages.put("severity", "info");
							JSONArray arr = new JSONArray();
							arr.put(messages);
							outputJSON.put("messages", arr);
						}	catch (JSONException x) {
							throw new UIException("Failed to parse json: ",x);
						} catch (ExistException x) {
							throw new UIException("Existence exception: ",x);
						} catch (UnimplementedException x) {
							throw new UIException("Unimplemented exception: ",x);
						} catch (UnderlyingStorageException x) {
							UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
							outputJSON =  uiexception.getJSON();
						} 
					}
					else{
						outputJSON.put("isError",false);
						JSONObject messages = new JSONObject();
						messages.put("message", "Token was not valid");
						messages.put("severity", "error");
						JSONArray arr = new JSONArray();
						arr.put(messages);
						outputJSON.put("messages", arr);
					}
				}
				else{
					outputJSON = userdetails;
				}

				request.getSession().setValue(UISession.USERID,"");
				request.getSession().setValue(UISession.PASSWORD,"");
				in.reset();
			} catch (JSONException x) {
				throw new UIException("Failed to parse json: ",x);
			}
		}
		else{
			try {
				outputJSON.put("isError",false);
				JSONObject messages = new JSONObject();
				messages.put("message", "The admin details in cspace-config.xml failed");
				messages.put("severity", "error");
				JSONArray arr = new JSONArray();
				arr.put(messages);
				outputJSON.put("messages", arr);
				
			} catch (JSONException x) {
				throw new UIException("Failed to parse json: ",x);
			}
			
		}
		/* should we automagically log them in or let them do that?, 
		 * I think we should let them login, it has the advantage 
		 * that they find out straight away if they can't remember the new password  */
		request.sendJSONResponse(outputJSON);
		request.setOperationPerformed(Operation.CREATE);
	}
	

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		if(setnewpassword){
			reset_password(q.getStorage(),q.getUIRequest(),q);
		}
		else{
			send_reset_email(q.getStorage(),q.getUIRequest(),q);
		}
	}
 

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {}
}