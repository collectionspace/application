/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.RuleSet;
import org.collectionspace.chain.csp.config.RuleTarget;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author caret
 * Handles the rules for parsing the default.xml / cspace-config.xml file
 * 
 * 
 */
public class Spec implements CSP, Configurable {
	private static final Logger log=LoggerFactory.getLogger(Spec.class);
	public static String SECTIONED="org.collectionspace.app.config.spec";
	public static String SECTION_PREFIX="org.collectionspace.app.config.spec.";
	public static String SPEC_ROOT=SECTION_PREFIX+"spec";

	private static final String required_version="11";

	private Map<String,Record> records=new HashMap<String,Record>();
	private Map<String,Relationship> relationships=new HashMap<String,Relationship>();
	private Map<String,Relationship> relationshipsPredicate=new HashMap<String,Relationship>();
	private Map<String,String> inverserelationships = new HashMap<String,String>();
	private Map<String,Schemas> schemas=new HashMap<String,Schemas>();
	
	private Map<String,Record> records_by_web_url=new HashMap<String,Record>();
	private Map<String,Record> records_by_services_url=new HashMap<String,Record>();
	private Map<String,Record> records_by_services_docType=new HashMap<String,Record>();
	private Map<String,Instance> instances=new LinkedHashMap<String,Instance>();
	private Map<String, Set<Field>> termlist = new HashMap<String, Set<Field>>();
	private Map<String, Structure> structure=new HashMap<String,Structure>();
	private String version;
	private String tenantid;
	private EmailData ed;
	private AdminData adminData;
	
	@Override
	public String getName() { return "schema"; }
	
	public String getVersion() {
		return version;
	}
	
	public String getTenantID() {
		return tenantid;
	}	

