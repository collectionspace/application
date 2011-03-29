package org.collectionspace.chain.csp.webui.external;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.schema.Util;
import org.collectionspace.chain.csp.webui.main.WebUI;

/**
 * 	<mappings> 
 * 		<map> 
 * 			<url>/cspace-ui/html/cataloging.html</url>
 * 			<recordtype>procedure</recordtype> 
 * 			<file>/cspace-ui/html/record.html</file>
 * 			<configure> 
 * 				<title>CollectionSpace - {record/web-url}</title> 
 * 			</configure>
 * 		</map> 
 * 	</mappings>
 * 
 * @author csm22
 * 
 */
public class UIMapping {

	private String url, type, file;
	private Map<String, UIMeta> uimetaConfigs = new HashMap<String, UIMeta>();

	public UIMapping(WebUI webui, ReadOnlySection section) {

		url = Util.getStringOrDefault(section, "/url", "");
		type = Util.getStringOrDefault(section, "/recordtype", "");
		file = Util.getStringOrDefault(section, "/file", "");

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
		return file;
	}

	public String getUrl() {
		return url;
	}

}
