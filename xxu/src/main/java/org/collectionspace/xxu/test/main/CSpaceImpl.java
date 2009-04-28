package org.collectionspace.xxu.test.main;

import java.io.InputStream;
import java.util.*;

public class CSpaceImpl implements CSpace {
	private Map<String,Field> fields=new HashMap<String,Field>();
	private ScriptStore script=new ScriptStore();
	
	public void addField(Field in) { fields.put(in.getID(),in); in.setCSpace(this); }
	public Field getField(String id) { return fields.get(id); }
	
	public Map<String,Field> getFields() { // XXX remove
		return fields;
	}
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		out.append("BEGIN CSpaceImpl\n");
		for(Field f : fields.values())
			out.append(f.dump());
		out.append(script.dump());
		out.append("END   CSpaceImpl\n");
		return out.toString();
	}
	public void setScript(ScriptStore in) { script=in; }
	public ScriptStore getScript() { return script; }
}
