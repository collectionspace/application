package org.collectionspace.chain.csp.webui.record;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class RecordAuthorities implements WebMethod {
	private static final Logger log = LoggerFactory
			.getLogger(RecordAuthorities.class);
	private String base;
	private Record record;

	public RecordAuthorities(Record r) {
		this.record = r;
		this.base = r.getID();
	}

	@Override
	public void configure(WebUI ui, Spec spec) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("unchecked")
	public JSONObject getTermsUsed(Storage storage, String path, JSONObject restrictions)
			throws ExistException, UnimplementedException,
			UnderlyingStorageException, JSONException {

		JSONObject paginated = new JSONObject();
		JSONArray out = new JSONArray();
		JSONObject pagination = new JSONObject();
		if (record.hasTermsUsed()) {

			JSONObject mini = storage.retrieveJSON(path + "/refs",restrictions);
			JSONObject items = mini;
			if(mini.has("listItems")){
				items = mini.getJSONObject("listItems");
			}
			if(mini.has("pagination")){
				pagination = mini.getJSONObject("pagination");
			}
			if(items.length() > 0) {
				Iterator t = items.keys();
				while (t.hasNext()) {
					String field = (String) t.next();
					if (items.get(field) instanceof JSONArray) {
						JSONArray array = (JSONArray) items.get(field);
						for (int i = 0; i < array.length(); i++) {
							JSONObject in = array.getJSONObject(i);
							JSONObject entry = getTermsUsedData(in);
							out.put(entry);
						}
					} else {
						JSONObject in = items.getJSONObject(field);
						JSONObject entry = getTermsUsedData(in);
						out.put(entry);
					}
				}
			}
		}
		paginated.put("results", out);
		paginated.put("pagination", pagination);
		return paginated;
	}

	private JSONObject getTermsUsedData(JSONObject in) throws JSONException {
		JSONObject entry = new JSONObject();
		if(in.has("csid")){
			entry.put("csid", in.getString("csid"));
			entry.put("recordtype", in.getString("recordtype"));
		}
		else{
			String uri = in.getString("uri");
			////personauthorities/urn:cspace:name(person)/items/urn:cspace:name(BobClampett1328099425416)
			String[] uribits = uri.split("/");

			entry.put("csid", uribits[4]);
			String instance = uribits[2].replaceAll("urn:cspace:name\\((.*)\\)", "$1"); //might need the authority type as well..
			String auth = uribits[1];
			String parentauth = this.record.getSpec().getRecordByServicesUrl(auth).getWebURL();
			entry.put("namespace", instance);
			entry.put("recordtype", parentauth);
		}
		// entry.put("sourceFieldName",field);
		entry.put("sourceFieldselector", in.getString("sourceFieldselector"));
		entry.put("sourceFieldName", in.getString("sourceFieldName"));
		entry.put("sourceFieldType", in.getString("sourceFieldType"));

		entry.put("number", in.getString("itemDisplayName"));
		return entry;
	}

	private void store_get(Storage storage, UIRequest ui, String path)
			throws UIException {
		try {

			JSONObject restriction = new JSONObject();
			String key = "items";

			Set<String> args = ui.getAllRequestArgument();
			for (String restrict : args) {
				if (ui.getRequestArgument(restrict) != null) {
					String value = ui.getRequestArgument(restrict);
					// if(restrict.equals("query") && search){
					// restrict = "keywords";
					// key="results";
					// }
					if (restrict.equals(WebMethod.PAGE_SIZE_PARAM)
							|| restrict.equals(WebMethod.PAGE_NUM_PARAM)
							|| restrict.equals("keywords")) {
						restriction.put(restrict, value);
					} else if (restrict.equals(WebMethod.SEARCH_QUERY_PARAM)) {
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

			outputJSON.put("termsUsed",
					getTermsUsed(storage, base + "/" + path, restriction));

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
			UIException uiexception = new UIException(x.getMessage(), x.getStatus(), x.getUrl(), x);
			ui.sendJSONResponse(uiexception.getJSON());
		}
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		Request q = (Request) in;
		store_get(q.getStorage(), q.getUIRequest(), StringUtils.join(tail, "/"));
	}

}
