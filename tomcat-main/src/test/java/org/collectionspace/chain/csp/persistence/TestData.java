/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.collectionspace.csp.helper.test.TestConfigFinder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class TestData {
	private static final Logger log=LoggerFactory.getLogger(TestData.class);

	// Set up test data strings 
	private Spec spec = null;

    public final String objectCreate(){ return addData("objectCreate.json").toString(); };

	public final String acquisitionCreate(){ return addData("acquisitionCreate.json").toString(); };
	public final String conditioncheckCreate(){ return addData("conditioncheckCreate.json").toString(); };
	public final String conservationCreate(){ return addData("conservationCreate.json").toString(); };
	public final String exhibitionCreate(){ return addData("exhibitionCreate.json").toString(); };
	public final String intakeCreate(){ return addData("intakeCreate.json").toString(); };
	public final String loaninCreate(){ return addData("loaninCreate.json").toString(); };
	public final String loanoutCreate(){ return addData("loanoutCreate.json").toString(); };
	public final String movementCreate(){ return addData("movementCreate.json").toString(); };
	public final String valuationcontrolCreate(){ return addData("valuationcontrolCreate.json").toString(); };
	public final String mediaCreate(){ return addData("mediaCreate.json").toString(); };
 	public final String objectexitCreate(){ return addData("objectexitCreate.json").toString(); };
 	public final String termlistCreate(){ return addData("termlistCreate.json").toString(); };
 	public final String groupCreate(){ return addData("groupCreate.json").toString(); };

	public final String roleCreate(){ return addData("roleCreate.json","displayName").toString(); };
	public final String role2Create(){ return addData("role2Create.json","displayName").toString(); };
	public final String personCreate(){ return addData("personCreate.json").toString(); };

	public final String roleRead(){ return  addData("role_read.json").toString(); };
	public final String roleWrite(){ return addData("role_write.json").toString(); };
	public final String roleNone(){ return  addData("role_none.json").toString(); };
	public final String roleNone1(){ return  addData("role_none1.json").toString(); };
	public final String roleNone2(){ return  addData("role_none2.json").toString(); };
	
	public final String userRead(){ return addData("userRead.json").toString(); };
	public final String userWrite(){ return addData("userWrite.json").toString(); };
	public final String userNone(){ return addData("userNone.json").toString(); };
	public final String userNone1(){ return  addData("userNone1.json").toString(); };
	public final String userNone2(){ return addData("userNone2.json").toString(); };

	public final String user2Create(){ return addData("user2Create.json").toString(); };
	public final String user2Update(){ return addData("userUpdate.json").toString(); };
	public final String user2Email(){ return addData("userEmail.json").toString(); };
	
	public final String user88Create (){ return addData("userCreate.json", "userId").toString(); };

	private  InputSource getSource(String fallbackFile) {
        TestData demo = new TestData();
		try {
			return TestConfigFinder.getConfigStream(fallbackFile, true);
		} catch(Exception ce){	
			try{
				return TestConfigFinder.getConfigStream();
			} catch (CSPDependencyException e) {
				String name=demo.getClass().getPackage().getName().replaceAll("\\.","/")+"/"+fallbackFile;
				return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
			}
		}
	}
	public final Spec getSpec(ServletTester tester){
		if(this.spec == null){
			CSPManager cspm=new CSPManagerImpl();
			cspm.register(new CoreConfig());
			cspm.register(new Spec());
			try {
				cspm.go();
				tester.getAttribute("config-filename");
				String filename=(String)tester.getAttribute("config-filename");
				cspm.configure(getSource(filename), new ConfigFinder(null), false);
			} catch (CSPDependencyException e) {
				log.info("CSPManagerImpl failed");
				log.info(tester.getAttribute("config-filename").toString());
				log.info(e.getLocalizedMessage() );
			}
	
			ConfigRoot root=cspm.getConfigRoot();
			Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
			this.spec = spec;
		}
		return this.spec;
	}

	public JSONObject getDefaultUser(ServletTester tester){
		Spec spec = getSpec(tester);
		String username = spec.getAdminData().getAuthUser();
		String pass = spec.getAdminData().getAuthPass();
		String tenant = spec.getAdminData().getTenant();
		JSONObject user = new JSONObject();
		try {
			user.put("userid", username);
			user.put("password", pass);
			user.put("tenant", tenant);
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
			Date d = new Date();
			userObj.put(field, stuff+d.toString());
		} catch (JSONException e) {
			errored(e);
		}
		return userObj;
	}

	public  void errored(Exception e){
		log.error("ERROR occured"+e.getMessage());
		org.junit.Assert.fail("ERROR occured"+e.getMessage());
	}

	@Test public void test(){
		assertTrue(true);
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

}
