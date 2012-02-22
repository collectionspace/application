/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.nuispec;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.schema.UISpecRunContext;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UISpec implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(UISpec.class);
	protected Record record;
	protected Storage storage;
	protected Spec spec;
	protected String tenantname = "html";
	protected String structureview;
	protected String spectype = "";
	protected CacheTermList ctl;

	public UISpec(Spec spec) {
		this.spec = spec;
	}

	// XXX should be moved into fs.getIDPath itself, but refactoring would take too long for 1.7 -- dan
	private String[] getFullIDPath(FieldSet fs,UISpecRunContext context) {
		List<String> out = new ArrayList<String>();
		out.addAll(Arrays.asList(context.getUIPrefix()));
		out.addAll(Arrays.asList(fs.getIDPath()));		
		return out.toArray(new String[0]);
	}
	
	public UISpec(Record record, String structureview) {
		this.record=record;
		this.spec = record.getSpec();
		this.structureview = structureview;
		this.spectype = "";
		if(structureview.equals("search")){
			this.spectype = "search";
		}

	}

	// XXX make common
	protected String veryplainWithoutEnclosure(FieldSet f,UISpecRunContext context) {

		if(!f.getSearchType().equals("") && this.spectype.equals("search")){
			return f.getID();
		}
		List<String> path=new ArrayList<String>();
		String pad="fields";
		path.add(pad);
		for(String part : getFullIDPath(f,context)) {
			if(pad.equals("0")){
				if(context.hasPad()){
					path.add(pad);
				}
				pad="";
			}
			else{
				pad="0";
			}
			path.add(part);
		}
		return StringUtils.join(path,'.');		
	}
	protected String veryplain(FieldSet f,UISpecRunContext context) {
		return "${"+veryplainWithoutEnclosure(f,context)+"}";		
	}
	protected String veryplain(String f) {
		return "${"+f+"}";		
	}

	// XXX make common
	protected String plain(Field f,UISpecRunContext context) {
		if(f.getParent().isExpander() ||  f.isRepeatSubRecord()){
			return radio(f);
		}
		if(f.getParent() instanceof Repeat){
			Repeat rp = (Repeat)f.getParent();//remove bogus repeats used in search
			if(!rp.getSearchType().equals("repeator") && !this.spectype.equals("search")){
				return radio(f);
			}
			if(this.spectype.equals("search")){
				return radio(f);
			}
		}
		return veryplain(f,context);
	}
	// XXX make common
	protected String radio(Field f) {
		List<String> path=new ArrayList<String>();
		String pad="{row}";
		path.add(pad);
		
		String[] paths = f.getIDPath();
		path.add(paths[paths.length - 1]);
		return "${"+StringUtils.join(path,'.')+"}";		
	}
	// XXX make common
	// ${items.0.name}
	protected String plainlist(Field f) {
		List<String> path=new ArrayList<String>();
		String name="items";
		path.add(name);
			String pad="0";
			path.add(pad);
			path.add(f.getID());
		
		return "${"+StringUtils.join(path,'.')+"}";		
	}

