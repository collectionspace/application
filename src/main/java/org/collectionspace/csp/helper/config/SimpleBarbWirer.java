package org.collectionspace.csp.helper.config;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.csp.api.config.Barb;
import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.EventConsumer;
import org.collectionspace.csp.helper.config.impl.SimpleBarbWirerDispatch;

public class SimpleBarbWirer implements BarbWirer {
	private SimpleBarbWirerDispatch dispatcher;
	private Map<String,SimpleBarb> aps=new HashMap<String,SimpleBarb>();
	private Map<String,BarbWirer> managers=new HashMap<String,BarbWirer>();
	
	public class SimpleBarb implements Barb {
		private String[] base_path;
		
		private SimpleBarb(String[] base) { base_path=base; }
		
		public void attach(BarbWirer manager,String root) { 
			String[] path=new String[base_path.length+1];
			if(base_path.length>0)
				System.arraycopy(base_path,0,path,0,base_path.length);
			path[path.length-1]=root;
			dispatcher.addHandler(path,manager.getConsumer());
			managers.put(root,manager);
		}
		public BarbWirer getManager() { return SimpleBarbWirer.this; }
		public BarbWirer getAttachment(String root) { return managers.get(root); }
	}

	public SimpleBarbWirer() {
		dispatcher=new SimpleBarbWirerDispatch();
	}
	
	public SimpleBarbWirer(String name) { 
		dispatcher=new SimpleBarbWirerDispatch(name);
	}
	
	public void addAttachmentPoint(String name,String[] path) {
		aps.put(name,new SimpleBarb(path));
	}
	
	public Barb getBarb(String name) {
		return aps.get(name);
	}

	public EventConsumer getConsumer() { return dispatcher; }
}
