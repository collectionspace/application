package org.collectionspace.chain.csp.schema;

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
 * within the rest of the xml the tag in-list-section will be used to determine which fields to show/hide - 
 * default is to show all.
 * <field id="screenName" in-list-section="yes|no">
 * 
 * 			  <structures>
 *			  	<structure id="screen"> <!-- screen|print|blackberry etc -->
 *			  		<titlebar show="false"></titlebar>
 *			  		<sidebar show="false"></sidebar>
 *			  		<edit-section>userDetails</edit-section>
 *			  		<list-section>userList</list-section>
 *			  	</structure>
 *			  </structures>
 * @author csm22
 *
 */
public class Structure {

	private Record record;
	private String id,titlebar, sidebar, listsection, editsection;
	private Boolean showtitlebar,showsidebar, showlistsection, showeditsection;
	
	public Structure(Record record,ReadOnlySection section) {
		this.record=record;
		id=(String)section.getValue("/@id");
		showtitlebar=Util.getBooleanOrDefault(section,"/titlebar/@show",false);
		showsidebar=Util.getBooleanOrDefault(section,"/sidebar/@show",false);
		showlistsection=Util.getBooleanOrDefault(section,"/edit-section/@show",false);
		showeditsection=Util.getBooleanOrDefault(section,"/list-section/@show",false);
		
		titlebar=Util.getStringOrDefault(section, "/titlebar", "titlebar");
		sidebar=Util.getStringOrDefault(section, "/sidebar", "sidebar");
		listsection=Util.getStringOrDefault(section, "/edit-section", "recordEditor");
		editsection=Util.getStringOrDefault(section, "/list-section", "list");
	}
	
	public Record getRecord() { return record; }
	public String getID() { return id; }
	

	public Boolean showTitleBar() { return showtitlebar; }
	public Boolean showSideBar() { return showsidebar; }
	public Boolean showEditSection() { return showeditsection; }
	public Boolean showListSection() { return showlistsection; }
	
	public String getTitleBar() { return titlebar; }
	public String getSideBar() { return sidebar; }
	public String getListSection() { return listsection; }
	public String getEditSection() { return editsection; }
	
	public void config_finish(Spec spec) {}
}
