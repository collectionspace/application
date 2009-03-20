package org.collectionspace.xxu.csp.attachment;

import java.util.Map;
import java.util.HashMap;
import org.collectionspace.xxu.api.CSP;
import org.collectionspace.xxu.api.CSPProvider;
import org.collectionspace.xxu.api.CSPProviderFactory;
import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.ConfigLoadingException;
import org.dom4j.Node;

public class ProviderAttachmentImpl implements CSPProvider {
	private String point,tag;
	private Map<String,String> points=new HashMap<String,String>();
	
	public ProviderAttachmentImpl(String point,String tag) {
		this.point=point;
		this.tag=tag;
	}
	
	public void addPoint(String name,String path) { points.put(name,path); }
	
	// XXX sort out namespaces
	public void act(ConfigLoader cfg) throws ConfigLoadingException {
		cfg.registerAttachment(point,tag,new AttachmentEventConsumer(cfg));
		for(Map.Entry<String,String> e : points.entrySet()) {
			cfg.registerAttachmentPoint(point,e.getKey().split("/"),e.getValue());
		}
	}
}
