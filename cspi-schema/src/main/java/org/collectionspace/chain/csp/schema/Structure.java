package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;
/**
 * Structures are used in creation of the UISPEC.
 * It allows for easy addition/removal of sections to the UIspec
 * It also will in future allow different UIspecs for different output formats e.g. print vs screen.
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
			  			<selector>.csc-user-userList-row</selector>
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

	private Record record;
	private String id,titlebar, sidebar, listsection, editsection, editselector;
	private Boolean showtitlebar,showsidebar, showlistsection, showeditsection;

	private Map<String,FieldSet> fields=new HashMap<String,FieldSet>();
	
	public Structure(Record record,ReadOnlySection section) {
		this.record=record;
		id=(String)section.getValue("/@id");
		showtitlebar=Util.getBooleanOrDefault(section,"/view/titlebar/@show",true);
		showsidebar=Util.getBooleanOrDefault(section,"/view/sidebar/@show",true);
		showeditsection=Util.getBooleanOrDefault(section,"/view/edit-section/@show",true);
		showlistsection=Util.getBooleanOrDefault(section,"/view/list-section/@show",false);
		
		titlebar=Util.getStringOrDefault(section, "/view/titlebar", "titlebar");
		sidebar=Util.getStringOrDefault(section, "/view/sidebar", "sidebar");
		editsection=Util.getStringOrDefault(section, "/view/edit-section/@id", "recordEditor");
		listsection=Util.getStringOrDefault(section, "/view/list-section/@id", "list");
		
	}
	
	public Record getRecord() { return record; }
	public String getID() { return id; }

	public void addField(FieldSet f) {
		fields.put(f.getID(),f);
	}

	public FieldSet[] getAllFields() { return fields.values().toArray(new FieldSet[0]); }
	public FieldSet getField(String id) { return fields.get(id); }
	
	public Boolean showTitleBar() { return showtitlebar; }
	public Boolean showSideBar() { return showsidebar; }
	public Boolean showEditSection() { return showeditsection; }
	public Boolean showListSection() { return showlistsection; }
	
	public String getTitleBar() { return titlebar; }
	public String getSideBar() { return sidebar; }
	public String getListSectionName() { return listsection; }
	public String getEditSectionName() { return editsection; }
	
	public void config_finish(Spec spec) {}
}