	@Override
	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
	}

	@Override
	public void configure(RuleSet rules) {

		
		
		/* MAIN/tenantid -> string */
		rules.addRule(SECTIONED,new String[]{"tenantid"},SECTION_PREFIX+"tenantid",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				tenantid=(String)section.getValue("");
				return this;
			}
		});
		
		/* MAIN/version -> string */
		rules.addRule(SECTIONED,new String[]{"version"},SECTION_PREFIX+"version",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				version=(String)section.getValue("");
				return this;
			}
		});		

		/* MAIN/email -> EmailData */
		rules.addRule(SECTIONED,new String[]{"email"},SECTION_PREFIX+"email",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				ed = new EmailData(Spec.this,section);
				return this;
			}
		});
		/* MAIN/admin -> AdminData */
		rules.addRule(SECTIONED,new String[]{"admin"},SECTION_PREFIX+"admin",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				adminData = new AdminData(Spec.this,section);
				return this;
			}
		});
		
		/* MAIN/spec -> SPEC */
		rules.addRule(SECTIONED,new String[]{"spec"},SECTION_PREFIX+"spec",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				((CoreConfig)parent).setRoot(SPEC_ROOT,Spec.this);
				return Spec.this;
			}
		});

		/* SPEC/schemas -> SCHEMAS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"schemas"},SECTION_PREFIX+"schemas",null,null);
		/* RELATIONSHIPS/relation -> RELATION(@id) */
		rules.addRule(SECTION_PREFIX+"schemas",new String[]{"schema"},SECTION_PREFIX+"schema",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Schemas s=new Schemas(Spec.this,section);
				schemas.put(s.getID(),s);
				return s;
			}
		});


		/* SPEC/relationships -> RELATIONSHIPS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"relationships"},SECTION_PREFIX+"relationships",null,null);
		/* RELATIONSHIPS/relation -> RELATION(@id) */
		rules.addRule(SECTION_PREFIX+"relationships",new String[]{"relation"},SECTION_PREFIX+"relation",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Relationship r=new Relationship(Spec.this,section);
				relationships.put(r.getID(),r);
				relationshipsPredicate.put(r.getPredicate(),r);
				if(r.hasInverse()){
					inverserelationships.put(r.getInverse(), r.getID());
				}
				return r;
			}
		});
		

		/* SPEC/records -> RECORDS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"records"},SECTION_PREFIX+"records",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Map<String,String> recordsdata=new HashMap<String,String>();
				String blanks = "";
				blanks=(String)section.getValue("/enum-blank");
				if(blanks == null){
					blanks = "Default String";
				}
				recordsdata.put("blank", blanks);

				return recordsdata;
			}
		});			
		/* RECORDS/record -> RECORD(@id) */
		rules.addRule(SECTION_PREFIX+"records",new String[]{"record"},SECTION_PREFIX+"record",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Map<String,String>data = (Map<String,String>)parent;
				Record r=new Record(Spec.this,section,data);
				records.put(r.getID(),r);
				records_by_web_url.put(r.getWebURL(),r);
				records_by_services_url.put(r.getServicesURL(),r);
				records_by_services_docType.put(r.getServicesTenantSg(),r);
				return r;
			}
		});
		/* SPEC/section -> Sections */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"section"},SECTION_PREFIX+"uisection",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				String id=(String)section.getValue("/@id");
				Record r=(Record)parent;
				r.addUISection("base",id);
				
				return r;
			}
		});

		/* Section/section -> Sections */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"section"},SECTION_PREFIX+"uisection",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				String id=(String)section.getValue("/@id");
				Record r=(Record)parent;
				r.addUISection("base",id);
				
				return r;
			}
		});
		
		/* RECORD/services-instances-path -> RECORDPATH */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"services-single-instance-path"},SECTION_PREFIX+"instance-path",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Record r=(Record)parent;
				String id=(String)section.getValue("/@id");
				if(id==null)
					id="common";
				r.setServicesInstancePath(id,(String)section.getValue(""));
				return r;
			}
		});		
		/* RECORD/services-record-path -> RECORDPATH */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"services-record-path"},SECTION_PREFIX+"record-path",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Record r=(Record)parent;
				String id=(String)section.getValue("/@id");
				if(id==null)
					id="common";
				r.setServicesRecordPath(id,(String)section.getValue(""));
				return r;
			}
		});		
		/* RECORD/instances/instance -> INSTANCE */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"instances","instance"},SECTION_PREFIX+"instance",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Instance n=new Instance((Record)parent,section);
				((Record)parent).addInstance(n);
				return n;
			}
		});			
		/* FIELD/options/option -> OPTION */
		rules.addRule(SECTION_PREFIX+"instance",new String[]{"options","option"},SECTION_PREFIX+"option",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Instance n=(Instance)parent;
				boolean dfault=false;
				String value=(String)section.getValue("/@default");
				dfault=(value!=null && ("yes".equals(value.toLowerCase()) || "1".equals(value.toLowerCase())));
				n.addOption((String)section.getValue("/@id"),(String)section.getValue(""),(String)section.getValue("/@sample"),dfault,(String)section.getValue("/@desc"));
				return n;
			}
		});

		/* RECORD/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"field"},SECTION_PREFIX+"field",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=new Field((Record)parent,section);
				((Record)parent).addField(f);
				
				String is_chooser=(String)section.getValue("/@chooser");
				if(is_chooser!=null && ("1".equals(is_chooser) || "yes".equals(is_chooser.toLowerCase())))
					f.setType("chooser");
				return f;
			}
		});	
		
		
		/* RECORD/structures/structure -> STRUCTURE */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"structures","structure"},SECTION_PREFIX+"structure",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Structure s=new Structure((Record)parent,section);
				((Record)parent).addStructure(s);
				return s;
			}
		});	


		/* STRUCTURE/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"structure",new String[]{"view","sidebar","repeat"},SECTION_PREFIX+"repeat",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Structure)parent,section);
				((Structure)parent).addSideBar(r);
				return r;
			}
		});		
		//
		/* FIELD/options/option -> OPTION */
		rules.addRule(SECTION_PREFIX+"structure",new String[]{"view","hierarchy-section", "options", "option"},SECTION_PREFIX+"option",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Structure n=(Structure)parent;
				boolean dfault=false;
				String value=(String)section.getValue("/@default");
				dfault=(value!=null && ("yes".equals(value.toLowerCase()) || "1".equals(value.toLowerCase())));
				n.addOption((String)section.getValue("/@id"),(String)section.getValue(""),(String)section.getValue("/@sample"),dfault);
				return n;
			}
		});


		/* STRUCTURE/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"structure",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Structure)parent,section);
				((Structure)parent).addField(r);
				((Structure)parent).addAllField(r);
				return r;
			}
		});

		/* RECORD/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Record)parent,section);
				((Record)parent).addField(r);
				return r;
			}
		});


		/* REPEAT/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"repeat",new String[]{"field"},SECTION_PREFIX+"field",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=new Field((Repeat)parent,section);
				f.getRecord().addField(f);
				((Repeat)parent).addChild(f);
				return f;
			}
		});


		/* RECORD/group -> GROUP */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"group"},SECTION_PREFIX+"group",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Group r=new Group((Record)parent,section);
				((Record)parent).addField(r);
				return r;
			}
		});
		/* GROUP/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"group",new String[]{"field"},SECTION_PREFIX+"field",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=new Field((Group)parent,section);
				f.getRecord().addField(f);
				((Group)parent).addChild(f);
				return f;
			}
		});
		/* GROUP/group -> GROUP */
		rules.addRule(SECTION_PREFIX+"group",new String[]{"group"},SECTION_PREFIX+"group",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Group r=new Group((Group)parent,section);
				((Group)parent).addChild(r);
				r.getRecord().addField(r);
				return r;
			}
		});
		/* REPEAT/group -> GROUP */
		rules.addRule(SECTION_PREFIX+"repeat",new String[]{"group"},SECTION_PREFIX+"group",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Group r=new Group((Repeat)parent,section);
				((Repeat)parent).addChild(r);
				r.getRecord().addField(r);
				return r;
			}
		});
		/* GROUP/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"group",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Repeat)parent,section);
				((Group)parent).addChild(r);
				r.getRecord().addField(r);
				return r;
			}
		});


		/* REPEAT/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"repeat",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Repeat)parent,section);
				((Repeat)parent).addChild(r);
				r.getRecord().addField(r);
				return r;
			}
		});
		

		/* FIELD/options/option -> OPTION */
		rules.addRule(SECTION_PREFIX+"field",new String[]{"options","option"},SECTION_PREFIX+"option",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=(Field)parent;
				boolean dfault=false;
				String value=(String)section.getValue("/@default");
				dfault=(value!=null && ("yes".equals(value.toLowerCase()) || "1".equals(value.toLowerCase())));
				f.addOption((String)section.getValue("/@id"),(String)section.getValue(""),(String)section.getValue("/@sample"),dfault);
				return f;
			}
		});
		/* FIELD/merges/merge -> OPTION */
		rules.addRule(SECTION_PREFIX+"field",new String[]{"merges","merge"},SECTION_PREFIX+"merge",null,new RuleTarget(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=(Field)parent;
				f.addMerge((String)section.getValue("/@id"), (String)section.getValue("/@rank"));
				/*
				boolean dfault=false;
				String value=(String)section.getValue("/@default");
				dfault=(value!=null && ("yes".equals(value.toLowerCase()) || "1".equals(value.toLowerCase())));
				f.addOption((String)section.getValue("/@id"),(String)section.getValue(""),(String)section.getValue("/@sample"),dfault);
				*/
				return f;
			}
		});
		
	}

	public EmailData getEmailData() { return ed.getEmailData(); }
	public AdminData getAdminData() { return adminData.getAdminData(); }

	public Boolean hasRelationship(String id){ if(relationships.containsKey(id)){return true;} else return false;}
	public Boolean hasRelationshipByPredicate(String id){ if(relationshipsPredicate.containsKey(id)){return true;} else return false;}
	public Relationship getRelation(String id) { return relationships.get(id); }
	public Relationship getRelationshipByPredicate(String id){ return relationshipsPredicate.get(id); }
	public Relationship[] getAllRelations(){ return relationships.values().toArray(new Relationship[0]); }
	public Relationship getInverseRelationship(String id) { if(inverserelationships.containsKey(id)){ return relationships.get(inverserelationships.get(id));	} else return null;	}
	public Boolean hasRelationshipInverse(String id){ if(inverserelationships.containsKey(id) && relationships.containsKey(inverserelationships.get(id))){return true;} else return false;}

	public Boolean hasSchema(String id){ if(schemas.containsKey(id)){return true;} else return false;}
	public Schemas getSchema(String id) { return schemas.get(id); }
	public Schemas[] getAllSchemas(){ return schemas.values().toArray(new Schemas[0]); }
	
	public Boolean hasRecord(String id){ if(records.containsKey(id)){return true;} else return false;}
	public Boolean hasRecordByServicesUrl(String url) { return records_by_services_url.containsKey(url); }
	public Boolean hasRecordByServicesDocType(String docType) { return records_by_services_docType.containsKey(docType); }
	
	public Boolean hasRecordByWebUrl(String url){ return records_by_web_url.containsKey(url);}
	public Record getRecord(String id) { return records.get(id); }
	public Record getRecordByWebUrl(String url) { return records_by_web_url.get(url); }
	public Record getRecordByServicesUrl(String url) { return records_by_services_url.get(url); }
	public Record getRecordByServicesDocType(String docType) { return records_by_services_docType.get(docType); }
	public Record[] getAllRecords() { return records.values().toArray(new Record[0]); }
	public String[] getAllRecordsOrdered() {

		List<String> precrds = new ArrayList<String>();
		List<String> arecrds = new ArrayList<String>();
		List<String> vrecrds = new ArrayList<String>();
		List<String> adrecrds = new ArrayList<String>();
		List<String> crecrds = new ArrayList<String>();
		for (Record rc : this.getAllRecords()) {
			if (rc.isInRecordList()) {
				if (rc.isShowType("procedure")) {
					precrds.add(rc.getWebURL());
				} else if (rc.isShowType("authority")) {
					vrecrds.add(rc.getWebURL());
					for(Instance ins : rc.getAllInstances()){
						arecrds.add(ins.getWebURL());
					}
				} else if (rc.isShowType("record")) {
					crecrds.add(rc.getWebURL());
				} else if(rc.isShowType("authorizationdata") || rc.isShowType("userdata")){
					adrecrds.add(rc.getWebURL());
				}
			}
		}
		//order: 
		/*
		 * Cataloging
		 * Procedures in alpha order
		 * Vocabularies in alpha order
		 * Admin in alpha order
		 */
		Collections.sort(crecrds);
		Collections.sort(precrds);
		Collections.sort(vrecrds);
		Collections.sort(arecrds);
		Collections.sort(adrecrds);
		
		crecrds.addAll(precrds);
		crecrds.addAll(vrecrds);
		crecrds.addAll(adrecrds);
		crecrds.addAll(arecrds);
		return crecrds.toArray(new String[0]);
	}
	
	public void addTermlist(String instanceid, Field fs){
		if(termlist.containsKey(instanceid)){
			Set<Field> temp = termlist.get(instanceid);
			temp.add(fs);
			termlist.put(instanceid,temp);
		}
		else{
			termlist.put(instanceid,new HashSet<Field>(Arrays.asList(fs)));
		}
	}
	public void addInstance(Instance n) {
		instances.put(n.getID(),n);
	}
	public void addStructure(Structure s) {
		structure.put(s.getID(),s);
	}

	public Boolean hasTermlist(String id){ if(termlist.containsKey(id)){return true;} else return false;}
	public Field[] getTermlist(String id) { return termlist.get(id).toArray(new Field[0]);}
	public Instance getInstance(String id) { return instances.get(id); }
	public Structure getStructure(String id) { return structure.get(id); }
	
	@Override
	public void config_finish() throws CSPDependencyException {
		if(!required_version.equals(version))
			throw new CSPDependencyException("Config is out of date: require="+required_version+" got="+version);
		for(Record r : records.values()) {
			r.config_finish(this);
		}
	}
	
	@Override
	public void complete_init(CSPManager cspManager, boolean forXsdGeneration) throws CSPDependencyException {
		// Intentionally blank
	}
	
	public Map<String,String> ui_url_to_id(){
		Map<String,String> url_to_type=new HashMap<String,String>();
		for(Record r : getAllRecords()) {
			url_to_type.put(r.getWebURL(),r.getID());
		}
		return url_to_type;		
	}
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		String[] allStrings = null;
		String[] allBooleans = null;
		String[] allSets = null;
		Map <String, StringBuffer> rout = new HashMap<String, StringBuffer>();
		for(Record r : records.values()){

			if(allStrings == null){
				allStrings = r.utils.getAllString();
				allBooleans = r.utils.getAllBoolean();
				allSets = r.utils.getAllSets();

				for(String s: allStrings){
					rout.put("String:"+s, new StringBuffer());
					rout.get("String:"+s).append(r.utils.getDefaultString(s) + ",");
				}
				for(String s: allBooleans){
					rout.put("Boolean:"+s, new StringBuffer());
					rout.get("Boolean:"+s).append(r.utils.getDefaultBoolean(s) + ",");
				}
				for(String s:allSets){
					rout.put("Set:"+s, new StringBuffer());
					rout.get("Set:"+s).append("\"" + r.utils.allDefaultSets.get(s) + "\"" + ",");
				}
				out.append("Field Name,Default Value,");
			}
			for(String s: allStrings){
				rout.get("String:"+s).append(r.utils.getString(s) + ",");
			}
			for(String s: allBooleans){
				rout.get("Boolean:"+s).append(r.utils.getBoolean(s) + ",");
			}
			for(String s:allSets){
				rout.get("Set:"+s).append("\""+ r.utils.allSets.get(s) + "\"" + ",");
			}
//might want to pivot the view to have records down and fields across to make it wasier to see patterns
			out.append(r.getID() + ",");
		}
		for (Map.Entry<String, StringBuffer> entry : rout.entrySet()) {
		    String key = entry.getKey();
		    StringBuffer value = entry.getValue();
		    out.append("\n");
		    out.append(key+",");
		    out.append(value);
		    // ...
		}
		return out.toString();
	}
	

	public JSONObject dumpJson() throws JSONException {

		JSONObject out=new JSONObject();
		ed.dumpJson(out);
		adminData.dumpJson(out);
		String[] allStrings = null;
		String[] allBooleans = null;
		String[] allSets = null;
		JSONObject RecordTable=new JSONObject();
		for(Record r : records.values()){
			if(allStrings == null){
				allStrings = r.utils.getAllString();
				allBooleans = r.utils.getAllBoolean();
				allSets = r.utils.getAllSets();

				for(String s: allStrings){
					JSONObject data = new JSONObject();
					data.put("default",  r.utils.getDefaultString(s));
					RecordTable.put("String:"+s, data);
				}
				for(String s: allBooleans){
					JSONObject data = new JSONObject();
					data.put("default",  r.utils.getDefaultBoolean(s));
					RecordTable.put("Boolean:"+s,data);
				}
				for(String s:allSets){
					JSONObject data = new JSONObject();
					data.put("default",  r.utils.getDefaultSet(s));
					RecordTable.put("Set:"+s,data);
				}
			}
			else{

				for(String s: allStrings){
					JSONObject data = RecordTable.getJSONObject("String:"+s);
					data.put(r.getID(), r.utils.getString(s));
					RecordTable.put("String:"+s, data);
				}
				for(String s: allBooleans){
					JSONObject data = RecordTable.getJSONObject("Boolean:"+s);
					data.put(r.getID(), r.utils.getBoolean(s));
					RecordTable.put("Boolean:"+s,data);
				}
				for(String s:allSets){
					JSONObject data = RecordTable.getJSONObject("Set:"+s);
					data.put(r.getID(), r.utils.getSet(s));
					RecordTable.put("Set:"+s,data);
				}
			}
		}
		out.put("allrecords", RecordTable);
		//	r.dumpJson(out);
		return out;
	
	}
	
}
