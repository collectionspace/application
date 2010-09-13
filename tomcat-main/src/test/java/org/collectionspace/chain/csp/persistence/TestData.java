package org.collectionspace.chain.csp.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestData {
	private static final Logger log=LoggerFactory.getLogger(TestData.class);

	// Set up test data strings 
		protected final static Date d = new Date();
	
	protected final String loanoutCreate = addData("loanoutCreate.json").toString();
	protected final String loaninCreate = addData("loaninCreate.json").toString();
	protected final String intakeCreate = addData("intakeCreate.json").toString();
	protected final String objectCreate = addData("objectCreate.json").toString();
	protected final String movementCreate = addData("movementCreate.json").toString();
	protected final String acquisitionCreate =  addData("acquisitionCreate.json").toString();
	protected final String roleCreate = addData("roleCreate.json","roleName").toString();
	protected final String role2Create = addData("role2Create.json","roleName").toString();
	protected final String personCreate = addData("personCreate.json").toString();
	
	protected final String user2Create =  addData("user2Create.json").toString();
	protected final String user2Update = addData("userUpdate.json").toString();
	protected final String user2Email = addData("userEmail.json").toString();
	
	protected final static String urnTestJoe = "{\"fields\":{\"responsibleDepartment\":\"\",\"dimensionMeasurementUnit\":\"\",\"objectNumber\":\"TestObject\",\"title\":\"Test Title for urn test object\",\"objectName\":\"Test Object for urn test object\",\"inscriptionContentInscriber\":\"urn:cspace:org.collectionspace.demo:personauthority:id(de0d959d-2923-4123-830d):person:id(8a6bf9d8-6dc4-4c78-84e9)'Joe+Adamson'\"},\"csid\":\"\"}";
	
	protected final String user88Create  = addData("userCreate.json", "userId").toString();
		

	protected static JSONObject getDefaultUser(){

		JSONObject user = new JSONObject();
		try {
			user.put("userid", "test@collectionspace.org");
			user.put("password", "testtest");
			return user;
		} catch (JSONException e) {
			errored(e);
		}
		return user;
	}
	
	
	
	private JSONObject addData(String jsonfile){
		JSONObject userObj = getJSON(jsonfile);
		return userObj;
	}	
	
	private JSONObject addData(String jsonfile, String field){
		JSONObject userObj = getJSON(jsonfile);
		try {
			String stuff = userObj.getString(field);
			userObj.put(field, stuff+d.toString());
		} catch (JSONException e) {
			errored(e);
		}
		return userObj;
	}

	protected static void errored(Exception e){
		log.error("ERROR occured"+e.getMessage());
		org.junit.Assert.fail("ERROR occured"+e.getMessage());
	}
	
	private JSONObject getJSON(String in) {
		try {
			String path=TestData.class.getPackage().getName().replaceAll("\\.","/");
			InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		
			assertNotNull(stream);
			String data;
			data = IOUtils.toString(stream,"UTF-8");
			stream.close();	
			return new JSONObject(data);
		} catch (IOException e) {
			errored(e);
		} catch (JSONException e) {
			errored(e);
		}
		return null;
	}

	@Test public void test(){
		assertTrue(true);
	}
}
