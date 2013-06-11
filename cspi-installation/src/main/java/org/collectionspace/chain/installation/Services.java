package org.collectionspace.chain.installation;

import java.awt.List;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.collectionspace.chain.csp.persistence.services.TenantSpec;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Services {
	private static final Logger log = LoggerFactory.getLogger(Services.class);
	
	private static final String COLLECTIONSPACE_CORE_PART_NAME = "collectionspace_core";
	private static final String COLLECTIONSPACE_COMMON_PART_NAME = "common";
	private static final String COLLECTIONSPACE_SYSTEM_PART_NAME = "system";
	private static final String COLLECTIONSPACE_SCHEMA_LOCATION_SYSTEM = "http://collectionspace.org/services/config/system http://collectionspace.org/services/config/system/system-response.xsd";
	private static final String COLLECTIONSPACE_NAMESPACE_URI_SYSTEM = "http://collectionspace.org/services/config/system";
	
	private static final String GROUP_XPATH_SUFFIX = "/*";
	private static final String NO_REPO_DOMAIN = "none";

	private static final String SERVICES_BINDING_TYPE_SECURITY = "security";

	private static final String AUTH_REF = "authRef";
	private static final String TERM_REF = "termRef";

	private static final String OBJECT_NAME_PROPERTY = "objectNameProperty";
	private static final String OBJECT_NUMBER_PROPERTY = "objectNumberProperty";

	private static final String BASE_AUTHORITY_RECORD = "baseAuthority";
	private static final String BASE_AUTHORITY_SECTION = BASE_AUTHORITY_RECORD;
	
	protected Spec spec;
	protected TenantSpec tenantSpec;
	protected Boolean defaultonly;
	protected String domainsection;

	/**
	 * not sure how configurable these need to be - they can be made more flexible
	 */
	protected Namespace nstenant = new Namespace("tenant", "http://collectionspace.org/services/common/tenant"); 
	protected Namespace nsservices = new Namespace("service", "http://collectionspace.org/services/config/service"); 
	protected Namespace nsxsi = new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");  
	protected Namespace nstypes = new Namespace("types", "http://collectionspace.org/services/config/types"); 
	protected String schemaloc = "http://collectionspace.org/services/config/tenant " +
	"http://collectionspace.org/services/config/tenant.xsd";

	public Services() {
	}

	public Services(Spec spec,TenantSpec td, Boolean isdefault) {
		this.spec = spec;
		this.tenantSpec = td;
		this.defaultonly = isdefault;
		this.domainsection = "common";
	}

	public String  doit() {
		return doServiceBindingsCommon();
	}

	/**
	 * if this.defaultonly - true then return
	 * service-bindings-common.xml - a shared prototype of core and common
	 * services, common to all tenants. This file has the basic definitions of
	 * the services, and the system, core, and common parts of each service. It
	 * includes the most common definitions of the field types, to make it easy
	 * to define a new tenant.
	 * 
	 * else
	 * return domain specific file
	 * 
	 * @return
	 */
	private String doServiceBindingsCommon() {
		Document doc = DocumentFactory.getInstance().createDocument();
		Element root = doc.addElement(new QName("TenantBindingConfig", this.nstenant));
		root.addAttribute("xsi:schemaLocation", this.schemaloc);
		// root.add(this.nsservices);
		root.add(this.nsxsi);
		// root.add(this.nstypes);

		// <tenant:tenantBinding version="0.1">
		Element ele = root.addElement(new QName("tenantBinding", nstenant));
		if (!this.defaultonly) {
			ele.addAttribute("name", this.tenantSpec.getTenant());
			ele.addAttribute("displayName", this.tenantSpec.getTenantDisplay());
		}
		ele.addAttribute("version", this.tenantSpec.getTenantVersion());

		// <tenant:repositoryDomain name="default-domain" repositoryClient="nuxeo-java"/>
		Element rele = ele.addElement(new QName("repositoryDomain", nstenant));
		rele.addAttribute("name", this.tenantSpec.getRepositoryDomain());
		rele.addAttribute("storageName", this.tenantSpec.getStorageName());
		rele.addAttribute("repositoryClient", this.tenantSpec.getRepoClient());

		// add in <tenant:properties> if required
		makeProperties(ele);

		// loop over each record type and add <tenant:serviceBindings
		for (Record r : this.spec.getAllRecords()) {
			if (r.isType("record") == true) {
				String rtype = "procedure";
				if (!r.isType("procedure") && r.isType("record")) {
					rtype = "object";
				}
				// <tenant:serviceBindings name="CollectionObjects" type="object" version="0.1">
				Element cele = ele.addElement(new QName("serviceBindings", nstenant));
				cele.addAttribute("id", r.getServicesTenantPl());
				cele.addAttribute("name", r.getServicesTenantPl());
				cele.addAttribute("type", rtype);
				cele.addAttribute("version", "0.1"); // FIXME:REM - Should not be hard coded.
				addServiceBinding(r, cele, nsservices, false);
			}

			if (r.isType("authority")) {
				addVocabularies(r, ele);
			}

			if (r.isType("userdata")) {
				Element cele = ele.addElement(new QName("serviceBindings", nstenant));
				cele.addAttribute("id", r.getServicesTenantPl());
				cele.addAttribute("name", r.getServicesTenantPl());
				cele.addAttribute("type", SERVICES_BINDING_TYPE_SECURITY);
				cele.addAttribute("version", "0.1"); // FIXME:REM - Should not be hard coded
				addServiceBinding(r, cele, nsservices, false);
			}

			if (r.isType("authorizationdata")) {
				// ignore at the moment as they are so non standard
				// addAuthorization(r,ele);
				log.debug("ignore at the moment as they are so non standard");
			}
			//
			// Debug-log the generated Service bindings for this App layer record
			//
			Element bindingsForRecord = ele.element(new QName("serviceBindings", nstenant));
			if (bindingsForRecord != null) {
				String tenantName = this.tenantSpec.getTenant();
				log.debug(String.format("Tenant name='%s'", tenantName));
				
				String recordName = r.getRecordName();
				String serviceBindings = bindingsForRecord.asXML();
				log.debug(String.format("Bindings for Record=%s: %s", recordName, serviceBindings));
			}
		}

		return doc.asXML();
	}

	/**
	 * add in dateformats and languages if required
	 * @param ele
	 */
	private void makeProperties(Element ele){
		Boolean showLang = true;
		Boolean showDate = true;
		if (!this.defaultonly) {
			if(this.tenantSpec.isDefaultLanguage()){
				showLang = false;
			}
			if(this.tenantSpec.isDefaultDate()){
				showDate = false;
			}
		}
		
		if (showDate || showLang) {
			Element pele = ele.addElement(new QName("properties", nstenant));
			if (showDate) {
				for (String dateformat : this.tenantSpec.getDateFormats()) {

					Element tele = pele.addElement(new QName("item",
							this.nstypes));
					Element tele2 = tele.addElement(new QName("key",
							this.nstypes));
					Element tele3 = tele.addElement(new QName("value",
							this.nstypes));
					tele2.addText("datePattern");
					tele3.addText(dateformat);
				}
			}
			if (showLang) {
				for (String lang : this.tenantSpec.getLanguages()) {

					Element tele = pele.addElement(new QName("item",
							this.nstypes));
					Element tele2 = tele.addElement(new QName("key",
							this.nstypes));
					Element tele3 = tele.addElement(new QName("value",
							this.nstypes));
					tele2.addText("localeLanguage");
					tele3.addText(lang);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param r
	 * @param cele
	 * @param id
	 * @param label
	 * @param order
	 * @param namespaceURI
	 * @param schemaLocation
	 * @param thisns
	 * @param addAuths
	 * @param section
	 */
	private void makePart(Record r, Element cele, String id, String label, String order,
			String namespaceURI, String schemaLocation, Namespace thisns, Boolean addAuths, String section) {
		Element pele = cele.addElement(new QName("part", thisns));
		pele.addAttribute("id", id);
		pele.addAttribute("control_group", "Managed"); //FIXME: This shouldn't be hard coded.
		pele.addAttribute("versionable", "true");
		pele.addAttribute("auditable", "false"); //FIXME: Should not be hard coded.
		pele.addAttribute("label", label);
		pele.addAttribute("updated", "");
		pele.addAttribute("order", order);
		
		Element conle = pele.addElement(new QName("content",thisns));
		conle.addAttribute("contentType", "application/xml"); //FIXME: This shouldn't be hard coded
		
		Element xconle = conle.addElement(new QName("xmlContent",thisns));
		xconle.addAttribute("namespaceURI", namespaceURI);
		xconle.addAttribute("schemaLocation", schemaLocation);
		
		if(addAuths){
			Element auths = pele.addElement(new QName("properties",thisns));
			doAuths(auths,this.nstypes,r,section);
			
		}
	}

	/**
	 * Define fields that needs to have indexes created in the Nuxeo DB
	 * @param r
	 * @param el
	 * @param thisns
	 * @param section
	 * @param isAuthority
	 */
	private void doInitHandler(Record record, Element el, Namespace thisns, Boolean isAuthority) {
		Record r = record;
		//
		// If we're dealing with an Authority/Vocabulary then we need to use the base Authority/Vocabulary record and
		// not the term/item record.
		//
		if (isAuthority == true) {
			Spec spec = r.getSpec();
			r = spec.getRecord(BASE_AUTHORITY_RECORD);
		}
		
		Element dhele = el.addElement(new QName("initHandler", thisns));
		Element cele = dhele.addElement(new QName("classname", thisns));
		cele.addText(this.tenantSpec.getIndexHandler());
		Element paramsElement = dhele.addElement(new QName("params", thisns));
		
		boolean noIndexedFields = true;
		//loop over all fields to find out is they should be indexed
		for (FieldSet f: r.getAllFieldFullList("")) {
			if (f instanceof Field) {
				Field fd = (Field)f;
				String fieldName = fd.getServicesTag().toLowerCase();
				String sectionName = fd.getSection();
				if (fd.shouldIndex() == true) {
					Element fieldElement = paramsElement.addElement(new QName("field", thisns));
					Element tableElement = fieldElement.addElement(new QName("table", thisns));
					String tableName = r.getServicesSchemaName(sectionName);
					if (isAuthority == true) {
						tableName = record.getAuthoritySchemaName();
					}
					tableElement.addText(tableName);

					Element columnElement = fieldElement.addElement(new QName("col", thisns));
					columnElement.addText(fieldName);
					noIndexedFields = false;
				}
			}
		}
		
		if (noIndexedFields == true) {
			// Since there were no fields to index, we don't need this empty <initHandler> element
			el.remove(dhele);
		}
	}
	
	/**
	 * <service:DocHandlerParams>
	 * @param r
	 * @param el
	 * @param thisns
	 * @param section
	 * @param isAuthority
	 */
	private void doDocHandlerParams(Record r, Element el, Namespace thisns, String section, Boolean isAuthority) { //FIXME: Rename this method to doDocHandlerParms
		//<service:DocHandlerParams>
		Element dhele = el.addElement(new QName("DocHandlerParams", thisns));
		Element pele = dhele.addElement(new QName("params", thisns));
		Element lrele = pele.addElement(new QName("ListResultsFields", thisns));
		doLists(r, lrele, thisns, section, isAuthority);
	}

	/**
	 * are we in common or a domain area?
	 * @param r
	 */
	@Deprecated
	private void setDomain(Record r) {
		if (!this.defaultonly) {
			this.domainsection = ""; // assumes only one domain
			for (String section : r.getServicesRecordPaths()) {
				if (!section.equals("common")
						&& !section.equals(COLLECTIONSPACE_CORE_PART_NAME)) {
					this.domainsection = section; // assumes only one domain
				}
			}
		}
	}
	
	/*
	 * Have we already processed this Service part?
	 */
	private boolean inProcessedPartsList(ArrayList<String> processedPartsList, String partName) {
		boolean result = false;
		
		for (String processedPart : processedPartsList) {
			if (processedPart.equalsIgnoreCase(partName) == true) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * domain specific tenant binding
	 */
	private void makeLowerParts(Record r, ArrayList<String> processedPartsList, Namespace thisns, Element cele, String label, String labelsg, Boolean isAuthority){
		Integer num = processedPartsList.size();
		
		for (String servicePart : r.getServicesRecordPaths()) {
			if (inProcessedPartsList(processedPartsList, servicePart) == false) {
				String namespaceURI = r.getServicesSchemaNameSpaceURI(servicePart);
				String schemaLocationCommon = namespaceURI + " " + r.getServicesSchemaBaseLocation() + labelsg + "/" + label + ".xsd";			
				makePart(r, cele, num.toString(), r.getServicesPartLabel(servicePart),
						num.toString(), namespaceURI, schemaLocationCommon, thisns, false, servicePart);
				processedPartsList.add(servicePart);
				num++;
			}
		}
	}
	
	/**
	 * return domain or core specific information
	 * @param r
	 * @param el
	 * @param thisns
	 * @param isAuthority
	 */
	private void doServiceObject(Record r, Element el, Namespace thisns, Boolean isAuthority){
		//<service:object name="Intake" version="0.1">
		Element cele = el.addElement(new QName("object", thisns));

		if (isAuthority == true) {
			cele.addAttribute("name", r.getServicesTenantAuthSg());
		} else {
			cele.addAttribute("name", r.getServicesTenantSg());
		}
		cele.addAttribute("version", "1.0"); // FIXME: Version should not be hardcoded.

		String label = r.getServicesTenantPl().toLowerCase();
		String labelsg = r.getServicesTenantSg().toLowerCase();

		if (isAuthority == true) {
			label = r.getServicesTenantAuthPl().toLowerCase();
			labelsg = r.getServicesTenantAuthSg().toLowerCase();
		}

		// Common, core, etc
		ArrayList<String> processedParts = makeUpperParts(r, thisns, cele, label, labelsg, isAuthority);
		
		// Local and domain extensions
		makeLowerParts(r, processedParts, thisns, cele, label, labelsg, isAuthority);
	}

	/**
	 * core specific tenantbinding
	 * @param r
	 * @param thisns
	 * @param cele
	 * @param label
	 * @param labelsg
	 * @param isAuthority
	 */
	private ArrayList<String> makeUpperParts(Record r, Namespace thisns, Element cele,
			String label, String labelsg, Boolean isAuthority) {
		ArrayList<String> processedParts = new ArrayList<String>();
		
		Integer num = 0;
		//<service:part id="0" control_group="Managed" versionable="true" auditable="false" label="intakes-system" updated="" order="0">
		String schemaLocationSystem = COLLECTIONSPACE_SCHEMA_LOCATION_SYSTEM;
		makePart(r, cele, num.toString(), label + "-" + COLLECTIONSPACE_SYSTEM_PART_NAME,
				num.toString(), COLLECTIONSPACE_NAMESPACE_URI_SYSTEM, schemaLocationSystem, thisns, false, COLLECTIONSPACE_SYSTEM_PART_NAME);
		processedParts.add(COLLECTIONSPACE_SCHEMA_LOCATION_SYSTEM);
		num++;

		// <service:part id="1" control_group="Managed" versionable="true" auditable="false" label="intakes_common" updated="" order="1">
		if (r.hasServicesRecordPath(COLLECTIONSPACE_COMMON_PART_NAME)) {
			String namespaceURI = r.getServicesSchemaNameSpaceURI(COLLECTIONSPACE_COMMON_PART_NAME);
			String schemaLocationCommon = namespaceURI + " " + r.getServicesSchemaBaseLocation() + labelsg + "/" + label + "_common.xsd";			
			if (r.isType("authorizationdata")) {
				schemaLocationCommon = namespaceURI + " "+ r.getServicesSchemaBaseLocation()  + "/" + label + ".xsd";
			}
			String servicesPartLabel = r.getServicesPartLabel(COLLECTIONSPACE_COMMON_PART_NAME);
			if (isAuthority == true) {
				servicesPartLabel = r.getAuthoritySchemaName();
			}
			makePart(r, cele, num.toString(), servicesPartLabel,
					num.toString(), namespaceURI, schemaLocationCommon, thisns, !isAuthority, COLLECTIONSPACE_COMMON_PART_NAME); // We don't support term and auth refs for Authority item/term parents -i.e., Authorities.
			processedParts.add(COLLECTIONSPACE_COMMON_PART_NAME);
			num++;
		}

		// <service:part id="2" control_group="Managed" versionable="true" auditable="false" label="collectionspace_core" updated="" order="2">
		if (r.hasServicesRecordPath(COLLECTIONSPACE_CORE_PART_NAME)) {
			String namespaceURI = r.getServicesSchemaNameSpaceURI(COLLECTIONSPACE_CORE_PART_NAME);
			String schemaLocationCore = namespaceURI + " " + r.getServicesSchemaBaseLocation() + "/collectionspace_core.xsd";
			makePart(r, cele, num.toString(), r.getServicesPartLabel(COLLECTIONSPACE_CORE_PART_NAME),
					num.toString(), namespaceURI, schemaLocationCore, thisns, false, COLLECTIONSPACE_CORE_PART_NAME);
			processedParts.add(COLLECTIONSPACE_CORE_PART_NAME);
			num++;
		}
	
		return processedParts;
	}
	
	/**
	 * vocabs have 2 parts in the tenant binding
	 * @param r
	 * @param el
	 */
	private void addVocabularies(Record r, Element el) {		
		//add standard procedure like bit
		Element cele = el.addElement(new QName("serviceBindings", nstenant));
		cele.addAttribute("name", r.getServicesTenantPl());
		cele.addAttribute("version", "0.1");
		addServiceBinding(r, cele, nsservices, false);

		//add vocabulary bit
		Element cele2 = el.addElement(new QName("serviceBindings", nstenant));
		cele2.addAttribute("name", r.getServicesTenantAuthPl());
		cele2.addAttribute("version", "0.1");
		addServiceBinding(r, cele2, nsservices, true);

	}
	
	/**
	 * Add Authorities into the mix
	 * seems to be pretty standard...
	 * @param r
	 * @param el
	 */
	private void addAuthorization(Record r, Element el){
		//add standard procedure like bit
		Element cele = el.addElement(new QName(
				"serviceBindings", nstenant));
		cele.addAttribute("name", r.getServicesTenantPl());
		cele.addAttribute("version", "0.1");
		addServiceBinding(r, cele, nsservices, false);
	}
	
	private void addServiceBinding(Record r, Element el, Namespace nameSpace, Boolean isAuthority) {
		String repositoryDomain = r.getServicesRepositoryDomain();
		if (repositoryDomain == null) {
			repositoryDomain = this.tenantSpec.getRepositoryDomain(); // Assume the default tenant repo if the service hasn't specified one
		}
		
		if (repositoryDomain.equalsIgnoreCase(NO_REPO_DOMAIN) == false) {  // Note that NO_REPO_DOMAIN is different than "null" or not specified.
			//<service:repositoryDomain>default-domain</service:repositoryDomain>
			Element repository = el.addElement(new QName("repositoryDomain", nameSpace));		
			repository.addText(this.tenantSpec.getRepositoryDomain());
		}
		
		//<service:documentHandler>
		String docHandlerName = r.getServicesDocHandler();
		Element docHandlerElement = el.addElement(new QName("documentHandler", nameSpace));
		docHandlerElement.addText(docHandlerName);
		
		//<service:DocHandlerParams> include fields to show in list results
		doDocHandlerParams(r, el, this.nsservices, this.domainsection, isAuthority);

		//<service:validatorHandler>
		Element validatorHandlerElement = el.addElement(new QName("validatorHandler", nameSpace));
		validatorHandlerElement.addText(r.getServicesValidatorHandler());
		
		//<service:initHandler> which fields need to be modified in the nuxeo db
		doInitHandler(r, el, this.nsservices, isAuthority);

		//<service:properties>
		Element servicePropertiesElement = el.addElement(new QName("properties", nameSpace));		
		if (doServiceProperties(r, servicePropertiesElement, this.nsservices, isAuthority) == false) {
			el.remove(servicePropertiesElement);
		}
		
		//<service:object>
		doServiceObject(r, el, this.nsservices, isAuthority);
	}
	
	/*
	 * Returns the fully qualified path of a field name
	 */
	private String getFullyQualifiedFieldPath(FieldSet in) {
		FieldSet fst = in;
		String name = "";
		while (fst.getParent().isTrueRepeatField() || fst.getParent() instanceof Group) {
			fst = (FieldSet) fst.getParent();
			if (fst instanceof Repeat) {
				Repeat rt = (Repeat) fst;
				if (rt.hasServicesParent()) {
					name += rt.getServicesParent()[0];
				} else {
					name += rt.getServicesTag();
				}
			} else {
				Group gp = (Group) fst;
				if (gp.hasServicesParent()) {
					name += gp.getServicesParent()[0];
				} else {
					name += gp.getServicesTag();
				}
			}
			name += "/[0]/";
		}
		name += in.getServicesTag();
		
		return name;
	}

	/*
	 * Added an element like the following to the passed in "props" element.
	 * 
	 * <types:item xmlns:types="http://collectionspace.org/services/config/types">
	 *		<types:key>authRef</types:key>
	 *     	<types:value>borrower</types:value>
	 * </types:item>
	 */
	private void addServiceProperty(Element props, FieldSet fs, String keyName, Namespace types) {
		//<item>
		Element itemElement = props.addElement(new QName("item", types));
		
		//<key>
		Element keyElement = itemElement.addElement(new QName("key", types));
		keyElement.addText(keyName);		
		
		//<value>
		Element valueElement = itemElement.addElement(new QName("value", types));
		String fieldPath = this.getFullyQualifiedFieldPath(fs);
		valueElement.addText(fieldPath);	
	}

	private boolean doServiceProperties(Record record, Element props, Namespace types, boolean isAuthority) {
		boolean result = false;
		Record r = record;
		//
		// If we're dealing with an Authority/Vocabulary then we need to use the base Authority/Vocabulary record and
		// not the term/item record.
		//
		if (isAuthority == true) {
			Spec spec = r.getSpec();
			r = spec.getRecord(BASE_AUTHORITY_RECORD);
		}		
		
		FieldSet objNameProp = r.getMiniSummary();
		if (objNameProp != null) {
			this.addServiceProperty(props, objNameProp, OBJECT_NAME_PROPERTY, types);
			result = true;
		}

		FieldSet objNumberProp = r.getMiniNumber();
		if (objNumberProp != null) {
			this.addServiceProperty(props, objNumberProp, OBJECT_NUMBER_PROPERTY, types);
			result = true;
		}
				
		return result;
	}
			
	//defines fields to show in list results
	private void doLists(Record record, Element el, Namespace thisns, String section, boolean isAuthority) {
		Record r = record;
		//
		// If we're dealing with an Authority/Vocabulary then we need to use the base Authority/Vocabulary record and
		// not the term/item record.
		//
		if (isAuthority == true) {
			Spec spec = r.getSpec();
			r = spec.getRecord(BASE_AUTHORITY_RECORD);
			section = COLLECTIONSPACE_COMMON_PART_NAME;
		}
		
		for (FieldSet fs : r.getAllMiniSummaryList()) {
			if (fs.isInServices() && fs.getSection().equals(section)) {
				String fieldNamePath = this.getFullyQualifiedFieldPath(fs);

				Element lrf = el.addElement(new QName("ListResultField", thisns));
				Element elrf = lrf.addElement(new QName("element", thisns));
				if (!section.equals(COLLECTIONSPACE_COMMON_PART_NAME)) {
					Element slrf = lrf.addElement(new QName("schema", thisns));
					slrf.addText(r.getServicesSchemaName(section));
				}
				Element xlrf = lrf.addElement(new QName("xpath", thisns));

				elrf.addText(fs.getServicesTag());
				xlrf.addText(fieldNamePath);
			}
		}
	}

	//
	// It's possible that the repeat is of a structured "group" type.  If so, we need to
	// create an XPath of the form <yxzGroupList>/*/<fieldName>.  To do this we need to split
	// the 'fullid' of this repeat fieldset into its component parts.
	//
	private String getXpathStructuredPart(Repeat repeat) {
		String result = null;
		
		String[] parts = repeat.getfullID().split("/");
		result = parts[0];
		if (parts.length > 1) { // If we have more than one part, we have a repeating structure.
			result = result + GROUP_XPATH_SUFFIX;
		}
		
		return result;
	}
	
	private void createAuthRef(Element auth, Namespace types, Record r, String section, FieldSet in) {
		if (in.getSection().equals(section) && in.hasAutocompleteInstance()) {
			Boolean isEnum = false;
			if (in instanceof Field) {
				isEnum = (((Field) in).getUIType()).equals("enum");
			}
			
			Element tele = auth.addElement(new QName("item", types));
			Element tele2 = tele.addElement(new QName("key", types));
			Element tele3 = tele.addElement(new QName("value", types));
			String refType = AUTH_REF;
			if (isEnum == true) {
				refType = TERM_REF;
			}
			tele2.addText(refType);
			String name = "";
			FieldSet fs = (FieldSet) in;
			while (fs.getParent() instanceof Repeat	|| fs.getParent() instanceof Group) {
				String xpathStructuredPart = fs.getServicesTag();
				if (fs.getParent() instanceof Repeat) {
					xpathStructuredPart = getXpathStructuredPart((Repeat)fs.getParent());
					log.debug("We've got a repeat");
				} else {
					log.debug("We've got a Group"); // I haven't yet seen an example of this in the App config files.
				}
				fs = (FieldSet) fs.getParent();
				name += xpathStructuredPart + "/";
			}
			name += in.getServicesTag();
			tele3.addText(name);
		}
	}
	
	/*
	 * Creates a set of Service binding authrefs of the following form:
	 *  <types:item>
	 *      <types:key>authRef</types:key>
	 *      <types:value>currentOwner</types:value>
	 *  </types:item>
	 */
	private void doAuths(Element auth, Namespace types, Record r, String section) {
		for (FieldSet in : r.getAllFieldFullList("")) {
			if (in.isASelfRenderer() == true) {
				String fieldSetServicesType = in.getServicesType(false /* not NS qualified */);
				Spec spec = in.getRecord().getSpec();
				Record subRecord = spec.getRecord(fieldSetServicesType); // find a record that corresponds to the fieldset's service type
				//
				// Iterate through each field of the subrecord
				//
				doAuths(auth, types, subRecord, section);
					
			} else {
				createAuthRef(auth, types, r, section, in);
			}
		}
	}
}
