package org.collectionspace.chain.csp.persistence.services.authorization;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Cache all permissions and their actions for speedy access
 * 
 * 
 * @author csm22
 *
 */
public class PermissionCache {

	private Map<String,List<String>> permissions=new HashMap<String,List<String>>();
	private Map<String,Map<String,String>> csids=new HashMap<String,Map<String,String>>();
	private Map<String,String> resourcenames=new HashMap<String,String>();
	private Map<String,String> actions=new HashMap<String,String>();
	private ServicesConnection conn;
	private Record r;

	public PermissionCache(Record r,ServicesConnection conn) {
		this.conn=conn;
		this.r=r;
		this.actions = getActions();
	}
	
	/**
	 * Allow user to configure default list of actions if they want
	 * @param r
	 * @param conn
	 * @param actions
	 */
	public PermissionCache(Record r,ServicesConnection conn,Map<String,String> actions) {
		this.conn=conn;
		this.r=r;
		this.actions = actions;
	}
	
	/**
	 * READ=1:CREATE=2;UPDATE=4;DELETE=8;SEARCH=16
	 * update = READ,CREATE,UPDATE,SEARCH = 23
	 * delete = READ,CREATE,UPDATE,DELETE,SEARCH =31 
	 * read = READ,SEARCH = 9
	 * 
	 * @return
	 */
	Map<String,String> getActions(){
		Map<String,String> actions = new HashMap<String,String>();
		actions.put("read", "17");
		actions.put("update", "23");
		actions.put("delete", "31");
		actions.put("none", "0");
		return actions;
	}
	
