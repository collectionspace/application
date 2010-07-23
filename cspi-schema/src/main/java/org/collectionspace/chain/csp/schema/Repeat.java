package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;

// XXX only one level of repetition at the moment. Should only be a matter of type furtling.
public class Repeat implements FieldSet, FieldParent {
	private String id,selector,userecord, enum_blank,parentID;
	private Boolean is_visible;
	private FieldParent parent;
	private Set<String> enum_default;
	private List<FieldSet> children=new ArrayList<FieldSet>();
	private boolean enum_hasblank=true,exists_in_service=true, has_primary = false, xxx_services_no_repeat=false,xxx_ui_no_repeat=false,asSiblings=false;

	/* Services */
	private String services_tag,services_section;

	public Repeat(Record record,ReadOnlySection section) {
		this.parent=record;
		this.parentID = record.getID();
		this.initialiseVariables(section);
	}
	public Repeat(Structure structure,ReadOnlySection section) {
		this.parent=structure;
		this.parentID = structure.getID();
		this.initialiseVariables(section);
	}

	public Repeat(Repeat repeat, ReadOnlySection section) {
		this.parent=repeat;
		this.parentID = repeat.getID();
		this.initialiseVariables(section);
	}
	
	public Repeat(Subrecord subrecord, ReadOnlySection section) {
		this.parent=subrecord;
		this.parentID = subrecord.getID();
		this.initialiseVariables(section);
	}
	/**
	 * all constructors get variables initialised in the same way
	 * @param section
	 */
	private void initialiseVariables(ReadOnlySection section){
		this.id=(String)section.getValue("/@id");
		this.selector=Util.getStringOrDefault(section,"/selector",".csc-"+this.parentID+"-"+id);
		this.services_tag=Util.getStringOrDefault(section,"/services-tag",id);
		this.is_visible=Util.getBooleanOrDefault(section,"/@show",true);
		this.xxx_services_no_repeat=Util.getBooleanOrDefault(section,"/@xxx-services-no-repeat",false);
		this.xxx_ui_no_repeat=Util.getBooleanOrDefault(section,"/@xxx-ui-no-repeat",false);
		this.asSiblings = Util.getBooleanOrDefault(section,"/@asSibling",false);
		this.services_section=Util.getStringOrDefault(section,"/@section","common");
		this.exists_in_service = Util.getBooleanOrDefault(section, "/@exists-in-services", true);
		// should this field allow a primary flag
		this.has_primary = Util.getBooleanOrDefault(section, "/@has-primary", false);	
		this.userecord = Util.getStringOrDefault(section, "/@userecord", "");
		this.enum_default = Util.getSetOrDefault(section, "/enum/default", new String[]{""});
		this.enum_hasblank = Util.getBooleanOrDefault(section, "/enum/@has-blank",true);
		this.enum_blank = Util.getStringOrDefault(section, "/enum/blank-value", "Please select an item");
	}

	public String getID() { return id; }

	void addChild(FieldSet f) { children.add(f); }
	public FieldSet[] getChildren() { return children.toArray(new FieldSet[0]); }

	public Record getRecord() { return parent.getRecord(); }
	public String getSelector() { return selector; }
	public String getServicesTag() { return services_tag; }
	public boolean isInServices() {	return exists_in_service;	}
	public boolean getXxxServicesNoRepeat() { return xxx_services_no_repeat; }
	public boolean getXxxUiNoRepeat() { return xxx_ui_no_repeat; }
	public boolean isVisible() { return is_visible; }
	public boolean asSibling() { return asSiblings;}
	public boolean hasPrimary() {return has_primary;}
	public String getSection() { return services_section; }
	
	public boolean usesRecord(){ if(userecord != null && !userecord.equals("")){ return true; } return false;}
	public Record usesRecordId(){ if(usesRecord()){ return this.getRecord().getSpec().getRecord(userecord); } return null; }
	
	public String[] getIDPath() {
		if(xxx_ui_no_repeat) {
			if(parent instanceof Repeat) {
				return ((Repeat)parent).getIDPath();
			} else {
				return new String[]{};
			}
		} else {
			if(parent instanceof Repeat) {
				String[] pre=((Repeat)parent).getIDPath();
				String[] out=new String[pre.length+1];
				System.arraycopy(pre,0,out,0,pre.length);
				out[pre.length]=id;
				return out;
			} else {
				return new String[]{id};
			}
		}
	}

	public boolean hasEnumBlank(){ return enum_hasblank; }
	public String enumBlankValue(){ return enum_blank; }
	public boolean isEnumDefault(String name){
		if(enum_default.contains(name)){
			return true;
		}
		return false;
	}
	
	public void config_finish(Spec spec) {
		for(FieldSet child : children)
			child.config_finish(spec);
	}
}
