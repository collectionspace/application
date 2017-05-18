package org.collectionspace.chain.installation;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;

import org.collectionspace.chain.csp.persistence.services.TenantSpec;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.services.common.api.FileTools;
import org.collectionspace.chain.csp.schema.Spec;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

public class MakeXsd {
	private static final Logger log = LoggerFactory.getLogger(MakeXsd.class);
	private static final String XSD_EXTENSION = ".xsd";
	private static final CharSequence SERVICES_CORE_SCHEMA = "_core"; // Schema with "_core" in them are special
	protected Record xsdRecord;
	protected TenantSpec tenantSpec;
	protected Storage storage;
	
	private Map<String, String> recordDefinedSchemasMap = null;
	
	private Map<String, String> recordDefinedComplexTypes = new HashMap<String, String>();
	private Map<String, Map<String, String>> schemaDefinedComplexTypes = new HashMap<String, Map<String, String>>();  // The list of all Group (complexType) types across all schemas of the current record
	
	private static String STRUCTURED_DATE_TYPE = "structuredDateGroup";
	private static String ADDRESS_TYPE = "XYZ";
	private static String DIMENSION_TYPE = "PDQ";
	
	private static List<String> SHARED_SERVICE_TYPES_LIST = Arrays.asList(STRUCTURED_DATE_TYPE);
	private String currentSchemaName;
	private String configFileName;
	private ServiceConfigGeneration xsdGeneration;

	public MakeXsd(
			ServiceConfigGeneration xsdGeneration,
			TenantSpec td) {
			//Map<Record, Map<String, Map<String, String>>> recordDefinedGroupTypes) {
		this.xsdGeneration = xsdGeneration;
		this.tenantSpec = td;
//		this.recordDefinedGroupTypes = recordDefinedGroupTypes;
	}
	
	public String getConfigFileName() {
		return configFileName;
	}
	
	public Map<String, Map<String, String>> getSchemaDefinedComplexTypes() {
		return schemaDefinedComplexTypes;
	}
	/*
	 * This method generates an XML Schema complex type definition for "ui-type" config attributes -e.g., "groupfield/structureddate"
	 */
	private String generateFieldGroup(FieldSet fieldSet, Element ele, Namespace ns,	Element root) throws Exception {
		String fieldSetServicesType = fieldSet.getServicesType(false /* not NS qualified */);
		Spec spec = fieldSet.getRecord().getSpec();
		Record record = spec.getRecord(fieldSetServicesType); // find a record that corresponds to the fieldset's service type		
		
		if (record == null) {
			record = spec.getRecord(fieldSetServicesType); // try again, this time try a group type
		}
		
		if (record == null) {
			String msg = String.format("Could not find parent record for field '%s'.",
					fieldSet.toString());
			throw new Exception(msg);
		}
		
		String servicesType = record.getServicesType();

		//
		// Special case for backwards compat with existing "dateGroup" table/schema in Services
		//
		String servicesGroupType = fieldSet.getServicesGroupType(false);
		if (fieldSetServicesType.equalsIgnoreCase(servicesGroupType) == false) {
			servicesType = servicesGroupType;
		}
		
		if (servicesType == null) {
			servicesType = fieldSetServicesType;
		}
		
		boolean definedNewType = false;
		Element currentElement = ele;
		Element complexElement = currentElement;
		//
		// structuredDates are a special case and we need to define the complex type first.
		//
		if (fieldSet.isAStructureDate() == true) {
			@SuppressWarnings("unused")
			String parentRecordName = fieldSet.getRecord().getID(); // for debugging puposes only
			
			currentElement = root; // We'll add the structured date type def to the root/outer level
			complexElement = root.addElement(new QName("complexType", ns));
			complexElement.addAttribute("name", servicesType);
			definedNewType = true;
			Element sequenced = complexElement.addElement(new QName("sequence", ns));
			currentElement = sequenced;
		} else {
			String msg = String.format("Not a structuredDate fieldset.  Field set services type: '%s', Services type: '%s'", 
					fieldSetServicesType, servicesType);
			sendToDebugLog(msg);
		}
		
		for (FieldSet subRecordFieldSet : record.getAllFieldTopLevel("")) {
			generateDataEntry(currentElement, subRecordFieldSet, ns, root, false);
		}
		
		if (definedNewType == true) {
			String complexElementXSD = complexElement.asXML();
			if (isValidNewType(servicesType, complexElementXSD, fieldSet.getID()) == true) {
				if (isDefinedLocally(servicesType, complexElementXSD) == false) {
					recordDefinedComplexTypes.put(servicesType, complexElementXSD);
					sendToDebugLog(String.format("The complex type '%s' is defined as: %s",
							servicesType, complexElementXSD));				
				} else {
					root.remove(complexElement); // No need to define this type more than once in the current schema, so remove the element we just created.
					sendToDebugLog(String.format("The complex type '%s'  redefinition will be ingnored.  Def of: %s",
							servicesType, complexElementXSD));				
				}
			}
		}

		return servicesType;
	}
	
