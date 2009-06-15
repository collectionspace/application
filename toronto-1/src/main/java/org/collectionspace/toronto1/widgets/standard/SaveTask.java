package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.collectionspace.toronto1.store.Store;
import org.collectionspace.toronto1.widgets.AJAXRequest;
import org.collectionspace.toronto1.widgets.Task;
import org.collectionspace.toronto1.widgets.TaskException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SaveTask implements Task {
	private static Store store=new Store(); // XXX eugh!

	public void preact(AJAXRequest data) throws TaskException {
		try {
			JSONObject a1=new JSONObject();
			a1.put("action","send-names");
			a1.put("code",data.getCode());
			data.addResult(a1);
			JSONObject a2=new JSONObject();
			a2.put("action","send-key");
			a2.put("key","__key");
			a2.put("code",data.getCode());
			data.addResult(a2);
			JSONObject a3=new JSONObject();
			a3.put("action","send-key");
			a3.put("key","__type");
			a3.put("code",data.getCode());
			data.addResult(a3);
		} catch (JSONException e) {
			throw new TaskException("Bad JSON",e);
		}
	}

	public void act(AJAXRequest request) throws TaskException {
		try {
			JSONObject action=new JSONObject();
			JSONObject data=new JSONObject((String)request.getUniqueParam("data"));
			String csid=data.optString("__key");
			if(csid==null || "".equals(csid)) {
				csid=UUID.randomUUID().toString();
			}
			store.createEntry(csid,data);
			request.setAttr("created-id",csid);
			// XXX save it
			action.put("good","saved");
			request.addResult(action);
		} catch (JSONException e) {
			throw new TaskException("Bad JSON",e);
		} catch (IOException e) {
			throw new TaskException("IO Exception writing",e);
		}
	}

	public void postact(AJAXRequest data) throws TaskException {
		if(!data.haveDestination()) {
			try {
				String url=data.servletRelativePath("/main/edit/"+data.getPage()+"/"+(String)data.getAttr("created-id"));
				JSONObject go=new JSONObject();				
				go.put("goto",url);
				data.addResult(go);
			} catch (JSONException e) {
				throw new TaskException("Bad JSON",e);				
			}
		}
	}
}