	/**
	 * list all the permission groups we have
	 * e.g. intake: needs resourceName intake and intake/@/items
	 * @param creds
	 * @param cache
	 * @param resourceName
	 * @throws UnderlyingStorageException 
	 * @throws ConnectionException 
	 * @throws ExistException 
	 */
	@SuppressWarnings("null")
	private void buildPermissionsList(CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException, UnderlyingStorageException, ExistException{
		
		String[] actions = getAllUIactions();
		
		//get all the records from the default xml
		Record[] records = r.getSpec().getAllRecords();
		
		//loop over all types of records in the default.xml
		for(Record record : records){
			String pName = record.getID();
			for(String action : actions){
				String hashkey = action+":"+pName;

				Map<String,String> alltypes = new HashMap<String,String>();
				String[] atypes = record.getAllAuthorizationTypes();
				for(String permResourceName : atypes){
					//will create if missing
					String csid = getPermissionObject(creds,cache, permResourceName,  action);
					String hashref = action+":"+permResourceName;
					alltypes.put(hashref, csid);
				}
				csids.put(hashkey, alltypes);
				if(permissions.containsKey(pName)){
					permissions.get(pName).add(action);
				}
				else{
					List<String> temp =  new ArrayList<String>();
					temp.add(action);
					permissions.put(pName, temp);
				}
			}
		}

		for(String action : actions){
			//XXX ACK = this will irritate me later in life...
			//do the bits that aren't in default.xml
			String pName = "other";
			String hashkey = action+":"+pName;
			String[] missingpermissions = {"vocabularies","vocabularyitems","/vocabularies/*/items/","relations","relations/subject/*/type/*/object/*","contacts","notes"};
			Map<String,String> alltypes = new HashMap<String,String>();
			for(String permResourceName : missingpermissions){
				//will create if missing
				String csid = getPermissionObject(creds,cache, permResourceName,  action);
				String hashref = action+":"+permResourceName;
				alltypes.put(hashref, csid);
			}
			csids.put(hashkey, alltypes);
			if(permissions.containsKey(pName)){
				permissions.get(pName).add(action);
			}
			else{
				List<String> temp2 =  new ArrayList<String>();
				temp2.add(action);
				permissions.put(pName, temp2);
			}
		}
		
	}

	/**
	 * return permissions grouped for UI
	 * {"resourceName":"intake","actions":{[a],[]}};
	 * @param creds
	 * @param cache
	 * @param UIResource
	 * @return
	 * @throws UnderlyingStorageException 
	 */
	public JSONObject retrieveJSON(CSPRequestCredentials creds, CSPRequestCache cache, String UIResource) throws UnderlyingStorageException{
		JSONObject out = new JSONObject();

		try {
			out.put("resourceName", UIResource);
			//out.put("effect", "PERMIT");
			Map<String, Map<String, String>> recordpermissions;
				recordpermissions = getPermissionsByRecordType(creds, cache, UIResource);
			
			JSONArray jsonActions = new JSONArray();
			for(String act : recordpermissions.keySet()){
				JSONObject actpar = new JSONObject();
				JSONObject named = new JSONObject();
				JSONArray namedarray = new JSONArray();
				named.put("name", act);
				namedarray.put(named);
				actpar.put("action",namedarray);
				jsonActions.put(actpar);
			}
			out.put("actions", jsonActions);
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (UnderlyingStorageException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (ExistException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}
	/**
	 * brake/adjust the expected functionality to get data from the cache rather than direct from the service layer
	 * 
	 * @param root
	 * @param creds
	 * @param cache
	 * @param rootPath
	 * @param restrictions
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public JSONObject getPathsJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String rootPath, JSONObject restrictions) 
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try{
			List<String> listitems=new ArrayList<String>();
			//if we specified some kind of pagination or keyword then honour it - else return everything
			//not sure how to do that yet - but willwork on it
			JSONObject out = new JSONObject();
			JSONObject pagination = new JSONObject();
			
			//populate permissions
			if(permissions.isEmpty()){
				synchronized(getClass()) {
					buildPermissionsList(creds,cache);
				}
			}
			
			for ( String key : permissions.keySet() ){
				//support keyword searching
				if(restrictions!=null && restrictions.has("keywords")){
					String data=URLEncoder.encode(restrictions.getString("keywords"),"UTF-8");
					if(key.contains(data)){
						listitems.add(key);
					}
				}
				else{
					listitems.add(key);
				}
			}

			pagination.put("pageNum", "0");
			pagination.put("pageSize", listitems.size());
			pagination.put("itemsInPage", listitems.size());
			pagination.put("totalItems",listitems.size());
			//default fill with everything
			out.put("listItems", listitems.toArray(new String[0]));	
			
			
			//pagination support
			if(restrictions!=null && restrictions.has("pageSize")){
				//only return a subset if needed
				Integer pageSize = Integer.parseInt(restrictions.getString("pageSize"));
				if(pageSize < listitems.size()){ // more results available than pagesize
					if(restrictions!=null && restrictions.has("pageNum")){
						Integer pagenum = Integer.parseInt(restrictions.getString("pageNum"));
						if((pagenum * pageSize) > listitems.size()){ //less results than needed for current page start
							//return null
							String[] empty = {};
							out.put("listItems", empty);
						}
						else{
							Integer start = pagenum * pageSize;
							Integer end = (pagenum + 1) * pageSize;
							if(end > listitems.size()){
								end = listitems.size();
							}
							List<String> subitems = listitems.subList(start, end);
							pagination.put("pageNum", pagenum);
							pagination.put("pageSize", pageSize);
							pagination.put("itemsInPage", end);
							pagination.put("totalItems",listitems.size());
							out.put("listItems", subitems.toArray(new String[0]));
						}
					}
					else{
						Integer start = 0;
						Integer end = pageSize;
						if(end > listitems.size()){
							end = listitems.size();
						}
						List<String> subitems = listitems.subList(start, end);
						pagination.put("pageNum", "0");
						pagination.put("pageSize", pageSize);
						pagination.put("itemsInPage", end);
						pagination.put("totalItems",listitems.size());
						out.put("listItems", subitems.toArray(new String[0]));
					}
				}
			}
			out.put("pagination", pagination);

		return out;
		
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}
	
	/**
	 * get a list of all existing permissions and their actions
	 * and add to resourcenames
	 * 
	 * @param creds
	 * @param cache
	 * @throws ConnectionException
	 * @throws UnderlyingStorageException
	 */
	@SuppressWarnings("unchecked")
	private void buildPermissions(CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException, UnderlyingStorageException {

		int resultsize =1;
		Integer pageNum = 0;
		String checkpagination = "";
		/* pagination support */

		while(resultsize >0){

			String postfix = "?";
			//postfix += "res="+resourceName+"&";
			postfix += "pgSz=100&";
			postfix += "pgNum="+pageNum.toString()+"&";

			postfix = postfix.substring(0, postfix.length()-1);
			if(postfix.length() == 0){postfix +="/";}
		
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,"/"+ r.getServicesURL() +postfix,null, creds, cache);
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve permissions");

			//next page
			pageNum++;
			
			Document list=null;
			list=doc.getDocument();
			
			List<Node> nodes=list.selectNodes("/"+r.getServicesListPath().split("/")[0]+"/*");
			

			if(nodes.size() == 0 ){
				resultsize=0;
				break;
			}
			else{
				Boolean testOnce = false;
				for(Node node : nodes) {
					Integer total = 0;
					if(node.matches("/"+r.getServicesListPath())){
						String csid = node.valueOf( "@csid" );
						if(!testOnce){
							if(checkpagination.equals(csid)){
								resultsize=0;
								//testing whether we have actually returned the same page or the next page - all csid returned should be unique
								
								break;
							}
							checkpagination = csid;
						}
						String resourcename = node.selectSingleNode("resourceName").getText();
						//<action Hjid="57"><name>DELETE</name></action><action Hjid="56"><name>UPDATE</name></action><action Hjid="55"><name>READ</name></action>
						List<Node> actions = node.selectNodes("action");
						for(Node acnode : actions){
							String actionName = acnode.selectSingleNode("name").getText();
							if(actionName.equals("READ"))
								total += 1;
							else if(actionName.equals("CREATE"))
								total += 2;
							else if(actionName.equals("UPDATE"))
								total += 4;
							else if(actionName.equals("DELETE"))
								total += 8;
							else if(actionName.equals("SEARCH"))
								total += 16;
						}
						
						String action = convertActionIntToUIName(total);
						String hashkey = action + ":" + resourcename;
					
						// add key if missing
						if(!resourcenames.containsKey(hashkey)){
							resourcenames.put(hashkey,csid);
						}
					}
				}
			}
		}
		
	}

	/**
	 * Get list of all UI actions e.g. delete,update,read,none
	 * @return
	 */
	private String[] getAllUIactions(){
		return this.actions.keySet().toArray(new String[0]);
	}
	
	/**
	 * Helper function to change Service permissions from numeric 
	 * to easily work out type of permission grouping
	 * @param actionInt
	 * @return
	 */
	private String convertActionIntToUIName(Integer actionInt){
		for ( String key : this.actions.keySet() ){
			if(this.actions.get(key).equals(actionInt.toString())){
				return key;
			}
		}
		return "";
	}
	

	/**
	 * create an individual permission resource and cache it
	 * if it does not exist
	 * @param creds
	 * @param cache
	 * @param resourcename
	 * @param action
	 * @throws ConnectionException
	 * @throws UnderlyingStorageException
	 * @throws ExistException
	 */
	private synchronized void createPermission(CSPRequestCredentials creds,CSPRequestCache cache,String resourcename,String action) throws ConnectionException, UnderlyingStorageException, ExistException {
		String hashkey = action+":"+resourcename;
		
		//might need to change to a hash if they need more complex permissions
		String[] read = {"search","read"};
		String[] update = {"search","read","create","update"};
		String[] delete = {"search","read","create","update","delete"};
		String[] none = {};
		
		String[] actions =null;
		if(action.equals("read")){	actions = read;	}
		else if(action.equals("update")){	actions=update;	}
		else if(action.equals("delete")){	actions=delete;	}
		else if(action.equals("none")){	actions=none;	}
		else{	actions=none;	}
		JSONObject jsonObject = new JSONObject();
		
		try {
			jsonObject.put("resourceName", resourcename);
			jsonObject.put("effect", "PERMIT");
			
			JSONArray jsonActions = new JSONArray();
			for(String act : actions){
				JSONObject actpar = new JSONObject();
				JSONObject named = new JSONObject();
				JSONArray namedarray = new JSONArray();
				named.put("name", act.toUpperCase());
				namedarray.put(named);
				actpar.put("action",namedarray);
				jsonActions.put(actpar);
			}
			jsonObject.put("actions", jsonActions);
			//String permissionDelete = "{ \"resourceName\": \"intake\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]}, {\"action\": [{ \"name\": \"DELETE\" }]} ], \"effect\": \"PERMIT\" }";
		
			Map<String,Document> parts=new HashMap<String,Document>();
			Document doc = null;
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				doc=XmlJsonConversion.convertToXml(r,jsonObject,section);
				parts.put(record_path[0],doc);
			}
			ReturnedURL url;
			//some records are accepted as multipart in the service layers, others arent, that's why we split up here
			if(r.isMultipart())
				url = conn.getMultipartURL(RequestMethod.POST,r.getServicesURL()+"/",parts,creds,cache);
			else
				url = conn.getURL(RequestMethod.POST, r.getServicesURL()+"/", doc, creds, cache);
			if(url.getStatus()>299 || url.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+url.getStatus());
			
			resourcenames.put(hashkey,url.getURLTail());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("JSONException",e);
		} 
	}


	/**
	 * individual permission items
	 * @param creds
	 * @param cache
	 * @param resourcename "loansin//stuff
	 * @param action
	 * @return
	 * @throws ConnectionException
	 * @throws UnderlyingStorageException
	 * @throws ExistException
	 * @throws UnimplementedException 
	 */
	String getPermissionObject(CSPRequestCredentials creds,CSPRequestCache cache,String resourcename, String action) throws ConnectionException, UnderlyingStorageException, ExistException {
		String hashkey = action+":"+resourcename;
		if(resourcenames.containsKey(hashkey))
			return resourcenames.get(hashkey);
		synchronized(getClass()) {
			buildPermissions(creds,cache);
			if(resourcenames.containsKey(hashkey))
				return resourcenames.get(hashkey);
			createPermission(creds,cache,resourcename,action);
			if(resourcenames.containsKey(hashkey))
				return resourcenames.get(hashkey);
			throw new UnderlyingStorageException("Bad permission "+hashkey);
		}
	}

	/**
	 * get the csid for a specific resource and action pairing
	 * 
	 * @param creds
	 * @param cache
	 * @param resourcename {'accounts','loansin',...}
	 * @param action {'read','update','delete','none'}
	 * @return list of csids that are needed to make this permission [csid,csid]
	 * @throws ConnectionException
	 * @throws UnderlyingStorageException
	 * @throws ExistException
	 */
	Map<String,String> getPermissionIdList(CSPRequestCredentials creds,CSPRequestCache cache,String resourcename,String action) throws ConnectionException, UnderlyingStorageException, ExistException {
		String hashkey = action+":"+resourcename;
		if(csids.containsKey(hashkey)){
			return csids.get(hashkey);
		}
		
		synchronized(getClass()) {
			buildPermissionsList(creds,cache);
			if(csids.containsKey(hashkey))
				return csids.get(hashkey);
			
			//if it still doesn't exist then we have something that isn't in default.xml and isn't in are fall back list
			throw new UnderlyingStorageException("Unknown permission "+resourcename+" for "+action);
		}
	}
	
	/**
	 * Get list of all the actions and their permission csids for a particular RecordType
	 * @param creds
	 * @param cache
	 * @param resourcename
	 * @return
	 * @throws ConnectionException
	 * @throws UnderlyingStorageException
	 * @throws ExistException
	 */
	Map<String, Map<String,String>> getPermissionsByRecordType(CSPRequestCredentials creds,CSPRequestCache cache,String resourcename) throws ConnectionException, UnderlyingStorageException, ExistException{
		Map<String,Map<String,String>> permissionActions=new HashMap<String,Map<String,String>>();
		if(permissions.containsKey(resourcename)){
			for(String action : permissions.get(resourcename)){
				Map<String,String> actionCsids = getPermissionIdList(creds,cache,resourcename,action); 
				permissionActions.put(action, actionCsids);
			}
			return permissionActions;
		}
		synchronized(getClass()) {
			buildPermissionsList(creds,cache);
			if(permissions.containsKey(resourcename)){
				for(String action : permissions.get(resourcename)){
					Map<String,String> actionCsids = getPermissionIdList(creds,cache,resourcename,action); 
					permissionActions.put(action, actionCsids);
				}
				return permissionActions;
			}
			
			//if it still doesn't exist then we have something that isn't in default.xml and isn't in are fall back list
			throw new UnderlyingStorageException("Unknown permission "+resourcename);
		}	
	}

	/**
	 * Get all the permissions we have set up
	 * @return ['loanin','loaninout','other']
	 */
	String[] getAllPermissions(){
		return csids.keySet().toArray(new String[0]);
	}
}
