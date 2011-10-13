package org.collectionspace.chain.csp.webui.record;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordRelated implements WebMethod {
	private static final Logger log = LoggerFactory
			.getLogger(RecordRelated.class);
	private String base;
	private Record record;
	private Record relatedrecord;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	private Map<String,String> servicename_to_serviceid=new HashMap<String,String>();

	public RecordRelated(Record r, Record r2) {
		this.record = r;
		this.relatedrecord = r2;
		this.base = r.getID();
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
			servicename_to_serviceid.put(r.getServicesTenantSg(), r.getID());
		}
	}
	public void configure(Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
			servicename_to_serviceid.put(r.getServicesTenantSg(), r.getID());
		}
	}
	
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {

		JSONObject restrictions = new JSONObject();
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view/relate",restrictions);
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;
	}
	
	private JSONObject generateRelationEntry(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		/* Retrieve entry */
		JSONObject restrictions = new JSONObject();
		JSONObject in=storage.retrieveJSON("relations/main/"+csid,restrictions);
		String[] dstid=in.getString("dst").split("/");
		String type=in.getString("type");
		
		JSONObject mini=generateMiniRecord(storage,servicename_to_serviceid.get(dstid[0]),dstid[1]);
		mini.put("relationshiptype",type);
		mini.put("relid",in.getString("csid"));
		return mini;
	}
	
	protected JSONObject getRelations(Storage storage, JSONObject restriction,  JSONObject recordtypes ) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException{

		JSONObject myres = restriction;
		myres.put("dstType", this.relatedrecord.getServicesTenantSg());
		// XXX needs pagination support CSPACE-1819
		JSONObject data = storage.getPathsJSON("relations/main",myres);
		String[] relations = (String[]) data.get("listItems");
		
		for(String r : relations) {
			try {
				JSONObject relateitem = generateRelationEntry(storage,r);
				String type = relateitem.getString("recordtype");
				if(!recordtypes.has(type)){
					recordtypes.put(type, new JSONArray());
				}
				recordtypes.getJSONArray(type).put(relateitem);
			} catch(Exception e) {
				// Never mind.
				//Probably should do something with the errors... could be a permissions issue
			}
		}
		return recordtypes;
	}
	
	private void store_get(Storage storage, UIRequest ui, String path)
			throws UIException {
		try {

			JSONObject restriction=new JSONObject();
			restriction.put("src",base+"/"+path);
			String key = "items";

			Set<String> args = ui.getAllRequestArgument();
			for (String restrict : args) {
				if (ui.getRequestArgument(restrict) != null) {
					String value = ui.getRequestArgument(restrict);
					// if(restrict.equals("query") && search){
					// restrict = "keywords";
					// key="results";
					// }
					if (restrict.equals("pageSize")
							|| restrict.equals("pageNum")
							|| restrict.equals("keywords")) {
						restriction.put(restrict, value);
					} else if (restrict.equals("query")) {
						// ignore - someone was doing something odd
					} else {
						// XXX I would so prefer not to restrict and just pass
						// stuff up but I know it will cause issues later
						restriction.put("queryTerm", restrict);
						restriction.put("queryString", value);
					}
				}
			}

			// Get the data
			JSONObject outputJSON = new JSONObject();
			JSONObject recordtypes = getRelations(storage, restriction, new JSONObject());
			outputJSON.put("relations", recordtypes);

			try {
				outputJSON.put("csid", path);
			} catch (JSONException e1) {
				throw new UIException("Cannot add csid", e1);
			}
			// Write the requested JSON out
			ui.sendJSONResponse(outputJSON);
		} catch (JSONException e) {
			throw new UIException("JSONException during store_get", e);
		} catch (ExistException e) {
			throw new UIException("ExistException during store_get", e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during store_get", e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception = new UIException(x.getMessage(), x
					.getStatus(), x.getUrl(), x);
			ui.sendJSONResponse(uiexception.getJSON());
		}
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		Request q = (Request) in;
		store_get(q.getStorage(), q.getUIRequest(), StringUtils.join(tail, "/"));
	}
}
