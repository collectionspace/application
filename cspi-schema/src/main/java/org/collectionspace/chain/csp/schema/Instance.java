package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;
/**
 * 
 * @author caret
 * used when creating authority records e.g. person,organization
 * 
 */
public class Instance {
	private Record record;
	private String id,title,title_ref,web_url,type;
	private Map<String,Option> options=new HashMap<String,Option>();
	private List<Option> options_list=new ArrayList<Option>();

	
	public Instance(Record record,ReadOnlySection section) {
		this.record=record;
		id=(String)section.getValue("/@id");
		title=Util.getStringOrDefault(section,"/title",id);
		title_ref=Util.getStringOrDefault(section,"/title-ref",id);		
		web_url=Util.getStringOrDefault(section,"/web-url",id);
		type=Util.getStringOrDefault(section,"/@ui-type","plain");
		
	}
	
	public Record getRecord() { return record; }
	public String getID() { return id; }
	public String getTitle() { return title; }
	public String getTitleRef() { return title_ref; }
	public String getWebURL() { return web_url; }

	void addOption(String id,String name,String sample,boolean dfault) {
		Option opt=new Option(id,name,sample);
		if(dfault)
			opt.setDefault();
		options.put(id,opt);
		options_list.add(opt);
		if("plain".equals(type))
			type="dropdown";
	}
	public Option getOption(String id) { return options.get(id); }
	public Option[] getAllOptions() { return options_list.toArray(new Option[0]); }
	
	
	public void config_finish(Spec spec) {}
}
