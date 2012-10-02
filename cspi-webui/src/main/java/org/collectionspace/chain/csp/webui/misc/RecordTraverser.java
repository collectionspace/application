package org.collectionspace.chain.csp.webui.misc;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.authorities.AuthoritiesVocabulariesSearchList;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.record.RecordSearchList;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** 
 * The current search sends back a complex object which consist of a pagination object and a list of one page search results to UI whenever user clicked search. 
 * 
 *		traverser.put("restriction", restriction);
 *		traverser.put("record", this.r.getID());
 *		traverser.put("instance", "");//only auth has instance info
 *		traverser.put("total", results.getJSONObject("pagination").getString("totalItems"));
 *		traverser.put("pageNum", results.getJSONObject("pagination").getString("pageNum"));
 *		traverser.put("pageSize", results.getJSONObject("pagination").getString("pageSize"));
 *		traverser.put("itemsInPage", results.getJSONObject("pagination").getString("itemsInPage"));
 *		traverser.put("results", results.getJSONArray(key));
 *
 * Some elements in the list could be of a mixed recordType type (e.g. related records)... not sure what is the best approach for this..
 * RecordTraverser is a UI component that will display the current record alongside with its adjacent records and will be present on recordEditor page. 
 * Thus, the component would allow navigation from current record to previous/next based on some predefined order.
 * 
 * The whole interaction/navigation would be dramatically simplified if we could refer to the latest/current search through some token and an overall index of the current record. 
 * RecordTraverser component would not need the whole list of search results
 * (only current, next and previous record information, if it's available).
 * http://issues.collectionspace.org/browse/CSPACE-4806
 * {
 * "current": { Here goes a mini-record, similar to what's in search results. },
 * "previous": {...},
 * "next": {...},
 * "token": "abc",
 * "index": 6
 * }
 * @author csm22
 *
 */
public class RecordTraverser implements WebMethod  {
	private static final Logger log=LoggerFactory.getLogger(RecordTraverser.class);
	private RecordSearchList searcher;
	private AuthoritiesVocabulariesSearchList avsearcher;
	Spec spec;

	public RecordTraverser(Spec spec) { 
		this.spec = spec;
	}
	private void store_get(Storage storage,UIRequest request,String path) throws UIException {
		JSONObject outputJSON = new JSONObject();
		try {
			String[] bits = path.split("/");
			String token = bits[0];
			Integer indexvalue = Integer.valueOf(bits[1]);
			String key = UISession.SEARCHTRAVERSER+""+token;
			if(request.getSession().getValue(key) instanceof JSONObject){
				JSONObject alldata = (JSONObject)request.getSession().getValue(key);
				JSONObject pagination = new JSONObject();
				if(alldata.has("pagination")){
					pagination = alldata.getJSONObject("pagination");
				}
				JSONArray data = alldata.getJSONArray("results");
				Integer pgSz = alldata.getInt("pageSize");
				Integer pageNm = alldata.getInt("pageNum");
				Integer total = alldata.getInt("total");
				Integer offset = pgSz * pageNm;
				Integer numInstances = alldata.getInt("numInstances");
				String base = alldata.getString("record");
				String instance = alldata.getString("instance");
				JSONObject restriction = alldata.getJSONObject("restriction");
				
				//only works 100% for records - auths is a problem...
				// multiplying by numInstance isn't quite enough...
				//Integer relativeindexvalue = indexvalue - offset ;
				Integer prevpageNm = 0;
				Integer prevval = indexvalue -1;
				if(prevval >=  0 && prevval<=total){
					JSONObject item = checkTraverserPageNum(storage, request, outputJSON, token,
							key, alldata, pagination, data, pgSz, pageNm,
							numInstances, base, instance, restriction,
							prevpageNm, prevval);

					outputJSON.put("previous", item);
				}

				Integer postpageNm = 0;
				Integer postval = indexvalue +1;
				if(postval < total && postval>=0){
					JSONObject item = checkTraverserPageNum(storage, request, outputJSON, token,
							key, alldata, pagination, data, pgSz, pageNm,
							numInstances, base, instance, restriction,
							postpageNm, postval);

					outputJSON.put("next", item);
				}
				Integer currpageNm = 0;
				

				if(indexvalue < total && indexvalue>=0){
					JSONObject item = checkTraverserPageNum(storage, request, outputJSON, token,
							key, alldata, pagination, data, pgSz, pageNm,
							currpageNm, base, instance, restriction,
							prevpageNm, indexvalue);
					outputJSON.put("current", item);
				}
				
				outputJSON.put("index",indexvalue);
				outputJSON.put("token", token);
				outputJSON.put("total", total);
			}
			else{
				outputJSON.put("error", "Cannot find the traverser token");
			}
		} catch (JSONException e) {
			throw new UIException("Error with the traverser data",e);
		} catch (ExistException e) {
			throw new UIException("Error with the traverser data",e);
		} catch (UnimplementedException e) {
			throw new UIException("Error with the traverser data",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		}
		request.sendJSONResponse(outputJSON);
	}

	/**
	 * abstract the checking of what page we are on and whether we need a different one
	 * @param storage
	 * @param request
	 * @param outputJSON
	 * @param token
	 * @param key
	 * @param alldata
	 * @param pagination
	 * @param data
	 * @param pgSz
	 * @param pageNm
	 * @param numInstances
	 * @param base
	 * @param instance
	 * @param restriction
	 * @param postpageNm
	 * @param postval
	 * @throws JSONException
	 * @throws UIException
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	private JSONObject checkTraverserPageNum(Storage storage, UIRequest request,
			JSONObject outputJSON, String token, String key,
			JSONObject alldata, JSONObject pagination, JSONArray data,
			Integer pgSz, Integer pageNm, Integer numInstances, String base,
			String instance, JSONObject restriction,
			Integer postpageNm, Integer postval) throws JSONException,
			UIException, ExistException, UnimplementedException,
			UnderlyingStorageException {
		if(postval >= pgSz){
			while(postval >= (pgSz * (postpageNm + 1)) ){
				postpageNm++; //get absolute page number of this item
			}
		}
		Integer relativeindexvalue = postval - (pgSz * postpageNm);
		if(pageNm == postpageNm){//on the same page
			return (JSONObject) data.get(relativeindexvalue);
		}
		else{//not on the same page
			JSONObject prevdata = new JSONObject();
			JSONArray pdata = new JSONArray();
			if(pagination.has(postpageNm.toString())){//previously cached these items
				String prevtoken = pagination.getString(postpageNm.toString());
				String prevkey = UISession.SEARCHTRAVERSER+""+prevtoken;
				prevdata = (JSONObject) request.getSession().getValue(prevkey);
				pdata = prevdata.getJSONArray("results");
			}
			else{//probably didn't cache these items
				//new search
				restriction.put("pageNum", postpageNm);
				prevdata = subTraverser(storage, request, key, numInstances, base, instance, restriction);
				String ptoken = prevdata.getJSONObject("pagination").getString("traverser");
				pagination.put(postpageNm.toString(), ptoken);
				alldata.put("pagination", pagination);
				request.getSession().setValue(UISession.SEARCHTRAVERSER+""+token,alldata); //add page info to search results
				pdata = prevdata.getJSONArray(key);
			}
			return  (JSONObject) pdata.get(relativeindexvalue);
		}
	}
	
	private JSONObject subTraverser(Storage storage, UIRequest request,
			String key, Integer numInstances, String base, String instance,
			JSONObject restriction) throws JSONException,
			UIException, ExistException, UnimplementedException,
			UnderlyingStorageException {
		JSONObject results = new JSONObject();
		Record myr = this.spec.getRecord(base);
		if(myr.isType("record") || myr.isType("searchall")){
			this.searcher = new RecordSearchList(myr,RecordSearchList.MODE_SEARCH);
			this.searcher.configure(this.spec);	// Need to set up maps for recordtype.
			results = this.searcher.getJSON(storage,restriction,key,base);
		}
		else if(myr.isType("authority")){
			if(myr.hasInstance(instance)){
				Instance myn = myr.getInstance(instance);
				this.avsearcher = new AuthoritiesVocabulariesSearchList(myn,true);
			}
			else{
				this.avsearcher = new AuthoritiesVocabulariesSearchList(myr,true);
			}
			if(this.avsearcher != null){
				results = this.avsearcher.getJSON(storage, restriction, key);
			}
		}
		
		//cache for record traverser
		if(results.has("pagination") && results.getJSONObject("pagination").has("separatelists")){
			GenericSearch.createTraverser(request, base, instance, results, restriction, key, numInstances);
		}
		return results;
	}
	
			
	@Override
	public void configure(WebUI ui, Spec spec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_get(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
