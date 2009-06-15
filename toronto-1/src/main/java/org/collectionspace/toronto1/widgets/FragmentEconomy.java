package org.collectionspace.toronto1.widgets;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;

public class FragmentEconomy {
	private Map<String,FragmentFactory> fragment_factories=new HashMap<String,FragmentFactory>();
	private Map<String,TaskFactory> task_factories=new HashMap<String,TaskFactory>();
	
	public FragmentFactory getFragmentFactory(Element n) throws FactoryException {
		FragmentFactory ff=fragment_factories.get(n.getName());
		if(ff==null)
			throw new FactoryException("Unknown tag "+n.getName());
		return ff;
	}

	public TaskFactory getTaskFactory(Element n) throws FactoryException {
		TaskFactory tf=task_factories.get(n.getName());
		if(tf==null)
			throw new FactoryException("Unknown tag "+n.getName());
		return tf;
	}
	
	public void registerFragmentFactory(String name,FragmentFactory ff) {
		fragment_factories.put(name,ff);
	}

	public void registerTaskFactory(String name,TaskFactory ff) {
		task_factories.put(name,ff);
	}
}