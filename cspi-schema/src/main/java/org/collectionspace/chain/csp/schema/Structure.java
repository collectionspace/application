/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.RuleSet;
import org.collectionspace.chain.csp.config.RuleTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Structures are used in creation of the UISPEC.
 * It allows for easy addition/removal of sections to the UIspec
 * It also will in future allow different UIspecs for different output formats e.g. print vs screen. tab vs main
 * 
 * The default is screen
 * if no structure area is explicitly defined then assumes:
 * titlebar = show with name "titleBar"
 * sidebar = show with name "sidebar"
 * edit-section = show with name "recordEditor"
 * list-section = hide with name "list"
 * 
 * 
<structures>
			  	<structure id="screen"> <!-- screen|print|blackberry etc -->
					<view>
			  			<titlebar show="false"></titlebar>
			  			<sidebar show="false"></sidebar>
			  			<edit-section id="userDetails"></edit-section>
			  			<list-section id="userList"></list-section>
					</view>
			  		<repeat id="userList">
			  			<selector>.csc-recordList-row</selector>
			  			<field id="name" ui-type="list">
			  				<selector>.csc-user-userList-name</selector>
			  			</field>
			  			<field id="status" ui-type="list">
			  				<selector>.csc-user-userList-status</selector>
			  			</field>
			  			<field id="csid" ui-type="list">
			  				<selector>.csc-user-userList-csid</selector>
			  			</field>
					</repeat>
			  		</list-section>
			  	</structure>
			  </structures>
 * @author csm22
 *
 */
public class Structure  implements FieldParent  {
	private static final Logger log=LoggerFactory.getLogger(Structure.class);

	private Record record;
	private String id,titlebar, sidebar, listsection, editsection, editselector, msgsection, hierarchysection;
	private Boolean showtitlebar = true,showsidebar = true, showlistsection = false, showhierarchysection = false, showeditsection = true, showmsgsection=false;
	public static String SECTION_PREFIX="org.collectionspace.app.config.structure.";
	public static String SPEC_ROOT=SECTION_PREFIX+"spec";

	private Set<String> option_default;
	private Map<String,Option> options=new HashMap<String,Option>();
	private List<Option> options_list=new ArrayList<Option>();

	private Map<String,FieldSet> fields=new HashMap<String,FieldSet>();
	private Map<String,FieldSet> repeatfields=new HashMap<String,FieldSet>();

	private Map<String,FieldSet> sidebar_sections=new HashMap<String,FieldSet>();
	private Map<String,FieldSet> mainarea_sections=new HashMap<String,FieldSet>();
	
	public Structure(Record record, String uid){
		this.record=record;
		id=uid;
		showtitlebar=true;
		showsidebar=true;
		showeditsection=true;
		showhierarchysection = false;
		showlistsection=false;
		showmsgsection=false;
		
		titlebar="titlebar";
		sidebar="sidebar";
		editsection="recordEditor";
		hierarchysection = "hierarchy";
		listsection="list";
		msgsection="messagekeys";
	}
	
	public Structure(Record record,ReadOnlySection section) {
		this.record=record;
		id=(String)section.getValue("/@id");
		showtitlebar=Util.getBooleanOrDefault(section,"/view/titlebar/@show",false);
		showsidebar=Util.getBooleanOrDefault(section,"/view/sidebar/@show",false);
		showhierarchysection=Util.getBooleanOrDefault(section,"/view/hierarchy-section/@show",false);
		showeditsection=Util.getBooleanOrDefault(section,"/view/edit-section/@show",true);
		showlistsection=Util.getBooleanOrDefault(section,"/view/list-section/@show",false);
		showmsgsection=Util.getBooleanOrDefault(section,"/view/label-section/@show",false);
		
		titlebar=Util.getStringOrDefault(section, "/view/titlebar", "titlebar");
		sidebar=Util.getStringOrDefault(section, "/view/sidebar", "sidebar");
		hierarchysection=Util.getStringOrDefault(section, "/view/hierarchy-section/@id", "hierarchy");
		editsection=Util.getStringOrDefault(section, "/view/edit-section/@id", "recordEditor");
		listsection=Util.getStringOrDefault(section, "/view/list-section/@id", "list");
		msgsection=Util.getStringOrDefault(section, "/view/label-section/@id", "messagekeys");
		
	}
	
	public Record getRecord() { return record; }
	public String getID() { return id; }

	public void addField(FieldSet f) {
		fields.put(f.getID(),f);
	}
	public void addAllField(FieldSet f) {
		repeatfields.put(f.getID(),f);
	}
	public void addSideBar(FieldSet f) {
		sidebar_sections.put(f.getID(),f);
	}
	public void addMainArea(FieldSet f){
		mainarea_sections.put(f.getID(),f);
	}
	
	public boolean isTrueRepeatField() {
		return false;
	}

	public FieldSet[] getAllFieldTopLevel() { return fields.values().toArray(new FieldSet[0]); }
	public FieldSet getFieldTopLevel(String id) { return fields.get(id); }
	public FieldSet[] getAllRepeatFields() { return repeatfields.values().toArray(new FieldSet[0]); }
	public FieldSet getRepeatField(String id) { return repeatfields.get(id); }
	public FieldSet getSideBarItems(String id) { return sidebar_sections.get(id); }
	public FieldSet getMainAreaItems(String id) { return mainarea_sections.get(id); }
	
	public Boolean showTitleBar() { return showtitlebar; }
	public Boolean showSideBar() { return showsidebar; }
	public Boolean showEditSection() { return showeditsection; }
	public Boolean showHierarchySection() { return showhierarchysection; }
	public Boolean showListSection() { return showlistsection; }
	public Boolean showMessageKey() { return showmsgsection; }
	
	public String getTitleBar() { return titlebar; }
	public String getSideBar() { return sidebar; }
	public String getListSectionName() { return listsection; }
	public String getHierarchySectionName() { return hierarchysection; }
	public String getEditSectionName() { return editsection; }
	public String getMessageSectionName() { return msgsection; }


	public void config_finish(Spec spec) {}


	@Override
	public String enumBlankValue(){
		return record.enumBlankValue();
	}
	
	public boolean isExpander() {
		// TODO Auto-generated method stub
		return false;
	}

	public FieldParent getParent() {
		return null;
	}
	//add hierarchical options
	public Option getOption(String id) { return options.get(id); }
	public Boolean hasOption(String id) { if(options.containsKey(id)){return true; }else{ return false;} }
	public Option[] getAllOptions() { return options_list.toArray(new Option[0]); }
	public void addOption(String id,String name,String sample,boolean dfault) {
		Option opt=new Option(id,name,sample);
		if(dfault){
			opt.setDefault();
			option_default.add(name);
		}
		options.put(id,opt);
		options_list.add(opt);
	}
	

}
