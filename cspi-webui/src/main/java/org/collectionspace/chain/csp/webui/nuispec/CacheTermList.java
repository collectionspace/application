package org.collectionspace.chain.csp.webui.nuispec;

import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheTermList {
	private static final Logger log=LoggerFactory.getLogger(CacheTermList.class);
	public JSONObject controlledCache;
	
	public CacheTermList(){
		this.controlledCache = new JSONObject();
	}

	private JSONObject getDisplayNameList(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//should be using cached results from the previous query.
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view", new JSONObject());
		return out;
	}
	
	public JSONArray controlledLists(Storage storage, String vocabtype,Record vr, Integer limit) throws JSONException{
		JSONArray displayNames = new JSONArray();
		try {
		    // Get List
			int resultsize =1;
			int pagenum = 0;
			int pagesize = 200;
			if(limit !=0 && limit < pagesize){
				pagesize = limit;
			}
			while(resultsize >0){
				JSONObject restriction=new JSONObject();
				restriction.put("pageNum", pagenum);
				restriction.put("pageSize", pagesize);
				Instance n = vr.getInstance(vocabtype);
				
				String url = vr.getID()+"/"+n.getTitleRef();
				JSONObject data = storage.getPathsJSON(url,restriction);
				if(data.has("listItems")){
					String[] results = (String[]) data.get("listItems");
					/* Get a view of each */
					for(String result : results) {
						//change csid into displayName
						JSONObject namedata = getDisplayNameList(storage,vr.getID(),n.getTitleRef(),result);
						displayNames.put(namedata);
					}

					Integer total = data.getJSONObject("pagination").getInt("totalItems");
					pagesize = data.getJSONObject("pagination").getInt("pageSize");
					//Integer itemsInPage = data.getJSONObject("pagination").getInt("itemsInPage");
					pagenum = data.getJSONObject("pagination").getInt("pageNum");
					pagenum++;
					//are there more results
					if(total <= (pagesize * (pagenum))){
						break;
					}
					//have we got enough results?
					if(limit !=0 && limit <= (pagesize * (pagenum)) ){
						break;
					}
				}
				else{
					resultsize=0;
				}
			}
		} catch (ExistException e) {
			throw new JSONException("Exist exception");
		} catch (UnimplementedException e) {
			throw new JSONException("Unimplemented exception");
		} catch (UnderlyingStorageException e) {
			throw new JSONException("Underlying storage exception"+vocabtype + e);
		}
		return displayNames;
	}
	
	public JSONArray controlledLists(Storage storage, String vocabtype, Record r) throws JSONException{
		Record vr = r.getSpec().getRecord("vocab");
		return controlledLists(storage, vocabtype,vr,0);
	}
}
