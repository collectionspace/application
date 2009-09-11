package org.collectionspace.csp.helper.config.impl;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.config.main.impl.EventContextImpl;
import org.collectionspace.csp.api.config.EventContext;
import org.collectionspace.csp.api.config.EventConsumer;

// TODO at the moment only full paths are recognised, we should support regexps
public class SimpleBarbWirerDispatch implements EventConsumer {
	private static class ChopResult {
		private EventContextImpl context;
		private EventConsumer consumer;
		
		private ChopResult(EventContextImpl context,EventConsumer consumer) {
			this.context=context;
			this.consumer=consumer;
		}
	};
	
	private static class Node {
		private Map<String,Node> subnodes=new HashMap<String,Node>();
		private Map<String,EventConsumer> consumers=new HashMap<String,EventConsumer>();		
	};
	
	private Node root=new Node();
	private String name;
	
	public SimpleBarbWirerDispatch() {}
	public SimpleBarbWirerDispatch(String name) { this.name=name; }
	
	public void addHandler(String[] path,EventConsumer consumer) {
		Node cur=root;
		// iterate through subnodes
		for(int i=0;i<path.length-1;i++) {
			Node next=cur.subnodes.get(path[i]);
			if(next==null) {
				next=new Node();
				cur.subnodes.put(path[i],next);
			}
			cur=next;
		}
		// add consumer
		cur.consumers.put(path[path.length-1],consumer);
	}

	private ChopResult getChop(EventContext context) {
		String[] path=context.getStack();
		Node cur=root;
		// iterate through subnodes
		for(int i=0;i<path.length;i++) {
			Node next=null;
			if(i<path.length-1)
				next=cur.subnodes.get(path[i]);
			if(next==null) {
				EventConsumer consumer=cur.consumers.get(path[i]);
				if(consumer==null) {
					System.err.println("Failed dispatch in "+name);
					return null;
				}
				// Split context into parent and child contexts
				String[] parent_nodes=new String[i+1];
				System.arraycopy(context.getStack(),0,parent_nodes,0,parent_nodes.length);
				String[] child_nodes=new String[path.length-i-1];
				if(child_nodes.length>0)
					System.arraycopy(context.getStack(),i+1,child_nodes,0,child_nodes.length);
				EventContextImpl parent=new EventContextImpl(context.getParentContext(),parent_nodes);
				if(name!=null)
					System.err.println("Dispatching for "+name+" to "+consumer.getName());
				return new ChopResult(new EventContextImpl(parent,child_nodes),consumer);
			} else
				cur=next;
		}
		System.err.println("Failed dispatch in "+name);
		return null;
	}
	
	public void end(int ev, EventContext context) {
		ChopResult c=getChop(context);
		if(c!=null)
			c.consumer.end(ev,c.context);
	}
	
	public void start(int ev, EventContext context) {
		ChopResult c=getChop(context);
		if(c!=null)
			c.consumer.start(ev,c.context);
	}

	public void text(int ev, EventContext context, String text) {
		ChopResult c=getChop(context);
		if(c!=null)
			c.consumer.text(ev,c.context,text);
	}
	
	public String getName() { return name; }
}
