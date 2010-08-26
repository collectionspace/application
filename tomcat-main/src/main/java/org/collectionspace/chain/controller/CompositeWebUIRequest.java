package org.collectionspace.chain.controller;

import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONObject;

public class CompositeWebUIRequest implements UIRequest {
	private WebUIRequest parent;
	
	public CompositeWebUIRequest(WebUIRequest parent) {
		this.parent=parent;
	}
	
	@Override
	public void deleteRedirectArgument(String key) throws UIException {
		// TODO Auto-generated method stub

	}

	@Override
	public JSONObject getJSONBody() throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getPostBody() throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getPrincipalPath() throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRedirectArgument(String key) throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestArgument(String key) throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Operation getRequestedOperation() throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UISession getSession() throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TTYOutputter getTTYOutputter() throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isJSON() throws UIException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendJSONResponse(JSONObject out) throws UIException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendJSONResponse(JSONArray out) throws UIException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFailure(boolean isit, Exception why) throws UIException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setOperationPerformed(Operation op) throws UIException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRedirectArgument(String key, String value)
			throws UIException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRedirectPath(String[] in) throws UIException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSecondaryRedirectPath(String[] in) throws UIException {
		// TODO Auto-generated method stub

	}

}
