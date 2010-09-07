package org.collectionspace.csp.api.ui;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public interface UIRequest {	
	public String[] getPrincipalPath() throws UIException;
	
	public TTYOutputter getTTYOutputter() throws UIException;
	public void sendJSONResponse(JSONObject out) throws UIException;
	public void sendJSONResponse(JSONArray out) throws UIException;

	JSONObject getJSONBody() throws UIException;
	JSONObject getPostBody() throws UIException;
	Boolean isJSON() throws UIException;
	
	public void setFailure(boolean isit,Exception why) throws UIException;
	
	public void setOperationPerformed(Operation op) throws UIException;
	
	public void setRedirectPath(String[] in) throws UIException;
	public void setSecondaryRedirectPath(String[] in) throws UIException;
	public void setRedirectArgument(String key,String value) throws UIException;
	public void deleteRedirectArgument(String key) throws UIException;
	public String getRedirectArgument(String key) throws UIException;

	public String getRequestArgument(String key) throws UIException;
	public Set<String> getAllRequestArgument() throws UIException;
	public Operation getRequestedOperation() throws UIException;
	
	public UISession getSession() throws UIException;

}
