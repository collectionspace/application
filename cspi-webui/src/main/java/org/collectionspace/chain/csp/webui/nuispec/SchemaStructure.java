package org.collectionspace.chain.csp.webui.nuispec;

import java.util.Iterator;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.schema.UISpecRunContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Master file that knows the structure of the record for each type of view.
 * Uses the data from the config file to create this view
 * @author csm22
 *
 */
public class SchemaStructure {
	private static final Logger log=LoggerFactory.getLogger(SchemaStructure.class);
	protected static final String DECORATORS_KEY = "decorators";

	protected Spec spec;
	protected String structureview;
	protected String spectype = "";
	protected String tenantname = "html";
	
	/**
	 * Intialise this schema for record type, view and type (search|tab|edit)
	 * @param spec
	 * @param r
	 * @param sview
	 * @param stype
	 */
	public SchemaStructure(Spec spec, String sview, String stype) {
		initialiseVariables(spec, sview, stype);
	}
	public SchemaStructure(Spec spec, String sview) {
		String stype = "";
		if(sview.equals("search")){
			stype = "search";
		}
		initialiseVariables(spec, sview, stype);
	}
	public SchemaStructure(Record r, String sview){
		String stype = "";
		if(sview.equals("search")){
			stype = "search";
		}
		initialiseVariables(r.getSpec(), sview, stype);
	}

	/**
	 * Abstract out the initialization process so we can just call it from both the constructors
	 * @param spec
	 * @param sview
	 * @param stype
	 */
	private void initialiseVariables(Spec spec, String sview, String stype){
		this.spec = spec;
		this.structureview = sview;
		this.spectype = stype;
		this.tenantname = this.spec.getAdminData().getTenantName();
		if(this.tenantname == null || this.tenantname.equals("")){
			this.tenantname = "html";
		}
	}
	/**
	 * Loop through fields and find all the data needed
	 * @param context
	 * @param r
	 * @param type - search|tab|edit
	 * @return
	 * @throws JSONException
	 */
	public JSONObject generateDataEntrySection(UISpecRunContext context, Record r, String type) throws JSONException {
		JSONObject out=new JSONObject();
		for(FieldSet fs : r.getAllFieldTopLevel(type)) {
			whatIsThisFieldSet(out,fs,context);
		}
		return out;
	}
	
	protected JSONArray getExistingDecoratorsArray(JSONObject out, String fieldSelector) throws JSONException {
		JSONArray decorators = null; 
		if(out.has(fieldSelector)) {
			Object value = out.get(fieldSelector);
			if(value instanceof JSONObject) {
				JSONObject entry = out.getJSONObject(fieldSelector);
				decorators = getExistingDecoratorsArray(entry);
			}
		}
		return decorators;
	}
	
