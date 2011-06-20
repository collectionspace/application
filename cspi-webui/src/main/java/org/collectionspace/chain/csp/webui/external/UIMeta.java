package org.collectionspace.chain.csp.webui.external;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;

/**
 * <configure> <meta id="title">CollectionSpace - {record/web-url}</meta>
 * </configure>
 * 
 * @author csm22
 * 
 */
public class UIMeta {
	String id, value;

	public UIMeta(UIMapping parent, ReadOnlySection section) {
		id = (String) section.getValue("/@id");
		value = (String) section.getValue("");
	}

	public String getId() {
		return id;
	}
	
	public String getValue(){
		return value;
	}
	
	/**
	 * get field/record level data inorder to populate values correctly
	 * e.g.CollectionSpace - {record/web-url}
	 * @return
	 */
	public String parseValue(Record r, Instance ins){
		String testvalue = value;
		if(testvalue.contains("{record/web-url}")){
			testvalue = testvalue.replace("{record/web-url}", r.getWebURL());
		}
		if(testvalue.contains("{instance/web-url}")){
			testvalue = testvalue.replace("{instance/web-url}", ins.getWebURL());
		}
		
		return testvalue;
	}

	public UIMeta getMetaConfig() {
		return this;
	}
}
