package org.collectionspace.chain.csp.webui.mediablob;

import org.collectionspace.chain.csp.schema.AdminData;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BlobRead implements WebMethod {
	private AdminData adminData = null;

	private void get_blob(Storage s,UIRequest q,String csid,String derivative) throws UIException {
		try {
			JSONObject out = s.retrieveJSON("/blobs/"+csid+"/"+derivative,null);
			byte[] data_array = (byte[])out.get("getByteBody");
			String contentDisp = out.has("contentdisposition")?out.getString("contentdisposition"):null;
			q.sendUnknown(data_array,out.getString("contenttype"), contentDisp);
			int cacheMaxAgeSeconds = adminData.getUploadedMediaCacheAge();
			if(cacheMaxAgeSeconds > 0) {
				q.setCacheMaxAgeSeconds(cacheMaxAgeSeconds);
			}
		} catch (ExistException e) {
			throw new UIException("Existence exception",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Underlying storage problem",e);
		} catch (JSONException e) {
			throw new UIException("JSON exception",e);
		}
	}
	
	@Override
	public void configure(WebUI ui, Spec spec) {
		adminData = spec.getAdminData();
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		get_blob(q.getStorage(),q.getUIRequest(),tail[0],tail[1]);
	}

}
