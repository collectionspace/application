/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.services.common.api.RefName;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlJsonConversion {
	private static final Logger log=LoggerFactory.getLogger(XmlJsonConversion.class);
	private static void addFieldToXml(Element root,Field field,JSONObject in, String permlevel) throws JSONException, UnderlyingStorageException {
		if (field.isServicesReadOnly()) {
			// Omit fields that are read-only in the services layer.
			log.debug("Omitting services-readonly field: " + field.getID());
			return;
		}
		
		Element element=root;

		if(field.getUIType().startsWith("groupfield") && field.getUIType().contains("selfrenderer")){
//ignore the top level if this is a self renderer as the UI needs it but the services doesn't
		}
		else{
			element=root.addElement(field.getServicesTag());
		}
		String value=in.optString(field.getID());
		if(field.getUIType().startsWith("groupfield")){
			if(field.getUIType().contains("selfrenderer")){
				addSubRecordToXml(element, field, in, permlevel);
			}
			else{
				if(in.has(field.getID())){
					addSubRecordToXml(element, field, in.getJSONObject(field.getID()), permlevel);
				}
			}
		} else if(field.getDataType().equals("boolean")){
			// Rather than dump what we have coming in, first convert to proper boolean and back.
			// Properly handles null, in particular.
			boolean bool = Boolean.parseBoolean(value);
			element.addText(Boolean.toString(bool));
		}else{
			element.addText(value);
		}
	}
	
	private static void addSubRecordToXml(Element root,Field field,JSONObject in, String operation) throws JSONException, UnderlyingStorageException{
		String parts[] = field.getUIType().split("/");
		Record subitems = field.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

		//hard coding the section here can not be a good thing....
		if(subitems.getAllServiceFieldTopLevel(operation,"common").length >0){
			for(FieldSet f : subitems.getAllServiceFieldTopLevel(operation,"common")) {
				addFieldSetToXml(root,f,in,"common",operation);
			}
			//log.debug(root.asXML());
			//return doc;
		}
		
	}
	
	//XXX could refactor this and addRepeatToXML as this is what happens in the middle of addRepeatToXML
	private static void addGroupToXml(Element root, Group group, JSONObject in, String section, String permlevel) throws JSONException, UnderlyingStorageException{
		if (group.isServicesReadOnly()) {
			// Omit fields that are read-only in the services layer.
			log.debug("Omitting services-readonly group: " + group.getID());
			return;
		}
		
		Element element=root;
		
		if(group.hasServicesParent()){
			for(String path : group.getServicesParent()){
				if(path !=null){
					element=element.addElement(path);
				}
			}
		}

		Object value=null;
		value=in.opt(group.getID());
		
		if(value==null || ((value instanceof String) && StringUtils.isBlank((String)value)))
			return;
		if(value instanceof String) { // And sometimes the services ahead of the UI
			JSONObject next=new JSONObject();
			next.put(group.getID(),value);
			value=next;
		}
		if(!(value instanceof JSONObject))
			throw new UnderlyingStorageException("Bad JSON in repeated field: must be string or object for group field not an array - that would a repeat field");
		JSONObject object=(JSONObject)value;
		

		Element groupelement=element;

			groupelement=element.addElement(group.getServicesTag());
			Object one_value=object;
			if(one_value==null || ((one_value instanceof String) && StringUtils.isBlank((String)one_value)))
			{ //do nothing 
				
			}
			else if(one_value instanceof String) {
				// Assume it's just the first entry (useful if there's only one)
				FieldSet[] fs=group.getChildren(permlevel);
				if(fs.length<1){ //do nothing 
					
				}
				else{
					JSONObject d1=new JSONObject();
					d1.put(fs[0].getID(),one_value);
					addFieldSetToXml(groupelement,fs[0],d1,section,permlevel);
				}
			} else if(one_value instanceof JSONObject) {
				List<FieldSet> children =getChildrenWithGroupFields(group,permlevel);
				for(FieldSet fs : children)
					addFieldSetToXml(groupelement,fs,(JSONObject)one_value,section,permlevel);
			}
		element=groupelement;
	}
	
	private static void addRepeatToXml(Element root,Repeat repeat,JSONObject in,String section, String permlevel) throws JSONException, UnderlyingStorageException {
		if (repeat.isServicesReadOnly()) {
			// Omit fields that are read-only in the services layer.
			log.debug("Omitting services-readonly repeat: " + repeat.getID());
			return;
		}
		
		Element element=root;
		
		if(repeat.hasServicesParent()){
			for(String path : repeat.getServicesParent()){
				if(path !=null){
					element=element.addElement(path);
				}
			}
		}
		else if(!repeat.getXxxServicesNoRepeat()) {	// Sometimes the UI is ahead of the services layer
			element=root.addElement(repeat.getServicesTag());
		}
		Object value=null;
		if(repeat.getXxxUiNoRepeat()) { // and sometimes the Servcies ahead of teh UI
			FieldSet[] children=repeat.getChildren(permlevel);
			if(children.length==0)
				return;
			addFieldSetToXml(element,children[0],in,section,permlevel);
			return;
		} else {
			value=in.opt(repeat.getID());			
		}		
		
		if(value==null || ((value instanceof String) && StringUtils.isBlank((String)value)))
			return;
		if(value instanceof String) { // And sometimes the services ahead of the UI
			JSONArray next=new JSONArray();
			next.put(value);
			value=next;
		}
		if(!(value instanceof JSONArray))
			throw new UnderlyingStorageException("Bad JSON in repeated field: must be string or array for repeatable field"+repeat.getID());
		JSONArray array=(JSONArray)value;

		
		//reorder the list if it has a primary
		//XXX this will be changed when service layer accepts non-initial values as primary
		if(repeat.hasPrimary()){
			Stack<Object> orderedarray = new Stack<Object>();
			for(int i=0;i<array.length();i++) {
				Object one_value=array.get(i);
				if(one_value instanceof JSONObject) {
					if(((JSONObject) one_value).has("_primary")){
						if(((JSONObject) one_value).getBoolean("_primary")){
							orderedarray.add(0, one_value);
							continue;
						}
					}					
				}
				orderedarray.add(one_value);
			}
			JSONArray newarray = new JSONArray();
			int j=0;
			for(Object obj : orderedarray){
				newarray.put(j, obj);
				j++;
			}
			array = newarray;
		}
		Element repeatelement=element;
		for(int i=0;i<array.length();i++) {
			if(repeat.hasServicesParent()){
				repeatelement=element.addElement(repeat.getServicesTag());
			}
			Object one_value=array.get(i);
			if(one_value==null || ((one_value instanceof String) && StringUtils.isBlank((String)one_value)))
				continue;
			if(one_value instanceof String) {
				// Assume it's just the first entry (useful if there's only one)
				FieldSet[] fs=repeat.getChildren(permlevel);
				if(fs.length<1)
					continue;
				JSONObject d1=new JSONObject();
				d1.put(fs[0].getID(),one_value);
				addFieldSetToXml(repeatelement,fs[0],d1,section,permlevel);
			} else if(one_value instanceof JSONObject) {
				List<FieldSet> children =getChildrenWithGroupFields(repeat,permlevel);
				for(FieldSet fs : children)
					addFieldSetToXml(repeatelement,fs,(JSONObject)one_value,section,permlevel);
			}
		}
		element=repeatelement;
	}

	private static void addFieldSetToXml(Element root,FieldSet fs,JSONObject in,String section, String permlevel) throws JSONException, UnderlyingStorageException {
		if(!section.equals(fs.getSection()))
			return;
		if(fs instanceof Field)
			addFieldToXml(root,(Field)fs,in,permlevel);
		else if(fs instanceof Group){
			addGroupToXml(root,(Group)fs,in,section,permlevel);
		}
		else if(fs instanceof Repeat){
			addRepeatToXml(root,(Repeat)fs,in,section,permlevel);
		}
	}
	
	public static Document getXMLRelationship(Element[] listItems){
		Document doc=DocumentFactory.getInstance().createDocument();
		Element root=doc.addElement(new QName("relations-common-list",new Namespace("ns3","http://collectionspace.org/services/relation")));
		root.addNamespace("ns2", "http://collectionspace.org/services/jaxb");
		//<ns3:relations-common-list xmlns:ns3="http://collectionspace.org/services/relation" xmlns:ns2="http://collectionspace.org/services/jaxb">

		if(listItems != null){
			for(Element bitdoc : listItems){
				root.add(bitdoc);
			}
		}

		return doc;
		
	}
	public static Document convertToXml(Record r,JSONObject in,String section, String permtype, Boolean useInstance) throws JSONException, UnderlyingStorageException {
		if(!useInstance){
			return convertToXml( r, in, section,  permtype);
		}
		
		Document doc=DocumentFactory.getInstance().createDocument();
		String[] parts=r.getServicesRecordPath(section).split(":",2);
		if(useInstance){
			parts =r.getServicesInstancesPath(section).split(":",2);
		}
		String[] rootel=parts[1].split(",");
		Element root=doc.addElement(new QName(rootel[1],new Namespace("ns2",rootel[0])));

		Element element=root.addElement("displayName");
		element.addText(in.getString("displayName"));
		Element element2=root.addElement("shortIdentifier");
		element2.addText(in.getString("shortIdentifier"));
		if(in.has("vocabType")){
			Element element3=root.addElement("vocabType");
			element3.addText(in.getString("vocabType"));
		}
		
		return doc;
//yes I know hardcode is bad - but I need this out of the door today
		/*
		<ns2:personauthorities_common xmlns:ns2="http://collectionspace.org/services/person">
		<displayName>PAHMA Person Authority</displayName>
		<vocabType>PersonAuthority</vocabType>
		<shortIdentifier>pamha</shortIdentifier>
		</ns2:personauthorities_common>
		 */
		
	}
	//
	//section should be an array
	//
	public static Document convertToXml(Record r,JSONObject in,String section, String operation) throws JSONException, UnderlyingStorageException {
		Document doc=DocumentFactory.getInstance().createDocument();
                try {
                    String path = r.getServicesRecordPath(section);
                    if (path != null) {
                        String[] parts=path.split(":",2);
                        String[] rootel=parts[1].split(",");
                        Element root=doc.addElement(new QName(rootel[1],new Namespace("ns2",rootel[0])));
                        if(r.getAllServiceFieldTopLevel(operation,section).length >0){
                                for(FieldSet f : r.getAllServiceFieldTopLevel(operation,section)) {
                                        addFieldSetToXml(root,f,in,section,operation);
                                }
                                return doc;
                        }
                    } else {
                        // Revert to DEBUG after v4.0 testing
                        log.warn(String.format("Record %s lacks expected section %s", r.getRecordName(), section));
                    }
                } catch (Exception ex) {
                    log.debug("Error in XmlJsonConversion.convertToXml",ex);
                    throw ex;
                }
		return null;
	}
	private static String getDeUrned(Element  el, Field f) throws JSONException{
		if(f.hasAutocompleteInstance()){
			String deurned = getDeURNedValue(f, el.getText());
			return deurned;
		}
		return "";
	}
	
	private static List<Object> getComplexNodes(Element root,String context,String condition,String value,String extract) {
		List<Object> out=new ArrayList<Object>();
		for(Object n : root.selectNodes(context)) {
			if(!(n instanceof Element))
				continue;
			Element candidate=(Element)n;
			boolean match = false;
			for(Object n2 : candidate.selectNodes(condition)) {
				if(!(n2 instanceof Element))
					continue;
				if(value.equals(((Element)n2).getText()))
					match=true;
			}
			if(!match)
				continue;
			for(Object n3 : candidate.selectNodes(extract)) {
				if(!(n3 instanceof Element))
					continue;
				out.add(n3);
			}
		}		
		return out;
	}
	
	private static List<?> getNodes(Element root,String spec) {
		if(spec!=null && spec.length()>0 && spec.charAt(0)==';') {
			String[] parts=spec.split(";");
			return getComplexNodes(root,parts[1],parts[2],parts[3],parts[4]);
		} else
			return root.selectNodes(spec);
	}
	
	private static Element getFieldNodeEl(Element root,Field f){
		List<?> nodes=getNodes(root,f.getServicesTag());
		if(nodes.size()==0)
			return null;
		// XXX just add first
		Element el=(Element)nodes.get(0);
		return el;
	}

	public static String csid_value(String csid,String spec,String ims_url) {
		String[] parts = spec.split(";");
		if(parts.length<2)
			parts = new String[]{spec,""};
		if(parts.length<3)
			parts = new String[]{null,parts[0],parts[1]};
		String prefix1="";
		if("ims".equals(parts[0]) && ims_url!=null)
			prefix1=ims_url;
		return prefix1+parts[1]+csid+parts[2];
	}
	
	private static void addFieldToJson(JSONObject out,Element root,Field field, String operation, 
			JSONObject tempSon,String csid,String ims_url) throws JSONException {
		String use_csid=field.useCsid();
		if(use_csid!=null) {
			if(field.useCsidField()!=null){
				csid = tempSon.getString(field.useCsidField());
			}
			out.put(field.getID(),csid_value(csid,field.useCsid(),ims_url));			
		} else {
			Element el = root;
			if(field.getUIType().startsWith("groupfield") && field.getUIType().contains("selfrenderer")){

				String parts[] = field.getUIType().split("/");
				Record subitems = field.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

				for(FieldSet fs : subitems.getAllServiceFieldTopLevel(operation,"common")) {
					addFieldSetToJson(out,el,fs,operation, tempSon,csid,ims_url);
				}

				return;
			}
			
			el=getFieldNodeEl(root,field);
			
			if(el == null){
				return;
			}
			addExtraToJson(out, el, field, tempSon);
			Object val = el.getText();

			if(field.getUIType().startsWith("groupfield")){
				String parts[] = field.getUIType().split("/");
				Record subitems = field.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

				JSONObject temp = new JSONObject();
				for(FieldSet fs : subitems.getAllServiceFieldTopLevel(operation,"common")) {
					addFieldSetToJson(temp,el,fs,operation, tempSon,csid,ims_url);
				}

				out.put(field.getID(),temp);
			}
			else if(field.getDataType().equals("boolean")){
				out.put(field.getID(),(Boolean.parseBoolean((String)val)?true:false));
			} else {
				out.put(field.getID(),val);
			}
	
			tempSon = addtemp(tempSon, field.getID(), val);
		}
	}
	
	// Fields that have an autocomplete tag, should also have a sibling with the
	// de-urned version of the urn to display nicely
	private static void addExtraToJson(JSONObject out, Element el, Field f, JSONObject tempSon)
			throws JSONException {
		String deurned = getDeUrned(el, f);
		if (deurned != "") {
			tempSon = addtemp(tempSon, f.getID(), deurned);
			out.put("de-urned-" + f.getID(), deurned);
		}
	}

	private static String getDeURNedValue(Field f, String urn) throws JSONException {
		//add a field with the de-urned version of the urn
		if(urn.isEmpty() || urn == null){
			return "";
		}
		//support multiassign of autocomplete instances
		for ( Instance ins : f.getAllAutocompleteInstances() ){
			if(ins !=null){ // this authority hasn't been implemented yet
		        RefName.AuthorityItem itemParsed = RefName.AuthorityItem.parse(urn);
		        if(itemParsed!=null){
		        	return itemParsed.displayName;
		        }
			}
			
		}
		return "";
	}

	private static void buildFieldList(List<String> list,FieldSet f, String permlevel) {
		if(f instanceof Repeat){
			list.add(f.getID());
			for(FieldSet a : ((Repeat)f).getChildren(permlevel)){
				//if(a instanceof Repeat)
				//	list.add(a.getID());
				if(a instanceof Field){
					list.add(a.getID());
				}
				buildFieldList(list,a,permlevel);
			}
		}
		if(f instanceof Field){
			list.add(f.getID());
		}
	}
	
	/* Repeat syntax is challenging for dom4j */
	private static List<Map<String, List <Element> >> extractRepeats(Element container,FieldSet f, String permlevel) {
		List<Map<String,List<Element>>> out=new ArrayList<Map<String,List<Element>>>();
		// Build index so that we can see when we return to the start
		List<String> fields=new ArrayList<String>();
		List<Element> repeatdatatypestuff = new ArrayList<Element>();
		buildFieldList(fields,f,permlevel);
		Map<String,Integer> field_index=new HashMap<String,Integer>();

		for(int i=0;i<fields.size();i++){
			field_index.put(fields.get(i),i);
		}
		// Iterate through
		Map<String, List <Element> > member=null;
		int prev=Integer.MAX_VALUE;
		for(Object node : container.selectNodes("*")) {

			if(!(node instanceof Element))
				continue;
			Integer next=field_index.get(((Element)node).getName());
			if(next==null)
				continue;
			if(next<prev) {
				// Must be a new instance
				if(member!=null)
					out.add(member);
				member=new HashMap<String, List <Element> >();
				repeatdatatypestuff = new ArrayList<Element>();
			}
			prev=next;
			repeatdatatypestuff.add((Element)node);
			member.put(((Element)node).getName(),repeatdatatypestuff);
		}
		if(member!=null)
			out.add(member);
		
		return out;
	}
	

	private static List<String> FieldListFROMConfig(FieldSet f, String operation) {
		List<String> children = new ArrayList<String>();
		
		if(f.getUIType().startsWith("groupfield")){
			String parts[] = f.getUIType().split("/");
			Record subitems = f.getRecord().getSpec().getRecordByServicesUrl(parts[1]);
			for(FieldSet fs : subitems.getAllFieldTopLevel(operation)) {
				children.add(fs.getID());
			}
		}
		
		if(f instanceof Repeat){
			for(FieldSet a : ((Repeat)f).getChildren(operation)){
				if(a instanceof Repeat && ((Repeat)a).hasServicesParent()){
					children.add(((Repeat)a).getServicesParent()[0]);
				}
				else if(a.getUIType().startsWith("groupfield")){
					//structuredates etc
					String parts[] = a.getUIType().split("/");
					Record subitems = a.getRecord().getSpec().getRecordByServicesUrl(parts[1]);
					
					if(a instanceof Group){
						if(((Group)a).getXxxServicesNoRepeat()){
							for(FieldSet fs : subitems.getAllFieldTopLevel(operation)) {
								if(fs instanceof Repeat && ((Repeat)fs).hasServicesParent()){
									children.add(((Repeat)fs).getServicesParent()[0]);
								}
								else{
									children.add(fs.getID());
								}
							}
						}
						else{
							children.add(a.getID());
						}
					}
				}
				else{
					children.add(a.getID());
				}
			}
		}
		if(f instanceof Group){
			for(FieldSet ab : ((Group)f).getChildren(operation)){
				children.add(ab.getID());
			}
		}
		if(f instanceof Field){
			
		}
		return children;
	}
	
	/* Repeat syntax is challenging for dom4j */
	private static JSONArray extractRepeatData(Element container,FieldSet f, String permlevel) throws JSONException {
		JSONArray newout = new JSONArray();
		// Build index so that we can see when we return to the start
		List<String> fields = FieldListFROMConfig(f,permlevel);
		
		Map<String,Integer> field_index=new HashMap<String,Integer>();
		
		for(int i=0;i<fields.size();i++){
			field_index.put(fields.get(i),i);
		}
		JSONObject test = new JSONObject();
		JSONArray testarray = new JSONArray();
		// Iterate through
		Integer prev=Integer.MAX_VALUE;
		for(Object node : container.selectNodes("*")) {
			if(!(node instanceof Element))
				continue;
			Integer next=field_index.get(((Element)node).getName());
			if(next==null)
				continue;
			if(next!=prev) {
				// Must be a new instance
				if(test.length()>0){
					newout.put(test);
				}
				test = new JSONObject();
				testarray = new JSONArray();
			}
			prev=next;
			testarray.put((Element)node);
			test.put(((Element)node).getName(), testarray);
		}
		if(test.length()>0){
			newout.put(test);
		}
		return newout;
	}
	
	private static JSONObject addtemp(JSONObject temp, String id, Object text) throws JSONException{
		if(!temp.has(id)){
			temp.put(id,text);
		}
		return temp;
	}
	//merges in the pseudo sub records 'groupfields' with the normal fields unless they need to be nested
	private static List<FieldSet> getChildrenWithGroupFields(Repeat parent, String operation){
		List<FieldSet> children = new ArrayList<FieldSet>();

		if(parent.getUIType().startsWith("groupfield")){
			String parts[] = parent.getUIType().split("/");
			Record subitems = parent.getRecord().getSpec().getRecordByServicesUrl(parts[1]);
			for(FieldSet fd : subitems.getAllFieldTopLevel(operation)) {
				children.add(fd);
			}
		}
		
		
		for(FieldSet fs : parent.getChildren(operation)) {

			if(fs.getUIType().startsWith("groupfield")){
				String parts[] = fs.getUIType().split("/");
				Record subitems = fs.getRecord().getSpec().getRecordByServicesUrl(parts[1]);
				
				if(fs instanceof Group){
					if(((Group)fs).getXxxServicesNoRepeat()){
						for(FieldSet fd : subitems.getAllFieldTopLevel(operation)) {
							children.add(fd); //non-nested groupfields?
						}
					}
					else{
						//this one should be nested
						children.add(fs);
					}
				}
				else{
					for(FieldSet fd : subitems.getAllFieldTopLevel(operation)) {
						children.add(fd); //what about nested groupfields?
					}
				}
			}
			else{
				children.add(fs);
			}
		}
		return children;
	}
	
	private static JSONArray addRepeatedNodeToJson(Element container,Repeat f, String permlevel, JSONObject tempSon) throws JSONException {
		JSONArray node = new JSONArray();
		List<FieldSet> children =getChildrenWithGroupFields(f, permlevel);
		JSONArray elementlist=extractRepeatData(container,f,permlevel);

		JSONObject siblingitem = new JSONObject();
		for(int i=0;i<elementlist.length();i++){
			JSONObject element = elementlist.getJSONObject(i);


			Iterator<?> rit=element.keys();
			while(rit.hasNext()) {
				String key=(String)rit.next();
				JSONArray arrvalue = new JSONArray();
				for(FieldSet fs : children) {

					if(fs instanceof Repeat && ((Repeat)fs).hasServicesParent()){
						if(!((Repeat)fs).getServicesParent()[0].equals(key)){
							continue;
						}
						Object value = element.get(key);
						arrvalue = (JSONArray)value;
					}
					else{
						if(!fs.getID().equals(key)){
							continue;
						}
						Object value = element.get(key);
						arrvalue = (JSONArray)value;
					} 

					if(fs instanceof Field) {
						for(int j=0;j<arrvalue.length();j++){
							JSONObject repeatitem = new JSONObject();
							//XXX remove when service layer supports primary tags
							if(f.hasPrimary() && j==0){
								repeatitem.put("_primary",true);
							}
							Element child = (Element)arrvalue.get(j);
							Object val = child.getText();
							Field field = (Field)fs;
							String id = field.getID();
							if(f.asSibling()){
								addExtraToJson(siblingitem, child, field, tempSon);
								if(field.getDataType().equals("boolean")){
									siblingitem.put(id,(Boolean.parseBoolean((String)val)?true:false));
								} else {
									siblingitem.put(id, val);
								}
							}
							else{
								addExtraToJson(repeatitem, child, field, tempSon);
								if(field.getDataType().equals("boolean")){
									repeatitem.put(id,(Boolean.parseBoolean((String)val)?true:false));
								} else {
									repeatitem.put(id, val);
								}
								node.put(repeatitem);
							}

							tempSon = addtemp(tempSon, fs.getID(), child.getText());
						}
					}
					else if(fs instanceof Group){
						JSONObject tout = new JSONObject();
						JSONObject tempSon2 = new JSONObject();
						Group rp = (Group)fs;
						addRepeatToJson(tout, container, rp, permlevel, tempSon2, "", "") ;

						if(f.asSibling()){
							JSONArray a1 = tout.getJSONArray(rp.getID());
							JSONObject o1 = a1.getJSONObject(0);
							siblingitem.put(fs.getID(), o1);
						}
						else{
							JSONObject repeatitem = new JSONObject();
							repeatitem.put(fs.getID(), tout.getJSONArray(rp.getID()));
							node.put(repeatitem);
						}

						tempSon = addtemp(tempSon, rp.getID(), tout.getJSONArray(rp.getID()));
						//log.info(f.getID()+":"+rp.getID()+":"+tempSon.toString());
					}
					else if(fs instanceof Repeat){
						JSONObject tout = new JSONObject();
						JSONObject tempSon2 = new JSONObject();
						Repeat rp = (Repeat)fs;
						addRepeatToJson(tout, container, rp, permlevel, tempSon2, "", "") ;

						if(f.asSibling()){
							siblingitem.put(fs.getID(), tout.getJSONArray(rp.getID()));
						}
						else{
							JSONObject repeatitem = new JSONObject();
							repeatitem.put(fs.getID(), tout.getJSONArray(rp.getID()));
							node.put(repeatitem);
						}

						tempSon = addtemp(tempSon, rp.getID(), tout.getJSONArray(rp.getID()));
						//log.info(f.getID()+":"+rp.getID()+":"+tempSon.toString());
					}
				}
			}
		}
		 
		if(f.asSibling()){
			node.put(siblingitem);
		}
		return node;
	}
	
	private static void addGroupToJson(JSONObject out, Element root, Group f,String operation, JSONObject tempSon,String csid,String ims_url) throws JSONException{
		String nodeName = f.getServicesTag();
		if(f.hasServicesParent()){
			nodeName = f.getfullID();
			//XXX hack because of weird repeats in accountroles permroles etc
			if(f.getServicesParent().length==0){
				nodeName = f.getID();
			}
		}
		if(!f.isGrouped()){
			Element el = root;
			if(f.getUIType().startsWith("groupfield") && f.getUIType().contains("selfrenderer")){

				String parts[] = f.getUIType().split("/");
				Record subitems = f.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

				JSONObject temp = new JSONObject();
				for(FieldSet fs : subitems.getAllServiceFieldTopLevel(operation,"common")) {
					addFieldSetToJson(temp,el,fs,operation, tempSon,csid,ims_url);
				}

				out.put(f.getID(),temp);
				return;
			}
			
			if(f.getUIType().startsWith("groupfield")){
				String parts[] = f.getUIType().split("/");
				Record subitems = f.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

				JSONObject temp = new JSONObject();
				for(FieldSet fs : subitems.getAllServiceFieldTopLevel(operation,"common")) {
					addFieldSetToJson(temp,el,fs,operation, tempSon,csid,ims_url);
				}

				out.put(f.getID(),temp);
			}
		}
		else{
			List<?> nodes=root.selectNodes(nodeName);
			if(nodes.size()==0)
				return;
			
			// Only first element is important in group container
			for(Object repeatcontainer : nodes){
				Element container=(Element)repeatcontainer;
				JSONArray repeatitem = addRepeatedNodeToJson(container,f,operation,tempSon);
				JSONObject repeated = repeatitem.getJSONObject(0);
				out.put(f.getID(), repeated);
			}
		}
		
	}
	
	private static void addRepeatToJson(JSONObject out,Element root,Repeat f,String permlevel, JSONObject tempSon,String csid,String ims_url) throws JSONException {
		if(f.getXxxServicesNoRepeat()) { //not a repeat in services yet but is a repeat in UI
			FieldSet[] fields=f.getChildren(permlevel);
			if(fields.length==0)
				return;
			JSONArray members=new JSONArray();
			JSONObject data=new JSONObject();
			addFieldSetToJson(data,root,fields[0],permlevel, tempSon,csid,ims_url);
			members.put(data);
			out.put(f.getID(),members);
			return;
		}
		String nodeName = f.getServicesTag();
		if(f.hasServicesParent()){
			nodeName = f.getfullID();
			//XXX hack because of weird repeats in accountroles permroles etc
			if(f.getServicesParent().length==0){
				nodeName = f.getID();
			}
		}
		List<?> nodes=root.selectNodes(nodeName);
		if(nodes.size()==0){// add in empty primary tags and arrays etc to help UI
			if(f.asSibling()){
				JSONObject repeated = new JSONObject();
				if(f.hasPrimary()){
					repeated.put("_primary",true);
				}
				if(!out.has(f.getID())){
					JSONArray temp = new JSONArray();
					out.put(f.getID(), temp);
				}
				out.getJSONArray(f.getID()).put(repeated);
			}
			else{
				JSONArray repeatitem = new JSONArray();
				out.put(f.getID(), repeatitem);
			}
			return;
		}
		
		
		// Only first element is important in container
		//except when we have repeating items
		int pos = 0;
		for(Object repeatcontainer : nodes){
			pos++;
			Element container=(Element)repeatcontainer;
			if(f.asSibling()){
				JSONArray repeatitem = addRepeatedNodeToJson(container,f,permlevel,tempSon);
				JSONArray temp = new JSONArray();
				if(!out.has(f.getID())){
					out.put(f.getID(), temp);
				}
				for(int arraysize=0 ;arraysize< repeatitem.length(); arraysize++){
					JSONObject repeated = repeatitem.getJSONObject(arraysize);

					if(f.hasPrimary() && pos==1){
						repeated.put("_primary",true);
					}
					out.getJSONArray(f.getID()).put(repeated);
				}
			}
			else{
				JSONArray repeatitem = addRepeatedNodeToJson(container,f,permlevel,tempSon);
				out.put(f.getID(), repeatitem);
			}
		}
	}
	
	private static void addFieldSetToJson(JSONObject out,Element root,FieldSet fs,String permlevel, JSONObject tempSon,String csid,String ims_url) throws JSONException {
		if(fs instanceof Field)
			addFieldToJson(out,root,(Field)fs,permlevel, tempSon,csid,ims_url);
		else if(fs instanceof Group)
			addGroupToJson(out,root,(Group)fs,permlevel,tempSon,csid,ims_url);
		else if(fs instanceof Repeat)
			addRepeatToJson(out,root,(Repeat)fs,permlevel, tempSon,csid,ims_url);
	}
	
	public static void convertToJson(JSONObject out,Record r,Document doc, String operation, String section,String csid,String ims_url) throws JSONException {
		Element root=doc.getRootElement();
		JSONObject tempSon = new JSONObject();
		for(FieldSet f : r.getAllServiceFieldTopLevel(operation,section)) {
			addFieldSetToJson(out,root,f,operation, tempSon,csid,ims_url);
		}
		
		if(r.hasMerged()){
			for(FieldSet f : r.getAllMergedFields()){
				for(String fm : f.getAllMerge()){
					if (fm != null) {
						if (r.hasFieldByOperation(fm, operation)) {
							if (tempSon.has(fm)) {
								String data = tempSon.getString(fm);
								if (data != null && !data.equals("")
										&& !out.has(f.getID())) {
									out.put(f.getID(), data);
								}
							}
						}
					}
				}
			}
			
		}
	}

	public static JSONObject convertToJson(Record r,Document doc, String permlevel, String section,String csid,String ims_url) throws JSONException {
		JSONObject out=new JSONObject();
		convertToJson(out,r,doc,permlevel,section,csid,ims_url);
		return out;
	}
}