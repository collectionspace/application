package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;

/**
 * Hold information about non standard schemas.
 * As more schemas are required this functionality with grow in complexity
 * @author csm22
 *
 */
public class Schemas {

	private String id,web_url; 
	
	Schemas(Spec spec,ReadOnlySection section) { 
		id=(String)section.getValue("/@id");
		web_url = Util.getStringOrDefault(section, "/web-url", id);
	}
	public String getID() { return id; }

	public String getWebURL() {
		return web_url;
	}
}
