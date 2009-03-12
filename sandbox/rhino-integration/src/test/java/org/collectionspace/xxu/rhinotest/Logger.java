package org.collectionspace.xxu.rhinotest;

import java.util.ArrayList;
import java.util.List;

import org.collectionspace.xxu.js.api.JavascriptMessages;

public class Logger implements JavascriptMessages {
	private List<String> log=new ArrayList<String>();
	
	public void message(String message) { log.add(message); }
	public String[] getAll() { return log.toArray(new String[0]); }
}
