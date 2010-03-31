package org.collectionspace.chain.controller;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIUmbrella;

public class WebUIUmbrella implements UIUmbrella {
	private Map<String,WebUISession> sessions=new HashMap<String,WebUISession>();
	
	WebUISession getSession(String in) {
		WebUISession out=sessions.get(in);
		out.setOld();
		return out;
	}
	
	WebUISession createSession() throws UIException {
		WebUISession out=new WebUISession(this);
		sessions.put(out.getID(),out);
		return out;
	}
}