//	protected JSONObject linktext(Field f) throws JSONException  {
//		JSONObject number=new JSONObject();
//		number.put("linktext",f.getLinkText());
//		number.put("target",f.getLinkTextTarget());
//		return number;	
//	}

	private String makeSelector(String pre, UISpecRunContext context, String post){
		List<String> affixes = Arrays.asList(context.getUIAffix());
		String selector = pre;
		for(String part : affixes) {
			if(part != null && part !=""){
				selector += part;
			}
		}
		selector += post;
		
		return selector;
	}
	
	protected String getSelectorAffix(FieldSet fs){
		return fs.getSelectorAffix();
	}
	protected String getContainerSelector(FieldSet fs,  UISpecRunContext context){
		return makeSelector(fs.getPreContainerSelector(),context,fs.getContainerSelector());
	}
	protected String getTitleSelector(FieldSet fs,  UISpecRunContext context){
		return makeSelector(fs.getPreTitleSelector(),context,fs.getTitleSelector());
	}

	protected String getSelector(FieldSet fs, UISpecRunContext context){
		return makeSelector(fs.getPreSelector(),context,fs.getSelector());
	}
	protected String getDecoratorSelector(FieldSet fs, UISpecRunContext context){
		return makeSelector(fs.getDecoratorSelector(),context,fs.getSelector());
	}
	
	protected Object generateOptionField(Field f,UISpecRunContext context) throws JSONException {
		// Dropdown entry
		JSONObject out=new JSONObject();
		if("radio".equals(f.getUIType())) {
			out.put("selection",radio(f));
		}
		else{
			out.put("selection",plain(f,context));
		}
		JSONArray ids=new JSONArray();
		JSONArray names=new JSONArray();
		int idx=0;
		boolean hasdefault = false;
		String dfault=null;
		for(Option opt : f.getAllOptions()) {
			if(opt.getID().equals("")){ hasdefault = true;}
			ids.put(opt.getID());
			names.put(opt.getName());
			if(opt.isDefault())
				dfault=opt.getID();
			idx++;
		}
		
		//need to make sure there is always a blank on search and default is ""
		if(this.spectype.equals("search") && !f.getSearchType().equals("")){
			out.put("default","");
			if(!hasdefault){ids.put("");names.put(f.enumBlankValue());}
			out.put("optionlist",ids);
			out.put("optionnames",names);	
		}
		else{

			//currently only supports single select dropdowns and not multiselect
			if(dfault!=null)
				out.put("default",dfault+"");
			out.put("optionlist",ids);
			out.put("optionnames",names);	
		}
		return out;
	}
	// XXX factor
	protected Object generateDataEntryField(Field f,UISpecRunContext context) throws JSONException {
		if("plain".equals(f.getUIType())) {
			// Plain entry
			return plain(f,context);
		} 
		else if("list".equals(f.getUIType())){
			return plainlist(f);
		}
		else if("dropdown".equals(f.getUIType())) {
			return generateOptionField(f,context);
		}
		else if("enum".equals(f.getUIType())) {
			return generateENUMField(f,context);
		}
		else if(f.getUIType().startsWith("groupfield")) {
			return generateGroupField(f,context);
		}
		//ignore ui-type uploader
		return plain(f,context);	
	}

	protected Object generateGroupField(FieldSet fs,UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		if(fs instanceof Field || fs instanceof Repeat){
		
			JSONArray decorators=new JSONArray();
			JSONObject options=new JSONObject();
			UISpecRunContext sub = context.createChild();
			sub.setUIPrefix(fs.getID());
			sub.setPad(false);
			//context.appendAffix("objectProductionDates-");
			String parts[] = fs.getUIType().split("/");
			JSONObject subexpander = new JSONObject();
			Record subitems = fs.getRecord().getSpec().getRecordByServicesUrl(parts[1]);
			options.put("elPath", "fields."+fs.getPrimaryKey());
			
			if(parts[1].equals("structureddate")){
				out.put("value",veryplain("fields."+fs.getPrimaryKey()));
				Boolean truerepeat = false;
				FieldParent fsp = fs.getParent();
				if(fsp instanceof Repeat && !(fsp instanceof Group)){
					Repeat rp = (Repeat)fsp;//remove bogus repeats used in search
					if(!rp.getSearchType().equals("repeator") && !this.spectype.equals("search")){
						String prefix = "";
						if(fs instanceof Group && !((Group) fs).getXxxServicesNoRepeat()){
							//XXX refacetor with some idea of UIContext.
							//add a prefix for nested non repeatables
							prefix = fs.getID()+".";
						}
						truerepeat = true;
						for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {	
							subexpander.put(getSelector(fs2,sub), prefix+fs2.getID());
						}
						if(fs instanceof Group && !((Group) fs).getXxxServicesNoRepeat()){
							options.put("elPath", prefix+fs.getPrimaryKey());
							options.put("root", "{row}");
							out.put("value",veryplain("{row}."+prefix+fs.getPrimaryKey()));
						}
						else if(fs instanceof Repeat){

							options.put("elPath", fs.getPrimaryKey());
							options.put("root", "{row}");
							out.put("value",veryplain("{row}."+fs.getPrimaryKey()));
						}
					}
				}
				if(!truerepeat){
					for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {	
						if(!fs2.getSearchType().equals("false") || !this.spectype.equals("search")){ //only hide if this is a search uispec - need to extend to all uispec stuff
							subexpander.put(getSelector(fs2,sub), veryplainWithoutEnclosure(fs2,sub));
						}
					}
				}

				options.put("elPaths", subexpander);
			}
			else if(parts.length>=3 && parts[2].equals("selfrenderer")){
				Boolean truerepeat = false;
				FieldParent fsp = fs.getParent();
				if(fsp instanceof Repeat && !(fsp instanceof Group)){
					Repeat rp = (Repeat)fsp;//remove bogus repeats used in search
					if(!rp.getSearchType().equals("repeator") && !this.spectype.equals("search")){
						truerepeat = true;
						for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {	
							generateDataEntry(subexpander,fs2, context);
						}
						JSONObject renderedcontents=new JSONObject();
						generateMessageKeys(context, subexpander, subitems);
						generateSelfRenderedEntry(renderedcontents, subexpander);
						options = renderedcontents;
					}
				}
				if(!truerepeat){
					for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {	
						generateDataEntry(subexpander,fs2, context);
					}
					JSONObject renderedcontents=new JSONObject();
					generateMessageKeys(context, subexpander, subitems);
					generateSelfRenderedEntry(renderedcontents,subexpander);
					options = renderedcontents;
				}
			}
			else{
				out.put("value",veryplain("fields."+fs.getPrimaryKey()));
				for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {		
					generateDataEntry(subexpander,fs2, sub);
				}

				JSONObject expander = new JSONObject();
				expander.put("type", "fluid.noexpand");
				expander.put("tree", subexpander);
				
				JSONObject protoTree = new JSONObject();
				protoTree.put("expander", expander);

				options.put("protoTree", protoTree);
				
			}
			
			sub.setUIAffix(fs.getID()+"-");
			
			
			JSONObject decorator=getDecorator("fluid",null,fs.getUIFunc(),options,fs.isReadOnly());
			decorators.put(decorator);
			out.put("decorators",decorators);

		}
		
		return out;
	}

	protected Object generateENUMField(Field f,UISpecRunContext context) throws JSONException {
		JSONObject out = new JSONObject();
		JSONArray decorator = new JSONArray();
		JSONObject options = new JSONObject();
		if(f.getParent() instanceof Repeat && !(((Repeat)f.getParent()).getSearchType().equals("repeator" ) && !this.spectype.equals("search")) ){
			options.put("elPath", f.getID());
			options.put("root", "{row}");
		}
		else{
			options.put("elPath", veryplainWithoutEnclosure(f,context));
		}
		options.put("termListType", f.getID());
		JSONObject decdata = getDecorator("fluid", null, "cspace.termList", options,f.isReadOnly());
		decorator.put(decdata);
		out.put("decorators", decorator);
	
		return out;
	}
	


	
	
	protected JSONArray generateRepeatExpanderEntry(Repeat r, UISpecRunContext context) throws JSONException {
		JSONArray expanders = new JSONArray();
		JSONObject siblingexpander = new JSONObject();
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.renderer.repeat");
		expander.put("controlledBy", veryplainWithoutEnclosure(r,context));//"fields."+r.getID());
		expander.put("pathAs", "row");
		expander.put("repeatID", getSelector(r,context)+":");

		if(r.getChildren("").length>0){
			JSONObject tree = new JSONObject();
			for(FieldSet child : r.getChildren("")) {
				if(!this.spectype.equals("search") || (this.spectype.equals("search") && !child.getSearchType().equals(""))){

					if(child.getUIType().equals("hierarchy")){

						expander.put("repeatID", getSelector(child,context)+":");
						generateHierarchyEntry(siblingexpander, child,  context);
						expanders.put(siblingexpander.getJSONObject("expander"));
						if(child instanceof Field){
							String classes = getDecoratorSelector(child,context);
							JSONObject decorator = getDecorator("addClass",classes,null,null,child.isReadOnly());
							tree.put("value", generateDataEntryField((Field)child,context));
							tree.put("decorators", decorator);
						}
					}
					else if(child.getUIType().equals("decorated")){

						expander.put("repeatID", getSelector(child,context)+":");
						if(child instanceof Field){
							String classes = getDecoratorSelector(child,context);
							JSONObject decorator = getDecorator("addClass",classes,null,null,child.isReadOnly());
							tree.put("value", generateDataEntryField((Field)child,context));
							tree.put("decorators", decorator);
						}
					}
					else{
						generateDataEntry(tree,child, context);
					}
				}
			}
			if(r.isConditionExpander()){
				expander.put("valueAs", "rowValue");
				JSONObject cexpander = new JSONObject();
				JSONObject texpander = new JSONObject();
				cexpander.put("type", "fluid.renderer.condition");
				JSONObject condpander = new JSONObject();
				condpander.put("funcName", "cspace.admin.assertRoleDisplay");
				condpander.put("args", "{rowValue}.display");

				cexpander.put("condition", condpander);
				cexpander.put("trueTree", tree);

				generateTrueTree(texpander, cexpander);
				expander.put("tree", texpander);
			}
			else{
				expander.put("tree", tree);
			}
		}
		expanders.put(expander);
		return expanders;
	}
	
	protected JSONObject generateSelectionExpanderEntry(Field f, UISpecRunContext context) throws JSONException {
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.renderer.selection.inputs");
		expander.put("rowID", getSelector(f,context)+"-row:");
		expander.put("labelID", getSelector(f,context)+"-label");
		expander.put("inputID", getSelector(f,context)+"-input");
		expander.put("selectID", f.getID());

		JSONObject tree = new JSONObject();
		tree = (JSONObject)generateOptionField(f,context);
		expander.put("tree", tree);
		return expander;
	}

	// Unused. Delete it? If not comment here. May 2011.
	private JSONObject generateNonExpanderEntry(Repeat r, UISpecRunContext context) throws JSONException {
		JSONObject out = new JSONObject();
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.renderer.noexpand");

		if(r.getChildren("").length>0){
			JSONObject tree = new JSONObject();
			for(FieldSet child : r.getChildren("")) {
				generateDataEntry(tree,child, context);
			}
			expander.put("tree", tree);
		}
		out.put("expander", expander);
		return out;
	}
	
	protected JSONObject generateRepeatEntry(Repeat r, UISpecRunContext context, JSONObject outer) throws JSONException {

		JSONObject out = new JSONObject();
		
		if(r.isExpander()){
			JSONArray expanders= generateRepeatExpanderEntry(r,context);
			out.put("expanders", expanders);
		}
		else{
			JSONArray decorators=new JSONArray();

			JSONObject options=new JSONObject();
			JSONObject preProtoTree=new JSONObject();

			if(r.usesRecord() ){
				if(!r.getUISpecInherit()){
					UISpecRunContext sub = context.createChild();
					if(!getSelectorAffix(r).equals("")){
						if(!context.equals("")){
							sub.setUIAffix(getSelectorAffix(r));
						}
						else{
							sub.setUIAffix(getSelectorAffix(r));
						}
					}
					String sp=r.getUISpecPrefix();
					if(sp!=null)
						sub.setUIPrefix(sp);
					generateSubRecord(preProtoTree, r,sub, outer);
				}
				else{
					generateSubRecord(preProtoTree, r,context, outer);
				}
			}
			else{
				for(FieldSet child :r.getChildren("")) {
					if(!this.spectype.equals("search") || (this.spectype.equals("search") && !child.getSearchType().equals(""))){
						generateDataEntry(preProtoTree,child, context);
					}
				}
			}
			
			JSONObject expander = new JSONObject();
			expander.put("type", "fluid.noexpand");
			expander.put("tree", preProtoTree);
			
			
			JSONObject repeatTree = new JSONObject();
			repeatTree.put("expander", expander);
			if(r.getParent() instanceof Record){
				options.put("elPath",veryplainWithoutEnclosure(r,context));
			}
			else{
				options.put("elPath", r.getID());
				options.put("root", "{row}");
			}
			options.put("repeatTree", repeatTree);
			//is this a nested repeat or a top level repeat...
			//is this a uispec for search - if so no primary tags wanted
			if(r.getSearchType().startsWith("repeator") && this.spectype.equals("search")){
				options.put("hidePrimary", true);
			}
			

			JSONObject decorator = getDecorator("fluid",null,"cspace.makeRepeatable",options,r.isReadOnly());
			decorators.put(decorator);
			out.put("decorators",decorators);
		}
		return out;
	}

	//get all children as well as pseudo sub records like groupfields
	private static List<FieldSet> getChildrenWithGroupFields(Repeat parent, String operation){
		List<FieldSet> children = new ArrayList<FieldSet>();
		
		for(FieldSet fs : parent.getChildren(operation)) {

			if(fs.getUIType().startsWith("groupfield")){
				String parts[] = fs.getUIType().split("/");
				Record subitems = fs.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

				for(FieldSet fd : subitems.getAllFieldTopLevel(operation)) {
					children.add(fd); //what about nested groupfields?
				}
			}
			else{
				children.add(fs);
			}
		}
		return children;
	}
	protected JSONObject generateAutocomplete(Field f,UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();

		
		JSONObject options=new JSONObject();
		String extra="";
		if(f.getRecord().isType("authority"))
			extra="vocabularies/";
		String[] contextdata = context.getUIRecordUrl();
		String autocompleteurl = extra + f.getRecord().getWebURL();
		if(contextdata.length>0){
			autocompleteurl = contextdata[0];
		}
		
		options.put("queryUrl","../../../tenant/"+tenantname+"/"+autocompleteurl+"/autocomplete/"+f.getID());
		options.put("vocabUrl","../../../tenant/"+tenantname+"/"+autocompleteurl+"/source-vocab/"+f.getID());

		if(!f.getAutocompleteFuncName().equals("")){
			JSONObject invokers = new JSONObject();
			JSONObject subitem = new JSONObject();
			String test[] = f.getAutocompleteFuncName().split("\\|");
			if(test.length>1){
				subitem.put("funcName",test[1]);
				invokers.put(test[0], subitem);				
			}
			options.put("invokers", invokers);
		}
		if(!f.getAutocompleteStrings().equals("")){
			JSONObject strings = new JSONObject();
			String val = f.getAutocompleteStrings();
			String lines[] = val.split("\\r?\\n");
			for(int i=0; i<lines.length;i++){
				String[] data = lines[i].split("\\|");
				if(data.length==2){
					strings.put(data[0].trim(), data[1].trim());
				}
			}
			options.put("strings", strings);
		}

		JSONObject decorator=getDecorator("fluid",null,"cspace.autocomplete",options,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		decorators.put(decorator);
		out.put("decorators",decorators);
		if(f.isRefactored()){
			out.put("value", generateDataEntryField(f,context));
		}
		return out;
	}

	protected JSONObject generateChooser(Field f,UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();

		JSONObject options=new JSONObject();
		JSONObject selectors=new JSONObject();
		selectors.put("numberField",getSelector(f,context));
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
		
		JSONObject decorator=getDecorator("fluid",null,"cspace.numberPatternChooser",options,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getContainerSelector(f,context));
			}
		}
		decorators.put(decorator);
		out.put("decorators",decorators);
		if(f.isRefactored()){
			out.put("valuebinding", generateDataEntryField(f,context));
		}
		return out;
	}
	
	/**
	 * This is a bit of JSON needed by the UI so they can validate data types like integer,float etc
	 * CSPACE-4330
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject generateDataTypeValidator(Field f, UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject options = new JSONObject();
		String type = f.getDataType();
		if(type.equals("")){type = "string";}
		options.put("type",type);
		options.put("label",f.getLabel());
		JSONObject decorator=getDecorator("fluid",null,"cspace.inputValidator",options,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		decorators.put(decorator);
		out.put("decorators",decorators);
		out.put("value", generateDataEntryField(f,context));
		return out;
	}

	/**
	 * This is a bit of JSON needed by the UI so they display dates
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject generateDate(Field f,UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject decorator=getDecorator("fluid",null,"cspace.datePicker",null,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		decorators.put(decorator);
		out.put("decorators",decorators);
		out.put("value", generateDataEntryField(f,context));
		return out;
	}

	protected void generateMessageKeys(UISpecRunContext affix, JSONObject temp, FieldSet fs) throws JSONException {
		Record r = fs.getRecord();
		if(this.spectype.equals("search")){ //is this a search uispec
			if(fs.getID()!=null){
				if(fs.getSearchType().startsWith("repeator") && this.spectype.equals("search")){
					Repeat rp = (Repeat)fs;

					for(FieldSet child : rp.getChildren("")) {
						generateMessageKey(temp, r.getUILabelSelector(child.getID()), child.getLabel());
					}
				}
				else{
					generateMessageKey(temp, r.getUILabelSelector(fs.getID()), fs.getLabel());
				}
			}
		}
		else{
			if(fs.getID()!=null){
				generateMessageKey(temp, fs.getUILabelSelector(), fs.getLabel());
			}
			if(fs instanceof Repeat){
				Repeat rp = (Repeat)fs;
				for(FieldSet child : rp.getChildren("")) {
					generateMessageKeys(affix,temp,child);
				}
			}
		}
	}

	protected JSONObject generateMessageKeys(UISpecRunContext affix, JSONObject temp, Record r) throws JSONException {
		if(this.spectype.equals("search")){ //is this a search uispec
			for(String st: r.getAllUISections("search")){
				if(st!=null){
					generateMessageKey(temp, r.getUILabelSelector(st),r.getUILabel(st));
				}
			}
			for(FieldSet fs : r.getAllFieldTopLevel(this.spectype)) {
				if(fs.getID()!=null){
					if(fs.getSearchType().startsWith("repeator") && this.spectype.equals("search")){
						Repeat rp = (Repeat)fs;

						for(FieldSet child : rp.getChildren("")) {
							generateMessageKey(temp, r.getUILabelSelector(child.getID()), child.getLabel());
						}
					}
					else{
						generateMessageKey(temp, r.getUILabelSelector(fs.getID()), fs.getLabel());
					}
				}
			}
		}
		else{
			for(String st: r.getAllUISections("")){
				if(st!=null){
					generateMessageKey(temp, r.getUILabelSelector(st),r.getUILabel(st));
				}
			}
			
			for(FieldSet fs : r.getAllFieldFullList("")) { //include children of repeats as well as top level
				if(fs.getID()!=null){
					generateMessageKey(temp, fs.getUILabelSelector(), fs.getLabel());
				}
			}
		}
		return temp;
	}
	
	private void generateMessageKey(JSONObject temp, String labelSelector, String label) throws JSONException {
		JSONObject msg = new JSONObject();
		msg.put("messagekey", label);
		temp.put(labelSelector, msg);
	}

	protected JSONObject generateHierarchySection(UISpecRunContext affix, Boolean addMessages) throws JSONException{
		JSONObject out = new JSONObject();
		Record subf = record.getSpec().getRecord("hierarchy");

		String extra = "";
		if(record.getRecord().isType("authority"))
			extra ="vocabularies/";
		affix.setUIRecordUrl(extra + record.getWebURL());
		generateSubRecord(subf, out, false, affix);
		
		if(addMessages){
			out = generateMessageKeys(affix,out, subf);
		}
		return out;
	}
	
	
	protected JSONObject generateRecordEditor(UISpecRunContext context, Boolean addMessages) throws JSONException{
		JSONObject out = generateDataEntrySection(context);
		if(addMessages){
			out = generateMessageKeys(context,out, record);
		}
		return out;
	}
	
	protected JSONObject generateDataEntrySection(UISpecRunContext affix) throws JSONException {
		return generateDataEntrySection(affix,record);
	}
	
	protected JSONObject generateDataEntrySection(UISpecRunContext context, Record r) throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet fs : r.getAllFieldTopLevel(this.spectype)) {
			generateDataEntry(out,fs,context);
		}
		return out;
	}
	
	protected JSONObject generateListSection(Structure s, UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		String id = s.getListSectionName();
		if(s.getFieldTopLevel(id) != null){
			FieldSet fs = s.getFieldTopLevel(id);
			generateDataEntry(out,fs, context);
		}
		
		return out;
	}
	protected void generateSubRecord(JSONObject out, FieldSet fs, UISpecRunContext context, JSONObject parent) throws JSONException {
		Record subrecord = fs.usesRecordId();
		Boolean repeated = false;
		if(fs.getParent() instanceof Repeat ||( fs instanceof Repeat && !(fs instanceof Group))){
			repeated = true;
		}
		if( parent == null){
			parent = out;
		}
		 if(fs instanceof Group){
			Group gp = (Group)fs;
			if(gp.isGrouped()){
				context.setPad(false);
			}
		}
		
		generateSubRecord(subrecord, out,  repeated,  context, parent);
		
	}
	protected void generateSubRecord(Record subr, JSONObject out, Boolean repeated, UISpecRunContext context) throws JSONException {
		for(FieldSet fs2 : subr.getAllFieldTopLevel("")) {
			if(repeated){
				fs2.setRepeatSubRecord(true);
			}
			generateDataEntry(out,fs2, context);
			fs2.setRepeatSubRecord(false);
		}
		Structure s = subr.getStructure(this.structureview);
		if(s.showMessageKey()){
			out = generateMessageKeys(context,out, subr);
		}
		
	}
	protected void generateSubRecord(Record subr, JSONObject out, Boolean repeated, UISpecRunContext context, JSONObject parent) throws JSONException {
		for(FieldSet fs2 : subr.getAllFieldTopLevel("")) {
			if(repeated){
				fs2.setRepeatSubRecord(true);
			}
			generateDataEntry(out,fs2, context);
			fs2.setRepeatSubRecord(false);
		}
		Structure s = subr.getStructure(this.structureview);
		if(s.showMessageKey()){
			parent = generateMessageKeys(context,parent, subr);
		}
		
	}
	
	protected void generateDataEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException {

		if("uploader".equals(fs.getUIType())) {
			generateUploaderEntry(out,fs,context);
		}
		if("hierarchy".equals(fs.getUIType())) {
			generateHierarchyEntry(out,fs,context);
		}
		//this is a subrecord bit that is surrounded by a group tag in the parent obj
		if(fs.usesRecord() && !(fs instanceof Repeat && !(fs instanceof Group))){
			if(!fs.getUISpecInherit()){
				//default behaviour do group or field as expected
				UISpecRunContext sub = context.createChild();
				if(!getSelectorAffix(fs).equals("")){
					if(!context.equals("")){
						sub.setUIAffix(getSelectorAffix(fs));
					}
					else{
						sub.setUIAffix(getSelectorAffix(fs));
					}
				}
				String sp=fs.getUISpecPrefix();
				if(sp!=null)
					sub.setUIPrefix(sp);
				generateSubRecord(out, fs,sub, null);
			}
			else{
				//create group item or field at the same level as parent fields - do not nest
				generateSubRecord(out, fs,context, null);
			}
		}
		else{
			if(fs instanceof Field) {
				// Single field
				Field f=(Field)fs;
				if(f.isExpander()){
					generateExpanderDataEntry(out, context, f);
				}
				else if(f.isInTrueTree()){
					//used when true tree magic is needed?
					JSONObject tout = new JSONObject();
					if(!f.isRefactored()){
						generateFieldDataEntry_notrefactored(tout, context, f);
					}
					else{
						generateFieldDataEntry_refactored(tout, context, f);
					}

					JSONObject cexpander = new JSONObject();
					cexpander.put("trueTree", tout);

					generateTrueTree(out, cexpander);
				}
				//XXX when all uispecs have moved across we can delete most of this
				else if(!f.isRefactored()){
					generateFieldDataEntry_notrefactored(out, context, f);
				}
				else{
					generateFieldDataEntry_refactored(out, context, f);
				}
			} 
			else if(fs instanceof Group) {
				generateGroupDataEntry(out, fs, context);
			} 
			else if(fs instanceof Repeat) {
				generateRepeatDataEntry(out, fs, context);
			}

		}

	}

	protected void generateExpanderDataEntry(JSONObject out, UISpecRunContext affix, Field f)
			throws JSONException {
		if("radio".equals(f.getUIType())){
			JSONObject obj = generateSelectionExpanderEntry(f,affix);
			out.put("expander",obj);
		}
	}

	protected void generateRepeatDataEntry(JSONObject out, FieldSet fs,
			UISpecRunContext affix) throws JSONException {
		// Container
		Repeat r=(Repeat)fs;
		if(r.getXxxUiNoRepeat()) { //this is not a repeat in the UI only repeats in the service layer
			FieldSet[] children=r.getChildren("");
			if(children.length!=0){
				generateDataEntry(out,children[0], affix);
			}
		} else {
			JSONObject row=new JSONObject();
			JSONArray children=new JSONArray();
			if(r.asSibling() && !r.hasServicesParent()){ // allow for row [{'','',''}] e.g. roles and permissions
				repeatSibling(out, affix, r, row, children);
			}
			else{//this should be most repeats
				repeatNonSibling(out, affix, r);
			}
		}
	}

	protected void repeatNonSibling(JSONObject out,  UISpecRunContext context,
			Repeat r) throws JSONException {
		JSONObject contents=generateRepeatEntry(r, context,out); //gather all standard repeatable bits
		String selector = getSelector(r,context);
		//CSPACE-2619 scalar repeatables are different from group repeats
		if(r.getChildren("").length==1){
			FieldSet child = null;
			if(r.getChildren("")[0] instanceof Field){
				child = (Field)r.getChildren("")[0];
				selector = getSelector(child,context);
			}
			else if(r.getChildren("")[0] instanceof Group){
				child = (Group)r.getChildren("")[0];
				selector = getSelector(child,context);
			}
		}
		if(r.isExpander()){
			selector="expander";
			JSONArray expanders = contents.getJSONArray("expanders");
			if(out.has("expander")){
				expanders.put(out.get("expander"));
			}
			if(expanders.length() == 1){
				out.put(selector,expanders.getJSONObject(0));
			}
			else{
				out.put(selector,expanders);
			}
		}
		else{
			out.put(selector,contents);
		}
	}

	protected void repeatSibling(JSONObject out, UISpecRunContext context, Repeat r,
			JSONObject row, JSONArray children) throws JSONException {
		JSONObject contents=new JSONObject();
		for(FieldSet child : r.getChildren("")) {
			generateDataEntry(contents,child, context);
		}
		children.put(contents);
		row.put("children",children);
		out.put(getSelector(r,context),row);
	}

	protected void generateGroupDataEntry(JSONObject out, FieldSet fs,
			UISpecRunContext context) throws JSONException {
		Group g = (Group)fs;


		JSONObject contents=new JSONObject();
		for(FieldSet child : g.getChildren("")) {
			generateDataEntry(contents,child, context);
		}		//UI specific marking: YURA said: these are renderer decorators that do their own rendering so need some sub nesting
		if("selfrenderer".equals(fs.getUIType())) {
			JSONObject renderedcontents=new JSONObject();
			generateSelfRenderedEntry(renderedcontents, contents);
			contents = renderedcontents;
		}
		else if(g.getUIType().startsWith("groupfield")) { //structured dates are being a little different
			out.put(getSelector(g,context),generateGroupField(g,context));
			return;
		}
		out.put(getSelector(g,context),contents);
	}

	protected void generateUploaderEntry(JSONObject out, FieldSet f, UISpecRunContext context) throws JSONException{
		String condition =  "cspace.mediaUploader.assertBlob";

		JSONObject cond = new JSONObject();
		if(f instanceof Group){
			Group gp = (Group)f;
			String test = gp.usesRecordValidator();
			FieldSet tester = record.getFieldTopLevel(test);
			if(tester instanceof Field){
				cond.put("args",plain((Field)tester,context));
			}
			cond.put("funcName", condition);
		}
		JSONObject ttree = new JSONObject();
		ttree.put(getSelector(f,context),new JSONObject());
		JSONObject decorator = getDecorator("addClass","hidden",null,null,f.isReadOnly());
		JSONObject decorators = new JSONObject();
		decorators.put("decorators", decorator);
		JSONObject ftree = new JSONObject();
		ftree.put(getSelector(f,context),decorators);
		JSONObject cexpander = new JSONObject();
		cexpander.put("type", "fluid.renderer.condition");
		cexpander.put("condition", cond);
		cexpander.put("trueTree", ttree);
		cexpander.put("falseTree", ftree);
		generateTrueTree(out,cexpander);
		
	}
	/*
	 * [10:57] <yura> csm22: because the markup for dimensions is delivered separately from the
	 *  rest of the record editor's markup, and thus dimensions decorator itself will render those. this is all similar to higherarchies
	 *  [10:58] <yura> csm22: there are basically 2 types of decorators
	 *  renderer decorators that do their own rendering: e.g. structuredate, heirarchical, dimensions
	 *  and ones like date picker and autocomplete
	 * generateSelfRenderedEntry
	 * ".csc-collection-object-dimension": {
            "decorators": [{
                "func": "cspace.dimension",
                "type": "fluid",
                "options": {
                    "protoTree": {
                        "expander": {
                            "tree": {
	 * */

	protected void generateSelfRenderedEntry(JSONObject out, JSONObject tree) throws JSONException{
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.noexpand");
		expander.put("tree", tree);
	
		JSONObject protoTree = new JSONObject();
		protoTree.put("expander", expander);
		out.put("protoTree", protoTree);
	
	}
	protected void generateHierarchyEntry(JSONObject out, FieldSet f, UISpecRunContext context) throws JSONException{
		String condition =  "cspace.hierarchy.assertEquivalentContexts";
		Record thisr = f.getRecord();
		JSONObject cond = new JSONObject();
		if(f instanceof Field){
			FieldSet fs = (FieldSet)f.getParent();
			JSONObject args = new JSONObject();
			args.put(fs.getID(), veryplain(fs,context));
			cond.put("args",args);
			cond.put("funcName", condition);
		}
		JSONObject ttree = new JSONObject();
		generateMessageKey(ttree, thisr.getUILabelSelector(f.getID()), f.getLabel());
		
		JSONObject decorator = getDecorator("addClass","hidden",null,null,f.isReadOnly());
		JSONObject decorators = new JSONObject();
		decorators.put("decorators", decorator);
		JSONObject ftree = new JSONObject();
		ftree.put(thisr.getUILabelSelector(f.getID()),decorators);
		
		
		JSONObject cexpander = new JSONObject();
		cexpander.put("type", "fluid.renderer.condition");
		cexpander.put("condition", cond);
		cexpander.put("trueTree", ttree);
		cexpander.put("falseTree", ftree);

		generateTrueTree(out, cexpander);
		
	}
	
	protected void generateTrueTree(JSONObject out, JSONObject trueTreeBits) throws JSONException{
		if(!out.has("expander")){
			out.put("expander",trueTreeBits);
		}
		else{
			JSONObject exp = out.getJSONObject("expander");
			Iterator rit=trueTreeBits.keys();
			while(rit.hasNext()) {
				String key=(String)rit.next();
				if(exp.has(key)){
					//can only amalgamte if they are objs
					if(exp.get(key) instanceof JSONObject){
						JSONObject merged = exp.getJSONObject(key);
						for(String keyd : JSONObject.getNames(trueTreeBits.getJSONObject(key)))
						{
							merged.put(keyd, trueTreeBits.getJSONObject(key).get(keyd));
						}
						exp.put(key,merged);
					}
				}
				else{
					exp.put(key, trueTreeBits.get(key));
				}
			}
			out.put("expander",exp);
		}
		
	}
	
	protected JSONObject getDecorator(String type, String className, String func, JSONObject options, Boolean readOnly) throws JSONException{
		JSONObject decorator = new JSONObject();
		decorator.put("type",type);
		if(className != null){
			decorator.put("classes",className);
		}
		if(func != null){
			decorator.put("func",func);
		}
		if(options != null){
			if(readOnly){
				options.put("readOnly",true);
			}
			decorator.put("options",options);
		}
		else if(readOnly){
			options = new JSONObject();
			options.put("readOnly",true);
			decorator.put("options",options);
		}
		return decorator;
	}
	
	protected void generateFieldDataEntry_refactored(JSONObject out, UISpecRunContext context, Field f)
			throws JSONException {
		if(f.hasAutocompleteInstance()) {
			makeAuthorities(out, context, f);
		}
		else if("chooser".equals(f.getUIType()) && !this.spectype.equals("search")) {
			out.put(getSelector(f,context),generateChooser(f,context));
		}
		else if("date".equals(f.getUIType())) {
			out.put(getSelector(f,context),generateDate(f,context));
		}
		else if("validated".equals(f.getUIType())){
			out.put(getSelector(f,context),generateDataTypeValidator(f,context));
		}
		else if("sidebar".equals(f.getUIType())) {
			//Won't work now if uncommented
			//out.put(getSelector(f)+affix,generateSideBar(f));
		}
		else{
			out.put(getSelector(f,context),generateDataEntryField(f,context));	
		}
	}

	protected void makeAuthorities(JSONObject out, UISpecRunContext context, Field f)
			throws JSONException {
		if("enum".equals(f.getUIType())){
			out.put(getSelector(f,context),generateDataEntryField(f,context));
		}
		else{
			out.put(getSelector(f,context),generateAutocomplete(f,context));
		}
	}

	protected void generateFieldDataEntry_notrefactored(JSONObject out, UISpecRunContext context, Field f)
			throws JSONException {
		// Single field
		out.put(getSelector(f,context),generateDataEntryField(f,context));	
		
		if(f.hasAutocompleteInstance()) {
			makeAuthorities(out, context, f);
		}
		if("chooser".equals(f.getUIType()) && !this.spectype.equals("search")) {
			out.put(getContainerSelector(f,context),generateChooser(f,context));
		}
		if("date".equals(f.getUIType())) {
			out.put(getSelector(f,context),generateDate(f,context));
		}
		if("validated".equals(f.getUIType())){
			out.put(getSelector(f,context),generateDataTypeValidator(f,context));
		}
	}

	private void generateTitleSectionEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException {
		if(fs instanceof Field) {
			Field f=(Field)fs;
			if(!f.isInTitle())
				return;
			out.put(getTitleSelector(f,context),veryplain(f,context));
		} else if(fs instanceof Repeat) {
			for(FieldSet child : ((Repeat)fs).getChildren(""))
				generateTitleSectionEntry(out,child, context);
		}
	}

	protected JSONObject generateTitleSection(UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet f : record.getAllFieldTopLevel("")) {
			generateTitleSectionEntry(out,f, context);
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
	private JSONObject generateSideDataEntry(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException {
		Repeat f=(Repeat)fs;
		JSONObject listrow=new JSONObject();
		generateDataEntry(listrow,fs, context);
		out.put(f.getID(),listrow);
		return out;
	}
	
	private JSONObject generateSideDataEntry(Structure s, JSONObject out, String fieldName,String url_frag,boolean include_type,boolean include_summary,boolean include_sourcefield, UISpecRunContext context )throws JSONException {
		FieldSet fs = s.getSideBarItems(fieldName);
		if(fs == null){
			//XXX default to show if not specified
			out.put(fieldName,generateSidebarPart(url_frag,include_type,include_summary,include_sourcefield));
		}
		else if(fs instanceof Repeat){
			if(((Repeat)fs).isVisible()){
				if(s.getFieldTopLevel(fs.getID()) != null){
					generateSideDataEntry(out,s.getFieldTopLevel(fs.getID()), context);
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
	protected JSONObject generateSidebarSection(Structure s, UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		generateSideDataEntry(s, out,"termsUsed","${items.0.recordtype}.html",true,false,true, context);
		generateSideDataEntry(s, out,"relatedProcedures","${items.0.recordtype}.html",true,true,false, context);
		generateSideDataEntry(s, out,"relatedCataloging","${items.0.recordtype}.html",false,true,false, context);
		return out;
	}

	protected JSONObject uispec(Storage storage) throws UIException {
		this.storage = storage;
		this.tenantname = this.spec.getAdminData().getTenantName();
		if(this.tenantname == null || this.tenantname.equals("")){
			this.tenantname = "html";
		}
		UISpecRunContext context = new UISpecRunContext();
		try {
			JSONObject out=new JSONObject();
			Structure s = record.getStructure(this.structureview);
			if(this.structureview.equals("search")){
				out = generateRecordEditor(context, s.showMessageKey());
			}
			else{

				if(s.showListSection()){
					out.put(s.getListSectionName(),generateListSection(s,context));
				}
				if(s.showEditSection()){
					out.put(s.getEditSectionName(),generateRecordEditor(context, s.showMessageKey()));
				}
				if(s.showHierarchySection()){
					out.put(s.getHierarchySectionName(),generateHierarchySection(context, s.showMessageKey()));
				}
				if(s.showTitleBar()){
					out.put("titleBar",generateTitleSection(context));
				}
				
				if(s.showSideBar()){
					out.put("sidebar",generateSidebarSection(s, context));
				}
			}
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException",e);
		}
	}

	public void configure() throws ConfigException {}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		ctl = new CacheTermList(q.getCache());
		JSONObject out=uispec(q.getStorage());
		q.getUIRequest().sendJSONResponse(out);
	}

	public void configure(WebUI ui,Spec spec) {}
}