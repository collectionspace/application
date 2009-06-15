package org.collectionspace.toronto1.widgets;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public interface Task {
	public void preact(AJAXRequest request) throws TaskException;
	public void act(AJAXRequest request) throws TaskException;
	public void postact(AJAXRequest request) throws TaskException;
}
