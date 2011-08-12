package org.collectionspace.chain.csp.webui.main;

import java.util.ArrayList;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUIAuthZ extends TestBase {
	private static final Logger log=LoggerFactory.getLogger(TestUIAuthZ.class);
	private ArrayList<String> deleteme = new ArrayList<String>(); 
	
	protected JSONObject getRestrictedUser(String type) throws JSONException{

		JSONObject user = new JSONObject();
		JSONObject usertemplate = new JSONObject(type);
		try {
			user.put("userid", usertemplate.getString("userName"));
			user.put("password", usertemplate.getString("password"));
			return user;
		} catch (JSONException e) {
			errored(e);
		}
		return user;
	}

	/**
	 @After public void destroyUser() throws Exception{

		log.info("Delete test users for restricted tests");
		//log in as default user who has delete privileges
		ServletTester jetty = setupJetty();
		if(role_id!=null){
			//delete role
			DELETEData(role_id,jetty);
		}
		if(userid!=null){
			//delete user
			DELETEData(userid,jetty);
		}
		if(deleteme.size()>0){
			for(String item: deleteme){
				DELETEData(item,jetty);
			}
		}
	}
	**/
	
	@Before public void createUsers() throws Exception{

		
		log.info("Create test users for restricted tests");
		ServletTester jetty = setupJetty();
		HttpTester out;
		

		//only create roles if there are less than 5 roles
		//yeap arbitrary hack 
		out = GETData("/role",jetty);

		JSONObject result = new JSONObject(out.getContent());
		JSONArray items = result.getJSONArray("items");
		if (items.length() > 4) {
			//do nothing we have enough users/roles
		}
		else{
//READ
		log.info("CREATE READ USER");
		out = POSTData("/role",roleRead,jetty);
		String roler_id = out.getHeader("Location");
		deleteme.add(roler_id);
		JSONObject userrdata = createUserWithRolesById(jetty,userRead,roler_id); 
		out=POSTData("/users/",makeRequest(userrdata).toString(),jetty);
		String user_r_id = out.getHeader("Location");
		deleteme.add(user_r_id);
//WRITE		
		log.info("CREATE WRITE USER");
		out = POSTData("/role",roleWrite,jetty);
		String rolew_id = out.getHeader("Location");
		deleteme.add(rolew_id);
		JSONObject userwdata = createUserWithRolesById(jetty,userWrite,rolew_id); 
		out=POSTData("/users/",makeRequest(userwdata).toString(),jetty);
		String user_w_id = out.getHeader("Location");
		deleteme.add(user_w_id);
//NONE
		log.info("CREATE HALF NONE USER");
		out = POSTData("/role",roleNone1,jetty);
		String rolen1_id = out.getHeader("Location");
		deleteme.add(rolen1_id);
		JSONObject usern1data = createUserWithRolesById(jetty,userNone1,rolen1_id); 
		out=POSTData("/users/",makeRequest(usern1data).toString(),jetty);
		String user_n1_id = out.getHeader("Location");
		deleteme.add(user_n1_id);
		
		log.info("CREATE OTHER HALF NONE USER");
		out = POSTData("/role",roleNone2,jetty);
		String rolen2_id = out.getHeader("Location");
		deleteme.add(rolen2_id);
		JSONObject usern2data = createUserWithRolesById(jetty,userNone2,rolen2_id); 
		out=POSTData("/users/",makeRequest(usern2data).toString(),jetty);
		String user_n2_id = out.getHeader("Location");
		deleteme.add(user_n2_id);
		
		log.info("CREATE NONE USER");
		out = POSTData("/role",roleNone,jetty);
		String rolen_id = out.getHeader("Location");
		deleteme.add(rolen_id);
		JSONObject userndata = createUserWithRolesById(jetty,userNone,rolen_id); 
		out=POSTData("/users/",makeRequest(userndata).toString(),jetty);
		String user_n_id = out.getHeader("Location");
		deleteme.add(user_n_id);
		}
	}


	
	/**
	 * Test Roles & Permissions CRUDL & UIspecs
	 */
	@Test public void testAuthZ() throws Exception {
		ServletTester jetty=setupJetty();
		testPostGetDelete(jetty, "/role/", roleCreate, "description");
		testLists(jetty, "role", roleCreate, "items");
		//testPostGetDelete(jetty, "/permission/", permissionRead, "resourceName");
		//testPostGetDelete(jetty, "/permrole/", permroleCreate, "");
		

		log.info("Testing UISPEC");
		testUIspec(jetty, "/role/uispec", "roles.uispec");
		testUIspec(jetty, "/users/uispec", "users.uispec");
		testUIspec(jetty, "/role/uispec", "roles.uispec");
		testUIspec(jetty, "/permission/uispec", "permissions.uispec");
		testUIspec(jetty, "/permrole/uispec", "permroles.uispec");
	}
	
	

	/**
	 * Test User roles
	 */
	@Test public void testUserRolesUI() throws Exception{
		ServletTester jetty = setupJetty();
		JSONObject userdata = createUserWithRoles(jetty,user88Create,roleCreate);
		JSONObject userdata2 = createUserWithRoles(jetty,user88Create,role2Create);
//create user with roles in payload
		HttpTester out = POSTData("/users/",makeRequest(userdata),jetty);
		String userid = out.getHeader("Location");
		log.info("2::"+userid);

		out = GETData(userid,jetty);

		String screenname = userdata2.getString("userName");
		userdata2.remove("userName");
		userdata2.put("screenName", screenname);
		
		
		out = PUTData(userid,makeRequest(userdata2),jetty);

		out = GETData(userid,jetty);

		JSONObject data = new JSONObject(out.getContent());
		JSONArray roles = data.getJSONObject("fields").getJSONArray("role");
		//delete roles

		//Delete the roles
		String roles_id1 = "/role/" + userdata.getJSONArray("role").getJSONObject(0).getString("roleId");
		String roles_id2 = "/role/" + userdata2.getJSONArray("role").getJSONObject(0).getString("roleId");


		DELETEData(roles_id1,jetty);
		DELETEData(roles_id2,jetty);
		
		//delete user
		DELETEData(userid,jetty);
		

		//test role_1 deleted to payload
		assertEquals("Should only be one role, if more then it didn't delete, if less then it didn't add",1,roles.length());

		//test role_2 added to payload
		for(int i=0; i<roles.length();i++){
			JSONObject role = roles.getJSONObject(i);
			//assertEquals()
			if(!role.getString("roleName").equals("ROLE_SPRING_ADMIN")){
				assertEquals(role.getString("roleName"),userdata2.getJSONArray("role").getJSONObject(0).getString("roleName"));
			}
		}
		
	}
	/**
	 * Test Roles Permissions
	 */
	@Test public void testRolesPermsUI() throws Exception {

		ServletTester jetty = setupJetty();
//		create role with permissions
		JSONObject rolepermsdata = createRoleWithPermission(roleCreate,"loanin", "loanout"); 
		JSONObject roleperms2data = createRoleWithPermission(roleCreate,"acquisition", "intake"); 

		log.info(rolepermsdata.toString());
		HttpTester out = POSTData("/role/",makeRequest(rolepermsdata),jetty);
		String role_id = out.getHeader("Location");

		//get role
		out = GETData(role_id,jetty);

		//test
		JSONObject data = new JSONObject(out.getContent());
		log.info(data.toString());
		

		//update role
		log.info("roleperms2data:"+roleperms2data.toString());
		out = PUTData(role_id,makeRequest(roleperms2data),jetty);
		//test
		JSONObject dataUP = new JSONObject(out.getContent());
		
		
		
		//delete role		
		DELETEData(role_id,jetty);
		
		
		//test data GET
		log.info("GET: "+data.toString());
		JSONArray perms = data.getJSONObject("fields").getJSONArray("permissions");
		int test = 0;
		for(int i=0; i<perms.length();i++){
			JSONObject thisperm = perms.getJSONObject(i);
			if(thisperm.getString("resourceName").equals("loanout")){
				assertEquals("write",thisperm.getString("permission"));
				test++;
			}
			if(thisperm.getString("resourceName").equals("loanin")){
				assertEquals("read",thisperm.getString("permission"));
				test++;
			}
		}
		assertEquals("failed to find loansout and loansin",2,test);


		//test data UPDATE
		log.info("UPDATA: "+dataUP.toString());
		JSONArray permsUP = dataUP.getJSONObject("fields").getJSONArray("permissions");
		int testUP = 0;
		for(int i=0; i<permsUP.length();i++){
			JSONObject thisperm = permsUP.getJSONObject(i);
			if(thisperm.getString("resourceName").equals("intake")){
				assertEquals("write",thisperm.getString("permission"));
				testUP++;
			}
			if(thisperm.getString("resourceName").equals("acquisition")){
				assertEquals("read",thisperm.getString("permission"));
				testUP++;
			}
		}
		assertEquals("failed to find acquisitions and intakes",2,testUP);
	}
	
	/**
	 * Test the User Profiles 
	 * 
	 * A password reset is a 2 stage process:
	 * The user asks to reset the password - POST with users email address ( App generates a token)
	 * The user sets a new pw - POST ( App layer checks pw, email and token)
	 * 
	 * I'm unsure as to what was intended here.  Seems to send a password reset with the pw unchanged.
	 * Checks User Id is unchanged then does update and again checks User Id unchanged
	 * I'm confused about what was intended by the CheckJSONEquiv... routines as with the current data they would
	 * never match. 
	 * @throws Exception
	 */
		@Test public void testUserProfilesWithReset() throws Exception {
			ServletTester jetty=setupJetty();
			HttpTester out;
			//delete user if already exists 
			out = createUser(jetty,user2Create);
			String id=out.getHeader("Location");

			out = GETData(id,jetty);
			log.info(out.getContent());
			//ask to reset
			out = POSTData("/passwordreset/",user2Email,jetty);
					
			//this should fail - switch this on when we want to test with a failing token
			/*JSONObject obj = new JSONObject(out.getContent());
			Long token = Long.parseLong(obj.getString("token"));
			token -= (8*24*60*60*10000);
			obj.put("token", token);
			*/
			JSONObject obj = new JSONObject(out.getContent());
			obj.put("password", "testetst");

			// Reset password
			out = POSTData("/resetpassword/",obj,jetty);
			
			// Read - seems to be failing -need to refresh jetty - probably because I hashed the login to get teh reset
			jetty=setupJetty();
			HttpTester out2 = GETData(id,jetty);
			
			// Checks User Id is unchanged
			log.info(out2.getContent());
			JSONObject user2AfterReset=new JSONObject(out2.getContent());
			JSONObject user2CreateCopy=new JSONObject(user2Create);
			assertEquals(user2AfterReset.getJSONObject("fields").get("userId").toString(),user2CreateCopy.get("userId").toString());
			
			// Don't know what this is aiming at (commented out already) but the much of the content is different eg status
			//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())), user2CreateCopy));
			
			// Updates - changes screen name and status
			out = PUTData(id,makeSimpleRequest(user2Update),jetty);
			
			// Read
			out = GETData(id,jetty);
			
			// Check User Id is unchanged
			JSONObject user2AfterUpdate=new JSONObject(out.getContent());
			JSONObject user2UpdateCopy=new JSONObject(user2Update);
			assertEquals(user2AfterUpdate.getJSONObject("fields").get("userId").toString(),user2UpdateCopy.get("userId").toString());
			//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())), user2UpdateCopy));
			
			// Delete
			DELETEData(id,jetty);
			
			
		}
		
	
}
