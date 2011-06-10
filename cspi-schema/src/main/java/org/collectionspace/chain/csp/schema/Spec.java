/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
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
	public static String SECTION_PREFIX="org.collectionspace.app.config.spec.";
	public static String SPEC_ROOT=SECTION_PREFIX+"spec";

	private static final String required_version="10";

	private Map<String,Record> records=new HashMap<String,Record>();
	private Map<String,Relationship> relationships=new HashMap<String,Relationship>();
	private Map<String,Relationship> relationshipsPredicate=new HashMap<String,Relationship>();
	private Map<String,String> inverserelationships = new HashMap<String,String>();
	private Map<String,Schemas> schemas=new HashMap<String,Schemas>();
	
	private Map<String,Record> records_by_web_url=new HashMap<String,Record>();
	private Map<String,Record> records_by_services_url=new HashMap<String,Record>();
	private Map<String,Instance> instances=new HashMap<String,Instance>();
	private Map<String, Structure> structure=new HashMap<String,Structure>();
	private String version;
	private EmailData ed;
	private AdminData adminData;
	
	public String getName() { return "schema"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
	}

	public void configure(Rules rules) {
		/* MAIN/version -> string */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"version"},SECTION_PREFIX+"version",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				version=(String)section.getValue("");
				return this;
			}
		});

		/* MAIN/email -> EmailData */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"email"},SECTION_PREFIX+"email",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				ed = new EmailData(Spec.this,section);
				return this;
			}
		});
		/* MAIN/admin -> AdminData */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"admin"},SECTION_PREFIX+"admin",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				adminData = new AdminData(Spec.this,section);
				return this;
			}
		});
		
		/* MAIN/spec -> SPEC */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"spec"},SECTION_PREFIX+"spec",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				((CoreConfig)parent).setRoot(SPEC_ROOT,Spec.this);
				return Spec.this;
			}
		});

		/* SPEC/schemas -> SCHEMAS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"schemas"},SECTION_PREFIX+"schemas",null,null);
		/* RELATIONSHIPS/relation -> RELATION(@id) */
		rules.addRule(SECTION_PREFIX+"schemas",new String[]{"schema"},SECTION_PREFIX+"schema",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Schemas s=new Schemas(Spec.this,section);
				schemas.put(s.getID(),s);
				return s;
			}
		});


		/* SPEC/relationships -> RELATIONSHIPS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"relationships"},SECTION_PREFIX+"relationships",null,null);
		/* RELATIONSHIPS/relation -> RELATION(@id) */
		rules.addRule(SECTION_PREFIX+"relationships",new String[]{"relation"},SECTION_PREFIX+"relation",null,new Target(){
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
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"records"},SECTION_PREFIX+"records",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Map<String,String> recordsdata=new HashMap<String,String>();
				String blanks = "Default String";
				if((String)section.getValue("/enum-blank") != null || (String)section.getValue("/enum-blank") != ""){
					blanks=(String)section.getValue("/enum-blank");
				}
				recordsdata.put("blank", blanks);

				return recordsdata;
			}
		});			
		/* RECORDS/record -> RECORD(@id) */
		rules.addRule(SECTION_PREFIX+"records",new String[]{"record"},SECTION_PREFIX+"record",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Map<String,String>data = (Map<String,String>)parent;
				Record r=new Record(Spec.this,section,data);
				records.put(r.getID(),r);
				records_by_web_url.put(r.getWebURL(),r);
				records_by_services_url.put(r.getServicesURL(),r);
				return r;
			}
		});
		/* SPEC/section -> Sections */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"section"},SECTION_PREFIX+"uisection",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				String id=(String)section.getValue("/@id");
				Record r=(Record)parent;
				r.addUISection(id);
				
				return r;
			}
		});

		/* Section/section -> Sections */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"section"},SECTION_PREFIX+"uisection",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				String id=(String)section.getValue("/@id");
				Record r=(Record)parent;
				r.addUISection(id);
				
				return r;
			}
		});
		
		/* RECORD/services-record-path -> RECORDPATH */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"services-record-path"},SECTION_PREFIX+"record-path",null,new Target(){
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
		rules.addRule(SECTION_PREFIX+"record",new String[]{"instances","instance"},SECTION_PREFIX+"instance",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Instance n=new Instance((Record)parent,section);
				((Record)parent).addInstance(n);
				return n;
			}
		});			
		/* FIELD/options/option -> OPTION */
		rules.addRule(SECTION_PREFIX+"instance",new String[]{"options","option"},SECTION_PREFIX+"option",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Instance n=(Instance)parent;
				boolean dfault=false;
				String value=(String)section.getValue("/@default");
				dfault=(value!=null && ("yes".equals(value.toLowerCase()) || "1".equals(value.toLowerCase())));
				n.addOption((String)section.getValue("/@id"),(String)section.getValue(""),(String)section.getValue("/@sample"),dfault);
				return n;
			}
		});

		/* RECORD/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"field"},SECTION_PREFIX+"field",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=new Field((Record)parent,section);
				((Record)parent).addField(f);
				((Record)parent).addAllField(f);
				
				String is_chooser=(String)section.getValue("/@chooser");
				if(is_chooser!=null && ("1".equals(is_chooser) || "yes".equals(is_chooser.toLowerCase())))
					f.setType("chooser");
				return f;
			}
		});	
		
		
		/* RECORD/structures/structure -> STRUCTURE */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"structures","structure"},SECTION_PREFIX+"structure",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Structure s=new Structure((Record)parent,section);
				((Record)parent).addStructure(s);
				return s;
			}
		});	


		/* STRUCTURE/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"structure",new String[]{"view","sidebar","repeat"},SECTION_PREFIX+"repeat",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Structure)parent,section);
				((Structure)parent).addSideBar(r);
				return r;
			}
		});		
		//
		/* FIELD/options/option -> OPTION */
		rules.addRule(SECTION_PREFIX+"structure",new String[]{"view","hierarchy-section", "options", "option"},SECTION_PREFIX+"option",null,new Target(){
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
		rules.addRule(SECTION_PREFIX+"structure",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Structure)parent,section);
				((Structure)parent).addField(r);
				((Structure)parent).addAllField(r);
				return r;
			}
		});

		/* RECORD/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Record)parent,section);
				((Record)parent).addAllField(r);
				((Record)parent).addField(r);
				return r;
			}
		});


		/* REPEAT/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"repeat",new String[]{"field"},SECTION_PREFIX+"field",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=new Field((Repeat)parent,section);
				f.getRecord().addAllField(f);
				f.getRecord().addField(f,false);
				((Repeat)parent).addChild(f);
				return f;
			}
		});


		/* RECORD/group -> GROUP */
		rules.addRule(SECTION_PREFIX+"uisection",new String[]{"group"},SECTION_PREFIX+"group",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Group r=new Group((Record)parent,section);
				((Record)parent).addAllField(r);
				((Record)parent).addField(r);
				return r;
			}
		});
		/* GROUP/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"group",new String[]{"field"},SECTION_PREFIX+"field",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=new Field((Group)parent,section);
				f.getRecord().addAllField(f);
				f.getRecord().addField(f,false);
				((Group)parent).addChild(f);
				return f;
			}
		});
		/* GROUP/group -> GROUP */
		rules.addRule(SECTION_PREFIX+"group",new String[]{"group"},SECTION_PREFIX+"group",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Group r=new Group((Group)parent,section);
				((Group)parent).addChild(r);
				return r;
			}
		});
		/* REPEAT/group -> GROUP */
		rules.addRule(SECTION_PREFIX+"repeat",new String[]{"group"},SECTION_PREFIX+"group",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Group r=new Group((Repeat)parent,section);
				((Repeat)parent).addChild(r);
				return r;
			}
		});
		/* GROUP/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"group",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Repeat)parent,section);
				((Group)parent).addChild(r);
				return r;
			}
		});


		/* REPEAT/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"repeat",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Repeat)parent,section);
				((Repeat)parent).addChild(r);
				return r;
			}
		});
		

		/* FIELD/options/option -> OPTION */
		rules.addRule(SECTION_PREFIX+"field",new String[]{"options","option"},SECTION_PREFIX+"option",null,new Target(){
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
		rules.addRule(SECTION_PREFIX+"field",new String[]{"merges","merge"},SECTION_PREFIX+"merge",null,new Target(){
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
	public Boolean hasRecordByWebUrl(String url){ return records_by_web_url.containsKey(url);}
	public Record getRecord(String id) { return records.get(id); }
	public Record getRecordByWebUrl(String url) { return records_by_web_url.get(url); }
	public Record getRecordByServicesUrl(String url) { return records_by_services_url.get(url); }
	public Record[] getAllRecords() { return records.values().toArray(new Record[0]); }
	
	public void addInstance(Instance n) {
		instances.put(n.getID(),n);
	}
	public void addStructure(Structure s) {
		structure.put(s.getID(),s);
	}

	public Instance getInstance(String id) { return instances.get(id); }
	public Structure getStructure(String id) { return structure.get(id); }
	
	public void config_finish() throws CSPDependencyException {
		if(!required_version.equals(version))
			throw new CSPDependencyException("Config is out of date: require="+required_version+" got="+version);
		for(Record r : records.values()) {
			r.config_finish(this);
		}
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
		for(Record r : records.values())
			r.dump(out);
		return out.toString();
	}
	

	public JSONObject dumpJson() throws JSONException {

		JSONObject out=new JSONObject();
		ed.dumpJson(out);
		adminData.dumpJson(out);
		//for(Record r : records.values())
		//	r.dumpJson(out);
		return out;
	
	}
}
