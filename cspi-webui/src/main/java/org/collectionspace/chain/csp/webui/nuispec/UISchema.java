package org.collectionspace.chain.csp.webui.nuispec;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UISchema  extends UISpec  {
	private static final Logger log=LoggerFactory.getLogger(UISchema.class);
	private Spec spec;
	protected JSONObject controlledCache;
	
	public UISchema(Spec spec) {
		super();
		this.spec = spec;
		this.controlledCache = new JSONObject();
	}

	public UISchema(Record record, String structureview) {
		super();
	}
	private JSONObject uischema(Storage storage) throws UIException {
		this.storage = storage;
		String affix = "";
		try {
			JSONObject out=new JSONObject();
			Structure s = record.getStructure(this.structureview);
			if(s.showListSection()){
				out.put(s.getListSectionName(),generateListSection(s,affix));
			}
			if(s.showEditSection()){
				out.put(s.getEditSectionName(),generateDataEntrySection(affix));
			}
			if(s.showTitleBar()){
				out.put("titleBar",generateTitleSection(affix));
			}
			
			if(s.showSideBar()){
				out.put("sidebar",generateSidebarSection(s, affix));
			}
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException",e);
		}
	}

	public void configure() throws ConfigException {}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		JSONObject out=uischema(q.getStorage());
		q.getUIRequest().sendJSONResponse(out);
	}

	public void configure(WebUI ui,Spec spec) {}
}
