package org.collectionspace.chain.csp.webui.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.chain.uispec.StubSchemaStore;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebAUISpec implements WebMethod {
	private Record record;

	public WebAUISpec(Record record) {
		this.record=record;
	}

	private String plain(Field f) {
		List<String> path=new ArrayList<String>();
		String pad="fields";
		for(String part : f.getIDPath()) {
			path.add(pad);
			pad="0";
			path.add(part);
		}
		return "${"+StringUtils.join(path,'.')+"}";		
	}

	// XXX factor
	private Object generateDataEntryField(Field f) throws JSONException {
		if("plain".equals(f.getUIType())) {
			// Plain entry
			return plain(f);
		} else if("dropdown".equals(f.getUIType())) {
			// Dropdown entry
			JSONObject out=new JSONObject();
			out.put("selection",plain(f));
			JSONArray ids=new JSONArray();
			JSONArray names=new JSONArray();
			int idx=0,dfault=-1;
			for(Option opt : f.getAllOptions()) {
				ids.put(opt.getID());
				names.put(opt.getName());
				if(opt.isDefault())
					dfault=idx;
				idx++;
			}
			if(dfault!=-1)
				out.put("default",dfault+"");
			out.put("optionlist",ids);
			out.put("optionnames",names);			
			return out;
		}
		return plain(f);	
	}

	private JSONObject generateAutocomplete(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject decorator=new JSONObject();
		decorator.put("type","fluid");
		decorator.put("func","cspace.autocomplete");
		decorator.put("container",f.getSelector());
		JSONObject options=new JSONObject();
		options.put("url","../../chain/"+f.getRecord().getWebURL()+"/autocomplete/"+f.getID());
		decorator.put("options",options);
		decorators.put(decorator);
		out.put("decorators",decorators);
		return out;
	}

	private JSONObject generateChooser(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject decorator=new JSONObject();
		decorator.put("type","fluid");
		decorator.put("func","cspace.numberPatternChooser");
		decorator.put("container",f.getContainerSelector());
		JSONObject options=new JSONObject();
		JSONObject selectors=new JSONObject();
		selectors.put("numberField",f.getSelector());
		options.put("selectors",selectors);
		JSONObject model=new JSONObject();
		JSONArray ids=new JSONArray();
		JSONArray samples=new JSONArray();
		JSONArray names=new JSONArray();
		for(Option opt : f.getAllOptions()) {
			ids.put(opt.getID());
			samples.put(opt.getSample());
			names.put(opt.getName());
		}
		model.put("list",ids);
		model.put("samples",samples);
		model.put("names",names);
		options.put("model",model);
		decorator.put("options",options);
		decorators.put(decorator);
		out.put("decorators",decorators);
		return out;
	}

	private JSONObject generateDate(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject decorator=new JSONObject();
		decorator.put("type","fluid");
		decorator.put("func","cspace.datePicker");
		decorator.put("container",f.getContainerSelector());
		decorators.put(decorator);
		out.put("decorators",decorators);
		return out;
	}

	private JSONObject generateDataEntrySection() throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet fs : record.getAllFields()) {
			generateDataEntry(out,fs);
		}
		return out;
	}

	private void generateDataEntry(JSONObject out,FieldSet fs) throws JSONException {
		if(fs instanceof Field) {
			// Single field
			Field f=(Field)fs;
			// Single field
			out.put(f.getSelector(),generateDataEntryField(f));
			if(f.isAutocomplete()) {
				out.put(f.getAutocompleteSelector(),generateAutocomplete(f));
			}
			if("chooser".equals(f.getUIType())) {
				out.put(f.getContainerSelector(),generateChooser(f));
			}
			if("date".equals(f.getUIType())) {
				out.put(f.getContainerSelector(),generateDate(f));
			}			
		} else if(fs instanceof Repeat) {
			// Container
			Repeat r=(Repeat)fs;
			JSONObject row=new JSONObject();
			JSONArray children=new JSONArray();
			for(FieldSet child : r.getChildren()) {
				JSONObject contents=new JSONObject();
				generateDataEntry(contents,child);
				children.put(contents);
			}
			row.put("children",children);
			out.put(r.getSelector(),row);
		}

	}

	private void generateTitleSectionEntry(JSONObject out,FieldSet fs) throws JSONException {
		if(fs instanceof Field) {
			Field f=(Field)fs;
			if(!f.isInTitle())
				return;
			out.put(f.getTitleSelector(),plain(f));
		} else if(fs instanceof Repeat) {
			for(FieldSet child : ((Repeat)fs).getChildren())
				generateTitleSectionEntry(out,child);
		}
	}

	private JSONObject generateTitleSection() throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet f : record.getAllFields()) {
			generateTitleSectionEntry(out,f);
		}
		return out;
	}

	private JSONObject generateSidebarPart(String url_frag,boolean include_type,boolean include_summary) throws JSONException {
		JSONObject out=new JSONObject();
		JSONObject row=new JSONObject();
		JSONArray children=new JSONArray();
		JSONObject child=new JSONObject();
		JSONObject number=new JSONObject();
		number.put("linktext","${items.0.number}");
		number.put("target",url_frag+"?csid=${items.0.csid}");
		child.put(".csc-related-number",number);
		if(include_type)
			child.put(".csc-related-recordtype","${items.0.recordtype}");
		if(include_summary)
			child.put(".csc-related-summary","${items.0.summary}");
		children.put(child);
		row.put("children",children);
		out.put(".csc-related-row:",row);
		return out;
	}

	// XXX sidebar is fixed for now
	private JSONObject generateSidebarSection() throws JSONException {
		JSONObject out=new JSONObject();
		out.put("termsUsed",generateSidebarPart("nameAuthority",true,false));
		out.put("relatedProcedures",generateSidebarPart("${items.0.recordtype}.html",true,true));
		out.put("relatedObjects",generateSidebarPart("object.html",false,true));
		return out;
	}

	private JSONObject uispec(UIRequest request,String suffix) throws UIException {
		try {
			JSONObject out=new JSONObject();
			out.put("dataEntry",generateDataEntrySection());
			out.put("titleBar",generateTitleSection());
			out.put("sidebar",generateSidebarSection());
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException",e);
		}
	}

	public void configure() throws ConfigException {
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		JSONObject out=uispec(q.getUIRequest(),StringUtils.join(tail,"/"));
		q.getUIRequest().sendJSONResponse(out);
	}

	public void configure(WebUI ui,Spec spec) {}
}
