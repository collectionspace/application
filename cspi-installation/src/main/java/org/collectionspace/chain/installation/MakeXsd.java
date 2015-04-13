package org.collectionspace.chain.installation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.collectionspace.chain.csp.persistence.services.TenantSpec;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
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
	private static final String XSD_EXTENSION = ".xsd";
	private static final CharSequence SERVICES_CORE_SCHEMA = "_core"; // Schema with "_core" in them are special
	protected Record xsdRecord;
	protected TenantSpec tenantSpec;
	protected Storage storage;
	protected HashMap<String, Boolean> definedGroupFields = new HashMap<String, Boolean>();

	public MakeXsd(TenantSpec td) {
		this.tenantSpec = td;
	}
	
	/*
	 * This method generates an XML Schema complex type definition for "ui-type" config attributes -e.g., "groupfield/structureddate"
	 */
	private String generateFieldGroup(FieldSet fieldSet, Element ele, Namespace ns,
			Element root) {
		String fieldSetServicesType = fieldSet.getServicesType(false /* not NS qualified */);
		Spec spec = fieldSet.getRecord().getSpec();
		Record record = spec.getRecord(fieldSetServicesType); // find a record that corresponds to the fieldset's service type
		String servicesType = record.getServicesType();
		
		//
		// Special case for backwards compat with existing "dateGroup" table/schema in Services
		//
		String servicesGroupType = fieldSet.getServicesGroupType(false);
		if (fieldSetServicesType.equalsIgnoreCase(servicesGroupType) == false) {
			servicesType = servicesGroupType;
		}
		
		if (definedGroupFields.containsKey(servicesType) == false) {
			Element currentElement = ele;
			//
			// structuredDates are a special case and we need to define the complex type first.
			//
			if (fieldSet.isAStructureDate() == true) {
				String parentRecordName = fieldSet.getRecord().getID();
				currentElement = root; // was not here
				Element complexElement = root.addElement(new QName("complexType", ns)); // was ele
				complexElement.addAttribute("name", servicesType);
				Element sequenced = complexElement.addElement(new QName("sequence", ns));
				currentElement = sequenced;
			}
			
			for (FieldSet subRecordFieldSet : record.getAllFieldTopLevel("")) {
				generateDataEntry(currentElement, subRecordFieldSet, ns, root, false);
			}
			definedGroupFields.put(servicesType, true); // We only need to define the complex type once per schema/XSD file.
		}
		
		return servicesType;
	}
	
	private void generateRepeat(Repeat r, Element fieldElement, String listName, Namespace ns,
			Element root) {
		if (r.hasServicesParent() && !r.hasOrphans()) { // for example, <repeat id="assocPersonGroupList/assocPersonGroup"> where the left side of the '/' is the services parent
			Element sequenced = null;
			int pathCount = 0;
			for (String path : r.getServicesParent()) {
				if (path != null) {
					Element ele = root.addElement(new QName("complexType", ns));
					ele.addAttribute("name", path);
					sequenced = ele.addElement(new QName("sequence", ns));
					pathCount++;
				}
				if (log.isDebugEnabled() == true && pathCount > 1) {  // pathCount should never be > 1
					log.error("Service parent path count is: " + pathCount);
				}
			}
			
			if (sequenced != null) {
				Element dele = sequenced.addElement(new QName("element", ns));
				String servicesTag = r.getServicesTag();
				dele.addAttribute("name", servicesTag);				
				String servicesType = r.getServicesType();
				if (servicesType == null) {
					servicesType = servicesTag; // If the type was not explicitly set in the config, then use the servicesTag as the type
				}
				dele.addAttribute("type", FieldSet.NS + servicesType);				
				dele.addAttribute("minOccurs", "0");
				dele.addAttribute("maxOccurs", "unbounded");
			}
		}
		//
		// If there was no explicitly declared service type, then we need need to define one
		//
		String servicesType = r.getServicesType();
		if (servicesType == null) {
			// Create the "complexType" node
			Element ele = fieldElement.addElement(new QName("complexType", ns));
			servicesType = listName;
			if (r.hasServicesParent() == true && r.hasOrphans() == true) {
				servicesType = r.getServicesParent()[0]; // If orphaned, we use the first half of the "foo/bar" id attribute tuple -i.e., the parent type
			}
			
			if (servicesType != null) {
				ele.addAttribute("name", servicesType);
			} else {
				log.debug("Created an anonymous complex type for Repeat ID=" + r.getID());
			}
			
			// Now create a "sequence" node and iterate over the children items of the Repeat instance
			Element sele = ele.addElement(new QName("sequence", ns));
			for (FieldSet fs : r.getChildren("")) {
				generateDataEntry(sele, fs, ns, root, isUnbounded(fs));
			}
		}

	}

	private Boolean isUnbounded(FieldSet fs) {
		Boolean result = false;
		
		if (isRepeatType(fs) == true) {
			result = true;
		} else {
			FieldParent parent = fs.getParent();
			if (parent.getClass() == Repeat.class) {
				Repeat repeat = (Repeat)parent;
				if (repeat.hasServicesParent() == false) {
					result = true;
				}
			}
		}
		
		return result;
	}
	
	private Boolean isRepeatType(FieldSet fs) {
		return fs.getClass() == Repeat.class;
	}
	
	/*
	 * Returns 'true' if the FieldSet instance is a child of a "Repeat" and is a "Group" instance and has an attribute
	 * of "ui-type=groupfield/foo".  Orphans inherit their parent's ID value.
	 */
	private boolean isOrphaned(FieldSet fs) {
		boolean result = false;
		
		if (fs instanceof Group && fs.isAGroupField()) {
			FieldParent parent = fs.getParent();
			if (parent instanceof Repeat) {
				Repeat repeat = (Repeat) parent;
				if (repeat.hasOrphans()) {
					result = true;
				}
			}
		}
		
		return result;
	}
	
	/*
	 * Return a list of schemas defined in a given configuration record
	 */
	private Object[] getServiceSchemas(Record record) {
		HashMap<String, String> serviceParts = new HashMap<String, String>();
		
		for (FieldSet fs : record.getAllFieldTopLevel("")) {
			serviceParts.put(fs.getSection(), fs.getID());
		}
		
		return serviceParts.keySet().toArray();
	}	

	private void generateDataEntry(Element ele, FieldSet fs, Namespace ns,
			Element root, Boolean unbounded) {		
		//
		// EXIT if the FieldSet instance is not defined in the Services
		// 
		if (fs.isInServices() == false) {
			log.warn(String.format("Field set is not part of the Services schema %s:%s", fs.getSection(), fs.getID()));
			return;
		}
		
		String sectionName = fs.getSection();
		String listName = fs.getServicesTag();
		
		// If we find a "GroupField" (e.g., ui-type="groupfield/structureddate") then we need to create a new complex type that corresponds
		// to the "GroupField's" structured types -e.g., structuredData and dimension types 
		if (fs.isAGroupField() == true) {
			String groupFieldType = generateFieldGroup(fs, ele, ns, root);
			fs.setServicesType(groupFieldType);
		}
		
		if (fs.isAGroupField() && fs.isAStructureDate() == false) {
			//
			// We're finished if we're a non-structuredDate "GroupField."
			//
			return;
		}
		
		if (fs instanceof Field || fs instanceof Group) {
			String servicesTag = fs.getServicesTag();
			if (isOrphaned(fs) == true) { // If we have a Repeat with a single child that is a "Group" with a "ui-type=groupfield/foo" attribute.
				servicesTag = fs.getParentID();
				unbounded = true;
			}
			// <xs:element name="csid" type="xs:string"/>
			Element field = ele.addElement(new QName("element", ns));
			if (fs.isAGroupField() && fs.isAStructureDate() == false) {
				servicesTag = fs.getServicesType();
			}
			field.addAttribute("name", servicesTag);
			
			String servicesType = fs.getServicesType();
			String fieldType = "xs:string";
			if (servicesType != null) {
				fieldType = servicesType;
			}
			field.addAttribute("type", fieldType);
			if (unbounded == true) {
				field.addAttribute("minOccurs", "0");
				field.addAttribute("maxOccurs", "unbounded");
			}
		}
		
		if (isRepeatType(fs) == true) { // Has to be a Repeat class instance and not any descendant (e.g. not a Group instance)
			Element fieldElement = root;
			Repeat rfs = (Repeat) fs;
			if (rfs.hasServicesParent()) {
				// group repeatable
				// <xs:element name="objectNameList" type="ns:objectNameList"/>
				Element newField = ele.addElement(new QName("element", ns));
				newField.addAttribute("name", rfs.getServicesParent()[0]);
				String fieldType = rfs.getServicesParent()[0];
				newField.addAttribute("type", FieldSet.NS + fieldType );
			} else {
				// single repeatable
				// <xs:element name="responsibleDepartments"
				// type="responsibleDepartmentList"/>
				fieldElement = ele.addElement(new QName("element", ns));
				fieldElement.addAttribute("name", rfs.getServicesTag());

				FieldSet[] fieldSetArray = rfs.getChildren("");
				if (fieldSetArray != null && fieldSetArray.length > 0) {
					if (rfs.isServicesAnonymousType() == true) {
						listName = null; // Ends up creating an embedded anonymous complex type
					} else {
						listName = rfs.getChildren("")[0].getServicesTag() + "List";
						fieldElement.addAttribute("type", FieldSet.NS + listName);
						fieldElement = fieldElement.getParent();
					}
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
	
	private void generateSearchList(Element root, Namespace ns) {

		Element ele = root.addElement(new QName("complexType", ns));
		ele.addAttribute("name", "abstractCommonList");
		Element aele = ele.addElement(new QName("annotation", ns));
		Element appele = aele.addElement(new QName("appinfo", ns));
		Element jxb = appele.addElement(new QName("class", new Namespace(
				"jaxb", "")));
		jxb.addAttribute("ref",
				"org.collectionspace.services.jaxb.AbstractCommonList");

		String[] listpath = xsdRecord.getServicesListPath().split("/");

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

		Set<String> searchflds = new HashSet<String>();
		for (String minis : xsdRecord.getAllMiniDataSets()) {
			if (minis != null && !minis.equals("")) {
				for (FieldSet flds : xsdRecord.getMiniDataSetByName(minis)) {
					searchflds.add(flds.getServicesTag());
					//log.info(flds.getServicesTag());
				}
			}
		}
		Iterator<String> iter = searchflds.iterator();
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
	
	private String generateXSDFilename(String recordType, String schemaName) {
		String result = schemaName;
		
		if (schemaName.contains(SERVICES_CORE_SCHEMA) == false) {
			result = recordType + "_" + schemaName;
		}
		
		result = result + XSD_EXTENSION;
		return result;
	}
	
	public HashMap<String, String> getDefinedSchemas(Record record, String schemaVersion) throws UIException {
		HashMap<String, String> result = new HashMap<String, String>();
		
		Object[] servicesSchemaList = this.getServiceSchemas(record);
		for (Object name : servicesSchemaList) {
			String recordType = record.getServicesTenantPl().toLowerCase(); //record.getServicesURL(); //record.getServicesType();
			String schemaName = (String)name;
			String schema = getServiceSchema(schemaName, record, schemaVersion); // Generates the XML Schema .xsd file -returns it as a String instance
			String filename = generateXSDFilename(recordType, schemaName);
			result.put(filename, schema);
			definedGroupFields.clear(); // for each new XSD file, clear the set of defined group/complex types -see CSPACE-6177.
		}
		
		return result;
	}
	
	private String getServiceSchema(String schemaName, Record record, String schemaVersion) throws UIException {
		this.xsdRecord = record;
		Document doc = DocumentFactory.getInstance().createDocument();
		
		Namespace ns = new Namespace("xs", "http://www.w3.org/2001/XMLSchema");
		String[] parts = record.getServicesRecordPath(schemaName).split(":", 2);
		String[] rootel = parts[1].split(",");
		Element root = doc.addElement(new QName("schema", new Namespace("xs",
				"http://www.w3.org/2001/XMLSchema")));
		root.addAttribute("xmlns:ns", rootel[0]);
		root.addAttribute("xmlns", rootel[0]);
		root.addAttribute("targetNamespace", rootel[0]);
		root.addAttribute("version", schemaVersion);

		// add top level items
		for (FieldSet fs : record.getAllFieldTopLevel("")) {
			try {
				if (fs.getSection().equalsIgnoreCase(schemaName) == true) {
					generateDataEntry(root, fs, ns, root, false);
					log.trace(String.format("Generated data entry for fieldset '%s:%s' of record %s:%s", 
							fs.getSection(), fs.getID(), schemaName, record.whoamI));					
				} else {
					log.trace(String.format("Ignoring fieldset %s:%s of record %s:%s",
							fs.getSection(), fs.getID(), schemaName, record.whoamI));
				}
			} catch (Exception e) {
				log.error(String.format("Could not generate data entry for fieldset '%s:%s' of record %s:%s", 
						fs.getSection(), fs.getID(), schemaName, record.whoamI), e);
			}
		}

		/** Enable this code for creating the abstract common list elements.
		generateSearchList(root, ns);
		// log.info(doc.asXML());
		// return doc.asXML();
		 */
		
		
		String schemaResult = doc.asXML();
		return schemaResult;		
	}

}