	private boolean isDefinedLocally(String groupTypeName, String xsdDef) throws Exception {
		boolean result = false;
		
		if (recordDefinedComplexTypes.containsKey(groupTypeName)) {
			if (recordDefinedComplexTypes.get(groupTypeName).equalsIgnoreCase(xsdDef)) {
				result = true;
			} else {
				String msg = String.format("The group type (XSD type) '%s' was previously defined differently in the current schema '%s' of the current record '%s'.", 
						groupTypeName, this.currentSchemaName, this.xsdRecord.getRecordName());
				sendToErrorLog(msg);
				logXsdDiffs(configFileName, recordDefinedComplexTypes.get(groupTypeName), xsdDef);
				throw new Exception(msg);
			}
		}
		
		return result;
	}
	
	/*
	 * Check to see if the current record already has a schema that has already defined this group type.
	 * If so, ensure that the definition is the same; otherwise, throw an exception.
	 */
	private String isDefinedInAnotherSchema(String complexTypeName, String xsdDef, String currentFieldName) throws Exception {
		String result = null;
		
		for (String schemaName : schemaDefinedComplexTypes.keySet()) {
			String existingXsd = schemaDefinedComplexTypes.get(schemaName).get(complexTypeName);
			if (existingXsd != null) {
				if (existingXsd.equalsIgnoreCase(xsdDef) == true) {
					return schemaName;
				} else {
					String msg = String.format("The group type (XSD type) '%s' of field '%s' in schema '%s' of the current record was previously defined differently inside the schema '%s'.", 
							complexTypeName, currentFieldName, currentSchemaName, schemaName);
					sendToErrorLog(msg);
					logXsdDiffs(configFileName, existingXsd, xsdDef);
					throw new Exception(msg);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Check to see if a complex type has already been defined in one of the schemas in one of the records of the current tenant.  If we find a
	 * duplicate definition, check that definition is identical.  If not, throw an exception.  If identical, return a concatenation of the defining record and schema
	 * @param complexTypeName
	 * @param xsdDef
	 * @param currentFieldName
	 * @return
	 * @throws Exception
	 */
	private String isDefinedInAnotherRecord(String complexTypeName, String xsdDef, String currentFieldName) throws Exception {
		String result = null;
		
		Map<Record, Map<String, Map<String, String>>> recordDefinedGroupTypes = this.xsdGeneration.getRecordDefinedComplexTypesMap();
		for (Record record : recordDefinedGroupTypes.keySet()) {
			for (String schemaName : recordDefinedGroupTypes.get(record).keySet()) {
				Map<String, String> groupTypeMap = recordDefinedGroupTypes.get(record).get(schemaName);
				if (groupTypeMap.get(complexTypeName) != null) {
					if (groupTypeMap.get(complexTypeName).equalsIgnoreCase(xsdDef)) {
						return record.getRecordName() + ":" + schemaName; // return the recordname and schema name
					} else {
						String msg = String.format("The group type (XSD type) '%s' of field '%s' in schema '%s' of record '%s' was previously defined differently in record '%s' inside schema '%s'.",
								complexTypeName, currentFieldName, currentSchemaName, this.xsdRecord.getRecordName(),
								record.getRecordName(), schemaName);
						sendToErrorLog(msg);
						logXsdDiffs(configFileName, groupTypeMap.get(complexTypeName), xsdDef);
						throw new Exception(msg);
					}
				}
			}
		}
				
		return result;
	}
	
	private String isDefinedInAnotherTenant(String complexTypeName, String xsdDef, String currentFieldName) throws Exception {
		String result = null;
		
		for (String tenantConfigName : xsdGeneration.getTenantConfigMap().keySet()) {
			ServiceConfigGeneration x = xsdGeneration.getTenantConfigMap().get(tenantConfigName);
			Map<Record, Map<String, Map<String, String>>> recordComplexTypesMap = x.getRecordDefinedComplexTypesMap();
			for (Record record : recordComplexTypesMap.keySet()) {
				Map<String, Map<String, String>> schemaComplexTypesMap = recordComplexTypesMap.get(record);
				for (String schemaName : schemaComplexTypesMap.keySet()) {
					Map<String, String> complexTypesMap = schemaComplexTypesMap.get(schemaName);
					if (complexTypesMap.get(complexTypeName) != null) {
						if (complexTypesMap.get(complexTypeName).equals(xsdDef)) {
							return tenantConfigName + ':' + record.getRecordName() + ":" + schemaName; // return the tenantConfig + record name + schema name
						} else {
							String msg = String.format("The group type (XSD complex type) '%s' of field '%s' in schema '%s' of record '%s' was previously defined differently in record '%s' inside schema '%s' of tenant config '%s'.",
									complexTypeName, currentFieldName, currentSchemaName, this.xsdRecord.getRecordName(),
									record.getRecordName(), schemaName, tenantConfigName);
							sendToErrorLog(msg);
							logXsdDiffs(this.configFileName, complexTypesMap.get(complexTypeName), xsdDef);
							throw new Exception(msg);
						}
					}
				}
			}
		}
		
		return result;
	}

	private void generateRepeat(Repeat repeatField, Element fieldElement, String listName, Namespace ns,
			Element root) throws Exception {
		if (repeatField.hasServicesParent() && !repeatField.hasOrphans()) { // for example, <repeat id="assocPersonGroupList/assocPersonGroup"> where the left side of the '/' is the services parent
			Element sequenced = null;
			int pathCount = 0;
			for (String path : repeatField.getServicesParent()) {
				if (path != null) {
					Element ele = root.addElement(new QName("complexType", ns));
					ele.addAttribute("name", path);
					sequenced = ele.addElement(new QName("sequence", ns));
					pathCount++;
				}
				if (log.isDebugEnabled() == true && pathCount > 1) {  // pathCount should never be > 1
					sendToDebugLog("Service parent path count is: " + pathCount);
				}
			}
			
			if (sequenced != null) {
				Element dele = sequenced.addElement(new QName("element", ns));
				String servicesTag = repeatField.getServicesTag();
				dele.addAttribute("name", servicesTag);				
				String servicesType = repeatField.getServicesType();
				if (servicesType == null) {
					servicesType = servicesTag; // If the type was not explicitly set in the config, then use the servicesTag as the type
				}
				dele.addAttribute("type", FieldSet.NS + servicesType);				
				dele.addAttribute("minOccurs", "0");
				dele.addAttribute("maxOccurs", "unbounded");
			}
		}
		//
		// If there was no explicitly declared service type, then we may need to define one.
		//
		String servicesType = repeatField.getServicesType();
		if (servicesType == null) {
			// Create the "complexType" node
			Element ele = fieldElement.addElement(new QName("complexType", ns));
			servicesType = listName;
			if (repeatField.hasServicesParent() == true && repeatField.hasOrphans() == true) {
				servicesType = repeatField.getServicesParent()[0]; // If orphaned, we use the first half of the "foo/bar" id attribute tuple -i.e., the parent type
			}
						
			boolean definedNewType = false;
			if (servicesType != null) {
				ele.addAttribute("name", servicesType);
				definedNewType = true;
			} else {
				sendToDebugLog("Created an anonymous complex type for Repeat ID=" + repeatField.getID());
			}
			
			// Now create a "sequence" node and iterate over the children items of the Repeat instance
			Element sele = ele.addElement(new QName("sequence", ns));
			for (FieldSet fs : repeatField.getChildren("")) {
				generateDataEntry(sele, fs, ns, root, isUnbounded(fs));
			}
			
			if (definedNewType == true) {
				String complextTypeXSD = ele.asXML();
				if (isValidNewType(servicesType, complextTypeXSD, repeatField.getID()) == true) {
					if (isDefinedLocally(servicesType, complextTypeXSD) == true) {
						fieldElement.remove(ele); // No need to define this type more than once in the current schema, so remove the element we just created.
					} else {
						recordDefinedComplexTypes.put(servicesType, complextTypeXSD);
						String msg = String.format("Defined new complex type '%s': %s",
								servicesType != null ? servicesType : listName, complextTypeXSD);
						sendToDebugLog(msg);
					}
				}
			}
		} else {
			sendToDebugLog(String.format("Creating repeat element with existing service type '%s'", servicesType));
		}
	}
		
	private boolean isSchemaDefinedLocally(String schemaName, String xsdDef) throws Exception {
		boolean result = false;
		
		if (recordDefinedSchemasMap.get(schemaName) != null) {
			if (recordDefinedSchemasMap.get(schemaName).equals(xsdDef) == true) {
				return true;
			} else {
				String msg = String.format("The schema '%s' of record '%s' was already defined differently in the same record.",
						schemaName, xsdRecord.getID());
				logXsdDiffs(configFileName, recordDefinedSchemasMap.get(schemaName), xsdDef);
				throw new Exception(msg);
			}
		}
		
		return result;
	}
	
	
	/**
	 * Ensure the schema was not already defined by another record of another tenant.
	 * 
	 * @param schemaName
	 * @param xsdDef
	 * @return
	 * @throws Exception
	 */
	private String isDefinedInAnotherTenant(String schemaName, String xsdDef) throws Exception {
		String result = null;
		
		for (String tenantConfigName : xsdGeneration.getTenantConfigMap().keySet()) {
			ServiceConfigGeneration x = xsdGeneration.getTenantConfigMap().get(tenantConfigName);
			Map<Record, Map<String, String>> recordSchemasMap = x.getRecordDefinedSchemasMap();
			for (Record record : recordSchemasMap.keySet()) {
				Map<String, String> schemasMap = recordSchemasMap.get(record);
				if (schemasMap.get(schemaName) != null) {
					if (areXsdSnippetsEqual(schemasMap.get(schemaName), xsdDef) == true) {
						return tenantConfigName + ':' + record.getRecordName() + ":" + schemaName; // return the tenantConfig + record name + schema name
					} else {
						String msg = String.format("The schema '%s' of record '%s' was already defined differently in the record '%s' of the tenant '%s'.",
								schemaName, xsdRecord.getID(),
								record.getID(), tenantConfigName);
						logXsdDiffs(configFileName, schemasMap.get(schemaName), xsdDef);
						throw new Exception(msg);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Ensure the schema was not already defined by another record of the current tenant configuration.
	 * 
	 * @param schemaName
	 * @param xsdDef
	 * @return
	 * @throws Exception
	 */
	private String isDefinedInAnotherRecord(String schemaName, String xsdDef) throws Exception {
		String result = null;
		
		Map<Record, Map<String, String>> recordSchemasMap = xsdGeneration.getRecordDefinedSchemasMap();
		for (Record record : recordSchemasMap.keySet()) {
			Map<String, String> schemasMap = recordSchemasMap.get(record);
			if (schemasMap.get(schemaName) != null) {
				if (areXsdSnippetsEqual(schemasMap.get(schemaName), xsdDef) == true) {
					return record.getID();
				} else {
					String msg = String.format("The schema '%s' of record '%s' was already defined differently in the record '%s' of the same tenant.",
							schemaName, xsdRecord.getID(), record.getID());
					logXsdDiffs(configFileName, schemasMap.get(schemaName), xsdDef);
					throw new Exception(msg);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Ensure schema is not already defined differently somewhere else.
	 * 
	 * @param schemaName
	 * @param xsdDef
	 * @return
	 */
	private boolean isValidNewSchema(String schemaName, String xsdDef) throws Exception {
		boolean result = true;
		
		String definedInRecord = isDefinedInAnotherRecord(schemaName, xsdDef);
		if (definedInRecord != null) { 
			String msg = String.format("Type schema '%s' of record '%s' is also defined indentically in the record '%s' of the same tenant.",
					schemaName, xsdRecord.getID(),
					definedInRecord);
			sendToTraceLog(msg);
		}
		
		String definedInTenant = isDefinedInAnotherTenant(schemaName, xsdDef);
		if (definedInTenant != null) { 
			String msg = String.format("Type schema '%s' of record '%s' is also defined indentically in the schema '%s' of the record '%s' of tenant config '%s'.",
					schemaName, xsdRecord.getID(),
					definedInTenant.split(":")[2], definedInTenant.split(":")[1], definedInTenant.split(":")[0]);  // The value in definedInRecord should look like this: "tenantConfig:recordName:schemaName"
			sendToTraceLog(msg);
		}		
		
		return result;
	}
	
	
	/**
	 * Ensure this new type is defined consistently across all schemas of all records.  If we find
	 * another type with the same name but defined differently, we'll throw an exception.
	 * 
	 * @param complexTypeName
	 * @param xsdDef
	 * @return
	 * @throws Exception
	 */
	private boolean isValidNewType(String complexTypeName, String xsdDef, String currentFieldName) throws Exception {
		boolean result = true;
		
		String definedInSchema = isDefinedInAnotherSchema(complexTypeName, xsdDef, currentFieldName);
		if (definedInSchema != null && !SHARED_SERVICE_TYPES_LIST.contains(complexTypeName)) {
			String msg = String.format("Type group type '%s' of field '%s' in schema '%s' of record '%s' is also defined indentically in the schema '%s' of the same record of the same tenant.", 
					complexTypeName, currentFieldName, currentSchemaName, xsdRecord.getID(),
					definedInSchema);
			sendToTraceLog(msg);
		}
		
		String definedInRecord = isDefinedInAnotherRecord(complexTypeName, xsdDef, currentFieldName);
		if (definedInRecord != null && !SHARED_SERVICE_TYPES_LIST.contains(complexTypeName)) { 
			String msg = String.format("Type group type '%s' of field '%s' in schema '%s' of record '%s' is also defined indentically in the schema '%s' of the record '%s' of the same tenant.",
					complexTypeName, currentFieldName, currentSchemaName, xsdRecord.getID(),
					definedInRecord.split(":")[1], definedInRecord.split(":")[0]);  // The value in definedInRecord should look like this: "recordName:schemaName"
			sendToTraceLog(msg);
		}
		
		String definedInTenant = isDefinedInAnotherTenant(complexTypeName, xsdDef, currentFieldName);
		if (definedInTenant != null && !SHARED_SERVICE_TYPES_LIST.contains(complexTypeName)) { 
			String msg = String.format("Type group type '%s' of field '%s' in schema '%s' of record '%s' is also defined indentically in the schema '%s' of the record '%s' of tenant config '%s'.",
					complexTypeName, currentFieldName, currentSchemaName, xsdRecord.getID(),
					definedInTenant.split(":")[2], definedInTenant.split(":")[1], definedInTenant.split(":")[0]);  // The value in definedInRecord should look like this: "tenantConfig:recordName:schemaName"
			sendToTraceLog(msg);
		}
		
		return result;
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
	private Object[] getSchemaNameList(Record record) {
		HashMap<String, String> serviceParts = new HashMap<String, String>();
		
		for (FieldSet fs : record.getAllFieldTopLevel("")) {
			serviceParts.put(fs.getSection(), fs.getID());
		}
		
		return serviceParts.keySet().toArray();
	}	

	private void generateDataEntry(Element ele, FieldSet fs, Namespace ns,
			Element root, Boolean unbounded) throws Exception {		
		//
		// EXIT if the FieldSet instance is not defined in the Services
		// 
		if (fs.isInServices() == false) {
			sendToDebugLog(String.format("Field set is not part of the Services schema %s:%s", fs.getSection(), fs.getID()));
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
						sendToErrorLog("Repeat/Group fieldset child array was null or the first element was null. Field attribute name: " 
								+ fieldElement.toString());
					}
				}

			}
			generateRepeat(rfs, fieldElement, listName, ns, root);
		}
	}
	
//	private void generateSearchList(Element root, Namespace ns) {
//
//		Element ele = root.addElement(new QName("complexType", ns));
//		ele.addAttribute("name", "abstractCommonList");
//		Element aele = ele.addElement(new QName("annotation", ns));
//		Element appele = aele.addElement(new QName("appinfo", ns));
//		Element jxb = appele.addElement(new QName("class", new Namespace(
//				"jaxb", "")));
//		jxb.addAttribute("ref",
//				"org.collectionspace.services.jaxb.AbstractCommonList");
//
//		String[] listpath = xsdRecord.getServicesListPath().split("/");
//
//		Element lele = root.addElement(new QName("element", ns));
//		lele.addAttribute("name", listpath[0]);
//		Element clele = lele.addElement(new QName("complexType", ns));
//		Element cplele = clele.addElement(new QName("complexContent", ns));
//		Element exlele = cplele.addElement(new QName("extension", ns));
//		exlele.addAttribute("base", "abstractCommmonList");
//		Element sexlele = exlele.addElement(new QName("sequence", ns));
//		Element slele = sexlele.addElement(new QName("element", ns));
//		slele.addAttribute("name", listpath[1]);
//		slele.addAttribute("maxOccurs", "unbounded");
//		Element cslele = slele.addElement(new QName("complexType", ns));
//		Element scslele = cslele.addElement(new QName("sequence", ns));
//
//		Set<String> searchflds = new HashSet<String>();
//		for (String minis : xsdRecord.getAllMiniDataSets()) {
//			if (minis != null && !minis.equals("")) {
//				for (FieldSet flds : xsdRecord.getMiniDataSetByName(minis)) {
//					searchflds.add(flds.getServicesTag());
//					//log.info(flds.getServicesTag());
//				}
//			}
//		}
//		Iterator<String> iter = searchflds.iterator();
//		while (iter.hasNext()) {
//			Element sfld = scslele.addElement(new QName("element", ns));
//			sfld.addAttribute("name", (String) iter.next());
//			sfld.addAttribute("type", "xs:string");
//			sfld.addAttribute("minOccurs", "1");
//		}
//
//		/*standard fields */
//
//		Element stfld1 = scslele.addElement(new QName("element", ns));
//		stfld1.addAttribute("name", "uri");
//		stfld1.addAttribute("type", "xs:string");
//		stfld1.addAttribute("minOccurs", "1");
//		
//		Element stfld2 = scslele.addElement(new QName("element", ns));
//		stfld2.addAttribute("name", "csid");
//		stfld2.addAttribute("type", "xs:string");
//		stfld2.addAttribute("minOccurs", "1");
//	}
	
	private String generateXSDFilename(String recordType, String schemaName) {
		String result = schemaName;
		
		if (schemaName.contains(SERVICES_CORE_SCHEMA) == false) {
			result = recordType + "_" + schemaName;
		}
		
		result = result + XSD_EXTENSION;
		return result;
	}
	
	private String getLogPrefix() {
		String result = String.format("Config Generation: '%s' - '%s' - '%s': ", 
				configFileName, xsdRecord.getID(), currentSchemaName);
		return result;
	}
	
	private void sendToInfoLog(String msg) {
		log.info(getLogPrefix() + msg);
	}
	
	private void sendToDebugLog(String msg) {
		log.debug(getLogPrefix() + msg);
	}

	private void sendToTraceLog(String msg) {
		log.trace(getLogPrefix() + msg);
	}

	private void sendToErrorLog(String msg, Throwable t) {
		log.error(getLogPrefix() + msg, t);
	}
	
	private void sendToErrorLog(String msg) {
		log.error(getLogPrefix() + msg);
	}
	

	/**
	 * Returns all the schemas defined in a given record. Map<schemaName, XSD>
	 * 
	 * @param record
	 * @param schemaVersion
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> generateSchemasForRecord(Record record, String schemaVersion) throws Exception {
		Map<String, String> result = null;
		configFileName = record.getConfigFileName();
		recordDefinedSchemasMap = new HashMap<String, String>();
		
		Object[] schemaNameList = getSchemaNameList(record);
		for (Object name : schemaNameList) {
			String schemaName = (String)name;
			String recordType = record.getServicesTenantPl().toLowerCase(); //record.getServicesURL(); //record.getServicesType();
			String fileName = generateXSDFilename(recordType, schemaName);
			String schema = generateSchema(schemaName, record, schemaVersion); // Generates the XML Schema (.xsd file) contents -returns it as a String instance

			String canonicalSchemaName = fileName;
			if (isValidNewSchema(canonicalSchemaName, schema) == true) { // ensure the schema is not already defined in another record
				if (isSchemaDefinedLocally(canonicalSchemaName, schema) == false) {  // ensure the schema is not alredy defined in this record
					recordDefinedSchemasMap.put(canonicalSchemaName, schema);
					if (recordDefinedComplexTypes.isEmpty() == false) {						
						schemaDefinedComplexTypes.put(schemaName, recordDefinedComplexTypes); // keep track of the complex types we defined for the record in this schema
						recordDefinedComplexTypes = new HashMap<String, String>(); // for each new XSD file, we'll create a set of defined group/complex types -see CSPACE-6177.
					}
				}
			}						
		}
						
		return recordDefinedSchemasMap;
	}
	
	/**
	 * Generates and returns and XSD schema.
	 * 
	 * @param schemaName
	 * @param record
	 * @param schemaVersion
	 * @return
	 * @throws Exception
	 */
	private String generateSchema(String schemaName, Record record, String schemaVersion) throws Exception {
		this.xsdRecord = record;
		this.currentSchemaName = schemaName;
		Document doc = DocumentFactory.getInstance().createDocument();
		
		String servicesRecordPath = null;
		String[] parts = null;
		try {
			servicesRecordPath = record.getServicesRecordPath(schemaName);
			parts = servicesRecordPath.split(":", 2);
		} catch (NullPointerException e) {
			String msg;
			if (servicesRecordPath == null) {
				msg = String.format("Missing '<services-record-path>' config element declaration for schema/section '%s' in record '%s'.",
						schemaName, record.toString());
			} else {
				msg = String.format("Could not split services record path for record '%s' with schema '%s'.",
						record.toString(), schemaName);
			}
			throw new Exception(msg, e);
		}
		
		String[] rootel = parts[1].split(",");
		Element root = doc.addElement(new QName("schema", new Namespace("xs",
				"http://www.w3.org/2001/XMLSchema")));
		root.addAttribute("xmlns:ns", rootel[0]);
		root.addAttribute("xmlns", rootel[0]);
		root.addAttribute("targetNamespace", rootel[0]);
		root.addAttribute("version", schemaVersion);

		// add top level items
		Namespace ns = new Namespace("xs", "http://www.w3.org/2001/XMLSchema");
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
				sendToErrorLog(String.format("Could not generate data entry for fieldset '%s:%s' of record %s:%s", 
						fs.getSection(), fs.getID(), schemaName, record.whoamI), e);
				throw e;
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
	
	boolean areXsdSnippetsEqual(String source, String target) {
		return areXsdSnippetsEqual(configFileName, source, target);
	}
	
	static boolean areXsdSnippetsEqual(String configFileName, String source, String target) {
		boolean result = true;
		
		if (log.isDebugEnabled() || true) {
			if (source.equals(target) == false) {
				log.trace("");
			}
		}
		
		try {
	        assertThat(target, 
	        		CompareMatcher.isSimilarTo(source)
	        		.normalizeWhitespace()
	                .ignoreComments()
	                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes)));
		} catch (Throwable e) {
			result = false;
			logXsdDiffs(configFileName, source, target);
		}
		
		return result;
	}
	
	static void logXsdDiffs(String configFileName, String source, String target) {
		try {
			File tmpDir = FileTools.createTmpDir(configFileName);

			// write the source to a file
			File sourceXsd = new File(tmpDir, "source.xml");
            FileUtils.writeStringToFile(sourceXsd, source);
            
            // write the target to a file
            File targetXsd = new File(tmpDir, "target.xml");
            FileUtils.writeStringToFile(targetXsd, target);
            
            // write a message to the log about these files
            String msg = String.format("The following two files show XML Schema declarations that are defined differently but should be indentical.\nsource: %s\ntarget: %s",
            		sourceXsd.getPath(), targetXsd.getPath());
            log.error(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(String.format("Could not create debug files to show XSD differences found while processing '%s' configuration file.",
					configFileName), e);
			String msg = String.format("Config Generation: '%s' - '%s'", 
					configFileName, e.getMessage());
			log.error(msg);
		}
	}

}
