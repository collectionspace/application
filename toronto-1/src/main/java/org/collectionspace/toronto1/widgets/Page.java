package org.collectionspace.toronto1.widgets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Page {
	private FragmentEconomy fe;
	private Fragment page;
	private int seq=0;
	private Map<String,Actionable> actions=new HashMap<String,Actionable>();
	private Map<String,Set<String>> explicit_modes=new HashMap<String,Set<String>>();
	
	public Page(FragmentEconomy fe,Element element) throws FactoryException {
		this.fe=fe;
		this.page=createFragment(element);
	}

	// IMPORTANT: You must always call this the same number of times, irrespective of user filtering, etc.
	public int nextInt() { return seq++; }

	public Fragment generatePage() throws FactoryException {
		return page;
	}

	public Fragment createFragment(Element n) throws FactoryException {
		FragmentFactory ff=fe.getFragmentFactory(n);
		return ff.createFragment(this,n);
	}

	public void registerAction(String control,Actionable actionable) {
		actions.put(control,actionable);
	}

	public static void executeTask(Task t,AJAXRequest request) {
		try {
			String type=request.getType();
			if("preact".equals(type)) {
				t.preact(request);
			} else if("act".equals(type)) {
				t.act(request);
				t.postact(request);
			}
		} catch (TaskException e) {
			try {
				JSONObject e1=new JSONObject();
				e1.put("error","Internal Error: "+e.getMessage());
				request.addResult(e1);
			} catch (JSONException e2) {}
		}
	}

	public void act(String control,AJAXRequest request) {
		Actionable a=actions.get(control);
		if(a==null)
			return;
		a.act(request);
	}

	public String summarize(String part,JSONObject data) throws FactoryException {
		StringBuffer out=new StringBuffer();
		page.getSummary(out,part,data);
		return out.toString();
	}
	
	public void addExplicitMode(String type,String mode) {
		Set<String> modes=explicit_modes.get(type);
		if(modes==null) {
			modes=new HashSet<String>();
			explicit_modes.put(type,modes);
		}
		modes.add(mode);
	}
	
	public boolean hasExplicitMode(String type,String mode) {
		Set<String> modes=explicit_modes.get(type);
		if(modes==null)
			return false;
		return modes.contains(mode);
	}
	
	// XXX branching etc
	@SuppressWarnings("unchecked")
	public Task createTask(Element e) throws FactoryException {
		TaskList out=new TaskList();
		List<Element> tags=e.elements();
		for(Element tag : tags) {
			TaskFactory tf=fe.getTaskFactory(tag);
			Task task=tf.createTask(this,tag);
			out.addTask(task);
		}
		return out;
	}
}
