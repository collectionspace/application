package org.collectionspace.chain.installation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.collectionspace.chain.csp.persistence.services.TenantSpec;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.chain.csp.schema.Spec;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MakeXsd {
	private static final Logger log = LoggerFactory.getLogger(MakeXsd.class);
	protected Record record;
	protected TenantSpec tenantSpec;
	protected Storage storage;
	protected String section;
	protected HashMap<String, Boolean> definedGroupFields = new HashMap<String, Boolean>();


	public MakeXsd(TenantSpec td, String section) {
		this.tenantSpec = td;
		this.section = section;
	}
	
	/*
	 * This method generates an XML Schema complex type definition for "ui-type" config attributes -e.g., "groupfield/structureddate"
	 */
	private String generateFieldGroup(FieldSet fieldSet, Element ele, Namespace ns,
			Element root) {
		String type = fieldSet.getServicesType();
		Spec spec = fieldSet.getRecord().getSpec();
		Record record = spec.getRecord(type);
		String servicesType = record.getServicesType();
		
		if (definedGroupFields.containsKey(servicesType) == false) {
			Element complexElement = ele.addElement(new QName("complexType", ns));
			complexElement.addAttribute("name", servicesType);
			Element sequenced = complexElement.addElement(new QName("sequence", ns));
			
			for (FieldSet subRecordFieldSet : record.getAllFieldTopLevel("")) {
				generateDataEntry(sequenced, subRecordFieldSet, ns, root, false);
			}
			definedGroupFields.put(servicesType, true); // We only need to define the complex type once.
		}
		
		return servicesType;
	}
	
	private void generateRepeat(Repeat r, Element fieldElement, String listName, Namespace ns,
			Element root) {

		if (r.hasServicesParent()) { // for example, something like this "assocPersonGroupList/assocPersonGroup" where the left side of the '/' char is the services parent
			Element sequenced = null;
			for (String path : r.getServicesParent()) {
				if (path != null) {
					if (null != sequenced) { // How can "sequenced" be anything BUT null at this point?  Looks like dead code inside this "if" clause
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
				String servicesTag = r.getServicesTag();
				dele.addAttribute("name", servicesTag);
				
				String servicesType = r.getServicesType();
				if (servicesType == null) {
					servicesType = servicesTag; // If the type was not explicitly set in the config, then use the servicesTag as the type
				} else {
					System.err.println();
				}
				dele.addAttribute("type", servicesType);
				
				dele.addAttribute("minOccurs", "0");
				dele.addAttribute("maxOccurs", "unbounded");
			}
		}

		String servicesType = r.getServicesType();
		if (servicesType == null) { // If there was no explicitly declared service type, then we need need to define the implied one
			Element ele = fieldElement.addElement(new QName("complexType", ns));
			if (r.hasServicesParent() == true) {
				ele.addAttribute("name", listName);
			}
			Element sele = ele.addElement(new QName("sequence", ns));
			for (FieldSet fs : r.getChildren("")) {
				if (fs instanceof Repeat) {
					System.err.println();
				}
				generateDataEntry(sele, fs, ns, root, fs instanceof Repeat);
			}
		}

	}

	private void generateDataEntry(Element ele, FieldSet fs, Namespace ns,
			Element root, Boolean isRepeat) {
		String listName = fs.getServicesTag();

		// If we find a "GroupField" (e.g., ui-type="groupfield/structureddate") then we need to create a new complex type that corresponds
		// to the "GroupField's" structured types -e.g., structuredData and dimension types 
		if (fs.isAGroupField() == true) {
			String groupFieldType = generateFieldGroup(fs, ele, ns, root);
			fs.setServicesType(groupFieldType);
		}
		
		if (fs instanceof Field) {
			Field ffs = (Field)fs;
			// <xs:element name="csid" type="xs:string"/>
			Element field = ele.addElement(new QName("element", ns));
			field.addAttribute("name", ffs.getServicesTag());
			String servicesType = ffs.getServicesType();
			String fieldType = "xs:string";
			if (servicesType != null) {
				fieldType = servicesType;
			}
			field.addAttribute("type", fieldType);
			if (isRepeat){
				field.addAttribute("minOccurs", "0");
				field.addAttribute("maxOccurs", "unbounded");
			}
		}
		if (fs instanceof Repeat) { // Group is an instance of Repeat
			Element fieldElement = root;
			Repeat rfs = (Repeat) fs;
			if (rfs.hasServicesParent()) {
				// group repeatable
				// <xs:element name="objectNameList" type="ns:objectNameList"/>
				Element newField = ele.addElement(new QName("element", ns));
				newField.addAttribute("name", rfs.getServicesParent()[0]);
				Namespace groupns = new Namespace("ns", "");
				newField.addAttribute("type", "ns:" + rfs.getServicesParent()[0]);
			} else {
				// single repeatable
				// <xs:element name="responsibleDepartments"
				// type="responsibleDepartmentList"/>
				fieldElement = ele.addElement(new QName("element", ns));
				fieldElement.addAttribute("name", rfs.getServicesTag());

				FieldSet[] fieldSetArray = rfs.getChildren("");
				if (fieldSetArray != null && fieldSetArray.length > 0) {
//					listName = rfs.getChildren("")[0].getServicesTag() + "List";
//					fieldElement.addAttribute("type", listName);
					System.out.println();
				} else { // If there is no children to define the type, there better be an explicit services type declaration
					String servicesType = rfs.getServicesType();
					if (servicesType != null) {
						fieldElement.addAttribute("type", servicesType);
					} else {
						log.error("Repeat/Group fieldset child array was null or the first element was null. Field attribute name: " 
								+ fieldElement.toString());
					}
				}

			}
			generateRepeat(rfs, fieldElement, listName, ns, root);
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
					//log.info(flds.getServicesTag());
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

	public String serviceschema(String path, Record record) throws UIException {
		if(path != null){
			section = path;
		}

		this.record = record;
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

}
