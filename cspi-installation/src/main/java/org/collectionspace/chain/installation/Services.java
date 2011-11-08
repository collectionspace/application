package org.collectionspace.chain.installation;

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
	protected Spec spec;
	protected TenantSpec tenantSpec;
	protected Boolean defaultonly;
	protected String domainsection;

	/**
	 * not sure how configurable these need to be - they can be made more flexible
	 */
	protected Namespace nstenant = new Namespace("tenant", "http://collectionspace.org/services/common/tenant"); 
	protected Namespace nsservices = new Namespace("service", "http://collectionspace.org/services/common/service"); 
	protected Namespace nsxsi = new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");  
	protected Namespace nstypes = new Namespace("types", "http://collectionspace.org/services/common/types"); 
	protected String schemaloc = "http://collectionspace.org/services/common/tenant " +
	"http://collectionspace.org/services/common/tenant.xsd";

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
//		root.add(this.nsservices);
		root.add(this.nsxsi);
//		root.add(this.nstypes);
		


		//<tenant:tenantBinding version="0.1">
		Element ele = root.addElement(new QName("tenantBinding", nstenant));
		if(!this.defaultonly){
			ele.addAttribute("name", this.tenantSpec.getTenant());
			ele.addAttribute("displayName", this.tenantSpec.getTenantDisplay());
		}
		ele.addAttribute("version", this.tenantSpec.getTenantVersion());

		//<tenant:repositoryDomain name="default-domain" repositoryClient="nuxeo-java"/>
		Element rele = ele.addElement(new QName("repositoryDomain", nstenant));

		if(this.defaultonly){
			rele.addAttribute("name", this.tenantSpec.getDefaultDomain());
		}
		else{
			rele.addAttribute("name", this.tenantSpec.getRepoDomain());
		}
		rele.addAttribute("repositoryClient", this.tenantSpec.getRepoClient());
		
		//add in <tenant:properties> if required
		makeProperties(ele);
		
		Boolean debug = false;
		//loop over each record type and add <tenant:serviceBindings
		for (Record r : this.spec.getAllRecords()) {
			if (r.isType("record") && debug) {

				setDomain(r);//if this domain does this record actually have domain specific info
				if (!this.domainsection.equals("")) {//empty string means no domain specific data
					String rtype="procedure";
					if (!r.isType("procedure") && r.isType("record")) {
						rtype = "object";
					}
					//<tenant:serviceBindings name="CollectionObjects" type="object" version="0.1">
						Element cele = ele.addElement(new QName(
								"serviceBindings", nstenant));
						cele.addAttribute("name", r.getServicesTenantPl());
						cele.addAttribute("type", rtype);
						cele.addAttribute("version", "0.1");

						addServiceBinding(r, cele, nsservices, false);
				}
			}
			if(r.isType("authority")){

				addVocabularies(r, ele);
			}
			if(r.isType("userdata")){

				Element cele = ele.addElement(new QName(
						"serviceBindings", nstenant));
				cele.addAttribute("name", r.getServicesTenantPl());
				cele.addAttribute("version", "0.1");

				addServiceBinding(r, cele, nsservices, false);
			}
			if(r.isType("authorizationdata")){
				//	ignore at the moment as they are so non standard
				//	addAuthorization(r,ele);
			}
		}

		//addExtras();
		
		
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
		pele.addAttribute("control_group", "Managed");
		pele.addAttribute("versionable", "true");
		pele.addAttribute("auditable", "false");
		pele.addAttribute("label", label);
		pele.addAttribute("updated", "");
		pele.addAttribute("order", order);
		
		Element conle = pele.addElement(new QName("content",thisns));
		conle.addAttribute("contentType", "application/xml");
		
		Element xconle = conle.addElement(new QName("xmlContent",thisns));
		xconle.addAttribute("namespaceURI", namespaceURI);
		xconle.addAttribute("schemaLocation", schemaLocation);
		
		if(addAuths){
			Element auths = pele.addElement(new QName("properties",thisns));
			doAuths(auths,this.nstypes,r,section);
			
		}
	}

	/**
	 * define fields that needs to have different datatypes in the nuxeo db
	 * @param r
	 * @param el
	 * @param thisns
	 * @param section
	 * @param isAuthority
	 */
	private void doInitHandler(Record r, Element el, Namespace thisns, String section, Boolean isAuthority){

		if(!isAuthority){

			Element dhele = el.addElement(new QName("initHandler", thisns));
			Element cele = dhele.addElement(new QName("classname", thisns));
			cele.addText("org.collectionspace.services.common.init.ModifyFieldDatatypes");
			Element pele = dhele.addElement(new QName("params", thisns));
			
			//loop over all fields to find out is they have a defined datatype
			for(FieldSet f: r.getAllFieldFullList("")){
				if(f instanceof Field){
					Field fd = (Field)f;
					if(fd.getSection().equals(section)){

						if(!fd.getDataType().equals("")){
							FieldParent fp = fd.getParent();
							String col = "item";

							String tablebase = r.getServicesRecordPath(section).split(":",2)[0];
							
							if(fp instanceof Repeat){
								Repeat rp = (Repeat)fp;
								tablebase += "_"+rp.getServicesTag().toLowerCase();
								if(rp.hasServicesParent()){
									//affects table and col
									tablebase = rp.getServicesTag().toLowerCase();
									col = fd.getServicesTag().toLowerCase();
								}
							}
							String table = "nuxeo."+tablebase;
							Element a = pele.addElement(new QName("field", thisns));
							Element b = a.addElement(new QName("table",thisns));
							b.addText(table);
							Element c = a.addElement(new QName("col",thisns));
							c.addText(col);
							Element d = a.addElement(new QName("type",thisns));
							d.addText(fd.getDataType().toUpperCase());
							Element e = a.addElement(new QName("param",thisns));
							
						}
					}
				}
			}
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
	private void doDocHandler(Record r, Element el, Namespace thisns, String section, Boolean isAuthority){
		//<service:DocHandlerParams>
		Element dhele = el.addElement(new QName("DocHandlerParams", thisns));
		Element pele = dhele.addElement(new QName("params", thisns));
		if(this.defaultonly){
			Element nuxeoscheme = pele.addElement(new QName("NuxeoSchemaName",thisns));
			nuxeoscheme.addText(r.getServicesTenantPl().toLowerCase());
			Element dublin = pele.addElement(new QName("DublinCoreTitle",thisns));
			dublin.addText(r.getServicesTenantPl().toLowerCase());
			Element abstractlist = pele.addElement(new QName("AbstractCommonListClassname",thisns));
			abstractlist.addText(r.getServicesAbstractCommonList());
			Element commonlist = pele.addElement(new QName("CommonListItemClassname",thisns));
			commonlist.addText(r.getServicesCommonList());
		}
		
		Element lrele = pele.addElement(new QName("ListResultsFields", thisns));
		
		if(!isAuthority){// only do if not authority view
			doLists(r,lrele,thisns,section);
		}
	}

	/**
	 * are we in common or a domain area?
	 * @param r
	 */
	private void setDomain(Record r) {
		if (!this.defaultonly) {
			this.domainsection = ""; // assumes only one domain
			for (String section : r.getServicesRecordPaths()) {
				if (!section.equals("common")
						&& !section.equals("collectionspace_core")) {
					this.domainsection = section; // assumes only one domain
				}
			}
		}
	}
	
	
	/**
	 * domain specific tenant binding
	 */
	private void makeLowerParts(Record r, Namespace thisns, Element cele, String label, String labelsg, Boolean isAuthority){
		Integer num = 3;
		String sectional = r.getServicesRecordPath(this.domainsection);
		String[] sectionparts=sectional.split(":",2);
		String schemaLocationDomain = sectionparts[1].split(",",2)[0] + " "+ r.getServicesSchemaBaseLocation() + labelsg + "/domain/" + label + "_"+this.domainsection+".xsd";

		makePart(r,cele,num.toString(),sectionparts[0],num.toString(),sectionparts[1].split(",",2)[0],schemaLocationDomain,thisns,true,this.domainsection);
		num++;
		
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

		if(isAuthority){
			cele.addAttribute("name", r.getServicesTenantAuthSg());
		}
		else{
			cele.addAttribute("name", r.getServicesTenantSg());
		}
		cele.addAttribute("version", "1.0");

		String label = r.getServicesTenantPl().toLowerCase();
		String labelsg = r.getServicesTenantSg().toLowerCase();

		if(isAuthority){
			label = r.getServicesTenantAuthPl().toLowerCase();
			labelsg = r.getServicesTenantAuthSg().toLowerCase();
		}

		if(this.defaultonly){
			makeUpperParts(r, thisns, cele, label, labelsg, isAuthority);
		}
		else{
			makeLowerParts(r, thisns, cele, label, labelsg, isAuthority);
		}
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
	private void makeUpperParts(Record r, Namespace thisns, Element cele,
			String label, String labelsg, Boolean isAuthority) {
		
		//<service:part id="0" control_group="Managed" versionable="true" auditable="false" label="intakes-system" updated="" order="0">
		
		String schemaLocation0 = "http://collectionspace.org/services/common/system http://collectionspace.org/services/common/system/system-response.xsd";
		makePart(r,cele,"0",label+"-system","0","http://collectionspace.org/services/common/system",schemaLocation0,thisns,false,"");

		Integer num = 1;
		if(r.hasServicesRecordPath("collectionspace_core")){
			String core = r.getServicesRecordPath("collectionspace_core");
			String[] coreparts=core.split(":",2);
			String schemaLocationCore = coreparts[1].split(",",2)[0] + " " + r.getServicesSchemaBaseLocation() + "/collectionspace_core.xsd";
			makePart(r,cele,num.toString(),coreparts[0],num.toString(),coreparts[1].split(",",2)[0],schemaLocationCore,thisns,false,"");
			num++;
		}
	
		if(r.hasServicesRecordPath("common")){
			String common = r.getServicesRecordPath("common");
			String[] commonparts=common.split(":",2);
			String schemaLocationCommon = commonparts[1].split(",",2)[0] + " "+ r.getServicesSchemaBaseLocation() + labelsg + "/" + label + "_common.xsd";
			
			if(r.isType("authorizationdata")){
				schemaLocationCommon = commonparts[1].split(",",2)[0] + " "+ r.getServicesSchemaBaseLocation()  + "/" + label + ".xsd";
				
			}
			makePart(r,cele,num.toString(),commonparts[0],num.toString(),commonparts[1].split(",",2)[0],schemaLocationCommon,thisns,true,"common");
			num++;
		}	
	}
	
	/**
	 * vocabs have 2 parts in the tenant binding
	 * @param r
	 * @param el
	 */
	private void addVocabularies(Record r, Element el){
		//add standard procedure like bit
		Element cele = el.addElement(new QName(
				"serviceBindings", nstenant));
		cele.addAttribute("name", r.getServicesTenantPl());
		cele.addAttribute("version", "0.1");
		addServiceBinding(r, cele, nsservices, false);
		
		//add vocabulary bit
		Element cele2 = el.addElement(new QName(
				"serviceBindings", nstenant));
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
	
	private void addServiceBinding(Record r, Element el, Namespace thisns, Boolean isAuthority) {
		//<service:repositoryDomain>default-domain</service:repositoryDomain>
		Element repository = el.addElement(new QName("repositoryDomain",thisns));

		if(this.defaultonly){
			repository.addText(this.tenantSpec.getDefaultDomain());
			//<service:documentHandler>
			String docHandler = r.getServicesDocHandler();
			Element documentHandler = el.addElement(new QName("documentHandler",thisns));
			documentHandler.addText(docHandler);
			//<service:validatorHandler>
			Element validatorHandler = el.addElement(new QName("validatorHandler",thisns));
			validatorHandler.addText(r.getServicesValidatorHandler());
		}
		else{
			repository.addText(this.tenantSpec.getRepoDomain());
		}
		
		if(!this.domainsection.equals("")){//only do if domain exists or is common
			//<service:object name="CollectionObject" version="0.1">
			//defines all authref fields
			doServiceObject(r, el, this.nsservices, isAuthority);

			//<service:DocHandlerParams> include fields to show in list results
			doDocHandler(r, el, this.nsservices, this.domainsection, isAuthority);
			
			//<service:initHandler> which fields need to be modified in the nuxeo db
			doInitHandler(r, el, this.nsservices, this.domainsection, isAuthority);
		}

	}
	

	
	//defines fields to show in list results
	private void doLists(Record r, Element el, Namespace thisns, String section) {

		for (FieldSet fs : r.getAllMiniSummaryList()) {

			if (fs.isInServices() && fs.getSection().equals(section)) {
				FieldSet fst = fs;
				String name = "";
				while (fst.getParent() instanceof Repeat
						|| fst.getParent() instanceof Group) {

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
				name += fs.getServicesTag();

				Element lrf = el.addElement(new QName("ListResultField",thisns));
				Element elrf = lrf.addElement(new QName("element",thisns));
				if(!section.equals("common")){
					Element slrf = lrf.addElement(new QName("schema",thisns));
					slrf.addText(r.getServicesRecordPath(section).split(":",2)[0]);
				}
				Element xlrf = lrf.addElement(new QName("xpath",thisns));

				elrf.addText(fs.getServicesTag());
				xlrf.addText(name);
			}
		}
	}

	private void doAuths(Element auth, Namespace types, Record r, String section) {
		/*
		 * 
						<types:item>
							<types:key>authRef</types:key>
							<types:value>currentOwner</types:value>
						</types:item>
		 */
		for (FieldSet in : r.getAllFieldFullList("")) {
			if (in.getSection().equals(section)) {
				if (in.hasAutocompleteInstance()) {
					Boolean typecheck = false;
					if (in instanceof Field) {
						typecheck = (((Field) in).getUIType()).equals("enum");
					}
					if (!typecheck) {
						FieldSet fs = in;

						Element tele = auth
								.addElement(new QName("item", types));
						Element tele2 = tele
								.addElement(new QName("key", types));
						Element tele3 = tele.addElement(new QName("value",
								types));
						tele2.addText("authRef");
						String name = "";
						while (fs.getParent() instanceof Repeat
								|| fs.getParent() instanceof Group) {

							fs = (FieldSet) fs.getParent();
							name += fs.getServicesTag();
							name += "/";
						}
						name += in.getServicesTag();
						tele3.addText(name);
					}
				}
			}
		}
	}
}
