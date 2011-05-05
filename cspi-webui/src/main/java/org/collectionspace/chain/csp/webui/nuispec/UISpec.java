/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.nuispec;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
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
	protected JSONObject controlledCache;
	protected String structureview;

	public UISpec() {
		this.controlledCache = new JSONObject();
	}

	public UISpec(Record record, String structureview) {
		this.record=record;
		this.structureview = structureview;
		this.controlledCache = new JSONObject();
	}

	// XXX make common
	protected String veryplain(FieldSet f) {

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
	protected String plain(Field f) {
		if(f.getParent().isExpander() || f.getParent() instanceof Repeat|| f.isRepeatSubRecord()){
			return radio(f);
		}
		return veryplain(f);
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

	protected JSONObject linktext(Field f) throws JSONException  {
		JSONObject number=new JSONObject();
		number.put("linktext",f.getLinkText());
		number.put("target",f.getLinkTextTarget());
		return number;
			
	}

	protected String getSelectorAffix(FieldSet fs){
		return fs.getSelectorAffix();
	}
	protected String getSelector(FieldSet fs){
		return fs.getSelector();
	}
	
	protected Object generateOptionField(Field f) throws JSONException {
		// Dropdown entry
		JSONObject out=new JSONObject();
		if("radio".equals(f.getUIType())) {
			out.put("selection",radio(f));
		}
		else{
			out.put("selection",plain(f));
		}
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
		//currently only supports single select dropdowns and not multiselect
		if(dfault!=-1)
			out.put("default",dfault+"");
		out.put("optionlist",ids);
		out.put("optionnames",names);			
		return out;
	}
	// XXX factor
	protected Object generateDataEntryField(Field f) throws JSONException {
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
			return generateOptionField(f);
		}
		else if("enum".equals(f.getUIType())) {
			return generateENUMField(f);
		}
		else if(f.getUIType().startsWith("groupfield")) {
			return generateGroupField(f);
		}
		//ignore ui-type uploader
		return plain(f);	
	}

	protected Object generateGroupField(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		
		JSONArray decorators=new JSONArray();
		JSONObject options=new JSONObject();
		
		
		String parts[] = f.getUIType().split("/");
		JSONObject subexpander = new JSONObject();
		Record subitems = f.getRecord().getSpec().getRecordByServicesUrl(parts[1]);
		//subexpander.put("subexpander",subitems.getID());
		for(FieldSet fs2 : subitems.getAllFields("")) {

			generateDataEntry(subexpander,fs2, "");
		}
		
		
		
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.noexpand");
		expander.put("tree", subexpander);
		
		
		JSONObject protoTree = new JSONObject();
		protoTree.put("expander", expander);
		
		options.put("protoTree", protoTree);
		options.put("summaryElPath", "fields."+f.getID());
		JSONObject decorator=getDecorator("fluid",null,f.getUIFunc(),options);
		decorators.put(decorator);
		out.put("decorators",decorators);
		out.put("value",plain(f));
		
		
		return out;
	}

	protected Object generateENUMField(Field f) throws JSONException {
		//XXX cache the controlled list as they shouldn't be changing if they are hard coded into the uispec
		//XXX they shouldn't really be in the uispec but they are here until the UI and App decide how to communicate about them
		if(!controlledCache.has(f.getAutocompleteInstance().getID())){
			JSONArray getallnames = controlledLists(f.getAutocompleteInstance().getID());
			controlledCache.put(f.getAutocompleteInstance().getID(), getallnames);
		}

		JSONArray allnames = controlledCache.getJSONArray(f.getAutocompleteInstance().getID());
		JSONArray ids=new JSONArray();
		JSONArray names=new JSONArray();
		int dfault = -1;
		int spacer =0;
		if(f.hasEnumBlank()){
			ids.put("");
			names.put(f.enumBlankValue());
			spacer = 1;
		}
		
		for(int i=0;i<allnames.length();i++) {
			JSONObject namedata = allnames.getJSONObject(i);
			String name = namedata.getString("displayName");
			String shortId="";
			if(namedata.has("shortIdentifier") && !namedata.getString("shortIdentifier").equals("")){
				shortId = namedata.getString("shortIdentifier");
			}
			else{
				shortId = name.replaceAll("\\W","");					
			}
			//currently only supports single select dropdowns and not multiselect
			if(f.isEnumDefault(name)){
				dfault = i + spacer;
			}
			ids.put(shortId.toLowerCase());
			names.put(name);
		}
		// Dropdown entry pulled from service layer data
		JSONObject out=new JSONObject();
		out.put("selection",plain(f));

		if(dfault!=-1)
			out.put("default",dfault+"");
		out.put("optionlist",ids);
		out.put("optionnames",names);	
		return out;
	}
	protected JSONArray controlledLists(String vocabtype,Record vr, Integer limit) throws JSONException{
		JSONArray displayNames = new JSONArray();
		try {
		    // Get List
			int resultsize =1;
			int pagenum = 0;
			int pagesize = 200;
			if(limit !=0 && limit < pagesize){
				pagesize = limit;
			}
			while(resultsize >0){
				JSONObject restriction=new JSONObject();
				restriction.put("pageNum", pagenum);
				restriction.put("pageSize", pagesize);
				Instance n = vr.getInstance(vocabtype);
				
				String url = vr.getID()+"/"+n.getTitleRef();
				JSONObject data = storage.getPathsJSON(url,restriction);
				if(data.has("listItems")){
					String[] results = (String[]) data.get("listItems");
					/* Get a view of each */
					for(String result : results) {
						//change csid into displayName
						JSONObject namedata = getDisplayNameList(storage,vr.getID(),n.getTitleRef(),result);
						displayNames.put(namedata);
					}

					Integer total = data.getJSONObject("pagination").getInt("totalItems");
					pagesize = data.getJSONObject("pagination").getInt("pageSize");
					//Integer itemsInPage = data.getJSONObject("pagination").getInt("itemsInPage");
					pagenum = data.getJSONObject("pagination").getInt("pageNum");
					pagenum++;
					//are there more results
					if(total <= (pagesize * (pagenum))){
						break;
					}
					//have we got enough results?
					if(limit !=0 && limit <= (pagesize * (pagenum)) ){
						break;
					}
				}
				else{
					resultsize=0;
				}
			}
		} catch (ExistException e) {
			throw new JSONException("Exist exception");
		} catch (UnimplementedException e) {
			throw new JSONException("Unimplemented exception");
		} catch (UnderlyingStorageException e) {
			throw new JSONException("Underlying storage exception"+vocabtype + e);
		}
		return displayNames;
	}
	protected JSONArray controlledLists(String vocabtype) throws JSONException{
		Record vr = this.record.getSpec().getRecord("vocab");
		return controlledLists(vocabtype,vr,0);
	}

	private JSONObject getDisplayNameList(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//should be using cached results from the previous query.
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view", new JSONObject());
		return out;
	}

	
	
	protected JSONArray generateRepeatExpanderEntry(Repeat r, String affix) throws JSONException {
		JSONArray expanders = new JSONArray();
		JSONObject siblingexpander = new JSONObject();
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.renderer.repeat");
		expander.put("controlledBy", "fields."+r.getID());
		expander.put("pathAs", "row");
		expander.put("repeatID", getSelector(r));

		if(r.getChildren("").length>0){
			JSONObject tree = new JSONObject();
			for(FieldSet child : r.getChildren("")) {
				if(child.getUIType().equals("hierarchy")){

					expander.put("repeatID", getSelector(child)+":");
					generateHierarchyEntry(siblingexpander, child,  affix);
					expanders.put(siblingexpander.getJSONObject("expander"));
					if(child instanceof Field){
						String classes = "cs-hierarchy-equivalentContext";
						JSONObject decorator = getDecorator("addClass",classes,null,null);
						tree.put("value", generateDataEntryField((Field)child));
						tree.put("decorators", decorator);
					}
				}
				else{
					generateDataEntry(tree,child, affix);
				}
			}
			if(r.isConditionExpander()){
				expander.put("valueAs", "rowValue");
				JSONObject cexpander = new JSONObject();
				JSONObject texpander = new JSONObject();
				cexpander.put("type", "fluid.renderer.condition");
				JSONObject condpander = new JSONObject();
				condpander.put("funcName", "cspace.adminRoles.assertDisplay");
				condpander.put("args", "{rowValue}.display");

				cexpander.put("condition", condpander);
				cexpander.put("trueTree", tree);
				texpander.put("expander", cexpander);
				expander.put("tree", texpander);
			}
			else{
				expander.put("tree", tree);
			}
		}
		expanders.put(expander);
		return expanders;
	}
	
	protected JSONObject generateSelectionExpanderEntry(Field f, String affix) throws JSONException {
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.renderer.selection.inputs");
		expander.put("rowID", getSelector(f)+"-row:");
		expander.put("labelID", getSelector(f)+"-label");
		expander.put("inputID", getSelector(f)+"-input");
		expander.put("selectID", f.getID());

		JSONObject tree = new JSONObject();
		tree = (JSONObject)generateOptionField(f);
		expander.put("tree", tree);
		return expander;
	}
	
	private JSONObject generateNonExpanderEntry(Repeat r, String affix) throws JSONException {
		JSONObject out = new JSONObject();
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.renderer.noexpand");

		if(r.getChildren("").length>0){
			JSONObject tree = new JSONObject();
			for(FieldSet child : r.getChildren("")) {
				generateDataEntry(tree,child, affix);
			}
			expander.put("tree", tree);
		}
		out.put("expander", expander);
		return out;
	}
	
	protected JSONObject generateRepeatEntry(Repeat r, String affix) throws JSONException {

		JSONObject out = new JSONObject();
		if(r.isExpander()){
			JSONArray expanders= generateRepeatExpanderEntry(r,affix);
			out.put("expanders", expanders);
		}
		else{
			JSONArray decorators=new JSONArray();

			
			JSONObject options=new JSONObject();
			JSONObject preProtoTree=new JSONObject();
			for(FieldSet child : r.getChildren("")) {
				generateDataEntry(preProtoTree,child, affix);
			}
			JSONObject subexpander = new JSONObject();
			subexpander.put("type", "fluid.renderer.repeat");
            subexpander.put("pathAs", "row");
            subexpander.put( "controlledBy", "fields."+r.getID());
            subexpander.put("repeatID", "repeat:");
            subexpander.put("tree", preProtoTree);
			
			JSONObject supertree = new JSONObject();
			supertree.put("expander", subexpander);
			
			JSONObject expander = new JSONObject();
			expander.put("type", "fluid.noexpand");
			expander.put("tree", supertree);
			
			
			JSONObject protoTree = new JSONObject();
			protoTree.put("expander", expander);
			
			options.put("protoTree", protoTree);
			options.put("elPath", "fields."+r.getID());

			JSONObject decorator = getDecorator("fluid",null,"cspace.makeRepeatable",options);
			decorators.put(decorator);
			out.put("decorators",decorators);
		}
		return out;
	}
	
	
	protected JSONObject generateAutocomplete(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();

		
		JSONObject options=new JSONObject();
		String extra="";
		if(f.getRecord().isType("authority"))
			extra="vocabularies/";
		options.put("queryUrl","../../chain/"+extra+f.getRecord().getWebURL()+"/autocomplete/"+f.getID());
		options.put("vocabUrl","../../chain/"+extra+f.getRecord().getWebURL()+"/source-vocab/"+f.getID());

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

		JSONObject decorator=getDecorator("fluid",null,"cspace.autocomplete",options);
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f));
			}
		}
		decorators.put(decorator);
		out.put("decorators",decorators);
		if(f.isRefactored()){
			out.put("value", generateDataEntryField(f));
		}
		return out;
	}

	protected JSONObject generateChooser(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();

		JSONObject options=new JSONObject();
		JSONObject selectors=new JSONObject();
		selectors.put("numberField",getSelector(f));
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
		
		JSONObject decorator=getDecorator("fluid",null,"cspace.numberPatternChooser",options);
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

	protected JSONObject generateDate(Field f) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject decorator=getDecorator("fluid",null,"cspace.datePicker",null);
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

	protected JSONObject generateMessageKeys(String affix, JSONObject temp, Record r) throws JSONException {
		for(String st: r.getAllUISections()){
			if(st!=null){
				generateMessageKey(temp, r.getUILabelSelector(st),r.getUILabel(st));
			}
		}
		
		for(FieldSet fs : r.getAllFields("")) {
			if(fs.getID()!=null){
				generateMessageKey(temp, r.getUILabelSelector(fs.getID()), fs.getLabel());
			}
		}
		return temp;
		
	}
	
	private void generateMessageKey(JSONObject temp, String labelSelector, String label) throws JSONException {
		JSONObject msg = new JSONObject();
		msg.put("messagekey", label);
		temp.put(labelSelector, msg);
	}

	protected JSONObject generateRecordEditor(String affix, Boolean addMessages) throws JSONException{
		JSONObject out = generateDataEntrySection(affix);
		if(addMessages){
			out = generateMessageKeys(affix,out, record);
		}
		return out;
	}
	protected JSONObject generateDataEntrySection(String affix) throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet fs : record.getAllFields("")) {
			generateDataEntry(out,fs,affix);
		}
		return out;
	}

	
	protected JSONObject generateListSection(Structure s, String affix) throws JSONException {
		JSONObject out=new JSONObject();
		String id = s.getListSectionName();
		if(s.getField(id) != null){
			FieldSet fs = s.getField(id);
			generateDataEntry(out,fs, affix);
		}
		
		return out;
	}
	private void generateSubRecord(JSONObject out, FieldSet fs, String affix) throws JSONException {
		Record subrecord = fs.usesRecordId();
		for(FieldSet fs2 : subrecord.getAllFields("")) {
			if(fs.getParent() instanceof Repeat){
				fs2.setRepeatSubRecord(true);
			}
			generateDataEntry(out,fs2, affix);
			fs2.setRepeatSubRecord(false);
		}
		Structure s = subrecord.getStructure(this.structureview);
		if(s.showMessageKey()){
			out = generateMessageKeys(affix,out, subrecord);
		}
		
	}
	
	protected void generateDataEntry(JSONObject out,FieldSet fs, String affix) throws JSONException {

		if("uploader".equals(fs.getUIType())) {
			generateUploaderEntry(out,fs,affix);
		}
		if("hierarchy".equals(fs.getUIType())) {
			generateHierarchyEntry(out,fs,affix);
		}
		if(fs.usesRecord()){
			if(!getSelectorAffix(fs).equals("")){
				if(!affix.equals("")){
					affix = affix+"-"+getSelectorAffix(fs);
				}
				else{
					affix = "-"+getSelectorAffix(fs);
				}
			}
			generateSubRecord(out, fs, affix);
		}
		else{
			
			if(fs instanceof Field) {
				// Single field
				Field f=(Field)fs;
				if(f.isExpander()){
					generateExpanderDataEntry(out, affix, f);
				}
				//XXX when all uispecs have moved across we can delete most of this
				else if(!f.isRefactored()){
					generateFieldDataEntry_notrefactored(out, affix, f);
				}
				else{
					generateFieldDataEntry_refactored(out, affix, f);
				}
			} 
			else if(fs instanceof Group) {
				generateGroupDataEntry(out, fs, affix);
			} 
			else if(fs instanceof Repeat) {
				generateRepeatDataEntry(out, fs, affix);
			}
		}

	}

	protected void generateExpanderDataEntry(JSONObject out, String affix, Field f)
			throws JSONException {
		if("radio".equals(f.getUIType())){
			JSONObject obj = generateSelectionExpanderEntry(f,affix);
			out.put("expander",obj);
		}
	}

	protected void generateRepeatDataEntry(JSONObject out, FieldSet fs,
			String affix) throws JSONException {
		// Container
		Repeat r=(Repeat)fs;
		if(r.getXxxUiNoRepeat()) {
			FieldSet[] children=r.getChildren("");
			if(children.length!=0){
				generateDataEntry(out,children[0], affix);
			}
		} else {
			JSONObject row=new JSONObject();
			JSONArray children=new JSONArray();
			if(r.asSibling() && !r.hasServicesParent()){ // allow for row [{'','',''}]
				repeatSibling(out, affix, r, row, children);
			}
			else{
				repeatNonSibling(out, fs, affix, r);
			}
		}
	}

	protected void repeatNonSibling(JSONObject out, FieldSet fs, String affix,
			Repeat r) throws JSONException {
		JSONObject contents=generateRepeatEntry(r, affix);
		String selector = getSelector(r);
		//CSPACE-2619 scalar repeatables are different from group repeats
		if(r.getChildren("").length==1){
			FieldSet child = null;
			if(r.getChildren("")[0] instanceof Field){
				child = (Field)r.getChildren("")[0];
				selector = getSelector(child);
			}
			else if(r.getChildren("")[0] instanceof Group){
				child = (Group)r.getChildren("")[0];
				selector = getSelector(child);
			}
			//XXX CSPACE-2706 hack
			if(child.getUIType().equals("date")){
				selector = getSelector(r);
			}
		}
		if(fs.isExpander()){
			selector="expander";
			JSONArray expanders = contents.getJSONArray("expanders");
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

	protected void repeatSibling(JSONObject out, String affix, Repeat r,
			JSONObject row, JSONArray children) throws JSONException {
		JSONObject contents=new JSONObject();
		for(FieldSet child : r.getChildren("")) {
			generateDataEntry(contents,child, affix);
		}
		children.put(contents);
		row.put("children",children);
		out.put(getSelector(r),row);
	}

	protected void generateGroupDataEntry(JSONObject out, FieldSet fs,
			String affix) throws JSONException {
		Group g = (Group)fs;
		JSONObject contents=new JSONObject();
		for(FieldSet child : g.getChildren("")) {
			generateDataEntry(contents,child, affix);
		}
		out.put(getSelector(g),contents);
	}

	protected void generateUploaderEntry(JSONObject out, FieldSet f, String affix) throws JSONException{
		String condition =  "cspace.mediaUploader.assertBlob";

		JSONObject cond = new JSONObject();
		if(f instanceof Group){
			Group gp = (Group)f;
			String test = gp.usesRecordValidator();
			FieldSet tester = record.getField(test);
			if(tester instanceof Field){
				cond.put("args",plain((Field)tester));
			}
			cond.put("funcName", condition);
		}
		JSONObject ttree = new JSONObject();
		ttree.put(f.getSelector(),new JSONObject());
		JSONObject decorator = getDecorator("addClass","hidden",null,null);
		JSONObject decorators = new JSONObject();
		decorators.put("decorators", decorator);
		JSONObject ftree = new JSONObject();
		ftree.put(f.getSelector(),decorators);
		JSONObject cexpander = new JSONObject();
		cexpander.put("type", "fluid.renderer.condition");
		cexpander.put("condition", cond);
		cexpander.put("trueTree", ttree);
		cexpander.put("falseTree", ftree);

		
		out.put("expander",cexpander);
		
	}
	protected void generateHierarchyEntry(JSONObject out, FieldSet f, String affix) throws JSONException{
		String condition =  "cspace.hierarchy.assertEquivalentContexts";
		Record thisr = f.getRecord();
		JSONObject cond = new JSONObject();
		if(f instanceof Field){
			FieldSet fs = (FieldSet)f.getParent();
			JSONObject args = new JSONObject();
			args.put(fs.getID(), veryplain(fs));
			cond.put("args",args);
			cond.put("funcName", condition);
		}
		JSONObject ttree = new JSONObject();
		generateMessageKey(ttree, thisr.getUILabelSelector(f.getID()), f.getLabel());
		
		JSONObject decorator = getDecorator("addClass","hidden",null,null);
		JSONObject decorators = new JSONObject();
		decorators.put("decorators", decorator);
		JSONObject ftree = new JSONObject();
		ftree.put(thisr.getUILabelSelector(f.getID()),decorators);
		JSONObject cexpander = new JSONObject();
		cexpander.put("type", "fluid.renderer.condition");
		cexpander.put("condition", cond);
		cexpander.put("trueTree", ttree);
		cexpander.put("falseTree", ftree);

		
		out.put("expander",cexpander);
	}
	
	protected JSONObject getDecorator(String type, String className, String func, JSONObject options) throws JSONException{
		JSONObject decorator = new JSONObject();
		decorator.put("type",type);
		if(className != null){
			decorator.put("classes",className);
		}
		if(func != null){
			decorator.put("func",func);
		}
		if(options != null){
			decorator.put("options",options);
		}
		return decorator;
	}
	
	protected void generateFieldDataEntry_refactored(JSONObject out, String affix, Field f)
			throws JSONException {
		if(f.hasAutocompleteInstance()) {
			makeAuthorities(out, affix, f);
		}
		else if("chooser".equals(f.getUIType())) {
			out.put(getSelector(f)+affix,generateChooser(f));
		}
		else if("date".equals(f.getUIType())) {
			out.put(getSelector(f)+affix,generateDate(f));
		}
		else if("sidebar".equals(f.getUIType())) {
			//out.put(getSelector(f)+affix,generateSideBar(f));
		}
		else{
			out.put(getSelector(f)+affix,generateDataEntryField(f));	
		}
	}

	protected void makeAuthorities(JSONObject out, String affix, Field f)
			throws JSONException {
		if("enum".equals(f.getUIType())){
			out.put(getSelector(f)+affix,generateDataEntryField(f));
		}
		else{
			out.put(getSelector(f)+affix,generateAutocomplete(f));
		}
	}

	protected void generateFieldDataEntry_notrefactored(JSONObject out, String affix, Field f)
			throws JSONException {
		// Single field
		out.put(getSelector(f)+affix,generateDataEntryField(f));	
		
		if(f.hasAutocompleteInstance()) {
			makeAuthorities(out, affix, f);
		}
		if("chooser".equals(f.getUIType())) {
			out.put(f.getContainerSelector()+affix,generateChooser(f));
		}
		if("date".equals(f.getUIType())) {
			out.put(f.getContainerSelector()+affix,generateDate(f));
		}
	}

	private void generateTitleSectionEntry(JSONObject out,FieldSet fs, String affix) throws JSONException {
		if(fs instanceof Field) {
			Field f=(Field)fs;
			if(!f.isInTitle())
				return;
			out.put(f.getTitleSelector()+affix,veryplain(f));
		} else if(fs instanceof Repeat) {
			for(FieldSet child : ((Repeat)fs).getChildren(""))
				generateTitleSectionEntry(out,child, affix);
		}
	}

	protected JSONObject generateTitleSection(String affix) throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet f : record.getAllFields("")) {
			generateTitleSectionEntry(out,f, affix);
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
	private JSONObject generateSideDataEntry(JSONObject out, FieldSet fs, String affix) throws JSONException {
		Repeat f=(Repeat)fs;
		JSONObject listrow=new JSONObject();
		generateDataEntry(listrow,fs, affix);
		out.put(f.getID(),listrow);
		return out;
	}
	
	private JSONObject generateSideDataEntry(Structure s, JSONObject out, String fieldName,String url_frag,boolean include_type,boolean include_summary,boolean include_sourcefield, String affix )throws JSONException {
		FieldSet fs = s.getSideBarItems(fieldName);
		if(fs == null){
			//XXX default to show if not specified
			out.put(fieldName,generateSidebarPart(url_frag,include_type,include_summary,include_sourcefield));
		}
		else if(fs instanceof Repeat){
			if(((Repeat)fs).isVisible()){
				if(s.getField(fs.getID()) != null){
					generateSideDataEntry(out,s.getField(fs.getID()), affix);
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
	protected JSONObject generateSidebarSection(Structure s, String affix) throws JSONException {
		JSONObject out=new JSONObject();
		generateSideDataEntry(s, out,"termsUsed","${items.0.recordtype}.html",true,false,true, affix);
		generateSideDataEntry(s, out,"relatedProcedures","${items.0.recordtype}.html",true,true,false, affix);
		generateSideDataEntry(s, out,"relatedCataloging","${items.0.recordtype}.html",false,true,false, affix);
		return out;
	}

	protected JSONObject uispec(Storage storage) throws UIException {
		this.storage = storage;
		String affix = "";
		try {
			JSONObject out=new JSONObject();
			Structure s = record.getStructure(this.structureview);
			if(s.showListSection()){
				out.put(s.getListSectionName(),generateListSection(s,affix));
			}
			if(s.showEditSection()){
				out.put(s.getEditSectionName(),generateRecordEditor(affix, s.showMessageKey()));
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
		JSONObject out=uispec(q.getStorage());
		q.getUIRequest().sendJSONResponse(out);
	}

	public void configure(WebUI ui,Spec spec) {}
}
