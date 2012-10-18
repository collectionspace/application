/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;


import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.authorization.AuthorizationStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.persistence.services.relation.ServicesRelationStorage;
import org.collectionspace.chain.csp.persistence.services.user.UserStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.ConfiguredVocabStorage;
import org.collectionspace.chain.csp.schema.AdminData;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.csp.helper.persistence.SplittingStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicesStorageGenerator extends SplittingStorage implements ContextualisedStorage, StorageGenerator, CSP, Configurable {
	private static final Logger log=LoggerFactory.getLogger(ServicesStorageGenerator.class);
	public static String SECTIONED="org.collectionspace.app.config.spec";
	public static String SECTION_PREFIX="org.collectionspace.app.config.persistence.service.";
	public static String SERVICE_ROOT=SECTION_PREFIX+"service";
	private String base_url,ims_url;
	private CSPContext ctx;
	private TenantSpec tenantData;
	
	public Storage getStorage(CSPRequestCredentials credentials,CSPRequestCache cache) {
		return new ServicesStorage(this,credentials,cache);
	}

	public String getName() { return "persistence.services"; }
	public String getBase() { return base_url; }
	public String getIMSBase() { return ims_url; }
	public TenantSpec getTenantData() { return tenantData; }

	private void real_init(Spec spec) throws CSPDependencyException {
		try {
			ServicesConnection conn=new ServicesConnection(base_url,ims_url);
			for(Record r : spec.getAllRecords()) {
				if(r.isType("blob") || r.isType("report") || r.isType("batch"))
					addChild(r.getID(),new BlobStorage(spec.getRecord(r.getID()),conn));
				else if(r.isType("userdata"))
					addChild(r.getID(),new UserStorage(spec.getRecord(r.getID()),conn));
				else if(r.isType("record") || r.isType("searchall"))
					addChild(r.getID(),new RecordStorage(spec.getRecord(r.getID()),conn));
				else if(r.isType("authority"))
					addChild(r.getID(),new ConfiguredVocabStorage(spec.getRecord(r.getID()),conn));
				else if(r.isType("authorizationdata"))
					addChild(r.getID(),new AuthorizationStorage(spec.getRecord(r.getID()), conn));
			}
			addChild("direct",new DirectRedirector(spec));
			addChild("id",new ServicesIDGenerator(conn,spec));
			addChild("relations",new ServicesRelationStorage(conn,spec));
			
		} catch (Exception e) {
			throw new CSPDependencyException("Could not set target",e); // XXX wrong type
		}
	}
	
	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addStorageType("service",this);
		ctx.addConfigRules(this);
		this.ctx=ctx;
	}

	public void configure(Rules rules) throws CSPDependencyException {
		/* MAIN/persistence/service -> SERVICE */
		rules.addRule(SECTIONED,new String[]{"persistence","service"},SECTION_PREFIX+"service",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				((ConfigRoot)parent).setRoot(SERVICE_ROOT,ServicesStorageGenerator.this);
				base_url=(String)milestone.getValue("/url");
				((ConfigRoot)parent).setRoot(CSPContext.XXX_SERVICE_NAME,"service");  // XXX should be path-selectable
				ims_url=(String)milestone.getValue("/ims-url");
				tenantData = new TenantSpec(milestone);
				return ServicesStorageGenerator.this;
			}
		});

		rules.addRule(SECTION_PREFIX+"service", new String[]{"repository","dateformats","pattern"},SECTION_PREFIX+"dateformat", null, new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				String format = (String)milestone.getValue("");
				tenantData.addFormat(format);
				return this;
			}
		});
		rules.addRule(SECTION_PREFIX+"service", new String[]{"languages","language"},SECTION_PREFIX+"language", null, new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				String lang = (String)milestone.getValue("");
				tenantData.addLanguage(lang);
				return this;
			}
		});
	}
		
	public void config_finish() throws CSPDependencyException {}
	
	public void complete_init() throws CSPDependencyException {
		Spec spec=(Spec)ctx.getConfigRoot().getRoot(Spec.SPEC_ROOT);
		if(spec==null)
			throw new CSPDependencyException("Could not load spec");
		real_init(spec);
	}

	public CSPRequestCredentials createCredentials() {
		return new ServicesRequestCredentials();
	}
}
