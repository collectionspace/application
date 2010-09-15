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

		/* SPEC/relationships -> RELATIONSHIPS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"relationships"},SECTION_PREFIX+"relationships",null,null);
		/* RELATIONSHIPS/relation -> RELATION(@id) */
		rules.addRule(SECTION_PREFIX+"relationships",new String[]{"relation"},SECTION_PREFIX+"relation",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Relationship r=new Relationship(Spec.this,section);
				relationships.put(r.getID(),r);
				return r;
			}
		});
		
		
		/* SPEC/records -> RECORDS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"records"},SECTION_PREFIX+"records",null,null);
		/* RECORDS/record -> RECORD(@id) */
		rules.addRule(SECTION_PREFIX+"records",new String[]{"record"},SECTION_PREFIX+"record",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Record r=new Record(Spec.this,section);
				records.put(r.getID(),r);
				records_by_web_url.put(r.getWebURL(),r);
				records_by_services_url.put(r.getServicesURL(),r);
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
		rules.addRule(SECTION_PREFIX+"record",new String[]{"field"},SECTION_PREFIX+"field",null,new Target(){
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
		rules.addRule(SECTION_PREFIX+"record",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new Target(){
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
				((Repeat)parent).addChild(f);
				return f;
			}
		});


		/* RECORD/group -> GROUP */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"group"},SECTION_PREFIX+"group",null,new Target(){
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
				Group r=new Group((Group)parent,section);
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
	public Relationship getRelation(String id) { return relationships.get(id); }
	public Relationship[] getAllRelations(){ return relationships.values().toArray(new Relationship[0]); }
	
	public Boolean hasRecord(String id){ if(records.containsKey(id)){return true;} else return false;}
	public Boolean hasRecordByServicesUrl(String url){ if(records_by_services_url.containsKey(url)){return true;} else return false;}
	public Boolean hasRecordByWebUrl(String url){ if(records_by_web_url.containsKey(url)){return true;} else return false;}
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
}