	protected JSONArray getExistingDecoratorsArray(JSONObject fieldInfo) throws JSONException {
		JSONArray decorators = null; 
		if(fieldInfo.has(DECORATORS_KEY)) {
			decorators = fieldInfo.getJSONArray(DECORATORS_KEY);
		}
		return decorators;
	}
	

	
	/**
	 * Find out what this fieldset is and so what we should do with it.
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void whatIsThisFieldSet(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		if(fs != null && isATrueField(fs)){
			if(isAnUploader(fs)) {
				makeAnUploaderEntry(out, fs, context);
			}
			if(isAnHeirarchy(fs)) {
				makeAnHeirarchyEntry(out, fs, context);
			}
			if(isASubRecord(fs)){
				makeASubRecordEntry(out, fs, context, null);
			}
			else if(fs instanceof Field){
				makeAFieldEntry(out, fs, context);
			}
			else if(fs instanceof Group){
				makeAGroupEntry(out, fs, context);
			}
			else if(fs instanceof Repeat){
				makeARepeatEntry(out, fs, context);
			}
		}
	}

	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param fs
	 * @param out
	 * @param context
	 * @throws JSONException 
	 */
	protected void 	actualGroupEntry(FieldSet fs, JSONObject out, UISpecRunContext context, JSONObject contents) throws JSONException{}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param temp
	 * @param labelSelector
	 * @param label
	 * @throws JSONException
	 */
	protected void actualMessageKey(JSONObject temp, String labelSelector, String label) throws JSONException {}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualFieldExpanderEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{}	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param tree
	 * @throws JSONException
	 */
	protected void actualSelfRenderer(JSONObject out, JSONObject tree) throws JSONException{}
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualFieldRefactored(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String uiType = fs.getUIType();
		if(fs.hasAutocompleteInstance()) {
			actualAuthorities(out, fs, context);
			// Allow dual behaviors here
			if("externalURL".equalsIgnoreCase(uiType)){
				actualExternalURLField(out, fs, context);
			}
		} else if("chooser".equals(uiType) && !this.spectype.equals("search")) {
			actualChooserField(out, fs,context, false);
		} else if("date".equals(uiType)) {
			actualDateField(out, fs, context);
		} else if("validated".equals(uiType)){
			actualValidatedField(out, fs, context);
		} else if("computed".equals(uiType)){
			actualComputedField(out, fs, context);
		} else if("richtext".equals(uiType)){
			actualRichTextField(out, fs, context);
		} else if("externalURL".equalsIgnoreCase(uiType)){
			actualExternalURLField(out, fs, context);
		} else if("valueDeURNed".equalsIgnoreCase(uiType)){
			actualDeURNedField(out, fs, context);
                } else if(fs.isASelfRenderer()){	// also based upon uiType
			actualSelfRendererField(out, fs, context);
		} else{
			actualField(out, fs, context);
		}
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param context
	 * @param f
	 * @throws JSONException
	 */
	protected void actualAuthorities(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @param useContainer
	 * @throws JSONException 
	 */
	protected void actualChooserField(JSONObject out, FieldSet fs, UISpecRunContext context, Boolean useContainer) throws JSONException{
	}
	/**
	 * 
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualDateField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualValidatedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}
	/**
	 * Write the JSON structure for a computed field.
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualComputedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}	
	/**
	 * Write the JSON structure for a rich text field.
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualRichTextField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}	
	/**
	 * 
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualExternalURLField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}
        /**
	 * 
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualDeURNedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException
	 */
	protected void actualSelfRendererField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualFieldNotRefactored(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		// Single field
		actualField(out, fs, context);	
		
		if(fs.hasAutocompleteInstance()) {
			actualAuthorities(out, fs, context);
		}
		String uiType = fs.getUIType();
		if("chooser".equals(uiType) && !this.spectype.equals("search")) {
			actualChooserField(out, fs,context, true);
		} else if("date".equals(uiType)) {
			actualDateField(out, fs, context);
		} else if("validated".equals(uiType)){
			actualValidatedField(out, fs, context);
		} else if("externalURL".equalsIgnoreCase(uiType)){
			actualExternalURLField(out, fs, context);
		} else if("valueDeurned".equalsIgnoreCase(uiType)){
			actualDeURNedField(out, fs, context);
                }
	}

	protected Object actualFieldEntry(FieldSet fs, UISpecRunContext context ) throws JSONException {
		return actualFieldEntry(fs, context, null);
	}

	/**
	 * Generates field entries in Schema or Spec for simple field types.
	 * @param fs The field we are working on
	 * @param context Information about the kind of spec we are producing.
	 * @param decorators the decorators array object, if we have to handle multiple ui behaviors.
	 * @return
	 * @throws JSONException
	 */
	protected Object actualFieldEntry(FieldSet fs,UISpecRunContext context, JSONArray decorators) throws JSONException {
		Field f = (Field)fs;
		if("plain".equals(f.getUIType())) {
			if("boolean".equals(f.getDataType())){
				return actualBooleanField(f,context);
			}
			else{
				return displayAsplain(f,context);
			}
		} 
		else if("list".equals(f.getUIType())){
			return displayAsplainlist(f);
		}
		else if("dropdown".equals(f.getUIType())) {
			return actualOptionField(f,context);
		}
		else if("enum".equals(f.getUIType())) {
			return actualENUMField(f,context);
		}
		else if(isAGroupField(f)) {
			// When generating the actual field entry for a structured date in the context
			// of range search, we have to output a plain field, and not the structured date info
			if(this.spectype.equals("search") && f.getSearchType().equals("range")
					&& isAStructureDate(f)){
				return displayAsplain(f, context);
			} else {
				return makeAGroupField(f,context);
			}
		}
		
		return makeAOtherField(f,context);	
	}

	/**
	 * Default behaviour if don't know what the field is
	 * test data type and then just assume a string
	 * @param fs
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	protected Object makeAOtherField(Field fs, UISpecRunContext context) throws JSONException {
		String datatype = fs.getDataType();
		if(datatype.equals("")){	datatype="string";	}
		if(datatype.equals("boolean")){	return actualBooleanField(fs, context);	}
		return displayAsplain(fs, context);
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param f
	 * @param context
	 * @return
	 */
	protected Object actualBooleanField(Field f,UISpecRunContext context) throws JSONException {
		return null;
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param f
	 * @param context
	 * @return
	 */
	protected Object displayAsplain(Field f,UISpecRunContext context) throws JSONException {
		return "";
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param f
	 * @return
	 */
	protected Object displayAsplainlist(Field f) throws JSONException {
		return "";
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	protected Object actualOptionField(Field f,UISpecRunContext context) throws JSONException {
		return null;
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	protected Object actualENUMField(Field f,UISpecRunContext context) throws JSONException {
		return actualENUMField(f, context, null);
	}

	protected Object actualENUMField(Field f,UISpecRunContext context, JSONArray decorators) throws JSONException {
		return null;
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
	protected void actualStructuredDate(FieldSet fs, JSONObject out, UISpecRunContext sub, JSONObject subexpander, Record subitems, JSONObject options) throws JSONException{}
	
	/**
	 * Overwrite with output you need for this sub record
	 * @param out
	 * @param fs
	 * @param context
	 * @param subr
	 * @param repeated
	 * @param parent
	 * @throws JSONException
	 */
	protected void actualSubRecordField(JSONObject out, FieldSet fs, UISpecRunContext context, Record subr, Boolean repeated, JSONObject parent) throws JSONException{
		makeASubRecord(subr, out,  repeated,  context, parent);
	}
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualTrueTreeEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param r
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualRepeatExpanderEntry(JSONObject out, Repeat r, UISpecRunContext context) throws JSONException{}
	/**
	 * Overwrite with the output you need for the thing you are doing.
	 * @param out
	 * @param r
	 * @param context
	 * @param contents
	 * @throws JSONException 
	 */
	protected void actualRepeatNonSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context, JSONObject contents) throws JSONException{}
	/**
	 * Overwrite with the output you need for the thing you are doing.
	 * @param out
	 * @param r
	 * @param context
	 * @param children
	 * @throws JSONException 
	 */
	protected void actualRepeatSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context, JSONArray children) throws JSONException{}
	/**
	 * Overwrite with the output you need for the thing you are doing.
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void actualUploaderEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{}
	
	/**
	 * Do all the magic to make an uploader thing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException
	 */
	protected void makeAnUploaderEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		actualUploaderEntry(out,fs,context);
	}
	/**
	 * Do all the magic to make an heirarchy thing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException
	 */
	protected void makeAnHeirarchyEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		//generateHierarchyEntry(out,fs,context);
	}
	/**
	 * Do all the magic to make a sub record thing
	 * first is it nested directly in the data or one level under
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void makeASubRecordEntry(JSONObject out,FieldSet fs, UISpecRunContext context, JSONObject outerlayer) throws JSONException{
		//create group item or field at the same level as parent fields - do not nest
		UISpecRunContext sub = context;
		if(!fs.getUISpecInherit()){
			//default behaviour do group or field as expected by changing the context (adds more info to the selectors etc)
			sub = context.createChild();
			if(!fs.getSelectorAffix().equals("")){
				sub.setUIAffix(fs.getSelectorAffix());
			}
			String sp=fs.getUISpecPrefix();
			if(sp!=null)
				sub.setUIPrefix(sp);
		}
		
		Record subrecord = fs.usesRecordId();
		Boolean repeated = false;
		if(fs.getParent() instanceof Repeat ||( fs instanceof Repeat && !(fs instanceof Group))){
			repeated = true;
		}
		if( outerlayer == null){
			outerlayer = out;
		}
		if(fs instanceof Group){
			Group gp = (Group)fs;
			if(gp.isGrouped()){
				sub.setPad(false);
			}
		}
		actualSubRecordField( out,  fs,  sub,  subrecord,  repeated,  outerlayer);
	}
	/**
	 * Do all the real magic to make a sub record thing
	 * @param subr
	 * @param out
	 * @param repeated
	 * @param context
	 * @param parent
	 * @throws JSONException
	 */
	protected void makeASubRecord(Record subr, JSONObject out, Boolean repeated, UISpecRunContext context, JSONObject parent) throws JSONException {
		for(FieldSet fs2 : subr.getAllFieldTopLevel("")) {
			if(repeated){
				fs2.setRepeatSubRecord(true);
			}
			whatIsThisFieldSet(out,fs2, context);
			fs2.setRepeatSubRecord(false);
		}
		Structure s = subr.getStructure(this.structureview);
		if(s.showMessageKey()){
			makeAllRecordMessageKey(context, parent, subr);
		}
		
	}
	
	/**
	 * Do all the magic to get the message keys for a record
	 * @param affix
	 * @param temp
	 * @param r
	 * @throws JSONException
	 */
	protected void makeAllRecordMessageKey(UISpecRunContext affix, JSONObject temp, Record r) throws JSONException{
		String type = "";
		if(this.spectype.equals("search")){ //is this a search uispec
			type = this.spectype;
			for(FieldSet fs : r.getAllFieldTopLevel(type)) {
				if(fs.getID()!=null){
					if(isATrueRepeat(fs)){
						Repeat rp = (Repeat)fs;
						for(FieldSet child : rp.getChildren("")) {
							actualMessageKey(temp, r.getUILabelSelector(child.getID()), child.getLabel());
						}
					}
					else{
						actualMessageKey(temp, r.getUILabelSelector(fs.getID()), fs.getLabel());
					}
				}
			}
		}
		else{
			for(FieldSet fs : r.getAllFieldFullList("")) { //include children of repeats as well as top level
				if(fs.getID()!=null){
					actualMessageKey(temp, fs.getUILabelSelector(), fs.getLabel());
				}
			}
		}
		for(String st: r.getAllUISections(type)){
			if(st!=null){
				actualMessageKey(temp, r.getUILabelSelector(st),r.getUILabel(st));
			}
		}
	}

	
	
	/**
	 * Do all the magic to make a field thing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void makeAFieldEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		if(isExpander(fs)){
			actualFieldExpanderEntry(out, fs, context);
		}
		else if(isTrueTree(fs)){
			actualTrueTreeEntry(out,fs,context);
		}
		else if(isRefactored(fs)){
			actualFieldRefactored(out,fs, context);
		}
		else{
			actualFieldNotRefactored(out, fs, context);
		}
	}

	/**
	 * Do all the magic to make a Group thing
	 * @param out
	 * @param fs
	 * @param contexts
	 * @throws JSONException 
	 */
	protected void makeAGroupEntry(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		Group g = (Group)fs;
		JSONObject contents=new JSONObject();
		for(FieldSet child : g.getChildren("")) {
			whatIsThisFieldSet(contents,child, context);
		}
		//make the item
		if(fs.isASelfRenderer()){
			JSONObject renderedcontents = new JSONObject();
			actualSelfRenderer(renderedcontents, contents);
			contents = renderedcontents;
		}
		else if(isAGroupField(fs)){
			contents = makeAGroupField(g,context);
		}
		actualGroupEntry(fs, out ,context, contents);
	}

	/**
	 * Do all the magic to make a GroupField
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected JSONObject makeAGroupField(FieldSet fs, UISpecRunContext context) throws JSONException{
		JSONObject out=new JSONObject();
		if(fs instanceof Field || fs instanceof Repeat){
			JSONObject subexpander = new JSONObject();
			JSONObject options=new JSONObject();
			String parts[] = fs.getUIType().split("/");
			Record subitems = fs.getRecord().getSpec().getRecordByServicesUrl(parts[1]);
			UISpecRunContext sub = context.createChild();
			sub.setUIPrefix(fs.getID());
			sub.setPad(false);

			if(isAStructureDate(fs)){
				makeAStructureDate(fs, out, subexpander, options, subitems, sub, context);
			}
			else if(fs.isASelfRenderer()){
				makeASelfRenderer(fs, context, out, subexpander, options,
						subitems, sub);
			}
			else{
				makeAOtherGroup(fs, out, subexpander, options, subitems, sub);
			}
		}
		return out;
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
	protected void makeAStructureDate(FieldSet fs, JSONObject out,
			JSONObject subexpander, JSONObject options, Record subitems,
			UISpecRunContext sub, UISpecRunContext mainContext) throws JSONException {
		throw new JSONException("makeAStructuredDate must be overridden");
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
	protected void makeAOtherGroup(FieldSet fs, JSONObject out,
			JSONObject subexpander, JSONObject options, Record subitems,
			UISpecRunContext sub) throws JSONException {
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
	protected void makeASelfRenderer(FieldSet fs, UISpecRunContext context,
			JSONObject out, JSONObject subexpander, JSONObject options,
			Record subitems, UISpecRunContext sub) throws JSONException {
	}
	/**
	 * render the mark up needed by the UISpec
	 * @param fs
	 * @param out
	 * @param sub
	 * @param subitems
	 * @throws JSONException
	 */
	protected void actualOtherGroup(FieldSet fs, JSONObject out, UISpecRunContext sub,  Record subitems) throws JSONException{}
	
	/**
	 * render the extra mark up needed by the UISpec for self renderers
	 * @param fs
	 * @param out
	 * @param sub
	 * @param options
	 * @throws JSONException
	 */
	protected void actualAddDecorator(FieldSet fs, JSONObject out, UISpecRunContext sub,  JSONObject options) throws JSONException{
	}

	/**
	 * render the mark up needed by the UISpec for self renderers
	 * @param fs
	 * @param context
	 * @param subexpander
	 * @param subitems
	 * @param options
	 * @throws JSONException
	 */
	protected void actualSelfRenderer(FieldSet fs, UISpecRunContext context, JSONObject subexpander, Record subitems ) throws JSONException{

		Boolean truerepeat = false;
		FieldParent fsp = fs.getParent();
		if(fsp instanceof Repeat && !(fsp instanceof Group)){
			Repeat rp = (Repeat)fsp;//remove bogus repeats used in search
			if(isATrueRepeat(rp)){
				truerepeat = true;
				for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {	
					whatIsThisFieldSet(subexpander,fs2, context);
				}
				makeAllRecordMessageKey(context, subexpander, subitems);
			}
		}
		if(!truerepeat){
			for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {	
				whatIsThisFieldSet(subexpander,fs2, context);
			}
			makeAllRecordMessageKey(context, subexpander, subitems);
		}
	}

	/**
	 * Do all the magic to make a repeat thing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	protected void makeARepeatEntry(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		Repeat r = (Repeat)fs;
		//is this a real repeat or a ghost because of the search repeators
		if(r.getXxxUiNoRepeat()){
			whatIsThisFieldSet(out, getFirstChild(r), context);
		}
		else{
			if(r.getXxxUiNoRepeat()){//only repeats in the services not in the UI
				whatIsThisFieldSet(out, getFirstChild(r), context);
			}
			else if(isASiblingRepeat(r)){//sibling repeat [{"","",""}] e.g roles and permissions
				makeARepeatSiblingEntry(out, r, context);
			}
			else{
				makeARepeatNonSiblingEntry(out, r, context);
			}
		}
		//is this a nested repeat or a top level repeat...
	}	
	/**
	 * Do all the magic for a Repeat that wants to be rendered like siblings
	 * @param out
	 * @param r
	 * @param context
	 * @throws JSONException
	 */
	protected void makeARepeatNonSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context) throws JSONException{
		if(isExpander(r)){
			actualRepeatExpanderEntry(out, r, context);
		}
		else{
			JSONObject preProtoTree=new JSONObject();
			if(isARepeatingSubRecord(r)){
				makeASubRecordEntry(preProtoTree, r, context, out);
			}
			else{
				for(FieldSet child :r.getChildren("")) {
					whatIsThisFieldSet(preProtoTree,child, context);
				}
			}
			actualRepeatNonSiblingEntry(out, r, context, preProtoTree);
		}
	}
	/**
	 * 
	 * @param out
	 * @param context
	 * @param r
	 * @param row
	 * @param children
	 * @throws JSONException
	 */
	protected void makeARepeatSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context) throws JSONException {
		JSONObject contents=new JSONObject();
		for(FieldSet child : r.getChildren("")) {
			whatIsThisFieldSet(contents,child, context);
		}
		JSONArray children = new JSONArray();
		children.put(contents);
		actualRepeatSiblingEntry(out, r, context, children);
	}

	
	/**
	 * when there is a mismatch between what the UI and services think are repeatables
	 * @param r
	 * @return
	 */
	protected FieldSet getFirstChild(Repeat r){
		FieldSet[] children=r.getChildren("");
		if(children.length!=0){
			return children[0];
		}
		return null;
	}
	/**
	 * test whether this is one of those repeats which should be represented as [{'','',''}]
	 * this is a weirdness of the roles and permissions
	 * @param r
	 * @return
	 */
	private Boolean isASiblingRepeat(Repeat r){
		return r.asSibling() && !r.hasServicesParent();
	}
	/**
	 * Test whether this is a ghost of a repeat because of the advance search quirkiness
	 * @param fs
	 * @return
	 */
	protected Boolean isATrueRepeat(FieldSet fs){
		if(fs instanceof Repeat){
			if(fs.getSearchType().equals("repeator") && !this.spectype.equals("search")){
				//repeator are adv search only repeats
				//repeatored are adv search and edit/view repeats
				//repeatable are just edit/view repeats
				//this isn't a real repeat so ignore it and go straight to child as it is in the wrong context
				return false;
			}
			if((fs instanceof Group || fs instanceof Repeat) && !((Repeat) fs).getXxxServicesNoRepeat()){
				return true;
			}
			return false;
		}
		return false;
	}
	/**
	 * test whether this is field should be included e.g. if this is adv search and this field isn't one then ignore
	 * @param fs
	 * @return
	 */
	private Boolean isATrueField(FieldSet fs){
		return (!this.spectype.equals("search") || (this.spectype.equals("search") && !fs.getSearchType().equals("")));
	}
	
	/**
	 * Check if something should be a structured date
	 * @param fs
	 * @return
	 */
	protected Boolean isAStructureDate(FieldSet fs){
		return fs.getUIType().startsWith("groupfield") && fs.getUIType().contains("structureddate");
	}
	/**
	 * Check if fieldSet is a subrecord surrounded by a Group tag but not a repeat tag
	 * repeats are done separately as they need more nesting
	 * @param fs
	 * @return
	 */
	private Boolean isASubRecord(FieldSet fs){
		return (fs.usesRecord() && !(fs instanceof Repeat && !(fs instanceof Group)));
	}
	/**
	 * Check if Repeat is a subrecord 
	 * @param r
	 * @return
	 */
	protected Boolean isARepeatingSubRecord(Repeat r){
		return r.usesRecord();
	}
	/**
	 * Check if this FieldSet is an uploader
	 * @param fs
	 * @return
	 */
	private Boolean isAnUploader(FieldSet fs){
		return "uploader".equals(fs.getUIType());
	}
	/**
	 * groupfields are used by structured dates 
	 * which are being a little different
	 * @param fs
	 * @return
	 */
	private Boolean isAGroupField(FieldSet fs){
		return fs.getUIType().startsWith("groupfield");
	}
	/**
	 * Check if this FieldSet is an Heirarchical thing
	 * @param fs
	 * @return
	 */
	private Boolean isAnHeirarchy(FieldSet fs){
		return "hierarchy".equals(fs.getUIType());
	}
	/**
	 * Check if the UI thinks of this thing as a true tree
	 * @param fs
	 * @return
	 */
	private Boolean isTrueTree(FieldSet fs){
		return ((Field)fs).isInTrueTree();
	}
	/**
	 * Check if the UI has refactored this as one item or wether it is multiple containers
	 * @param fs
	 * @return
	 */
	protected Boolean isRefactored(FieldSet fs){
		return ((Field)fs).isRefactored();
	}
	/**
	 * Check whether this item should be enclosed by expander tags
	 * @param fs
	 * @return
	 */
	private Boolean isExpander(FieldSet fs){
		return fs.isExpander();
	}


	
	
	
	
	
}
