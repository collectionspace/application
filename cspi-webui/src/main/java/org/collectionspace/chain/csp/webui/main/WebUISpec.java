package org.collectionspace.chain.csp.webui.main;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.chain.uispec.StubSchemaStore;
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
	
	public void configure() throws ConfigException {
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		uispec(q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure(WebUI ui,Spec spec) {
		schema=new StubSchemaStore(ui.getUISpecPath());
	}
}
