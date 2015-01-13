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
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UISpec extends SchemaStructure implements WebMethod {
	
	private static final Logger log=LoggerFactory.getLogger(UISpec.class);
	protected CacheTermList ctl;
	protected Record record;
	protected Storage storage;

	public UISpec(Spec spec, String sview, String stype) {
		super(spec, sview, stype);
	}
	public UISpec(Record r, String sview){
		super(r, sview);
		this.record = r;
	}

	@Override
	public void configure(WebUI ui, Spec spec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		ctl = new CacheTermList(q.getCache());
		JSONObject out=uispec(q.getStorage());

		UIRequest uir = q.getUIRequest();
		uir.sendJSONResponse(out);

		int cacheMaxAgeSeconds = spec.getAdminData().getUiSpecSchemaCacheAge();
		if(cacheMaxAgeSeconds > 0) {
			uir.setCacheMaxAgeSeconds(cacheMaxAgeSeconds);
		}
	}
	
	/**
	 * create the UISpec and return the JSONObject 
	 * @param storage
	 * @return
	 * @throws UIException
	 */
	protected JSONObject uispec(Storage storage) throws UIException {
		this.storage = storage;
		UISpecRunContext context = new UISpecRunContext();
		try {
			JSONObject out=new JSONObject();
			Structure s = record.getStructure(this.structureview);
			if(this.structureview.equals("search")){
				out = generateUISpecRecordEditor(context, s.showMessageKey());
			}
			else{

				if(s.showListSection()){ /* used in termlist, reports, all */
					out.put(s.getListSectionName(),generateUISpecListSection(s,context));
				}
				if(s.showEditSection()){
					out.put(s.getEditSectionName(),generateUISpecRecordEditor(context, s.showMessageKey()));
				}
				if(s.showHierarchySection()){
					out.put(s.getHierarchySectionName(),generateUISpecHierarchySection(context, s.showMessageKey()));
				}
			}
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException",e);
		}
	}
	
	/**
	 * Return all the message keys and the run off and get the data
	 * @param context
	 * @param addMessages
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject generateUISpecRecordEditor(UISpecRunContext context, Boolean addMessages) throws JSONException{
		JSONObject out = generateDataEntrySection(context, this.record, this.spectype);
		if(addMessages){
			makeAllRecordMessageKey(context, out, this.record);
		}
		return out;
	}
	
	protected JSONObject generateUISpecListSection(Structure s, UISpecRunContext context) throws JSONException {
		JSONObject out=new JSONObject();
		String id = s.getListSectionName();
		if(s.getFieldTopLevel(id) != null){
			FieldSet fs = s.getFieldTopLevel(id);
			whatIsThisFieldSet(out,fs, context);
		}
		return out;
	}
	
	/**
	 * Create hierarchy section for the uispec
	 * @param affix
	 * @param addMessages
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject generateUISpecHierarchySection(UISpecRunContext affix, Boolean addMessages) throws JSONException{
		JSONObject out = new JSONObject();
		Record subf = this.spec.getRecord("hierarchy");

		String extra = "";
		if(this.record.getRecord().isType("authority"))
			extra ="vocabularies/";
		affix.setUIRecordUrl(extra + record.getWebURL());

		makeASubRecord(subf, out,  false,  affix, out);
		
		if(addMessages){
			makeAllRecordMessageKey(affix,out, subf);
		}
		return out;
	}
	/**
	 * display the autocomplete markup for the uispec
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualAutocomplete(FieldSet fs,UISpecRunContext context, JSONArray decorators) throws JSONException {
		JSONObject out=new JSONObject();
		Field f = (Field)fs;

		
		JSONObject options=new JSONObject();
		String extra="";
		if(f.getRecord().isType("authority")) {
			extra="vocabularies/";
                }
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
		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
		if(f.isRefactored()){
			out.put("value", actualFieldEntry(f,context));
		}
		return out;
	}

	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param context
	 * @param f
	 * @throws JSONException
	 */
	protected void actualAuthorities(JSONObject out, FieldSet fs, UISpecRunContext context)
			throws JSONException {
		String fieldSelector = getSelector(fs,context);
		JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
		if("enum".equals(fs.getUIType())){
			out.put(fieldSelector,actualFieldEntry(fs,context));
		}
		else{
			out.put(fieldSelector,actualAutocomplete(fs,context, decorators));
		}
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @param useContainer
	 * @throws JSONException 
	 */
        @Override
	protected void actualChooserField(JSONObject out, FieldSet fs, UISpecRunContext context, Boolean useContainer) throws JSONException{
		if(useContainer){
			String containerSelector = getContainerSelector(fs,context);
			JSONArray decorators = getExistingDecoratorsArray(out, containerSelector);
			out.put(containerSelector,actualChooser(fs,context, decorators));
		} else {
			String fieldSelector = getSelector(fs,context);
			JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
			out.put(fieldSelector,actualChooser(fs,context, decorators));
		}
	}

        @Override
	protected Object actualBooleanField(Field f,UISpecRunContext context) throws JSONException {
		return displayAsplain(f,context);
	}
	
	/**
	 * The UISpec mark up for Chooser elements
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualChooser(FieldSet fs,UISpecRunContext context, JSONArray decorators) throws JSONException {
		Field f = (Field)fs;
		JSONObject out=new JSONObject();

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
		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
		if(f.isRefactored()){
			out.put("valuebinding", actualFieldEntry(f,context));
		}
		return out;
	}
        
	/**
	 * This is a bit of JSON needed by the UI so they display dates in the UIspec
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualDate(FieldSet fs,UISpecRunContext context, JSONArray decorators) throws JSONException {
		JSONObject out=new JSONObject();
		Field f = (Field)fs;
		JSONObject decorator=getDecorator("fluid",null,"cspace.datePicker",null,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
		out.put("value", actualFieldEntry(f,context));
		return out;
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualDateField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String fieldSelector = getSelector(fs,context);
		JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
		out.put(fieldSelector,actualDate(fs,context, decorators));
	}
	/**
	 * Generate UISpec JSON needed by the UI to show a rich text editor
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualRichText(FieldSet fs,UISpecRunContext context, JSONArray decorators) throws JSONException {
		JSONObject out=new JSONObject();
		Field f = (Field)fs;
		JSONObject decorator=getDecorator("fluid",null,"cspace.richTextEditor",null,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
		out.put("value", actualFieldEntry(f,context));
		return out;
	}
	/**
	 * Generate UISpec JSON needed by the UI to show a rich text editor
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualRichTextField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String fieldSelector = getSelector(fs,context);
		JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
		out.put(fieldSelector,actualRichText(fs,context, decorators));
	}

	/**
	 * This is a bit of JSON needed by the UI so they can validate data types like integer,float etc
	 * CSPACE-4330
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject generateDataTypeValidator(FieldSet fs, UISpecRunContext context, JSONArray decorators) throws JSONException {
		Field f = (Field)fs;
		JSONObject out=new JSONObject();
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
		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
		out.put("value", actualFieldEntry(f,context));
		return out;
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualValidatedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String fieldSelector = getSelector(fs,context);
		JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
		out.put(fieldSelector,generateDataTypeValidator(fs,context, decorators));
	}
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualExternalURLField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String fieldSelector = getSelector(fs,context);
		JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
		out.put(fieldSelector,actualExternalURL(fs,context, decorators));
	}
	/**
	 * Generate the JSON needed by the UI to create a computed field.
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject generateComputedField(FieldSet fs, UISpecRunContext context) throws JSONException {
		Field f = (Field)fs;
		JSONObject out=new JSONObject();
		JSONArray decorators=new JSONArray();
		JSONObject options = new JSONObject();
		String type = f.getDataType();
		if(type.equals("")){type = "string";}
		options.put("type",type);
		options.put("label",f.getLabel());
		options.put("readOnly", f.isReadOnly());
		
		if (StringUtils.isNotEmpty(f.getUIFunc())) {
			options.put("func",f.getUIFunc());
		}
		
		if (StringUtils.isNotEmpty(f.getUIArgs())) {
			options.put("args",f.getUIArgs().split(","));
		}
		
		String root = "";
		String elPath = "";
		
		// Determine the root and elPath. This is basically a reworking of displayAsplain(Field,UISpecRunContext), which
		// really needs to be refactored so it doesn't have to be repeated here.
		if(f.getParent().isExpander() ||  f.isRepeatSubRecord()){
			root = "{row}";
			String[] paths = f.getIDPath();
			elPath = paths[paths.length - 1];
		}
		else if(f.getParent() instanceof Repeat){
			Repeat rp = (Repeat)f.getParent();//remove bogus repeats used in search
			if(!rp.getSearchType().equals("repeator") && !this.spectype.equals("search")){
				root = "{row}";
				String[] paths = f.getIDPath();
				elPath = paths[paths.length - 1];
			}
			else if(this.spectype.equals("search")){
				root = "{row}";
				String[] paths = f.getIDPath();
				elPath = paths[paths.length - 1];
			}
			else {
				elPath = displayAsveryplainWithoutEnclosure(f,context);
			}
		}
		else {
			elPath = displayAsveryplainWithoutEnclosure(f,context);
		}

		options.put("root",root);
		options.put("elPath",elPath);
		
		JSONObject decorator=getDecorator("fluid",null,"cspace.computedField",options,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		decorators.put(decorator);
		out.put("decorators",decorators);
		out.put("value", actualFieldEntry(f,context));
		return out;
	}
	/**
	 * Write the JSON structure for a computed field.
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualComputedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		out.put(getSelector(fs,context),generateComputedField(fs,context));
	}
	
	/**
	 * This is a bit of JSON needed by the UI so they display fields decorated
         * with external URL-handling behavior in the UIspec
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualExternalURL(FieldSet fs,UISpecRunContext context, JSONArray decorators) throws JSONException {
		JSONObject out=new JSONObject();
		Field f = (Field)fs;
		JSONObject decorator=getDecorator("fluid",null,"cspace.externalURL",null,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
		out.put("value", actualFieldEntry(f,context));
		return out;
	}
        
        /**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualDeURNedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String fieldSelector = getSelector(fs,context);
		JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
		out.put(fieldSelector,actualDeURN(fs,context, decorators));
	}
	
	/**
	 * This is a bit of JSON needed by the UI so they display fields decorated
         * with de-URN behavior (extracting display names from URNs) in the UIspec
         * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualDeURN(FieldSet fs,UISpecRunContext context, JSONArray decorators) throws JSONException {
		JSONObject out=new JSONObject();
		Field f = (Field)fs;
		JSONObject decorator=getDecorator("fluid",null,"cspace.util.urnToStringFieldConverter",null,f.isReadOnly());
		if(!f.isRefactored()){
			if(f.hasContainer()){
				decorator.put("container",getSelector(f,context));
			}
		}
		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
		out.put("value", actualFieldEntry(f,context));
		return out;
	}

	/**
	 * treat just the same as a normal field - only need the distinction in UISchema
	 */
        @Override
	protected void actualSelfRendererField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		actualField(out, fs, context);
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		// When generating the actual field entry for a structured date in the context
		// of range search, we have to output a simple date spec
		if(this.spectype.equals("search") && fs.getSearchType().equals("range")
				&& isAStructureDate(fs)){
			actualDateField(out, fs, context);
		} else {
			String fieldSelector = getSelector(fs,context);
			JSONArray decorators = getExistingDecoratorsArray(out, fieldSelector);
			out.put(fieldSelector,actualFieldEntry(fs,context, decorators));
		}
	}
	
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param fs
	 * @param out
	 * @param context
         * @param contents 
         * @throws JSONException 
	 */
        @Override
	protected void actualGroupEntry(FieldSet fs, JSONObject out, UISpecRunContext context, JSONObject contents) throws JSONException{
		out.put(getSelector(fs,context),contents);
	}
	/**
	 * Create hierarchy specific output for uispec
	 * @param out
	 * @param f
	 * @param context
	 * @throws JSONException
	 */
	private void actualHierarchyEntry(JSONObject out, FieldSet f, UISpecRunContext context) throws JSONException{
		String condition =  "cspace.hierarchy.assertEquivalentContexts";
		Record thisr = f.getRecord();
		JSONObject cond = new JSONObject();
		if(f instanceof Field){
			FieldSet fs = (FieldSet)f.getParent();
			JSONObject args = new JSONObject();
			args.put(fs.getID(), displayAsveryplain(fs,context));
			cond.put("args",args);
			cond.put("funcName", condition);
		}
		JSONObject ttree = new JSONObject();
		actualMessageKey(ttree, thisr.getUILabelSelector(f.getID()), f.getLabel());

		// This looks wrong - a single decorator is being set as the value of the
		// plural decorators key, which usually holds an array of decorators.
		// However, it turns out that UI is forgiving, and handles either Object or Array
		JSONObject decorator = getDecorator("addClass","hidden",null,null,f.isReadOnly());
		JSONObject decorators = new JSONObject();
		decorators.put(DECORATORS_KEY, decorator);
		JSONObject ftree = new JSONObject();
		ftree.put(thisr.getUILabelSelector(f.getID()),decorators);
		
		
		JSONObject cexpander = new JSONObject();
		cexpander.put("type", "fluid.renderer.condition");
		cexpander.put("condition", cond);
		cexpander.put("trueTree", ttree);
		cexpander.put("falseTree", ftree);

		actualTrueTreeSub(out, cexpander);
		
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param temp
	 * @param labelSelector
	 * @param label
	 * @throws JSONException
	 */
        @Override
	protected void actualMessageKey(JSONObject temp, String labelSelector, String label) throws JSONException {
		JSONObject msg = new JSONObject();
		msg.put("messagekey", label);
		temp.put(labelSelector, msg);
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualFieldExpanderEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		if("radio".equals(fs.getUIType())){
			JSONObject expander = new JSONObject();
			expander.put("type", "fluid.renderer.selection.inputs");
			expander.put("rowID", getSelector(fs,context)+"-row:");
			expander.put("labelID", getSelector(fs,context)+"-label");
			expander.put("inputID", getSelector(fs,context)+"-input");
			expander.put("selectID", fs.getID());

			JSONObject tree = new JSONObject();
			Field f = (Field)fs;
			tree = (JSONObject)actualOptionField(f,context);
			expander.put("tree", tree);
			out.put("expander",expander);
		}
	}	


	
	/**
	 * output the ENUM markup for the UISpec
	 * @param f
	 * @param context
         * @param decorators 
         * @return
	 * @throws JSONException
	 */
        @Override
	protected Object actualENUMField(Field f,UISpecRunContext context, JSONArray decorators) throws JSONException {
		JSONObject out = new JSONObject();
		JSONObject options = new JSONObject();
		if(f.getParent() instanceof Repeat && (isATrueRepeat((Repeat)f.getParent()))){
			options.put("elPath", f.getID());
			options.put("root", "{row}");
		}
		else{
			options.put("elPath", displayAsveryplainWithoutEnclosure(f,context));
		}
		options.put("termListType", f.getID());
		JSONObject decorator = getDecorator("fluid", null, "cspace.termList", options,f.isReadOnly());

		if(decorators==null) {
			decorators=new JSONArray();
		}
		decorators.put(decorator);
		out.put(DECORATORS_KEY,decorators);
	
		return out;
	}
	/**
	 * output the option field markup for UISpecs
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
        @Override
	protected Object actualOptionField(Field f,UISpecRunContext context) throws JSONException {
		// Dropdown entry
		JSONObject out=new JSONObject();
		if("radio".equals(f.getUIType())) {
			out.put("selection",displayAsradio(f));
		}
		else{
			out.put("selection",displayAsplain(f,context));
		}
		JSONArray ids=new JSONArray();
		JSONArray names=new JSONArray();
		boolean hasdefault = false;
		String dfault=null;
		for(Option opt : f.getAllOptions()) {
			if(opt.getID().equals("")){ hasdefault = true;}
			ids.put(opt.getID());
			names.put(opt.getName());
			if(opt.isDefault()) {
				dfault=opt.getID();
                        }
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
			if(dfault!=null) {
				out.put("default",dfault+"");
                        }
			out.put("optionlist",ids);
			out.put("optionnames",names);	
		}
		return out;
	}

        @Override
	protected void actualAddDecorator(FieldSet fs, JSONObject out, UISpecRunContext sub,
					JSONObject options) throws JSONException{
		sub.setUIAffix(fs.getID()+"-");
		JSONObject decorator=getDecorator("fluid",null,fs.getUIFunc(),options,fs.isReadOnly());
		JSONArray decorators = getExistingDecoratorsArray(out);
		if(decorators!=null) {
			decorators.put(decorator);
		} else {
			decorators=new JSONArray();
			decorators.put(decorator);
			out.put(DECORATORS_KEY,decorators);
		}
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param tree
	 * @throws JSONException
	 */
        @Override
	protected void actualSelfRenderer(JSONObject out, JSONObject tree) throws JSONException{
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.noexpand");
		expander.put("tree", tree);
	
		JSONObject protoTree = new JSONObject();
		protoTree.put("expander", expander);
		out.put("protoTree", protoTree);
	}
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param fs
	 * @param out
	 * @param sub
	 * @param subitems
	 * @throws JSONException
	 */
        @Override
	protected void actualOtherGroup(FieldSet fs, JSONObject out, UISpecRunContext sub,  Record subitems) throws JSONException{
		out.put("value",displayAsveryplain("fields."+fs.getPrimaryKey()));
		for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {		
			whatIsThisFieldSet(out,fs2, sub);
		}
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param fs
	 * @param out
	 * @param sub
	 * @param subexpander
	 * @param subitems
	 * @param options
	 * @throws JSONException 
	 */
        @Override
	protected void actualStructuredDate(FieldSet fs, JSONObject out, UISpecRunContext sub, JSONObject subexpander, Record subitems, JSONObject options) throws JSONException{
		out.put("value",displayAsveryplain("fields."+fs.getPrimaryKey()));
		Boolean truerepeat = false;
		FieldParent fsp = fs.getParent();
		if(fsp instanceof Repeat && !(fsp instanceof Group)){
			Repeat rp = (Repeat)fsp;//remove bogus repeats used in search
			if(isATrueRepeat(rp)){
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
					out.put("value",displayAsveryplain("{row}."+prefix+fs.getPrimaryKey()));
				}
				else if(fs instanceof Repeat){
					options.put("elPath", fs.getPrimaryKey());
					options.put("root", "{row}");
					out.put("value",displayAsveryplain("{row}."+fs.getPrimaryKey()));
				}
			}
		}
		if(!truerepeat){
			options.put("elPath", "fields."+fs.getPrimaryKey());
			for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {	
				if(!fs2.getSearchType().equals("false") || !this.spectype.equals("search")){ //only hide if this is a search uispec - need to extend to all uispec stuff
					subexpander.put(getSelector(fs2,sub), displayAsveryplainWithoutEnclosure(fs2,sub));
				}
			}
		}
		options.put("elPaths", subexpander);
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualTrueTreeEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{

		JSONObject tout = new JSONObject();
		if(isRefactored(fs)){
			actualFieldRefactored(tout,fs, context);
		}
		else{
			actualFieldNotRefactored(tout, fs, context);
		}

		JSONObject cexpander = new JSONObject();
		cexpander.put("trueTree", tout);

		actualTrueTreeSub(out, cexpander);
	}
	protected void actualTrueTreeSub(JSONObject out, JSONObject cexpander)
			throws JSONException {
		if(!out.has("expander")){
			out.put("expander",cexpander);
		}
		else{
			JSONObject exp = out.getJSONObject("expander");
			Iterator<?> rit= cexpander.keys();
			while(rit.hasNext()) {
				String key=(String)rit.next();
				if(exp.has(key)){
					//can only amalgamte if they are objs
					if(exp.get(key) instanceof JSONObject){
						JSONObject merged = exp.getJSONObject(key);
						if(cexpander.getJSONObject(key).length()!=0){
							for(String keyd : JSONObject.getNames(cexpander.getJSONObject(key)))
							{
								merged.put(keyd, cexpander.getJSONObject(key).get(keyd));
							}
						}
						exp.put(key,merged);
					}
				}
				else{
					exp.put(key, cexpander.get(key));
				}
			}
			out.put("expander",exp);
		}
	}
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param r
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualRepeatExpanderEntry(JSONObject out, Repeat r, UISpecRunContext context) throws JSONException{
		JSONArray expanders = new JSONArray();
		JSONObject siblingexpander = new JSONObject();
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.renderer.repeat");
		expander.put("controlledBy", displayAsveryplainWithoutEnclosure(r,context));//"fields."+r.getID());
		expander.put("pathAs", "row");
		expander.put("repeatID", getSelector(r,context)+":");

		if(r.getChildren("").length>0){
			JSONObject tree = new JSONObject();
			for(FieldSet child : r.getChildren("")) {
				if(!this.spectype.equals("search") || (this.spectype.equals("search") && !child.getSearchType().equals(""))){

					if(child.getUIType().equals("hierarchy")){

						expander.put("repeatID", getSelector(child,context)+":");
						actualHierarchyEntry(siblingexpander, child,  context);
						expanders.put(siblingexpander.getJSONObject("expander"));
						if(child instanceof Field){
							String classes = getDecoratorSelector(child,context);
							JSONObject decorator = getDecorator("addClass",classes,null,null,child.isReadOnly());
							tree.put("value", actualFieldEntry((Field)child,context));
							JSONArray decorators = new JSONArray();
							decorators.put(decorator);
							decorator=getDecorator("fluid",null,"cspace.externalURL",null,child.isReadOnly());
							decorators.put(decorator);
							tree.put(DECORATORS_KEY, decorators);
						}
					}
					else if(child.getUIType().equals("decorated")){

						expander.put("repeatID", getSelector(child,context)+":");
						if(child instanceof Field){
							String classes = getDecoratorSelector(child,context);
							JSONObject decorator = getDecorator("addClass",classes,null,null,child.isReadOnly());
							tree.put("value", actualFieldEntry((Field)child,context));
							tree.put(DECORATORS_KEY, decorator);
						}
					}
					else{
						whatIsThisFieldSet(tree,child, context);
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

				actualTrueTreeSub(texpander, cexpander);
				expander.put("tree", texpander);
			}
			else{
				expander.put("tree", tree);
			}
		}
		expanders.put(expander);
		makeExpander(out, expanders);
	}
	/**
	 * Overwrite with the output you need for the thing you are doing.
	 * @param out
	 * @param r
         * @param context
         * @param preProtoTree
         * @throws JSONException
	 */
    @Override
	protected void actualRepeatNonSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context, JSONObject preProtoTree) throws JSONException{

		JSONArray decorators=new JSONArray();
		JSONObject content=new JSONObject();
		JSONObject options=new JSONObject();
		JSONObject expander = new JSONObject();
		expander.put("type", "fluid.noexpand");
		expander.put("tree", preProtoTree);
		
		
		JSONObject repeatTree = new JSONObject();
		repeatTree.put("expander", expander);
		if(r.getParent() instanceof Record){
			options.put("elPath",displayAsveryplainWithoutEnclosure(r,context));
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
		content.put(DECORATORS_KEY,decorators);

		String selector = getSelector(r,context);
		//CSPACE-2619 scalar repeatables are different from group repeats
		if(r.getChildren("").length==1){
			FieldSet child = getFirstChild(r);
			if(child instanceof Field || child instanceof Group){
				selector = getSelector(child,context);
			}
		}
		if(r.isExpander()){
			JSONArray expanders = out.getJSONArray("expander");
			makeExpander(out, expanders);
		}
		else{
			out.put(selector,content);
		}
	}
	/**
	 * Overwrite with the output you need for the thing you are doing.
	 * @param out
	 * @param r
	 * @param context
	 * @param children
	 * @throws JSONException 
	 */
        @Override
	protected void actualRepeatSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context, JSONArray children) throws JSONException{
		JSONObject row=new JSONObject();
		row.put("children",children);
		out.put(getSelector(r,context),row);
	}
	/**
	 * Overwrite with the output you need for the thing you are doing.
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
        @Override
	protected void actualUploaderEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		String condition =  "cspace.mediaUploader.assertBlob";

		JSONObject cond = new JSONObject();
		if(fs instanceof Group){
			Group gp = (Group)fs;
			String test = gp.usesRecordValidator();
			FieldSet tester = record.getFieldTopLevel(test);
			if(tester instanceof Field){
				cond.put("args", displayAsplain((Field)tester,context));
			}
			cond.put("funcName", condition);
		}
		JSONObject ttree = new JSONObject();
		ttree.put(getSelector(fs,context),new JSONObject());
		JSONObject decorator = getDecorator("addClass","hidden",null,null,fs.isReadOnly());
		JSONObject decorators = new JSONObject();
		decorators.put(DECORATORS_KEY, decorator);
		JSONObject ftree = new JSONObject();
		ftree.put(getSelector(fs,context),decorators);
		JSONObject cexpander = new JSONObject();
		cexpander.put("type", "fluid.renderer.condition");
		cexpander.put("condition", cond);
		cexpander.put("trueTree", ttree);
		cexpander.put("falseTree", ftree);
		actualTrueTreeSub(out,cexpander);
	}

	// XXX make common
        @Override
	protected Object displayAsplain(Field f,UISpecRunContext context) {
		if(f.getParent().isExpander() ||  f.isRepeatSubRecord()){
			return displayAsradio(f);
		}
		if(f.getParent() instanceof Repeat){
			Repeat rp = (Repeat)f.getParent();//remove bogus repeats used in search
			if(!rp.getSearchType().equals("repeator") && !this.spectype.equals("search")){
				return displayAsradio(f);
			}
			if(this.spectype.equals("search")){
				return displayAsradio(f);
			}
		}
		return displayAsveryplain(f,context);
	}
	// XXX make common
	protected String displayAsradio(Field f) {
		List<String> path=new ArrayList<String>();
		String pad="{row}";
		path.add(pad);
		
		String[] paths = f.getIDPath();
		path.add(paths[paths.length - 1]);
		return "${"+StringUtils.join(path,'.')+"}";		
	}
	protected String displayAsveryplain(FieldSet f,UISpecRunContext context) {
		return "${"+displayAsveryplainWithoutEnclosure(f,context)+"}";		
	}
	protected String displayAsveryplain(String f) {
		return "${"+f+"}";		
	}
	protected String displayAsveryplainWithoutEnclosure(FieldSet f,UISpecRunContext context) {

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
	/**
	 * ${items.0.name}
	 * @param f
         * @return
         * @throws JSONException  
	 */
        @Override
	protected Object displayAsplainlist(Field f) throws JSONException {
		List<String> path=new ArrayList<String>();
		String name="items";
		path.add(name);
			String pad="0";
			path.add(pad);
			path.add(f.getID());
		
		return "${"+StringUtils.join(path,'.')+"}";		
	}
	/**
	 * should be moved into fs.getIDPath itself, but refactoring would take too long for 1.7 -- dan
	 * @param fs
	 * @param context
	 * @return
	 */
	private String[] getFullIDPath(FieldSet fs,UISpecRunContext context) {
		List<String> out = new ArrayList<String>();
		out.addAll(Arrays.asList(context.getUIPrefix()));
		out.addAll(Arrays.asList(fs.getIDPath()));		
		return out.toArray(new String[0]);
	}
	

	/**
	 * Get the container Selector
	 * @param fs
	 * @param context
	 * @return
	 */
	protected String getContainerSelector(FieldSet fs,  UISpecRunContext context){
		return makeSelector(fs.getPreContainerSelector(),context,fs.getContainerSelector());
	}
	/**
	 * Get the generic selector
	 * @param fs
	 * @param context
	 * @return
	 */
	protected String getSelector(FieldSet fs, UISpecRunContext context){
		return makeSelector(fs.getPreSelector(),context,fs.getSelector());
	}
	
	/**
	 * add the UI decorator element
	 * @param type
	 * @param className
	 * @param func
	 * @param options
	 * @param readOnly
	 * @return
	 * @throws JSONException
	 */
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
	/**
	 * Get the decorator selector
	 * @param fs
	 * @param context
	 * @return
	 */
	protected String getDecoratorSelector(FieldSet fs, UISpecRunContext context){
		return makeSelector(fs.getDecoratorSelector(),context,fs.getSelector());
	}
	
	/**
	 * logic to put together the selectors needed
	 * @param pre
	 * @param context
	 * @param post
	 * @return
	 */
	private String makeSelector(String pre, UISpecRunContext context, String post){
		List<String> affixes = Arrays.asList(context.getUIAffix());
		String selector = pre;
		for(String part : affixes) {
                    // FIXME: NetBeans flagged this String comparison for
                    // using != and ==, rather than String.equals().
                    //
                    // This is one of many places it might be appropriate to use
                    // org.collectionspace.services.common.api.Tools.notBlank()
                    // - ADR 2012-12-18
			if(part != null && part !=""){
				selector += part;
			}
		}
		selector += post;
		
		return selector;
	}
	/**
	 * Abstraction to remove model and markup
	 * @param out
	 * @param expanders
	 * @throws JSONException
	 */
	protected void makeExpander(JSONObject out, JSONArray expanders) throws JSONException{
		String selector="expander";
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

	/**
	 * 
	 * @param fs
	 * @param out
	 * @param subexpander
	 * @param options
	 * @param subitems
	 * @param sub
         * @param mainContext 
         * @throws JSONException
	 */
        @Override
	protected void makeAStructureDate(FieldSet fs, JSONObject out,
			JSONObject subexpander, JSONObject options, Record subitems,
			UISpecRunContext sub, UISpecRunContext mainContext) throws JSONException {
		// We map structured-date range searches onto a range search on the scalar date fields. 
		// We continue to refer to the structured date field so that the search builder can
		// do the right thing, but here in the search schema, we act as though the structured
		// date is really a scalar date, so the UI code will build the right UI
		// Note that at this point, the fs is actually a synthetic copy of the original
		// with the id changed to append "Start" or "End"
		if( (this.spectype.equals("search") && fs.getSearchType().equals("range"))){
			actualDateField(out, fs, mainContext);
		} else {
			actualStructuredDate(fs, out, sub, subexpander, subitems, options);
			actualAddDecorator(fs, out, sub, options);
		}
	}
	/**
	 * 
	 * @param fs
	 * @param out
	 * @param subexpander
	 * @param options
	 * @param subitems
	 * @param sub
	 * @throws JSONException
	 */
        @Override
	protected void makeAOtherGroup(FieldSet fs, JSONObject out,
			JSONObject subexpander, JSONObject options, Record subitems,
			UISpecRunContext sub) throws JSONException {
		actualOtherGroup(fs, subexpander, sub, subitems);
		actualSelfRenderer(options, subexpander);
		actualAddDecorator(fs, out, sub, options);
	}
	/**
	 * 
	 * @param fs
	 * @param context
	 * @param out
	 * @param subexpander
	 * @param options
	 * @param subitems
	 * @param sub
	 * @throws JSONException
	 */
        @Override
	protected void makeASelfRenderer(FieldSet fs, UISpecRunContext context,
			JSONObject out, JSONObject subexpander, JSONObject options,
			Record subitems, UISpecRunContext sub) throws JSONException {
		actualSelfRenderer(fs, context, subexpander, subitems );
		actualSelfRenderer(options, subexpander);
		actualAddDecorator(fs, out, sub, options);
	}
}