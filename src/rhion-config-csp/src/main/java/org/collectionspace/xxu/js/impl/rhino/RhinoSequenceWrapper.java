package org.collectionspace.xxu.js.impl.rhino;

import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class RhinoSequenceWrapper implements Scriptable {
	private Sequence sequence;
	private Scriptable scope;
	private Context ctx;
	
	@SuppressWarnings("unchecked")
	RhinoSequenceWrapper(Context ctx,Scriptable scope,Object thing) {
		this.ctx=ctx;
		this.scope=scope;
		if(thing.getClass().isArray())
			sequence=new ArraySequence(thing);
		if(thing instanceof List)
			sequence=new ListSequence((List)thing);
	}
	
	public Object getThing() { return sequence.getThing(); }
	
	public void delete(String arg0) {} // No!

	public void delete(int idx) {
		if(idx<0 || idx>=sequence.length())
			return;
		sequence.setIndex(idx,null);
	}

	public int length() { return sequence.length(); }
	
	public Object get(String key, Scriptable start) {
		if("length".equals(key))
			return length();
		return null;
	}

	public Object get(int idx, Scriptable start) {
		if(idx<0 || idx>=sequence.length())
			return null;
		Object out=sequence.getIndex(idx);
		if(out==null)
			return null;
		return RhinoContext.staticWrapIfNeeded(ctx,scope,out);
	}

	public String getClassName() { return "Array"; }

	public Object getDefaultValue(Class hint) {
		if(hint==Boolean.class)
			return true;
		if(hint==Number.class)
			return null;
		if(hint==String.class)
			return sequence.stringify();
		return sequence.getThing();
	}

	public Object[] getIds() {
		int len=sequence.length();
		Object[] out=new Object[len+1];
		for(int i=0;i<len;i++)
			out[i]=i;
		out[len]="length";
		return out;
	}

	public Scriptable getParentScope() { return scope; }

	public Scriptable getPrototype() {
		Scriptable out=ScriptableObject.getClassPrototype(scope,"Object");
		return out;
	}

	public boolean has(String key, Scriptable start) {
		return "length".equals(key);
	}

	public boolean has(int idx, Scriptable start) {
		return idx>0 && idx<sequence.length();
	}

	public boolean hasInstance(Scriptable lhs) { return lhs instanceof RhinoSequenceWrapper; }

	public void put(String key,Scriptable start,Object value) {
		if(!"length".equals(key))
			return;
		if(value instanceof String)
			try { value=Integer.parseInt((String)value); } catch(NumberFormatException x) { }
		if(!(value instanceof Integer))
			return;
		int len=((Integer)value).intValue();
		if(len<0)
			len=0;
		sequence.truncate(len); // Note: may not work for some sequence types
	}

	public void put(int idx,Scriptable start,Object value) {
		if(idx<0 || idx>=sequence.length())
			return;
		sequence.setIndex(idx,value);
	}

	public void setParentScope(Scriptable s) { scope=s; }
	public void setPrototype(Scriptable arg0) {} // No!
}
