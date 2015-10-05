package org.collectionspace.chain.csp.webui.nuispec;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Caching termlists isn't the best thing to do but with termlists being used 
 * in UIspecs which are called every page request then there is a need to cache
 * 
 * The hope is that standard calls thr the system to update the termlists will refresh the cache
 * We can't use the vocab cache as we have modelled termlists more like records and procedures.
 * And VocabInstanceCache doesn't cache all the vocab items as authorities are too large for that to be a sensible approach
 * 
 * @author csm22
 *
 */
public class CacheTermList {
	private static final Logger log=LoggerFactory.getLogger(CacheTermList.class);
	private CSPRequestCache cache;
	
	public CacheTermList(CSPRequestCache cache){
		this.cache = cache;
	}

	public JSONArray get(Storage storage, String name, Record r, Integer limit) throws JSONException{
		JSONArray data = getCached(cache, name+":LIMIT"+limit);
		if(data !=null){
			return data;
		}
		JSONArray getallnames = controlledLists(storage, name,r, limit);
		
		setCached(cache,name+":LIMIT"+limit,getallnames);
		return getallnames;
	}
	public JSONArray get(Storage storage, String name, Record r) throws JSONException{
		return get(storage, name, r, 0);
	}
	private JSONObject getDisplayNameList(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//should be using cached results from the previous query.
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view", new JSONObject());
		return out;
	}
	

	private void setCached(CSPRequestCache cache, String name, Object obj){
		cache.setCached(getClass(),new String[]{"termlist",name},obj);
	}
	private JSONArray  getCached(CSPRequestCache cache, String name){
		return (JSONArray)cache.getCached(getClass(),new String[]{"termlist",name});
	}
	private void removeCached(CSPRequestCache cache, String name){
		cache.removeCached(getClass(),new String[]{"termlist",name});
	}
	
	public JSONArray controlledLists(Storage storage, String vocabname,Record vr, Integer limit) throws JSONException{
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

					String url = vr.getID()+"/"+vocabname;
					JSONObject data = null;
					try{
						data = storage.getPathsJSON(url,restriction);
					}
					catch(UnderlyingStorageException e){  
						// need to initialise this vocab
						log.error("Could not retreive term list with URL '%s' from Services layer.", url);
						// REM 8/28/2015 - This code is broken? See https://issues.collectionspace.org/browse/CSPACE-6151 
						Instance n  = vr.getInstance(vr.getID()+"-"+vocabname);
						JSONObject fields=new JSONObject("{'displayName':'"+n.getTitle()+"','shortIdentifier':'"+n.getWebURL()+"'}");
						if(vr.getFieldFullList("termStatus") instanceof Field){
							fields.put("termStatus", ((Field)vr.getFieldFullList("termStatus")).getOptionDefault());
						}
						storage.autocreateJSON(vr.getID(),fields,restriction);
						data = storage.getPathsJSON(url,restriction);
					}
					if(data.has("listItems")){
						String[] results = (String[]) data.get("listItems");
						/* Get a view of each */
						for(String result : results) {
							//change csid into displayName
							JSONObject namedata = getDisplayNameList(storage,vr.getID(),vocabname,result);
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
			throw new JSONException("Underlying storage exception"+vocabname + e);
		}
		return displayNames;
	}
	
	public JSONArray controlledLists(Storage storage, String vocabtype, Record r) throws JSONException{
		Record vr = r.getSpec().getRecord("vocab");
		return controlledLists(storage, vocabtype,vr,0);
	}
}
