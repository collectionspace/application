package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.csp.inner.BootstrapCSP;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.Target;
import org.collectionspace.chain.csp.persistence.file.FileStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.persistence.services.relation.ServicesRelationStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.GenericVocabStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.ServicesOrgStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.ServicesPersonStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.ServicesVocabStorage;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.csp.helper.persistence.SplittingStorage;

public class ServicesStorageGenerator extends SplittingStorage implements ContextualisedStorage, StorageGenerator, CSP, NConfigurable {
	public static String SECTION_PREFIX="org.collectionspace.app.config.persistence.service.";
	public static String SERVICE_ROOT=SECTION_PREFIX+"service";
	private String base_url;
	private CSPContext ctx;
	
	public Storage getStorage(CSPRequestCache cache) {
		return new ServicesStorage(this,cache);
	}

	public String getName() { return "persistence.services"; }

	private void real_init() throws CSPDependencyException {
		try {
			ServicesConnection conn=new ServicesConnection(base_url);
			addChild("collection-object",new ServicesCollectionObjectStorage(conn));
			addChild("intake",new ServicesIntakeStorage(conn));
			addChild("acquisition",new ServicesAcquisitionStorage(conn));
			addChild("id",new ServicesIDGenerator(conn));
			addChild("relations",new ServicesRelationStorage(conn));
			addChild("person",new ServicesPersonStorage(conn));
			//addChild("orgs",new ServicesOrgStorage(conn));
			addChild("vocab",new ServicesVocabStorage(conn));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new CSPDependencyException("Could not set target",e); // XXX wrong type
		}
	}
	
	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addStorageType("service",this);
		ctx.addConfigRules(this);
		this.ctx=ctx;
	}

	public ServicesStorageGenerator() {}
	public ServicesStorageGenerator(String store) throws CSPDependencyException {
		this.base_url=store;
		real_init();
	}
	
	public void nconfigure(Rules rules) throws CSPDependencyException {
		/* MAIN/persistence/file -> SERVICE */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"persistence","service"},SECTION_PREFIX+"service",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				((CoreConfig)parent).setRoot(SERVICE_ROOT,ServicesStorageGenerator.this);
				base_url=(String)milestone.getValue("/url");
				return ServicesStorageGenerator.this;
			}
		});
	}
	
	public void config_finish() throws CSPDependencyException {
		BootstrapConfigController bootstrap=(BootstrapConfigController)ctx.getNConfigRoot().getRoot(BootstrapCSP.BOOTSTRAP_ROOT);
		String boot_root=bootstrap.getOption("store-url");
		if(boot_root!=null)
			base_url=boot_root;
		real_init();
	}
}
