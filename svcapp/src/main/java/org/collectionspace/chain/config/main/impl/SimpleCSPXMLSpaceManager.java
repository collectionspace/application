package org.collectionspace.chain.config.main.impl;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.config.main.XMLEventConsumer;
import org.collectionspace.chain.config.main.csp.CSPXMLSpaceAttachmentPoint;
import org.collectionspace.chain.config.main.csp.CSPXMLSpaceManager;

public class SimpleCSPXMLSpaceManager implements CSPXMLSpaceManager {
	private XMLEventDispatch dispatcher;
	private Map<String,SimpleCSPXMLSpaceAttachmentPoint> aps=new HashMap<String,SimpleCSPXMLSpaceAttachmentPoint>();
	
	public class SimpleCSPXMLSpaceAttachmentPoint implements CSPXMLSpaceAttachmentPoint {
		private String[] base_path;
		
		private SimpleCSPXMLSpaceAttachmentPoint(String[] base) { base_path=base; }
		
		public void attach(CSPXMLSpaceManager manager,String root) { 
			String[] path=new String[base_path.length+1];
			if(base_path.length>0)
				System.arraycopy(base_path,0,path,0,base_path.length);
			path[path.length-1]=root;
			dispatcher.addHandler(path,manager.getConsumer());
		}
		public CSPXMLSpaceManager getManager() { return SimpleCSPXMLSpaceManager.this; }
	}

	public SimpleCSPXMLSpaceManager() {
		dispatcher=new XMLEventDispatch();
	}
	
	public SimpleCSPXMLSpaceManager(String name) { 
		dispatcher=new XMLEventDispatch(name);
	}
	
	public void addAttachmentPoint(String name,String[] path) {
		aps.put(name,new SimpleCSPXMLSpaceAttachmentPoint(path));
	}
	
	public CSPXMLSpaceAttachmentPoint getAttachmentPoint(String name) {
		return aps.get(name);
	}

	public XMLEventConsumer getConsumer() { return dispatcher; }
}
