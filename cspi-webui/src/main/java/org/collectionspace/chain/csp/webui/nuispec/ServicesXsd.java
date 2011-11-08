/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.nuispec;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ServicesXsd implements WebMethod {
	private static final Logger log = LoggerFactory
			.getLogger(ServicesXsd.class);
	protected Record record;
	protected Storage storage;
	protected String section;

	public ServicesXsd() {
	}

	public ServicesXsd(Record record, String section) {
		this.record = record;
		this.section = section;
	}

	private void generateRepeat(Repeat r, Element root, Namespace ns,
			String listName) {

		if (r.hasServicesParent()) {
			Element sequenced = null;
			for (String path : r.getServicesParent()) {
				if (path != null) {
					if (null != sequenced) {
						Element dele = sequenced.addElement(new QName(
								"element", ns));
						dele.addAttribute("name", path);
						dele.addAttribute("type", path);
						dele.addAttribute("minOccurs", "0");
						dele.addAttribute("maxOccurs", "unbounded");
					}
					Element ele = root.addElement(new QName("complexType", ns));
					ele.addAttribute("name", path);
					sequenced = ele.addElement(new QName("sequence", ns));
				}
			}
			if (sequenced != null) {
				Element dele = sequenced.addElement(new QName("element", ns));
				dele.addAttribute("name", r.getServicesTag());
				dele.addAttribute("type", r.getServicesTag());
				dele.addAttribute("minOccurs", "0");
				dele.addAttribute("maxOccurs", "unbounded");
			}
		}

		Element ele = root.addElement(new QName("complexType", ns));
		ele.addAttribute("name", listName);
		Element sele = ele.addElement(new QName("sequence", ns));
		for (FieldSet fs : r.getChildren("")) {
			generateDataEntry(sele, fs, ns, root, true);
		}

	}

	private void generateDataEntry(Element ele, FieldSet fs, Namespace ns,
			Element root, Boolean isRepeat) {
		if (fs instanceof Field) {
			// <xs:element name="csid" type="xs:string"/>
			Element field = ele.addElement(new QName("element", ns));
			field.addAttribute("name", fs.getServicesTag());
			field.addAttribute("type", "xs:string");
			if(isRepeat){
				field.addAttribute("minOccurs", "0");
				field.addAttribute("maxOccurs", "unbounded");
			}
		}
		if (fs instanceof Repeat) {
			Repeat rfs = (Repeat) fs;
			String listName = rfs.getServicesTag();
			if (rfs.hasServicesParent()) {
				// group repeatable
				// <xs:element name="objectNameList" type="ns:objectNameList"/>
				Element field = ele.addElement(new QName("element", ns));
				field.addAttribute("name", rfs.getServicesParent()[0]);
				Namespace groupns = new Namespace("ns", "");
				field.addAttribute("type", "ns:" + rfs.getServicesParent()[0]);
			} else {
				// single repeatable
				// <xs:element name="responsibleDepartments"
				// type="responsibleDepartmentList"/>
				Element field = ele.addElement(new QName("element", ns));
				field.addAttribute("name", rfs.getServicesTag());

				listName = rfs.getChildren("")[0].getServicesTag() + "List";
				field.addAttribute("type", listName);

			}
			generateRepeat(rfs, root, ns, listName);
		}

	}

	@SuppressWarnings("null")
	private void generateSearchList(Element root, Namespace ns) {

		Element ele = root.addElement(new QName("complexType", ns));
		ele.addAttribute("name", "abstractCommonList");
		Element aele = ele.addElement(new QName("annotation", ns));
		Element appele = aele.addElement(new QName("appinfo", ns));
		Element jxb = appele.addElement(new QName("class", new Namespace(
				"jaxb", "")));
		jxb.addAttribute("ref",
				"org.collectionspace.services.jaxb.AbstractCommonList");

		String[] listpath = record.getServicesListPath().split("/");

		Element lele = root.addElement(new QName("element", ns));
		lele.addAttribute("name", listpath[0]);
		Element clele = lele.addElement(new QName("complexType", ns));
		Element cplele = clele.addElement(new QName("complexContent", ns));
		Element exlele = cplele.addElement(new QName("extension", ns));
		exlele.addAttribute("base", "abstractCommmonList");
		Element sexlele = exlele.addElement(new QName("sequence", ns));
		Element slele = sexlele.addElement(new QName("element", ns));
		slele.addAttribute("name", listpath[1]);
		slele.addAttribute("maxOccurs", "unbounded");
		Element cslele = slele.addElement(new QName("complexType", ns));
		Element scslele = cslele.addElement(new QName("sequence", ns));

		Set<String> searchflds = new HashSet();
		for (String minis : record.getAllMiniDataSets()) {
			if (minis != null && !minis.equals("")) {
				for (FieldSet flds : record.getMiniDataSetByName(minis)) {
					searchflds.add(flds.getServicesTag());
					log.info(flds.getServicesTag());
				}
			}
		}
		Iterator iter = searchflds.iterator();
		while (iter.hasNext()) {
			Element sfld = scslele.addElement(new QName("element", ns));
			sfld.addAttribute("name", (String) iter.next());
			sfld.addAttribute("type", "xs:string");
			sfld.addAttribute("minOccurs", "1");

		}

		/*standard fields */

		Element stfld1 = scslele.addElement(new QName("element", ns));
		stfld1.addAttribute("name", "uri");
		stfld1.addAttribute("type", "xs:string");
		stfld1.addAttribute("minOccurs", "1");
		
		Element stfld2 = scslele.addElement(new QName("element", ns));
		stfld2.addAttribute("name", "csid");
		stfld2.addAttribute("type", "xs:string");
		stfld2.addAttribute("minOccurs", "1");
	}

	private String serviceschema(Storage s, String path) throws UIException {
		if(path != null){
			section = path;
		}

		Document doc = DocumentFactory.getInstance().createDocument();
		Namespace ns = new Namespace("xs", "http://www.w3.org/2001/XMLSchema");
		String[] parts = record.getServicesRecordPath(section).split(":", 2);
		String[] rootel = parts[1].split(",");
		Element root = doc.addElement(new QName("schema", new Namespace("xs",
				"http://www.w3.org/2001/XMLSchema")));
		root.addAttribute("xmlns:ns", rootel[0]);
		root.addAttribute("xmlns", rootel[0]);
		root.addAttribute("targetNamespace", rootel[0]);
		root.addAttribute("version", "0.1");

//		Element ele = root.addElement(new QName("element", ns));
//		ele.addAttribute("name", rootel[1]);
//		Element cele = ele.addElement(new QName("complexType", ns));

		// add toplevel items

		for (FieldSet fs : record.getAllFieldTopLevel("")) {
			generateDataEntry(root, fs, ns, root, false);
		}

		generateSearchList(root, ns);
		// log.info(doc.asXML());
		// return doc.asXML();

		return doc.asXML();

	}

	@Override
	public void configure(WebUI ui, Spec spec) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		Request q = (Request) in;
		String out = serviceschema(q.getStorage(),StringUtils.join(tail,"/"));
		q.getUIRequest().sendXMLResponse(out);

	}
}
