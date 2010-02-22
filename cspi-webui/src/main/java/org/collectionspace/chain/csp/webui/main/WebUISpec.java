package org.collectionspace.chain.csp.webui.main;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.chain.uispec.StubSchemaStore;
import org.collectionspace.csp.api.config.ConfigException;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;

public class WebUISpec implements WebMethod {
	private String base;
	private SchemaStore schema;
	
	public WebUISpec(String base) {
		this.base=base;
	}
	
	private void uispec(UIRequest request,String suffix) throws UIException {
		try {
			request.sendJSONResponse(schema.getSchema(base+"/"+suffix));
		} catch (IOException e) {
			throw new UIException("IOException building UISpec",e);
		} catch (JSONException e) {
			throw new UIException("JSONException building UISpec",e);
		}
	}
	
	private String getSchemaPath(ConfigRoot config) {
		if(config.hasValue(new String[]{"ui","web","tmp-schema-path"}))
			return System.getProperty("java.io.tmpdir")+"/ju-cspace"; // XXX fix
		return (String)config.getValue(new String[]{"ui","web","schema-path"});	
	}
	
	public void configure(ConfigRoot config) throws ConfigException {
		schema=new StubSchemaStore(getSchemaPath(config));
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		uispec(q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
