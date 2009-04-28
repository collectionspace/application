package org.collectionspace.xxu.test.main;

import java.util.Map;

public interface CSpace {
	
	/* Used by digester, but feel free to use yourself */
	public void addField(Field in);
	public void setScript(ScriptStore in);
	
	public Field getField(String id);
	public ScriptStore getScript();
	
	public Map<String,Field> getFields(); // XXX remove
	
	/* For debugging */
	public String dump();
}
