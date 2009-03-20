package org.collectionspace.xxu.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttachmentRegistryNode {
	private Map<String,AttachmentRegistryNode> steps=new HashMap<String,AttachmentRegistryNode>();
	private List<String> points=new ArrayList<String>();
	
	public AttachmentRegistryNode step(String step) { return steps.get(step); }
	public String[] getPoints() { 
		if(points.size()==0)
			return null;
		return points.toArray(new String[0]);
	}
	
	public void addStep(String step,AttachmentRegistryNode node) { steps.put(step,node); }
	public void addPoint(String in) { points.add(in); }
}
