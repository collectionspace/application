package org.collectionspace.chain.installation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import org.collectionspace.chain.csp.persistence.services.TenantSpec;
import org.collectionspace.chain.csp.persistence.services.TenantSpec.RemoteClient;
import org.collectionspace.chain.csp.schema.EmailData;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.services.common.api.FileTools;
import org.collectionspace.services.common.api.Tools;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

public class ServiceBindingsGeneration {
	private static final Logger log = LoggerFactory.getLogger(ServiceBindingsGeneration.class);
	
	private static final String RECORD_TYPE_VOCABULARY = "vocabulary";
	private static final String RECORD_TYPE_AUTHORITY = "authority";
	private static final String RECORD_TYPE_UTILITY = "utility";
	private static final String RECORD_TYPE_OBJECT = "object";
	private static final String RECORD_TYPE_PROCEDURE = "procedure";
	private static final String RECORD_TYPE_RECORD = "record";
	private static final String RECORD_TYPE_USERDATA = "userdata";
	private static final String RECORD_TYPE_AUTHORIZATIONDATA = "authorizationdata";
	
	private static final String RECORD_ID_TERMLIST = "termlist";
	private static final String RECORD_ID_VOCAB = "vocab";
	
	private static final String GROUP_LIST_SUFFIX = "/[0]";
	private static final String GROUP_XPATH_SUFFIX = "/*";
	private static final String NO_REPO_DOMAIN = "none";

	private static final String SERVICES_BINDING_TYPE_SECURITY = "security";

	private static final String AUTH_REF = "authRef";
	private static final String TERM_REF = "termRef";

	private static final String OBJECT_NAME_PROPERTY = "objectNameProperty";
	private static final String OBJECT_NUMBER_PROPERTY = "objectNumberProperty";

	private static final String BASE_AUTHORITY_RECORD = "baseAuthority";
	private static final String BASE_AUTHORITY_SECTION = BASE_AUTHORITY_RECORD;

	private static final String DEFAULT_SERVICEOBJECT_ID = "1";

	private static final String DEFAULT_HIERARCHY_TYPE = "screen";

	private static final boolean NOT_NAMESPACED = false;

	private AuthCache authCache = new AuthCache();
	
	protected File configFile;
	protected Spec spec;
	protected TenantSpec tenantSpec;
	protected Boolean defaultonly;
	protected String domainsection;
	protected File tempDirectory;
	protected File infoOutputDirectory;

	/**
	 * not sure how configurable these need to be - they can be made more flexible
	 */
	protected Namespace nsxsi = new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");  
	protected Namespace nstenant = new Namespace("tenant", "http://collectionspace.org/services/config/tenant"); // http://collectionspace.org/services/config/tenant
	protected Namespace nsservices = new Namespace("service", "http://collectionspace.org/services/config/service"); 
	protected Namespace nstypes = new Namespace("types", "http://collectionspace.org/services/config/types"); 
	
	protected String schemaloc = "http://collectionspace.org/services/config/tenant " +
	"http://collectionspace.org/services/config/tenant.xsd";

	private ServiceBindingsGeneration() {
		// Intentionally left blank?
	}
	
	public ServiceBindingsGeneration(File configFile, Spec spec,TenantSpec td, Boolean isdefault, File infoOutputDir) throws IOException {
		this.configFile = configFile;
		this.spec = spec;
		this.tenantSpec = td;
		this.defaultonly = isdefault;
		this.domainsection = "common";
		this.tempDirectory = FileTools.createTmpDir("cspace-bindings-");
		this.infoOutputDirectory = infoOutputDir;
	}
	
	public File getTempDirectory() {
		return this.tempDirectory;
	}	
	
	public File getOutputDirectory() {
		return this.infoOutputDirectory;
	}

	public String  doit(String serviceBindingVersion) {
		return doServiceBindingsCommon(serviceBindingVersion);
	}

	//
	// Try to figure out what the service binding type should be for this record. Good luck.
	//
	private String getServiceBindingType(Record record) {
		String result = RECORD_TYPE_PROCEDURE;
		
		if (record.isType(RECORD_TYPE_RECORD) == true) {
			if (record.isType(RECORD_TYPE_PROCEDURE) == false) {
				result = RECORD_TYPE_OBJECT;
			} else if (record.isType(RECORD_TYPE_VOCABULARY)) {
				if (record.isInRecordList() == true) {	// termlist is in the "record list"
					result = RECORD_TYPE_UTILITY;
				} else {
					result = RECORD_TYPE_VOCABULARY;	// termlistitem is *not* in the "record list"			
				}
			} else {
				log.debug(String.format("Config Generation: '%s' - Record '%s' is neither an object nor a vocabulary.  Please verify it is a procedure.", 
						getConfigFile().getName(), record.getRecordName()));
			}
		} else {
			log.warn(String.format("Config Generation: '%s' - Record '%s' is not of type 'record'.  We're going to assume it is a 'procedure' but please verify by hand.",
					getConfigFile().getName(), record.getRecordName()));
		}
		
		return result;
	}
	
