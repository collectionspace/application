package org.collectionspace.toronto1.widgets.standard;

import java.util.Map;

import org.collectionspace.toronto1.widgets.AJAXRequest;
import org.collectionspace.toronto1.widgets.Task;
import org.collectionspace.toronto1.widgets.TaskException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GotoTask implements Task {
	private String page;

	public GotoTask(String page) {
		this.page=page;
	}

	public void preact(AJAXRequest request) throws TaskException {}
	public void postact(AJAXRequest request) throws TaskException {}
	
	public void act(AJAXRequest data) throws TaskException {
		try {
			JSONObject go=new JSONObject();
			go.put("goto",page);
			data.addResult(go);
			data.setDestination();
		} catch (JSONException e) {
			throw new TaskException("Bad JSON",e);
		}
	}

}
