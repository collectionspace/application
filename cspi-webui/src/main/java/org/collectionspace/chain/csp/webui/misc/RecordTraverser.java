package org.collectionspace.chain.csp.webui.misc;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
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
 * Some elements in the list could be of a mixed recordType type (e.g. related records).
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
			if(request.getSession().getValue(key) instanceof JSONArray){
				JSONArray data = (JSONArray)request.getSession().getValue(key);
				if((indexvalue -1) >=0){
					outputJSON.put("previous", data.get(indexvalue -1));
				}
				if((indexvalue +1) <=data.length()){
					outputJSON.put("next", data.get(indexvalue +1));
				}
				outputJSON.put("current", data.get(indexvalue));
				outputJSON.put("index",indexvalue.toString());
				outputJSON.put("token", token);
			}
			else{
				outputJSON.put("error", "Cannot find the traverser token");
			}
		} catch (JSONException e) {
			throw new UIException("Error with the traverser data",e);
		}
		request.sendJSONResponse(outputJSON);
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
