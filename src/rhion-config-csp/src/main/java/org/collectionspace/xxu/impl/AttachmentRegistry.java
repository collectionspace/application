package org.collectionspace.xxu.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.xxu.api.ConfigLoadingException;
import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;

public class AttachmentRegistry {
	private Map<String,List<String>> pointpath=new HashMap<String,List<String>>();
	private AttachmentRegistryNode root=new AttachmentRegistryNode();
	private Map<String,List<AttachmentConsumer>> consumers=new HashMap<String,List<AttachmentConsumer>>();
	
	private AttachmentRegistryNode getNode(String[] stack,boolean create) {
		AttachmentRegistryNode node=root;
		for(String step : stack) {
			AttachmentRegistryNode next=node.step(step);
			if(next==null) {
				if(!create)
					return null;
				next=new AttachmentRegistryNode();
				node.addStep(step,next);
			}
			node=next;
		}
		return node;
	}
	
	public void registerAttachmentPoint(String parent,String[] stack,String name) throws ConfigLoadingException {
		List<String> path=new ArrayList<String>();
		if(parent!=null) {
			List<String> p=pointpath.get(parent);
			if(p==null)
				throw new ConfigLoadingException("No parent point "+parent);
			path.addAll(p);
		}
		path.addAll(Arrays.asList(stack));
		getNode(path.toArray(new String[0]),true).addPoint(name);
		pointpath.put(name,Arrays.asList(stack));
	}
	
	public void registerConsumer(String name,String tag,XMLEventConsumer consumer) {
		List<AttachmentConsumer> c=consumers.get(name);
		if(c==null) {
			c=new ArrayList<AttachmentConsumer>();
			consumers.put(name,c);
		}
		c.add(new AttachmentConsumer(consumer,tag));
	}
	
	private String[] resolveAndTruncatePoints(XMLEventContext in) {
		int steps=0;
		AttachmentRegistryNode node=getNode(in.getPreStack(),false);
		if(node==null)
			return null;
		String[] points=node.getPoints();
		String[] stack=in.getStack();
		for(String step : stack) {
			steps++;
			node=node.step(step);
			if(node==null)
				return null;
			points=node.getPoints();
			if(points!=null && steps<stack.length) {
				in.truncate(steps);
				return points;
			}
		}
		return null;
	}
	
	public AttachmentConsumer[] resolveAndTruncate(XMLEventContext in) {
		String[] points=resolveAndTruncatePoints(in);
		if(points==null)
			return null;
		List<AttachmentConsumer> out=new ArrayList<AttachmentConsumer>();
		for(String p : points) {
			List<AttachmentConsumer> c=consumers.get(p);
			if(c!=null)
				out.addAll(c);
		}
		return out.toArray(new AttachmentConsumer[0]);
	}
}
