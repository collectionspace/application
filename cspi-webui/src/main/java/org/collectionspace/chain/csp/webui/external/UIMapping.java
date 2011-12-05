package org.collectionspace.chain.csp.webui.external;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Util;
import org.collectionspace.chain.csp.webui.main.WebUI;

/**
 * 	<mappings> 
 * 		<map> 
 * 			<url>/cspace-ui/html/cataloging.html</url>
 * 			<recordtype>procedure</recordtype> 
 * 			<file>/cspace-ui/html/record.html</file>
 * 			<configure> 
 *				<meta id="recordType">{record/web-url}</meta>
 * 			</configure>
 * 		</map> 
 * 	</mappings>
 * 
 * @author csm22
 * 
 */
public class UIMapping {

	private String url, type, file, configfile, recordfile;
	private Map<String, UIMeta> uimetaConfigs = new HashMap<String, UIMeta>();
	private Record r;
	private Instance ins;

	public UIMapping(WebUI webui, ReadOnlySection section) {

		url = Util.getStringOrDefault(section, "/url", "");
		type = Util.getStringOrDefault(section, "/recordtype", "");
		recordfile = Util.getStringOrDefault(section, "/file", "");
		configfile = Util.getStringOrDefault(section, "/configfile", "");
		
	}
	
	public void addMetaConfig(UIMeta metaconfig){
		uimetaConfigs.put(metaconfig.getId(), metaconfig);
	}

	public boolean hasMetaConfig(){
		if(uimetaConfigs.isEmpty()){
			return false;
		}
		return true;
	}
	
	public boolean hasMetaConfig(String name){
		if(uimetaConfigs.containsKey(name)){
			return true;
		}
		return false;
	}

	public UIMeta getMetaConfig(String name){
		if( uimetaConfigs.containsKey(name)){
			return uimetaConfigs.get(name);
		}
		return null;
	}
	
	public String[] getAllMetaConfigs(){

		if (uimetaConfigs.isEmpty()) {
			return new String[0];
		}
		return uimetaConfigs.keySet().toArray(new String[0]);
	}
	
	public Boolean hasFile() {
		if (file.equals("")) {
			return false;
		}
		return true;
	}

	public Boolean hasUrl() {
		if (url.equals("")) {
			return false;
		}
		return true;
	}
	public Boolean hasType() {
		if (type.equals("")) {
			return false;
		}
		return true;
	}

	public String getType() {
		return type;
	}

	public String getFile() {
		return recordfile;
	}
	public void setFile(String filed) {
		this.recordfile = filed;
	}
	public void setConfigFile(String filed) {
		this.configfile = filed;
	}
	public void setAsConfig(){
		this.file = this.configfile;
	}
	public void setAsRecord(){
		this.file = this.recordfile;
	}
	public void setRecord(Record r){
		this.r = r;
	}
	public Record getRecord(){
		return this.r;
	}
	public void setInstance(Instance ins){
		this.ins = ins;
	}
	public Instance getInstance(){
		return this.ins;
	}

	public String getUrl() {
		return url;
	}

}