	private boolean shouldGenerateServiceBinding(Record record) {
		boolean result = false;

		if ((record.isInRecordList() == true || record.isType(RECORD_TYPE_VOCABULARY) == true)
				&& !record.getRecordName().equals(RECORD_ID_VOCAB)) {
			result = true;
		}

		return result;
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
	private String doServiceBindingsCommon(String serviceBindingVersion) {
		Document doc = DocumentFactory.getInstance().createDocument();
		
		Element root = doc.addElement(new QName("TenantBindingConfig", this.nstenant));
		root.addAttribute("xsi:schemaLocation", this.schemaloc);
		root.add(this.nsxsi);

		// <tenant:tenantBinding version="0.1">
		Element ele = root.addElement(new QName("tenantBinding", nstenant));
		if (!this.defaultonly) {
			ele.addAttribute("id", this.tenantSpec.getTenantId());
			ele.addAttribute("name", this.tenantSpec.getTenant());
			ele.addAttribute("displayName", this.tenantSpec.getTenantDisplay());
			ele.addAttribute("createDisabled", Boolean.toString(this.tenantSpec.getCreateDisabled()));
		}
		ele.addAttribute("version", this.tenantSpec.getTenantVersion());

		// <tenant:repositoryDomain name="default-domain" repositoryClient="nuxeo-java"/>
		Element rele = ele.addElement(new QName("repositoryDomain", nstenant));
		rele.addAttribute("name", this.tenantSpec.getRepositoryDomain());
		rele.addAttribute("storageName", this.tenantSpec.getStorageName());
                if (Tools.notEmpty(this.tenantSpec.getRepositoryName())) {
		    rele.addAttribute("repositoryName", this.tenantSpec.getRepositoryName());
                }
		rele.addAttribute("repositoryClient", this.tenantSpec.getRepositoryClient());
		
		// Set remote clients
		makeRemoteClients(ele);
		
		// Set the bindings for email notifications
		makeEmailBindings(ele);

		// add in <tenant:properties> if required
		makeProperties(ele);

		// loop over each record type and add a <tenant:serviceBindings> element
		for (Record r : this.spec.getAllRecords()) {
			String tenantName = this.tenantSpec.getTenant();
			String tenantId = this.tenantSpec.getTenantId();
			String recordName = r.getRecordName();
			
			if (log.isDebugEnabled()) {
				log.debug(String.format("Config Generation: '%s' - Processing potential service bindings for record type '%s'", 
						getConfigFile().getName(), recordName));
			}

			if (shouldGenerateServiceBinding(r) == true) {
				Element serviceBindingsElement = null;
				if (r.isType(RECORD_TYPE_RECORD) == true) { // Records can be of several types -i.e., can be both a "record" type and a "vocabulary" type
					String rtype = getServiceBindingType(r);
					// e.g., <tenant:serviceBindings name="CollectionObjects" type="object" version="0.1">
					serviceBindingsElement = ele.addElement(new QName("serviceBindings", nstenant));
					serviceBindingsElement.addAttribute("id", r.getServicesTenantPl());
					serviceBindingsElement.addAttribute("name", r.getServicesTenantPl());
					serviceBindingsElement.addAttribute("type", rtype);
					serviceBindingsElement.addAttribute("version", serviceBindingVersion);
					if (r.isType(RECORD_TYPE_VOCABULARY) == true) {
						serviceBindingsElement.addAttribute(Record.REQUIRES_UNIQUE_SHORTID, Boolean.TRUE.toString()); // Vocabularies need unique short IDs
						serviceBindingsElement.addAttribute(Record.SUPPORTS_REPLICATING, Boolean.toString(r.supportsReplicating()));  //  they also need to support replication
						String remoteClientConfigName = r.getRemoteClientConfigName();
						if (remoteClientConfigName != null && remoteClientConfigName.isEmpty() == false) {
							serviceBindingsElement.addAttribute(Record.REMOTECLIENT_CONFIG_NAME, remoteClientConfigName);
						}
					}
					addServiceBinding(r, serviceBindingsElement, nsservices, false, serviceBindingVersion);
				} else if (r.isType(RECORD_TYPE_AUTHORITY)) {
					// e.g., <tenant:serviceBindings id="Persons" name="Persons" type="authority" version="0.1">
					addAuthorities(r, ele, serviceBindingVersion);
				} else if (r.isType(RECORD_TYPE_USERDATA)) {
					serviceBindingsElement = ele.addElement(new QName("serviceBindings", nstenant));
					serviceBindingsElement.addAttribute("id", r.getServicesTenantPl());
					serviceBindingsElement.addAttribute("name", r.getServicesTenantPl());
					serviceBindingsElement.addAttribute("type", SERVICES_BINDING_TYPE_SECURITY);
					serviceBindingsElement.addAttribute("version", serviceBindingVersion);
					addServiceBinding(r, serviceBindingsElement, nsservices, false, serviceBindingVersion);
				} else if (r.isType(RECORD_TYPE_AUTHORIZATIONDATA)) {
					// ignore at the moment as they are so non standard
					// addAuthorization(r,ele);
					log.debug(String.format("Config Generation: '%s' - Ignoring record '%s:%s' at the moment as it is of type '%s' and so non-standard",
							getConfigFile().getName(), tenantName, r.getRecordName(), RECORD_TYPE_AUTHORIZATIONDATA));
				} else {
					// Should never get here
					log.warn(String.format("Config Generation: '%s' - Record '%s.%s' is of an unknown type so we could not create a service binding for it.",
							getConfigFile().getName(), tenantName, recordName));
				}
				//
				// Debug-log the generated Service bindings for this App layer record
				//
				if (log.isDebugEnabled() == true) {
					Element bindingsForRecord = serviceBindingsElement;
					if (bindingsForRecord != null && !r.isType(RECORD_TYPE_AUTHORITY)) {
						this.debugWriteToFile(r, bindingsForRecord, false);
					}
				}
			} else {
				log.trace(String.format("Config Generation: '%s' - Skipping record '%s' because it does not require a service binding.",
						getConfigFile().getName(), recordName));
			}
		}
		
		//
		// Print out a reverse-lookup JSON rep of the terms
		//
		String jsonPayload = authCache.toString(4).toLowerCase();
		String authCacheFileName = this.getOutputDirectory().getAbsolutePath() + File.separator
					+ this.configFile.getName() + ".json";
		authCacheFileName = authCacheFileName.replace(".xml", ".terms");
		File outfile = new File(authCacheFileName);
		try {
			FileUtils.writeStringToFile(outfile, jsonPayload, Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info(String.format("Reverse terms lookup JSON payload written here: '%s'.", authCacheFileName));
		
		return doc.asXML(); 
	}
	
	//
	// For debugging purposes, this method writes a service bindings element to disk.
	//
	void debugWriteToFile(Record r, Element bindingsForRecord, boolean isAuthority) {
		if (log.isDebugEnabled() == true) {
			String tenantName = this.tenantSpec.getTenant();
			String recordName = r.getRecordName();
			if (isAuthority == true) {
				recordName = r.getServicesTenantAuthPl();
			}
			//
			// Write out the service bindings if log level is TRACE
			//
			String serviceBindings = bindingsForRecord.asXML();
			log.trace(String.format("Config Generation: '%s' - Service bindings for record='%s': %s", getConfigFile().getName(), recordName, serviceBindings));
			//
			// Write the service bindings to a file for debugging/troubleshooting.
			//
			String serviceBindingsFileName = String.format("%s.%s.bindings.xml", tenantName, recordName);
			File serviceBindingsFile = new File(getTempDirectory(), serviceBindingsFileName); // Write this to a temp directory for debugging purposes
			try {
				FileUtils.writeStringToFile(serviceBindingsFile, serviceBindings);
				log.debug(String.format("Config Generation: '%s' - Wrote Service bindings for record='%s' to file: %s",
						getConfigFile().getName(), recordName, serviceBindingsFile.getAbsoluteFile()));
			} catch (Exception e) {
				log.debug(String.format("Config Generation: '%s' - Could not write Service bindings for record='%s' to file: %s",
						getConfigFile().getName(), recordName, serviceBindingsFile.getAbsoluteFile()), e);
			}
		}
	}

	private void makeRemoteClients(Element tenantBindingElement) {
		RemoteClient[] remoteClientList = tenantSpec.getRemoteClients();
		
		if (remoteClientList != null && remoteClientList.length > 0) {
			Element remoteClientsElement = tenantBindingElement.addElement(new QName("remoteClientConfigurations", nstenant));
			for (TenantSpec.RemoteClient remoteClient : remoteClientList) {
				Element remoteClientElement = remoteClientsElement.addElement(new QName("remoteClientConfig", nstenant));			
				Element ele;
				
				ele = remoteClientElement.addElement(new QName("name", nstenant));
				ele.addText(remoteClient.getName());
				
				ele = remoteClientElement.addElement(new QName("url", nstenant));
				ele.addText(remoteClient.getUrl());
				
				ele = remoteClientElement.addElement(new QName("user", nstenant));
				ele.addText(remoteClient.getUser());
				
				ele = remoteClientElement.addElement(new QName("password", nstenant));
				ele.addText(remoteClient.getPassword());
				
				// Optional
				if (remoteClient.getTenantId() != null) {
					ele = remoteClientElement.addElement(new QName("tenantId", nstenant));
					ele.addText(remoteClient.getTenantId());
				}
				
				// Optional
				if (remoteClient.getTenantName() != null) {
					ele = remoteClientElement.addElement(new QName("tenantName", nstenant));
					ele.addText(remoteClient.getTenantName());
				}
				
				ele = remoteClientElement.addElement(new QName("ssl", nstenant));
				ele.addText(remoteClient.getSSL());
				
				ele = remoteClientElement.addElement(new QName("auth", nstenant));
				ele.addText(remoteClient.getAuth());
			}
		}
	}
	
	/*
	 * Builds something like the following XML:
	 *   <tenant:emailConfig>
            <tenant:baseurl>http://qa.collectionspace.org</tenant:baseurl>
            <tenant:from>admin@collectionspace.org</tenant:from>
            <tenant:smtpConfig>
                <tenant:host>smtp.gmail.com</tenant:host>
                <tenant:port>587</tenant:port>
                <tenant:debug>true</tenant:debug>
                <tenant:smtpAuth enabled="true">
                    <tenant:username>tom@example.com</tenant:username>
                    <tenant:password>password123</tenant:password>
                </tenant:smtpAuth>
            </tenant:smtpConfig>
            <tenant:passwordResetConfig>
                <tenant:tokenExpirationDays>3600</tenant:tokenExpirationSeconds>
                <tenant:loginpage>/collectionspace/ui/core/html/index.html</tenant:loginpage>
                <tenant:subject>CollectionSpace Password Reset</tenant:subject>
                <tenant:message>You've started the process to reset your CollectionSpace account password.</tenant:message>
            </tenant:passwordResetConfig>
        </tenant:emailConfig>
	 */
	private void makeEmailBindings(Element tenantBindingElement) {
		EmailData emailData = spec.getEmailData();
		
		if (emailData != null) {
			Element emailConfigElement = tenantBindingElement.addElement(new QName("emailConfig", nstenant));
			if (emailData.getBaseURL() != null) {
				Element ele = emailConfigElement.addElement(new QName("baseurl", nstenant));
				ele.addText(emailData.getBaseURL());
			}
			
			if (emailData.getFromAddress() != null) {
				Element ele = emailConfigElement.addElement(new QName("from", nstenant));
				ele.addText(emailData.getFromAddress());
			}
			
			//
			// Build <tenant:smtpConfig>
			//
			Element smtpConfigElement = emailConfigElement.addElement(new QName("smtpConfig", nstenant));
			if (emailData.getSMTPHost() != null) {
				Element ele = smtpConfigElement.addElement(new QName("host", nstenant));
				ele.addText(emailData.getSMTPHost());
			}
			
			if (emailData.getSMTPPort() != null) {
				Element ele = smtpConfigElement.addElement(new QName("port", nstenant));
				ele.addText(emailData.getSMTPPort());
			}
			
			if (emailData.doSMTPDebug() != null) {
				Element ele = smtpConfigElement.addElement(new QName("debug", nstenant));
				ele.addText(emailData.doSMTPDebug().toString());
			}
			
			//
			// Build <tenant:smtpAuth enabled="false">
			//
			Element smtpAuthElement = smtpConfigElement.addElement(new QName("smtpAuth", nstenant));
			if (emailData.doSMTPAuth() != null) {
				smtpAuthElement.addAttribute("enabled", emailData.doSMTPAuth().toString());
			}
			
			if (emailData.getSMTPAuthUsername() != null) {
				Element ele = smtpAuthElement.addElement(new QName("username", nstenant));
				ele.addText(emailData.getSMTPAuthUsername());
			}
			
			if (emailData.getSMTPAuthPassword() != null) {
				Element ele = smtpAuthElement.addElement(new QName("password", nstenant));
				ele.addText(emailData.getSMTPAuthPassword());
			}
			
			//
			// Build <tenant:passwordResetConfig>
			//
			Element passwordResetElement = emailConfigElement.addElement(new QName("passwordResetConfig", nstenant));
			if (emailData.getTokenExpirationSeconds() != null) {
				Element ele = passwordResetElement.addElement(new QName("tokenExpirationSeconds", nstenant));
				ele.addText(emailData.getTokenExpirationSeconds().toString());
			} else if (emailData.getTokenExpirationDays() != null) {
				//
				// If the new 'tokenExpirationSeconds' config field is not set, then convert the 'daysvalid' days value
				// into seconds; otherwise, ignore the 'daysvalid' field and use the configured 'tokenExpirationSeconds' value.
				//
				Element ele = passwordResetElement.addElement(new QName("tokenExpirationSeconds", nstenant));
				Integer secondsValid = emailData.getTokenExpirationDays() * 60 * 60 * 24; // Convert days into seconds
				ele.addText(emailData.getTokenExpirationSeconds().toString());
			}
			
			if (emailData.getLoginUrl() != null) {
				Element ele = passwordResetElement.addElement(new QName("loginpage", nstenant));
				ele.addText(emailData.getLoginUrl());
			}
			
			if (emailData.getPasswordResetSubject() != null) {
				Element ele = passwordResetElement.addElement(new QName("subject", nstenant));
				ele.addText(emailData.getPasswordResetSubject());
			}

			if (emailData.getPasswordResetMessage() != null) {
				Element ele = passwordResetElement.addElement(new QName("message", nstenant));
				ele.addText(emailData.getPasswordResetMessage());
			}
		}
	}	
	
	/**
	 * Add in date formats and languages if required
	 * @param ele
	 */
	private void makeProperties(Element tenantBindingElement) {
		Boolean showLang = true;
		Boolean showDate = true;
		if (!this.defaultonly) {
			if (tenantSpec.isDefaultLanguage()) {
				showLang = false;
			}
			if (tenantSpec.isDefaultDate()) {
				showDate = false;
			}
		}

		if (showDate || showLang) {
			Element pele = tenantBindingElement.addElement(new QName("properties", nstenant));
			if (showDate) {
				for (String dateformat : tenantSpec.getDateFormats()) {
					Element tele = pele.addElement(new QName("item", this.nstypes));
					Element tele2 = tele.addElement(new QName("key", this.nstypes));
					Element tele3 = tele.addElement(new QName("value", this.nstypes));
					tele2.addText("datePattern");
					tele3.addText(dateformat);
				}
			}
			if (showLang) {
				for (String lang : tenantSpec.getLanguages()) {
					Element tele = pele.addElement(new QName("item", this.nstypes));
					Element tele2 = tele.addElement(new QName("key", this.nstypes));
					Element tele3 = tele.addElement(new QName("value", this.nstypes));
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
		
		if (addAuths) {
			Element authRefsElement = pele.addElement(new QName("properties", thisns));
			boolean createdAuthRefs = doAuthRefsAndTermRefs(authRefsElement, this.nstypes, r, section);
			if (createdAuthRefs == false) {
				pele.remove(authRefsElement);
			}
		}
	}
	
	private String getServiceTableName(FieldSet fieldSet, Record record, Boolean isAuthority) {
		String result = record.getServicesSchemaName(fieldSet.getSection());		
		if (isAuthority == true) {
			result = record.getAuthoritySchemaName();
		}
		//
		// If the parent is a Repeat instance then we need to use a different method to
		// get the table name.
		//
		FieldParent parent = fieldSet.getParent();
		if (parent instanceof Repeat) {
			Repeat fieldParent = (Repeat)parent;
			result = fieldParent.getServicesTag().toLowerCase();
		} else {
			
		}
		
		return result;
	}
	
	/*
	 * Attempt to create an entry in the service bindings for this field to be indexed.  Returns true if created and false if not.
	 */
	private boolean createInitFieldEntry(Element paramsElement, Namespace namespace, Record record, FieldSet f, Boolean isAuthority) {
		boolean result = false;
		
		if (f instanceof Field) {
			Field fd = (Field)f;
			String fieldName = fd.getServicesTag().toLowerCase();
			if (fd.shouldIndex() == true) {
				Element fieldElement = paramsElement.addElement(new QName("field", namespace));
				
				Element tableElement = fieldElement.addElement(new QName("table", namespace));				
				String tableName = fd.getServiceTableName(isAuthority); //getServiceTableName(fd, record, isAuthority);
				tableElement.addText(tableName);

				Element columnElement = fieldElement.addElement(new QName("col", namespace));
				columnElement.addText(fieldName);
				result = true;
			}
		} else {
			log.trace(String.format("Config Generation: '%s' - Ignoring FieldSet instance '%s:%s'",
					getConfigFile().getName(), Arrays.toString(f.getIDPath()), f.getID()));
		}
		
		return result;
	}
	
	/*
	 * Returns true if we end up creating service binding entries for field initialization.
	 */
	private boolean createInitFieldList(Element paramsElement, Namespace namespace, Record record, Boolean isAuthority) {
		boolean result = false;
		
		// Loop through each field to see if we need to create a service binding entry
		for (FieldSet f: record.getAllFieldFullList("")) {
			boolean createdEntry = false;
			
			if (f.isASelfRenderer() == true) {
				//
				// Self rendered fields are essentially sub-records
				//
				String fieldSetServicesType = f.getServicesType(NOT_NAMESPACED);
				Spec spec = f.getRecord().getSpec();
				Record subRecord = spec.getRecord(fieldSetServicesType); // find a record that corresponds to the fieldset's service type
				createdEntry = createInitFieldList(paramsElement, namespace, subRecord, isAuthority); // make a recursive call with the subrecord
			} else {
				createdEntry = createInitFieldEntry(paramsElement, namespace, record, f, isAuthority); // Create a service binding for the field
			}
			//
			// We'll return true even if just one of the fields causes us to create a service binding entry
			//
			if (createdEntry == true) {
				result = true;
			}
		}
			
		return result;
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
			r.setLastAuthorityProxy(record); // Since all the authorities share the same "baseAuthority" record, we need to set the actual authority record; e.g., Person, Organization, etc...
		}
		
		Element dhele = el.addElement(new QName("initHandler", thisns));
		Element cele = dhele.addElement(new QName("classname", thisns));
		cele.addText(this.tenantSpec.getIndexHandler());
		Element paramsElement = dhele.addElement(new QName("params", thisns));
		
		boolean createdIndexedFields = createInitFieldList(paramsElement, thisns, r, isAuthority);
		if (createdIndexedFields == false) {
			// Since there were no fields to index, we don't need this empty <initHandler> element
			el.remove(dhele);
		}
	}
	
	/*
	 * Creates the <> element in the Service binding. Is used to generate the 'refName' in service payloads
	 * For example,
	 * 	<service:RefnameDisplayNameField> <!-- The field used as the display name in an object's refname -->
	 *		<service:element>objectNumber</service:element>
	 *		<service:xpath>objectNumber</service:xpath>
	 *	</service:RefnameDisplayNameField>
	 */
	private void doRefnameDisplayNameField(Record record, Element ele, Namespace thisns) {
		for (FieldSet fieldSet : record.getAllFieldFullList()) {
			if (fieldSet.isServicesRefnameDisplayName() == true) {
				Element doRefnameDisplayNameEle = ele.addElement(new QName("RefnameDisplayNameField", thisns));
				
				Element elrf = doRefnameDisplayNameEle.addElement(new QName("element", thisns));
				elrf.addText(fieldSet.getServicesTag());
				
				Element xlrf = doRefnameDisplayNameEle.addElement(new QName("xpath", thisns));
				String fieldNamePath = this.getFullyQualifiedFieldPath(fieldSet);
				xlrf.addText(fieldNamePath);
				
				break; // Allow only a single <RefnameDisplayNameField> element
			}
		}
	}
	
	/**
     * <service:DocHandlerParams>
     *
     * @param r
     * @param el
     * @param thisns
     * @param isAuthority
     */
    
	private void doDocHandlerParams(Record r, Element el, Namespace nsservices2, Boolean isAuthority) {
		//<service:DocHandlerParams>
		Element dhele = el.addElement(new QName("DocHandlerParams", nsservices2));
		Element pele = dhele.addElement(new QName("params", nsservices2));
				
		if (r.hasHierarchyUsed(DEFAULT_HIERARCHY_TYPE) == true && isAuthority == false) {
			//<service:SupportsHierarchy>true</service:SupportsHierarchy>
			Element sh_rele = pele.addElement(new QName("SupportsHierarchy", nsservices2));
			sh_rele.addText("true");
		}
		
		if (r.supportsVersioning() == true) {
			//<service:SupportsVersioning>true</service:SupportsVersioning>
			Element sh_rele = pele.addElement(new QName("SupportsVersioning", nsservices2));
			sh_rele.addText("true");
		}

		doRefnameDisplayNameField(r, pele, nsservices2);
		
		Element lrele = pele.addElement(new QName("ListResultsFields", nsservices2));
		doLists(r, lrele, nsservices2, isAuthority);
	}

	/**
	 * are we in common or a domain area?
	 * @param r
	 */
	@Deprecated
	private void setDomain(Record r) {
		if (!this.defaultonly) {
			this.domainsection = ""; // assumes only one domain
			for (String section : r.getServicesRecordPathKeys()) {
				if (!section.equals("common")
						&& !section.equals(Record.COLLECTIONSPACE_CORE_PART_NAME)) {
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
	private void makeLowerParts(Record r, ArrayList<String> processedPartsList, Namespace thisns, Element cele,
			String label, String labelsg, Boolean isAuthority) {
		Integer num = processedPartsList.size();

		for (String servicePart : r.getServicesRecordPathKeys()) {
			if (inProcessedPartsList(processedPartsList, servicePart) == false) {
				String schemaNamespace = r.getServicesSchemaNameSpaceURI(servicePart);
				String schemaLocationCommon = r.getXMLSchemaLocation(servicePart);
				makePart(r, cele, num.toString(), r.getServicesPartLabel(servicePart), num.toString(), schemaNamespace,
						schemaLocationCommon, thisns, !isAuthority, servicePart);
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
	private void doServiceObject(Record r, Element el, Namespace thisns, Boolean isAuthority, String serviceBindingVersion){
		Element cele = el.addElement(new QName("object", thisns));

		// the default ID for the service object is "1" since most services have just one service object
		cele.addAttribute("id", DEFAULT_SERVICEOBJECT_ID);

		// For example, <service:object name="Intake" version="0.1">
		String serviceObjectName = r.getServicesTenantDoctype(isAuthority);
		cele.addAttribute("name", serviceObjectName);
		cele.addAttribute("version", serviceBindingVersion);

		// the labels will be used to create the bindings for the parts
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
		String schemaLocationSystem = Record.COLLECTIONSPACE_SCHEMA_LOCATION_SYSTEM;
		makePart(r, cele, num.toString(), label + "-" + Record.COLLECTIONSPACE_SYSTEM_PART_NAME,
				num.toString(), Record.COLLECTIONSPACE_NAMESPACE_URI_SYSTEM, schemaLocationSystem, thisns, false, Record.COLLECTIONSPACE_SYSTEM_PART_NAME);
		processedParts.add(Record.COLLECTIONSPACE_SCHEMA_LOCATION_SYSTEM);
		num++;

		// <service:part id="1" control_group="Managed" versionable="true" auditable="false" label="intakes_common" updated="" order="1">
		if (r.hasServicesRecordPath(Record.COLLECTIONSPACE_COMMON_PART_NAME)) {
			String namespaceURI = r.getServicesSchemaNameSpaceURI(Record.COLLECTIONSPACE_COMMON_PART_NAME);
			String schemaLocationCommon = namespaceURI + " " + r.getServicesSchemaBaseLocation() + "/" + (isAuthority ? r.getRecordName() : labelsg) + "/" + label + "_common.xsd";			
			if (r.isType("authorizationdata")) {
				schemaLocationCommon = namespaceURI + " "+ r.getServicesSchemaBaseLocation()  + "/" + label + ".xsd";
			}
			String servicesPartLabel = r.getServicesPartLabel(Record.COLLECTIONSPACE_COMMON_PART_NAME);
			if (isAuthority == true) {
				servicesPartLabel = r.getAuthoritySchemaName();
			}
			makePart(r, cele, num.toString(), servicesPartLabel,
					num.toString(), namespaceURI, schemaLocationCommon, thisns, !isAuthority, Record.COLLECTIONSPACE_COMMON_PART_NAME); // We don't support term and auth refs for Authority item/term parents -i.e., Authorities.
			processedParts.add(Record.COLLECTIONSPACE_COMMON_PART_NAME);
			num++;
		}

		// <service:part id="2" control_group="Managed" versionable="true" auditable="false" label="collectionspace_core" updated="" order="2">
		if (r.hasServicesRecordPath(Record.COLLECTIONSPACE_CORE_PART_NAME)) {
			String namespaceURI = r.getServicesSchemaNameSpaceURI(Record.COLLECTIONSPACE_CORE_PART_NAME);
			String schemaLocationCore = namespaceURI + " " + r.getServicesSchemaBaseLocation() + "/collectionspace_core.xsd";
			makePart(r, cele, num.toString(), r.getServicesPartLabel(Record.COLLECTIONSPACE_CORE_PART_NAME),
					num.toString(), namespaceURI, schemaLocationCore, thisns, false, Record.COLLECTIONSPACE_CORE_PART_NAME);
			processedParts.add(Record.COLLECTIONSPACE_CORE_PART_NAME);
			num++;
		}
	
		return processedParts;
	}
	
	/**
	 * Since the Application layer declares a single config record for both the Vocabulary/Authority and their corresponding items,
	 * we need to create the two corresponding service bindings from the one Application record.
	 */
	private void addAuthorities(Record r, Element el, String serviceBindingVersion) {		
		// Add the bindings for the Authority/Vocabulary item.
		Element bindingsForAuthorityItemx = el.addElement(new QName("serviceBindings", nstenant));
		bindingsForAuthorityItemx.addAttribute("name", r.getServicesTenantPl());
		bindingsForAuthorityItemx.addAttribute("id", r.getServicesTenantPl());
		bindingsForAuthorityItemx.addAttribute("type", RECORD_TYPE_AUTHORITY);
		bindingsForAuthorityItemx.addAttribute("version", serviceBindingVersion);
		bindingsForAuthorityItemx.addAttribute(Record.SUPPORTS_REPLICATING, Boolean.toString(r.supportsReplicating()));
		bindingsForAuthorityItemx.addAttribute(Record.REQUIRES_UNIQUE_SHORTID, Boolean.TRUE.toString());

		addServiceBinding(r, bindingsForAuthorityItemx, nsservices, false, serviceBindingVersion);
		if (log.isDebugEnabled() == true) {
			this.debugWriteToFile(r, bindingsForAuthorityItemx, false);
		}

		// Add the bindings for the Authority/Vocabulary.
		Element bindingsForAuthority = el.addElement(new QName("serviceBindings", nstenant));
		bindingsForAuthority.addAttribute("name", r.getServicesTenantAuthPl());
		bindingsForAuthority.addAttribute("id", r.getServicesTenantAuthPl());
		bindingsForAuthority.addAttribute("type", RECORD_TYPE_UTILITY);
		bindingsForAuthority.addAttribute("version", serviceBindingVersion);
		bindingsForAuthority.addAttribute(Record.SUPPORTS_REPLICATING, Boolean.toString(r.supportsReplicating()));
		bindingsForAuthority.addAttribute(Record.REQUIRES_UNIQUE_SHORTID, Boolean.TRUE.toString());
		String remoteClientConfigName = r.getRemoteClientConfigName();
		if (remoteClientConfigName != null && remoteClientConfigName.isEmpty() == false) {
			bindingsForAuthority.addAttribute(Record.REMOTECLIENT_CONFIG_NAME, remoteClientConfigName);
		}

		addServiceBinding(r, bindingsForAuthority, nsservices, true, serviceBindingVersion);
		if (log.isDebugEnabled() == true) {
			this.debugWriteToFile(r, bindingsForAuthority, true);
		}		
	}
	
	/**
	 * Add Authorities into the mix
	 * seems to be pretty standard...
	 * @param r
	 * @param el
	 */
	private void addAuthorization(Record r, Element el, String serviceBindingVersion){
		//add standard procedure like bit
		Element cele = el.addElement(new QName(
				"serviceBindings", nstenant));
		cele.addAttribute("name", r.getServicesTenantPl());
		cele.addAttribute("version", "0.1");
		addServiceBinding(r, cele, nsservices, false, serviceBindingVersion);
	}
	
	private void addServiceBinding(Record r, Element el, Namespace nameSpace, Boolean isAuthority, String serviceBindingVersion) {
		try {
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
			String docHandlerName = r.getServicesDocHandler(isAuthority);
			Element docHandlerElement = el.addElement(new QName("documentHandler", nameSpace));
			docHandlerElement.addText(docHandlerName);
			
			//<service:DocHandlerParams> include fields to show in list results
			doDocHandlerParams(r, el, this.nsservices, isAuthority);
			
			//<service:AuthorityInstanceList>
			if (isAuthority == true || r.isType(RECORD_TYPE_VOCABULARY) == true) {
				doAuthorityInstanceList(r, el, this.nsservices);
			}
	
			//<service:validatorHandler>
			String validatorHandlerName = r.getServicesValidatorHandler(isAuthority);
			Element validatorHandlerElement = el.addElement(new QName("validatorHandler", nameSpace));
			validatorHandlerElement.addText(validatorHandlerName);
			
			//<service:clientHandler>
			String clientHandlerName = r.getServicesClientHandler(isAuthority);
			Element clientHandlerElement = el.addElement(new QName("clientHandler", nameSpace));
			clientHandlerElement.addText(clientHandlerName);		
			
			//<service:initHandler> which fields need to be modified in the nuxeo db
			doInitHandler(r, el, this.nsservices, isAuthority);
	
			//<service:properties>
			Element servicePropertiesElement = el.addElement(new QName("properties", nameSpace));		
			if (doServiceProperties(r, servicePropertiesElement, this.nstypes, isAuthority) == false) {
				el.remove(servicePropertiesElement);
			}
			
			//<service:object>
			doServiceObject(r, el, this.nsservices, isAuthority, serviceBindingVersion);
			
			if (log.isTraceEnabled()) {
				String msg = String.format("Config Generation: '%s' - Created service bindings for record type '%s'.", 
						this.getConfigFile().getName(), r.getRecordName());
				log.trace(msg);
			}
		} catch (RuntimeException rte) {
			throw rte;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(String.format("Config Generation: '%s' - Could not create service bindings for record '%s'.", 
					getConfigFile().getName(), r.getRecordName()));
		}
	}
	
	/*
	 * Repeatable scalars (multivalued scalars)...
	 */
	private boolean isScalarRepeat(FieldSet fieldSet) {
		boolean result = false;
		
		FieldParent parent = fieldSet.getParent();
		if (parent instanceof Repeat) {
			Repeat repeat = (Repeat)parent;
			
			if (repeat.isTrueRepeatField() == true) {
				String[] parts = repeat.getfullID().split("/");
				if (parts.length == 1) {
					result = true;
				}
			}
		}

		return result;
	}
	
	/*
	 * Returns the fully qualified path of a field name
	 */
	private String getFullyQualifiedFieldPath(FieldSet in) {
		String tail = null;		
		String name = in.getServicesTag(); // Start by assuming it's just a plain scalar field, so we would just return the name
		
		//
		// Debug info
		//
		String[] fullName = in.getIDPath();
		boolean isPartOfGroup = in.getParent() instanceof Group;
		boolean isGroup = in instanceof Group;
		boolean isGroupField = in.isAGroupField();
		
		//
		// If it's a repeatable field
		//
		if (in.getParent().isTrueRepeatField() == true) {
			name = "";
			FieldSet fst = in;
			while (fst.getParent().isTrueRepeatField() == true) {
				tail = "/"; // Fields that are part of a repeatable structured group have a form like this 'titleGroupList/[0]/title'
				if (isScalarRepeat(fst) == true) {
					tail = ""; // Scalar-repeats (aka, multi-valued fields) look like 'responsibleDepartments/[0]' and *not* 'responsibleDepartments/[0]/responsibleDepartment'
				}
				
				fst = (FieldSet) fst.getParent();
				if (fst instanceof Repeat) {
					Repeat rt = (Repeat) fst;
					if (rt.hasServicesParent()) {
						name += rt.getServicesParent()[0];
					} else {
						name += rt.getServicesTag();
					}
				} else { // REM - This 'else' clause is not necessary and should be removed.
					Group gp = (Group) fst;
					if (gp.hasServicesParent()) {
						name += gp.getServicesParent()[0];
					} else {
						name += gp.getServicesTag();
					}
				}
				
				name += GROUP_LIST_SUFFIX + tail;
			}
			
			if (name.contains(GROUP_LIST_SUFFIX) && name.endsWith("/")) {
				name += in.getServicesTag();
			}
		}
				
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
	private void addServiceProperty(Element props, FieldSet fieldSet, String keyName, Namespace types, boolean isAuthority) {
		//<item>
		Element itemElement = props.addElement(new QName("item", types));
		
		//<key>
		Element keyElement = itemElement.addElement(new QName("key", types));
		keyElement.addText(keyName);		
		
		//<value>
		Element valueElement = itemElement.addElement(new QName("value", types));
		String fieldPath = this.getFullyQualifiedFieldPath(fieldSet);
		
		Record record = fieldSet.getRecord();
		if (fieldSet.shouldSchemaQualify() == true) {
			String schemaName = record.getServicesSchemaName(fieldSet.getSection());
			fieldPath = schemaName + ":" + fieldPath;
		}
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
			String serviceFieldAlias = objNameProp.getServiceFieldAlias();
			if (serviceFieldAlias != null) {
				objNameProp = r.getField(serviceFieldAlias);
			}
			this.addServiceProperty(props, objNameProp, OBJECT_NAME_PROPERTY, types, isAuthority);
			result = true;
		}

		FieldSet objNumberProp = r.getMiniNumber();
		if (objNumberProp != null) {
			this.addServiceProperty(props, objNumberProp, OBJECT_NUMBER_PROPERTY, types, isAuthority);
			result = true;
		}
				
		return result;
	}
			
	//defines fields to show in list results
	private void doLists(Record record, Element el, Namespace thisns, boolean isAuthority) {
		Record r = record;
		//
		// If we're dealing with an Authority/Vocabulary then we need to use the base Authority/Vocabulary record and
		// not the term/item record.
		//
		if (isAuthority == true) {
			Spec recordSpec = r.getSpec();
			r = recordSpec.getRecord(BASE_AUTHORITY_RECORD);
		}

		FieldSet[] allMiniSummaryList = r.getAllMiniSummaryList();
		if (allMiniSummaryList == null) {
			log.error(String.format("Config Generation: '%s' - allMiniSummaryList for record '%s' is null.", 
					getConfigFile().getName(), r.getRecordName()));
		}
		String section;
		for (FieldSet fs : allMiniSummaryList) {
			if ((fs.isInServices() && !fs.excludeFromServicesList()) || fs.isServicesDerived()) {
				String fieldNamePath = this.getFullyQualifiedFieldPath(fs);
				section = fs.getSection();
				//
				// Add the <ListResultField> element
				//
				Element lrf = el.addElement(new QName("ListResultField", thisns));
				// Only list result fields in sections other than the common part should
				// have a 'schema' element. (By convention, if this element is missing,
				// the field is from the common part.)
				if (!section.equals(Record.COLLECTIONSPACE_COMMON_PART_NAME)) {
					Element slrf = lrf.addElement(new QName("schema", thisns));
					String schemaName = r.getServicesSchemaName(fs.getSection());
					if (schemaName != null && schemaName.trim().isEmpty() == false) {
						slrf.addText(r.getServicesSchemaName(fs.getSection()));
					} else {
						String errMsg = String.format("Can not find a valid schema name for field '%s' of section '%s' for record type '%s'.  Check the section name if the field is part of a group.  The section name for all fields in a group neeed to be the same.",
								fs.getID(), fs.getSection(), r);
						log.error(errMsg);
						throw new RuntimeException(errMsg);
					}
				}

				Element elrf = lrf.addElement(new QName("element", thisns));
				elrf.addText(fs.getServicesTag());

				Element xlrf = lrf.addElement(new QName("xpath", thisns));
				xlrf.addText(fieldNamePath);

				String setter = fs.getServicesSetter();
				if (setter != null && setter.trim().isEmpty() == false) {
					Element slrf = lrf.addElement(new QName("setter", thisns));
					slrf.addText(setter);
				}
			}
		}
	}
	
	class AuthCache {
		JSONObject authCache = new JSONObject();
			
		JSONObject getAuthorities(String type) {
			return authCache.getJSONObject(type);
		}
			
		public void addAuthorityInstance(String authType, String shortId, JSONObject termList) {
			JSONObject auth = null;
			try {
				auth = getAuthorities(authType);
			} catch (JSONException e) {
				authCache.put(authType, new JSONObject());
				auth = getAuthorities(authType);
			}
			auth.put(shortId, termList);
		}
			
		public String toString(int indent) {
			return authCache.toString(indent);
		}
	}
		
	private void doAuthorityInstanceList(Record r, Element el, Namespace nsservices2) {
		
		Instance[] allInstances = r.getAllInstances();
		if (r.getID().equalsIgnoreCase(RECORD_ID_TERMLIST) == true) {
			//
			// Records "termlist" and "vocab" have special relationship, so we need to do things a little differently.
			//
			Record vocabRecord = this.spec.getRecord(RECORD_ID_VOCAB);
			allInstances = vocabRecord.getAllInstances();
		} else {
			//debug only
			log.info("Not a vocab instance.");
		}
		
		if (allInstances.length > 0) {
			Element authorityInstanceList = el.addElement(new QName("AuthorityInstanceList", nsservices2));
			for (Instance instance : allInstances) {
				Element authorityInstance = authorityInstanceList.addElement(new QName("AuthorityInstance", nsservices2));
				doAuthorityInstance(instance, r, authorityInstance, nsservices2);
			}
		}

	}
	
	private void doAuthorityInstance(Instance instance, Record r, Element el, Namespace nsservices2) {
		Element webUrl = el.addElement(new QName("web-url", nsservices2));
		webUrl.addText(instance.getWebURL());
		
		Element titleRef = el.addElement(new QName("title-ref", nsservices2));
		titleRef.addText(instance.getTitleRef());
		
		Element title = el.addElement(new QName("title", nsservices2));
		title.addText(instance.getTitle());
		
		Option[] allOptions = instance.getAllOptions();
		if (allOptions.length > 0) {
			Element termList = el.addElement(new QName("termList", nsservices2));
			JSONObject jsonTermList = new JSONObject();
			for (Option option : allOptions) {
				doTerm(option, r, termList, nsservices2);
				jsonTermList.put(option.getName(), option.getID());
			}
			String serviceUrl = r.getServicesURL();
			authCache.addAuthorityInstance(serviceUrl, instance.getTitleRef(), jsonTermList);
		}

	}
	
	private void doTerm(Option option, Record r, Element el, Namespace nsservices2) {
		Element term = el.addElement(new QName("term", nsservices2));
		term.addAttribute("id", option.getID());
		term.addText(option.getName());
	}

	//
	// It's possible that the repeat is of a structured "group" type.  If so, we need to
	// create an XPath of the form <yxzGroupList>/*/<fieldName>.  To do this we need to split
	// the 'fullid' of this repeat fieldset into its component parts.
	//
	private String getXpathStructuredPart(Repeat repeat) {
		String result = null;
		
		String servicesTag = repeat.getServicesTag();
		String[] parts = repeat.getfullID().split("/");
		if (parts.length > 1) { // If we have more than one part, we have a repeating structure.
			result = parts[0] + GROUP_XPATH_SUFFIX;
		} else {
			result = servicesTag != null ? servicesTag : parts[0];
			//
			// For debugging.  The Service's Media schema uses the "List" field suffix differently (and against convention) from the other Services
			//
			String parentRecordName = repeat.getRecord().getRecordName();
			if (servicesTag != null && !servicesTag.equals(parts[0])) {
				log.warn(String.format("Config Generation: '%s' - Potential internal error.  The Services tag '%s' does not match the field name '%s' for Repeat field '%s:%s.'",
						getConfigFile().getName(), servicesTag, parts[0], parentRecordName, repeat.getfullID()));
			} else if (servicesTag == null) {
				log.warn(String.format("Config Generation: '%s' - Internal error.  Services tag '%s' was null for Repeat field '%s:%s'.",
						getConfigFile().getName(), servicesTag, parentRecordName, repeat.getfullID()));
			}
		}
		
		return result;
	}
	
	private boolean isAuthOrTermRef(FieldSet fieldSet) {
		boolean result = false;
		
		if (fieldSet.hasAutocompleteInstance() || fieldSet.isAuthRefInServices()) {
                    // CSPACE-6187: Require that any field for which an authRef
                    // or termRef declaration is generated actually exists in
                    // Services schemas. This filters out synthetic fields
                    // that may be created for various purposes.
                    if (fieldSet.isInServices()) {
			result = true;
                    }
		}
		
		return result;
	}
	
	/*
	 * If we have authRef's or termRef's, then we create an entry in the bindings xml and return 'true'; otherwise, we return 'false'
	 */
	private boolean createAuthRefOrTermRef(Element auth, Namespace types, Record r, String section, FieldSet in) {
		boolean result = false;
		String fieldName = in.getID(); // for debugging - remove after
        String sec = in.getSection(); // for debugging - remove after
		
                // Ignore passed-in section, in order to create authRefs and termRefs for every section
		if (isAuthOrTermRef(in) && in.getSection().equals(section)) {
			result = true; // Let the caller know we created a referenced term 
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
						
			while (fs.getParent().isTrueRepeatField() == true) {
				String xpathStructuredPart = fs.getServicesTag();
				if (fs.getParent() instanceof Repeat) {
					xpathStructuredPart = getXpathStructuredPart((Repeat)fs.getParent());
				}
				fs = (FieldSet) fs.getParent();
				
				String separator = "|"; // Repeatable scalars are separated by the '|' character; otherwise, we use the '/' for xpath expressions
				if (xpathStructuredPart.endsWith("*")) {
					separator = "/";
				}
				name = xpathStructuredPart + separator + name;
			}
			name += in.getServicesTag();
			tele3.addText(name);
		}
		
		return result;
	}
	
	protected File getConfigFile() {
		return this.configFile;
	}
	
	/*
	 * Creates a set of Service binding authRefs and termRefs similar in form to these examples:
	 *  <types:item>
	 *      <types:key>authRef</types:key>
	 *      <types:value>currentOwner</types:value>
	 *  </types:item>
         *  <types:item>
	 *      <types:key>termRef</types:key>
	 *      <types:value>termLanguage</types:value>
	 *  </types:item>
	 *  
	 *  If we don't create any entries then we'll return 'false' to the caller;
	 */
	private boolean doAuthRefsAndTermRefs(Element auth, Namespace types, Record r, String section) {
		boolean result = false;
		
		for (FieldSet in : r.getAllFieldFullList("")) {
			String fieldName = in.getID() + ":" + in.getLabel(); // for debugging only

			if (in.isASelfRenderer() == true) {
				if (in.getSection().equals(section)) {
					String fieldSetServicesType = in.getServicesType(false /* not NS qualified */);
					Spec recordSpec = in.getRecord().getSpec();
					Record subRecord = recordSpec.getRecord(fieldSetServicesType); // find a record that corresponds to the fieldset's service type
					String subSection = "common"; // the fields in an included subrecord should always be in the "common" section
					
					//
					// Iterate through each field of the subrecord
					//
					boolean createdAuths = doAuthRefsAndTermRefs(auth, types, subRecord, subSection); // Recursive call
					if (createdAuths == true) {
						result = true; // Let the caller know we created at least one auth/term reference
					}
				}
			} else {
				boolean createdAuths = createAuthRefOrTermRef(auth, types, r, section, in);
				if (createdAuths == true) {
					result = true; // Let the caller know we created at least one auth/term reference
				}
			}
		}
		
		return result;
	}
}
