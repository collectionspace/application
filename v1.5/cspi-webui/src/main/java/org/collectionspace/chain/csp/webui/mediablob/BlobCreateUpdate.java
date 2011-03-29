package org.collectionspace.chain.csp.webui.mediablob;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobCreateUpdate  implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(BlobCreateUpdate.class);
	private WebUI ui;
	private Spec spec;
	private String upload_dest;
	
	public BlobCreateUpdate(WebUI ui,Spec spec) {
		this.spec=spec;
		this.ui=ui;
	}
	

	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject data=new JSONObject();
			String sata="{\"termsUsed\":[],\"relations\":{},\"csid\":\"d6b99ab1-0db5-4f6f-84f2\",\"fields\":{\"name\":\"03-31-09_1404.jpg\",\"length\":\"69430\",\"csid\":\"d6b99ab1-0db5-4f6f-84f2\",\"uri\":\"/blobs/d6b99ab1-0db5-4f6f-84f2/content\"}}";
			data = new JSONObject(sata);
			request.sendJSONResponse(data);
			request.setOperationPerformed(Operation.CREATE);
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		} catch (Exception x) {
			throw new UIException(x);
		}
	
	}
	
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}

	public void configure(WebUI ui,Spec spec) {
		//upload_dest=ui.getUploadDest();
	}
}
