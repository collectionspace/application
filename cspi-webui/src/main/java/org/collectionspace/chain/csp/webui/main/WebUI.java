/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Schemas;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.authorities.AuthoritiesVocabulariesInitialize;
import org.collectionspace.chain.csp.webui.authorities.AuthoritiesVocabulariesSearchList;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesCreateUpdate;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesDelete;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesRead;
import org.collectionspace.chain.csp.webui.external.UIMapping;
import org.collectionspace.chain.csp.webui.external.UIMeta;
import org.collectionspace.chain.csp.webui.mediablob.BlobCreateUpdate;
import org.collectionspace.chain.csp.webui.mediablob.BlobRead;
import org.collectionspace.chain.csp.webui.misc.RecordTraverser;
import org.collectionspace.chain.csp.webui.misc.VocabRedirector;
import org.collectionspace.chain.csp.webui.misc.WebAuto;
import org.collectionspace.chain.csp.webui.misc.WebAutoComplete;
import org.collectionspace.chain.csp.webui.misc.WebTermList;
import org.collectionspace.chain.csp.webui.misc.WebLogin;
import org.collectionspace.chain.csp.webui.misc.WebLoginStatus;
import org.collectionspace.chain.csp.webui.misc.WebLogout;
import org.collectionspace.chain.csp.webui.misc.WebReset;
import org.collectionspace.chain.csp.webui.nuispec.CacheTermList;
import org.collectionspace.chain.csp.webui.nuispec.DataGenerator;
import org.collectionspace.chain.csp.webui.nuispec.ServicesXsd;
import org.collectionspace.chain.csp.webui.nuispec.UISchema;
import org.collectionspace.chain.csp.webui.nuispec.UISpec;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsCreateUpdate;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsDelete;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsRead;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsReset;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsSearchList;
import org.collectionspace.chain.csp.webui.userroles.UserRolesCreate;
import org.collectionspace.chain.csp.webui.userroles.UserRolesDelete;
import org.collectionspace.chain.csp.webui.userroles.UserRolesRead;
import org.collectionspace.chain.csp.webui.record.RecordCreateUpdate;
import org.collectionspace.chain.csp.webui.record.RecordDelete;
import org.collectionspace.chain.csp.webui.record.RecordRead;
import org.collectionspace.chain.csp.webui.record.RecordRelated;
import org.collectionspace.chain.csp.webui.record.RecordSearchList;
import org.collectionspace.chain.csp.webui.record.RecordAuthorities;
import org.collectionspace.chain.csp.webui.relate.RelateCreateUpdate;
import org.collectionspace.chain.csp.webui.relate.RelateDelete;
import org.collectionspace.chain.csp.webui.relate.RelateRead;
import org.collectionspace.chain.csp.webui.relate.RelateSearchList;
import org.collectionspace.chain.pathtrie.Trie;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.helper.core.RequestCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebUI implements CSP, UI, Configurable {
	private static final Logger log=LoggerFactory.getLogger(WebUI.class);
	public static String SECTION_PREFIX="org.collectionspace.app.config.ui.web.";
	public static String SECTIONED="org.collectionspace.app.config.spec";
	public static String WEBUI_ROOT=SECTION_PREFIX+"web";
	
	private UIMapping uiMapping;
	private UIMeta uiMeta;
	private Set<UIMapping> allmappings = new HashSet<UIMapping>();

	private Map<Operation,Trie> tries=new HashMap<Operation,Trie>();
	private List<WebMethod> all_methods=new ArrayList<WebMethod>();
	private CSPContext ctx;
	private StorageGenerator xxx_storage;
	public CacheTermList xxx_ctl;
	private String uispec_path;
	private String login_dest,login_failed_dest,front_page,find_page;

	@Override
	public String getName() { return "ui.webui"; }
	public String getUISpecPath() { return uispec_path; }
	public String getLoginDest() { return login_dest; }
	public String getLoginFailedDest() { return login_failed_dest; }
	public String getFrontPage() { return front_page; }
	
	/**
	 * Explicitly add in urls that the app layer will respond to
	 * e.g. addMethod(Operation.READ,new String[]{"logout"},0,new WebLogout());
	 * would open up the url /chain/logout for GET method with no parameters on the url and call WebLogout.java
	 * 
	 * addMethod(Operation.READ,new String[]{r.getWebURL()},1,new RecordRead(r));
	 * would open up the url /chain/<web-url>/{parameter} where web-url is the name in cspace-config.xml
	 * expecting one parameter and calling RecordRead.java and passing the record info from cspace-config.xml
	 * 
	 * @param op - GET/POST that request came in as
	 * Operation.CREATE = POST
	 * Operation.READ = GET
	 * @param path - url this json came in via e.g. /chain/login/ (remove the /chain/ part)
	 * @param extra - does this url have extra info e.g. /chain/object/1234{id} 0=no, 1= one item 2= two etc
	 * @param method - Java class to call to process this request
	 */
	private void addMethod(Operation op,String[] path,int extra,WebMethod method) {
		tries.get(op).addMethod(path,extra,method);
		all_methods.add(method);
	}

	@Override
	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
		ctx.addUI("web",this);
		this.ctx=ctx;
	}

	@Override
	public void configure(Rules rules) throws CSPDependencyException {
		/* MAIN/ui/web -> UI */
		rules.addRule(SECTIONED,new String[]{"ui","web"},SECTION_PREFIX+"web",null,new Target(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				((CoreConfig)parent).setRoot(WEBUI_ROOT,WebUI.this);
				if(section.getValue("/tmp-schema-path")!=null) {
					uispec_path=System.getProperty("java.io.tmpdir")+"/ju-cspace"; // XXX fix
				} else {
					uispec_path=(String)section.getValue("/schema-path");
				}
				login_dest=(String)section.getValue("/login-dest");
				login_failed_dest=(String)section.getValue("/login-failed-dest");
				front_page=(String)section.getValue("/front-page");
				find_page=(String)section.getValue("/find-page");
				
				return WebUI.this;
			}
		});	
		
		/* MAIN/ui/web/mappings ->UI */

		rules.addRule(SECTION_PREFIX+"web", new String[]{"mappings","map"},SECTION_PREFIX+"uimapping", null, new Target(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				uiMapping = new UIMapping((WebUI)parent,section);
				addMapping(uiMapping);
				return uiMapping;
			}
		});
		rules.addRule(SECTION_PREFIX+"uimapping", new String[]{"configure","meta"},SECTION_PREFIX+"uimetamapping", null, new Target(){
			@Override
			public Object populate(Object parent, ReadOnlySection section) {
				UIMapping map = (UIMapping)parent;
				uiMeta = new UIMeta((UIMapping)parent, section);
				map.addMetaConfig(uiMeta);
				return uiMeta;
			}
		});
		
	}
 
	private void addMapping(UIMapping uim){
		allmappings.add(uim);
	}
	public UIMapping[] getAllMappings(){
		return allmappings.toArray(new UIMapping[0]);
	}
	public UIMeta getMetaConfig(){
		return uiMeta;
	}

	private void configure_finish(Spec spec) {
		final String MEDIA_RECORD_ID = "media";
		
		for(Operation op : Operation.values())
			tries.put(op,new Trie());	
		
		//Operation.CREATE/UPDATE = method= POST
		//Operation.READ = method= GET

		addMethod(Operation.READ,new String[]{"login"},0,new WebLogin(this,spec)); /* XXX need to remove this one when UI layer moves across to POST */
		addMethod(Operation.CREATE,new String[]{"login"},0,new WebLogin(this,spec));
		addMethod(Operation.CREATE,new String[]{"uploads"},0,new BlobCreateUpdate(spec.getRecord("blobs"),true));
		addMethod(Operation.READ,new String[]{"uploads"},0,new BlobCreateUpdate(spec.getRecord("blobs"),true));
		addMethod(Operation.READ,new String[]{"download"},1,new BlobRead());
		addMethod(Operation.READ,new String[]{"logout"},0,new WebLogout());
		addMethod(Operation.READ,new String[]{"loginstatus"},0, new WebLoginStatus(spec));
		addMethod(Operation.READ,new String[]{"authorities","initialise"},0,new WebReset(false,false));
		addMethod(Operation.READ,new String[]{"reset"},0,new WebReset(false,true));
		addMethod(Operation.READ,new String[]{"quick-reset"},0,new WebReset(true,true));
		//addMethod(Operation.READ,new String[]{find_page,"uispec"},0,new FindEditUISpec(spec.getAllRecords()));//removed as I don't think anyone uses it
		addMethod(Operation.CREATE,new String[]{"passwordreset"},0,new UserDetailsReset(false,spec));
		addMethod(Operation.CREATE,new String[]{"resetpassword"},0,new UserDetailsReset(true,spec));
		addMethod(Operation.READ,new String[]{"adjacentRecords"},2,new RecordTraverser(spec));
		

		for(Schemas s : spec.getAllSchemas()){
			addMethod(Operation.READ,new String[]{s.getWebURL(), "uischema" },0,new UISchema(spec,s));
		}
		addMethod(Operation.READ,new String[]{"generator"},0,new DataGenerator(spec));
		Record mediaR = spec.getRecord(MEDIA_RECORD_ID);
		if(mediaR==null)
			log.error("No media record configured!!!");
		for(Record r : spec.getAllRecords()) {
			addMethod(Operation.READ,new String[]{r.getWebURL(),"generator"},0,new DataGenerator(r,"screen"));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"serviceschema"},0,new ServicesXsd(r,"common"));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"uischema"},0,new UISchema(r,"screen"));
			addMethod(Operation.READ,new String[]{r.getTabURL(),"uischema"},0,new UISchema(r,"tab"));
			addMethod(Operation.READ,new String[]{r.getSearchURL(),"uischema"},0,new UISchema(r,"search"));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"uispec"},0,new UISpec(r,"screen"));
			addMethod(Operation.READ,new String[]{r.getTabURL(),"uispec"},0,new UISpec(r,"tab"));
			addMethod(Operation.READ,new String[]{r.getSearchURL(),"uispec"},0,new UISpec(r,"search"));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"termList"},1,new WebTermList(r));
			
			if(r.isType("authority")){
				//urls like record urls to make life easier for the UI CSPACE-1139
				addMethod(Operation.READ,new String[]{r.getWebURL()},0,new AuthoritiesVocabulariesSearchList(r,false));
				addMethod(Operation.READ,new String[]{r.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(r,true));
				addMethod(Operation.CREATE,new String[]{r.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(r,true));
				
				addMethod(Operation.READ,new String[]{"authorities",r.getWebURL()},0,new AuthoritiesVocabulariesSearchList(r,false));
				addMethod(Operation.READ,new String[]{"authorities",r.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(r,true));
				addMethod(Operation.CREATE,new String[]{"authorities",r.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(r,true));
				addMethod(Operation.CREATE,new String[]{"authorities",r.getWebURL()},0,new VocabulariesCreateUpdate(r,true));
				addMethod(Operation.READ,new String[]{"authorities",r.getWebURL(),"initialize"},0,new AuthoritiesVocabulariesInitialize(r,true));
				addMethod(Operation.READ,new String[]{"authorities",r.getWebURL(),"refresh"},0,new AuthoritiesVocabulariesInitialize(r,false));
				// We need to do a special case for related media. Weird, but makes a certain sense...
				// We do not actually care about the type of r, but rather just the CSID tail.
				// We'll search on media related to the CSID tail.
				if(mediaR!=null) {
					addMethod(Operation.READ,new String[]{r.getWebURL(),mediaR.getWebURL()},1,new RecordSearchList(mediaR,RecordSearchList.MODE_SEARCH_RELATED));
				}
				for(Instance n : r.getAllInstances()) {
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL()},0,new AuthoritiesVocabulariesSearchList(n,false));
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(n,true));
					addMethod(Operation.CREATE,new String[]{"vocabularies",n.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(n,true));				
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"initialize"},0,new AuthoritiesVocabulariesInitialize(n,true));				
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"refresh"},0,new AuthoritiesVocabulariesInitialize(n,false));				
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL()},1,new VocabulariesRead(n,VocabulariesRead.GET_FULL_INFO));
					addMethod(Operation.READ,new String[]{"vocabularies","basic",n.getWebURL()},1,new VocabulariesRead(n,VocabulariesRead.GET_BASIC_INFO));
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"authorities"},1,new VocabulariesRead(n,VocabulariesRead.GET_TERMS_USED_INFO));
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"refobjs"},1,new VocabulariesRead(n,VocabulariesRead.GET_REF_OBJS_INFO));
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"autocomplete"},0,new WebAutoComplete(spec.getRecord(r.getID())));
					addMethod(Operation.CREATE,new String[]{"vocabularies",n.getWebURL()},0,new VocabulariesCreateUpdate(n,true));
					addMethod(Operation.UPDATE,new String[]{"vocabularies",n.getWebURL()},1,new VocabulariesCreateUpdate(n,false));
					addMethod(Operation.DELETE,new String[]{"vocabularies",n.getWebURL()},0,new VocabulariesDelete(n));
					addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"source-vocab"},1,new VocabRedirector(r));
				}
			}
			else if(r.isType("record") || r.isType("blob") || r.isType("authorizationdata")){
				addMethod(Operation.READ,new String[]{r.getWebURL(),"__auto"},0,new WebAuto());
				addMethod(Operation.READ,new String[]{r.getWebURL(),"autocomplete"},0,new WebAutoComplete(spec.getRecord(r.getID())));
				addMethod(Operation.READ,new String[]{r.getWebURL(),"search"},0,new RecordSearchList(r,RecordSearchList.MODE_SEARCH));
				addMethod(Operation.CREATE,new String[]{r.getWebURL(),"search"},0,new RecordSearchList(r,RecordSearchList.MODE_SEARCH));
				addMethod(Operation.READ,new String[]{r.getWebURL()},0,new RecordSearchList(r,RecordSearchList.MODE_LIST));
				addMethod(Operation.READ,new String[]{r.getWebURL()},1,new RecordRead(r));
				addMethod(Operation.READ,new String[]{"basic",r.getWebURL()},1,new RecordRead(r,true));
				addMethod(Operation.DELETE,new String[]{r.getWebURL()},1,new RecordDelete(r.getID()));
				addMethod(Operation.CREATE,new String[]{r.getWebURL()},0,new RecordCreateUpdate(r,true));
				addMethod(Operation.UPDATE,new String[]{r.getWebURL()},1,new RecordCreateUpdate(r,false));
				addMethod(Operation.READ,new String[]{r.getWebURL(),"source-vocab"},1,new VocabRedirector(r));
				addMethod(Operation.READ,new String[]{r.getWebURL(),"authorities"},1,new RecordAuthorities(r));

				//addMethod(Operation.READ,new String[]{r.getWebURL(),"procedures"},1,new RecordRelated(r)); 
				
				// All the relatable records need to be able to fetch the other relatable records.
				if((r.isType("procedure") && !r.isType("vocabulary"))
						|| "collection-object".equals(r.getID())) {
					for(Record r2 : spec.getAllRecords()) {
						if((r2.isType("procedure") && !r2.isType("vocabulary"))
								|| "collection-object".equals(r2.getID())) {
							// We do not actually care about the type of r, but rather just the CSID tail.
							// We'll search on r2's related to the CSID tail.
							addMethod(Operation.READ,new String[]{r.getWebURL(),r2.getWebURL()},1,new RecordSearchList(r2,RecordSearchList.MODE_SEARCH_RELATED)); 
						} else if(r2.isType("searchall")){
							// Allow to ask for all procedures related to this record. Use SearchAll with a related search.
							addMethod(Operation.READ,new String[]{r.getWebURL(),"procedures"},1,new RecordSearchList(r2,RecordSearchList.MODE_SEARCH_RELATED, "procedure"));
						}
					}
				}
				
			}
			else if(r.isType("userdata")){
				addMethod(Operation.READ,new String[]{r.getWebURL(),"search"},0,new UserDetailsSearchList(r,true));
				addMethod(Operation.READ,new String[]{r.getWebURL()},1,new UserDetailsRead(r));
				addMethod(Operation.READ,new String[]{r.getWebURL()},0,new UserDetailsSearchList(r,false));
				addMethod(Operation.DELETE,new String[]{r.getWebURL()},1,new UserDetailsDelete(r.getID()));
				addMethod(Operation.CREATE,new String[]{r.getWebURL()},0,new UserDetailsCreateUpdate(r,true));
				addMethod(Operation.UPDATE,new String[]{r.getWebURL()},1,new UserDetailsCreateUpdate(r,false));
				
				addMethod(Operation.READ,new String[]{r.getWebURL()},3,new UserRolesRead(spec.getRecordByWebUrl("userrole")));
				addMethod(Operation.CREATE,new String[]{r.getWebURL()},2,new UserRolesCreate(spec.getRecordByWebUrl("userrole")));
				addMethod(Operation.DELETE,new String[]{r.getWebURL()},3,new UserRolesDelete(spec.getRecordByWebUrl("userrole").getID()));
			}
			else if(r.isType("id")){
				// XXX this isn't right but it does work. NEEDS to have it's own methods rather than piggy backing on RECORD
				addMethod(Operation.READ,new String[]{r.getWebURL(),"__auto"},0,new WebAuto());
				addMethod(Operation.READ,new String[]{r.getWebURL(),"autocomplete"},0,new WebAutoComplete(spec.getRecord(r.getID())));
				addMethod(Operation.READ,new String[]{r.getWebURL(),"search"},0,new RecordSearchList(r,RecordSearchList.MODE_SEARCH));
				addMethod(Operation.READ,new String[]{r.getWebURL()},0,new RecordSearchList(r,RecordSearchList.MODE_LIST));
				addMethod(Operation.READ,new String[]{r.getWebURL()},1,new RecordRead(r));
				addMethod(Operation.DELETE,new String[]{r.getWebURL()},1,new RecordDelete(r.getID()));
				addMethod(Operation.CREATE,new String[]{r.getWebURL()},0,new RecordCreateUpdate(r,true));
				addMethod(Operation.UPDATE,new String[]{r.getWebURL()},1,new RecordCreateUpdate(r,false));
				addMethod(Operation.READ,new String[]{r.getWebURL(),"source-vocab"},1,new VocabRedirector(r));
			}
			else if(r.isType("searchall")){
				addMethod(Operation.READ,new String[]{r.getWebURL(),"search"},0,new RecordSearchList(r,RecordSearchList.MODE_SEARCH));
			}
			
		}

		addMethod(Operation.CREATE,new String[]{"relationships"},0,new RelateCreateUpdate(true));
		addMethod(Operation.UPDATE,new String[]{"relationships"},1,new RelateCreateUpdate(false));
		addMethod(Operation.READ,new String[]{"relationships"},1,new RelateRead());
		addMethod(Operation.READ,new String[]{"relationships","hierarchical"},1,new RelateRead("hierarchical"));
		addMethod(Operation.DELETE,new String[]{"relationships"},1,new RelateDelete(false));
		addMethod(Operation.DELETE,new String[]{"relationships","one-way"},1,new RelateDelete(true));
		addMethod(Operation.READ,new String[]{"relationships","search"},0,new RelateSearchList(true));
		addMethod(Operation.READ,new String[]{"relationships","hierarchical","search"},0,new RelateSearchList(true,"hierarchical"));
		addMethod(Operation.READ,new String[]{"relationships","hierarchical"},0,new RelateSearchList(false,"hierarchical"));
		addMethod(Operation.READ,new String[]{"relationships"},0,new RelateSearchList(false));

	}
	
	@Override
	public void config_finish() throws CSPDependencyException {}
	
	@Override
	public void complete_init() throws CSPDependencyException {
		Spec spec=(Spec)ctx.getConfigRoot().getRoot(Spec.SPEC_ROOT);
		if(spec==null)
			throw new CSPDependencyException("Could not load spec");
		configure_finish(spec);
		for(WebMethod m : all_methods)
			m.configure(this,spec);		
		String name=(String)ctx.getConfigRoot().getRoot(CSPContext.XXX_SERVICE_NAME);
		xxx_storage=ctx.getStorage(name);
	}

	@Override
	public void serviceRequest(UIRequest ui) throws UIException {		
		CSPRequestCache cache=new RequestCache();
		String[] path=ui.getPrincipalPath();
		Request r=new Request(xxx_storage,cache,ui);
		
		String test = ui.getRequestedOperation().toString();
		log.debug("ServiceRequest path: "+StringUtils.join(path,"/"));
		log.debug(test);
		try {
			if(tries.get(ui.getRequestedOperation()).call(path,r))
				return;
		} catch(UIException e) {
			throw e;
		} catch (Exception e) {
			throw new UIException("Error in read",e);
		}
		throw new UIException("path not used");
	}
}
