package org.collectionspace.chain.installation;

import org.collectionspace.chain.csp.persistence.services.TenantSpec;
import org.collectionspace.chain.csp.schema.Field;
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
		//XXX need to pull in from default rather from tenant specific eek
		//as well as loop over all tenatn specific instances
	}

	public String doit() {
		String data = "";
		data += doServiceBindingsCommon();

		return data;
	}

	/**
	 * service-bindings-common.xml - a shared prototype of core and common
	 * services, common to all tenants. This file has the basic definitions of
	 * the services, and the system, core, and common parts of each service. It
	 * includes the most common definitions of the field types, to make it easy
	 * to define a new tenant.
	 * 
	 * @return
	 */
	private String doServiceBindingsCommon() {
		
		
		Document doc = DocumentFactory.getInstance().createDocument();
		//String[] parts = record.getServicesRecordPath(section).split(":", 2);
		//String[] rootel = parts[1].split(",");

		Element root = doc.addElement(new QName("TenantBindingConfig", this.nstenant));
		root.addAttribute("xsi:schemaLocation", this.schemaloc);
		root.add(this.nsservices);
		root.add(this.nsxsi);
		root.add(this.nstypes);
		


		Element ele = root.addElement(new QName("tenantBinding", nstenant));
		ele.addAttribute("name", this.tenantSpec.getTenant());
		ele.addAttribute("displayName", this.tenantSpec.getTenantDisplay());
		ele.addAttribute("version", this.tenantSpec.getTenantVersion());

		Element rele = ele.addElement(new QName("repositoryDomain", nstenant));
		rele.addAttribute("name", this.tenantSpec.getRepoDomain());
		rele.addAttribute("repositoryClient", this.tenantSpec.getRepoClient());
		
		
		makeProperties(ele);
		
		String data = "doServiceBindingsCommon\n\n";
		for (Record r : this.spec.getAllRecords()) {
			if (r.isType("record")) {
				data += r.getID();
				data += "::";

				setDomain(r);
				if (!this.domainsection.equals("")) {
					String rtype="procedure";
					if (!r.isType("procedure") && r.isType("record")) {
						rtype = "object";
					}
						Element cele = ele.addElement(new QName(
								"serviceBindings", nstenant));
						cele.addAttribute("name", r.getServicesTenantPl());
						cele.addAttribute("type", rtype);
						cele.addAttribute("version", "1.0");

						doProcedure(r, cele, nsservices);
					

				}
				data += "\n\n";
			}
		}

		log.info(doc.asXML());
		return data;
	}

	private void makeProperties(Element ele){
		Boolean showLang = true;
		Boolean showDate = true;
		if (!this.domainsection.equals("")) {
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

	private void doDocHandler(Record r, Element el, Namespace thisns, String section){
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
		
		doLists(r,lrele,thisns,section);
		
		
		
	}
	
	private void setDomain(Record r){
		this.domainsection = ""; //assumes only one domain
		for(String section : r. getServicesRecordPaths()){
			if(!section.equals("common") && !section.equals("collectionspace_core")){
				this.domainsection = section; //assumes only one domain
			}
		}
	}
	
	private void makeLowerParts(Record r, Namespace thisns, Element cele, String label, String labelsg){
		Integer num = 3;
		String sectional = r.getServicesRecordPath(this.domainsection);
		String[] sectionparts=sectional.split(":",2);
		String schemaLocationDomain = sectionparts[1].split(",",2)[0] + " "+ r.getServicesSchemaBaseLocation() + labelsg + "/domain/" + label + "_"+this.domainsection+".xsd";

		makePart(r,cele,num.toString(),sectionparts[0],num.toString(),sectionparts[1].split(",",2)[0],schemaLocationDomain,thisns,true,this.domainsection);
		num++;
		
	}
	
	private void doServiceObject(Record r, Element el, Namespace thisns){
		//<service:object name="Intake" version="0.1">
		Element cele = el.addElement(new QName("object", thisns));
		cele.addAttribute("name", r.getServicesTenantSg());
		cele.addAttribute("version", "1.0");

		String label = r.getServicesTenantPl().toLowerCase();
		String labelsg = r.getServicesTenantSg().toLowerCase();

		if(this.defaultonly){
			makeUpperParts(r, thisns, cele, label, labelsg);
		}
		else{
			makeLowerParts(r, thisns, cele, label, labelsg);
		}
	}

	private void makeUpperParts(Record r, Namespace thisns, Element cele,
			String label, String labelsg) {
		
		String common = r.getServicesRecordPath("common");
		String core = r.getServicesRecordPath("collectionspace_core");
		String[] commonparts=common.split(":",2);
		String[] coreparts=core.split(":",2);
		
		String schemaLocation0 = "http://collectionspace.org/services/common/system http://collectionspace.org/services/common/system/system-response.xsd";
		String schemaLocationCore = coreparts[1].split(",",2)[0] + " " + r.getServicesSchemaBaseLocation() + "/collectionspace_core.xsd";
		String schemaLocationCommon = commonparts[1].split(",",2)[0] + " "+ r.getServicesSchemaBaseLocation() + labelsg + "/" + label + "_common.xsd";
//<service:part id="0" control_group="Managed" versionable="true" auditable="false" label="intakes-system" updated="" order="0">
		makePart(r,cele,"0",label,"0","http://collectionspace.org/services/common/system",schemaLocation0,thisns,false,"");
		makePart(r,cele,"1",coreparts[0],"1",coreparts[1].split(",",2)[0],schemaLocationCore,thisns,false,"");
		makePart(r,cele,"2",commonparts[0],"2",commonparts[1].split(",",2)[0],schemaLocationCommon,thisns,true,"common");
	}
	
	private void doProcedure(Record r, Element el, Namespace thisns) {

		Element repository = el.addElement(new QName("repositoryDomain",thisns));
		repository.addText(this.tenantSpec.getRepoDomain());
		
		if(this.defaultonly){
			String docHandler = r.getServicesDocHandler();
			Element documentHandler = el.addElement(new QName("documentHandler",thisns));
			documentHandler.addText(docHandler);
			Element validatorHandler = el.addElement(new QName("validatorHandler",thisns));
			validatorHandler.addText(r.getServicesValidatorHandler());
		}
		if(!this.domainsection.equals("")){//only do if domain exists
			doServiceObject(r, el, this.nsservices);
			doDocHandler(r, el, this.nsservices, this.domainsection);
		}

		
		String data = "";

		//data += getAuthRefs(r);
		//data += getLists(r);
		data += "\n\n";
	}

	private void doLists(Record r, Element el, Namespace thisns, String section) {
		String data = "\n\nGETLISTRS \n\n";
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
				
				data += "\n";
			}

		}
		log.info(data);
	}

	private void doAuths(Element auth, Namespace types, Record r, String section) {
		/*
		 * 
						<types:item>
							<types:key>authRef</types:key>
							<types:value>currentOwner</types:value>
						</types:item>
		 */
		for (FieldSet in : r.getAllGenFields("")) {
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
