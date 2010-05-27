package org.collectionspace.chain.csp.webui.nuispec;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UISpec implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(UISpec.class);
	private Record record;
	String structureview;

	public UISpec(Record record, String structureview) {
		this.record=record;
		this.structureview = structureview;
	}

	// XXX make common
	static String plain(Field f) {
		List<String> path=new ArrayList<String>();
		String pad="fields";
		for(String part : f.getIDPath()) {
			path.add(pad);
			pad="0";
			path.add(part);
		}
		return "${"+StringUtils.join(path,'.')+"}";		
	}
	// XXX make common
	// ${items.0.name}
	static String plainlist(Field f) {
		List<String> path=new ArrayList<String>();
		String name="items";
		path.add(name);
			String pad="0";
			path.add(pad);
			path.add(f.getID());
		
		return "${"+StringUtils.join(path,'.')+"}";		
	}

	static JSONObject linktext(Field f) throws JSONException  {
		JSONObject number=new JSONObject();
		number.put("linktext",f.getLinkText());
		number.put("target",f.getLinkTextTarget());
		return number;
			
	}
	
	// XXX factor
	private Object generateDataEntryField(Field f) throws JSONException {
		if("plain".equals(f.getUIType())) {
			// Plain entry
			return plain(f);
		} 
		else if("list".equals(f.getUIType())){
			return plainlist(f);
		}
		else if("linktext".equals(f.getUIType())){
			return linktext(f);
		}
		else if("dropdown".equals(f.getUIType())) {
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

		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",f.getSelector());
			}
		}
		JSONObject options=new JSONObject();
		String extra="";
		if(f.getRecord().isType("authority"))
			extra="vocabularies/";
		options.put("queryUrl","../../chain/"+extra+f.getRecord().getWebURL()+"/autocomplete/"+f.getID());
		options.put("vocabUrl","../../chain/"+extra+f.getRecord().getWebURL()+"/source-vocab/"+f.getID());
		decorator.put("options",options);
		decorators.put(decorator);
		out.put("decorators",decorators);
		if(f.isRefactored()){
			out.put("valuebinding", generateDataEntryField(f));
		}
		return out;
	}

	private JSONObject generateChooser(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject decorator=new JSONObject();
		decorator.put("type","fluid");
		decorator.put("func","cspace.numberPatternChooser");
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",f.getContainerSelector());
			}
		}
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
		if(f.isRefactored()){
			out.put("valuebinding", generateDataEntryField(f));
		}
		return out;
	}

	private JSONObject generateDate(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject decorator=new JSONObject();
		decorator.put("type","fluid");
		decorator.put("func","cspace.datePicker");
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",f.getContainerSelector());
			}
		}
		decorators.put(decorator);
		out.put("decorators",decorators);
		if(f.isRefactored()){
			out.put("valuebinding", generateDataEntryField(f));
		}
		return out;
	}

	private JSONObject generateDataEntrySection() throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet fs : record.getAllFields()) {
			generateDataEntry(out,fs);
		}
		return out;
	}

	
	private JSONObject generateListSection(Structure s) throws JSONException {
		JSONObject out=new JSONObject();
		String id = s.getListSectionName();
		if(s.getField(id) != null){
			FieldSet fs = s.getField(id);
			generateDataEntry(out,fs);
		}
		
		return out;
	}
	
	private void generateDataEntry(JSONObject out,FieldSet fs) throws JSONException {
		if(fs instanceof Field) {
			// Single field
			Field f=(Field)fs;
			
			//XXX when all uispecs have moved across we can delete most of this
			if(!f.isRefactored()){
				// Single field
				out.put(f.getSelector(),generateDataEntryField(f));	
				
				if(f.hasAutocompleteInstance()) {
					out.put(f.getAutocompleteSelector(),generateAutocomplete(f));
				}
				if("chooser".equals(f.getUIType())) {
					out.put(f.getContainerSelector(),generateChooser(f));
				}
				if("date".equals(f.getUIType())) {
					out.put(f.getContainerSelector(),generateDate(f));
				}
			}
			else{
				
				if(f.hasAutocompleteInstance()) {
					out.put(f.getSelector(),generateAutocomplete(f));
				}
				if("chooser".equals(f.getUIType())) {
					out.put(f.getSelector(),generateChooser(f));
				}
				if("date".equals(f.getUIType())) {
					out.put(f.getSelector(),generateDate(f));
				}
				if("sidebar".equals(f.getUIType())) {
					//out.put(f.getSelector(),generateSideBar(f));
				}
			}
		} else if(fs instanceof Repeat) {
			// Container
			Repeat r=(Repeat)fs;
			if(r.getXxxUiNoRepeat()) {
				FieldSet[] children=r.getChildren();
				if(children.length==0)
					return;
				generateDataEntry(out,children[0]);
			} else {
				JSONObject row=new JSONObject();
				JSONArray children=new JSONArray();
				if(r.asSibling()){ // allow for row [{'','',''}]
					JSONObject contents=new JSONObject();
					for(FieldSet child : r.getChildren()) {
						generateDataEntry(contents,child);
					}
					children.put(contents);
				}
				else{//default row [{},{},{}]
					for(FieldSet child : r.getChildren()) {
						JSONObject contents=new JSONObject();
						generateDataEntry(contents,child);
						children.put(contents);
					}
				}
				row.put("children",children);
				out.put(r.getSelector(),row);
			}
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

	// XXX refactor
	static JSONObject generateSidebarPart(String url_frag,boolean include_type,boolean include_summary,boolean include_sourcefield) throws JSONException {
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
		if(include_sourcefield)
			child.put(".csc-related-field","${items.0.sourceFieldName}");
		children.put(child);
		row.put("children",children);
		out.put(".csc-recordList-row:",row);
		return out;
	}

	// XXX refactor
	private JSONObject generateSideDataEntry(JSONObject out, FieldSet fs) throws JSONException {
		Repeat f=(Repeat)fs;
		JSONObject listrow=new JSONObject();
		generateDataEntry(listrow,fs);
		out.put(f.getID(),listrow);
		return out;
	}
	
	private JSONObject generateSideDataEntry(Structure s, JSONObject out, String fieldName,String url_frag,boolean include_type,boolean include_summary,boolean include_sourcefield )throws JSONException {
		FieldSet fs = s.getSideBarItems(fieldName);
		if(fs == null){
			//XXX default to show if not specified
			out.put(fieldName,generateSidebarPart(url_frag,include_type,include_summary,include_sourcefield));
		}
		else if(fs instanceof Repeat){
			if(((Repeat)fs).isVisible()){
				if(s.getField(fs.getID()) != null){
					generateSideDataEntry(out,s.getField(fs.getID()));
				}
				else{
					out.put(fieldName,generateSidebarPart(url_frag,include_type,include_summary,include_sourcefield));
				}
			}
		}
		
		return out;
	}

	// XXX sidebar is partially fixed for now
	//need to clean up this code - reduce duplication
	private JSONObject generateSidebarSection(Structure s) throws JSONException {
		JSONObject out=new JSONObject();
		generateSideDataEntry(s, out,"termsUsed","${items.0.recordtype}.html",true,false,true);
		generateSideDataEntry(s, out,"relatedProcedures","${items.0.recordtype}.html",true,true,false);
		generateSideDataEntry(s, out,"relatedObjects","${items.0.recordtype}.html",false,true,false);
		return out;
	}

	private JSONObject uispec(UIRequest request,String suffix) throws UIException {
		try {
			JSONObject out=new JSONObject();
			Structure s = record.getStructure(this.structureview);
			if(s.showListSection()){
				out.put(s.getListSectionName(),generateListSection(s));
			}
			if(s.showEditSection()){
			out.put(s.getEditSectionName(),generateDataEntrySection());
			}
			if(s.showTitleBar()){
				out.put("titleBar",generateTitleSection());
			}
			
			if(s.showSideBar()){
				out.put("sidebar",generateSidebarSection(s));
			}
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException",e);
		}
	}

	public void configure() throws ConfigException {}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		JSONObject out=uispec(q.getUIRequest(),StringUtils.join(tail,"/"));
		q.getUIRequest().sendJSONResponse(out);
	}

	public void configure(WebUI ui,Spec spec) {}
}
