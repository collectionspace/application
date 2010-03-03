package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.bconfigutils.bootstrap.BootstrapCSP;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.persistence.services.relation.ServicesRelationStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.GenericVocabStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.ServicesOrgStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.ServicesPersonStorage;
import org.collectionspace.chain.csp.persistence.services.vocab.ServicesVocabStorage;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.csp.helper.persistence.SplittingStorage;

public class ServicesStorageGenerator extends SplittingStorage implements ContextualisedStorage, StorageGenerator, CSP, Configurable {
	public static String SECTION_PREFIX="org.collectionspace.app.config.persistence.service.";
	public static String SERVICE_ROOT=SECTION_PREFIX+"service";
	private String base_url;
	private CSPContext ctx;
	
	public Storage getStorage(CSPRequestCache cache) {
		return new ServicesStorage(this,cache);
	}

	public String getName() { return "persistence.services"; }

	private void real_init(Spec spec) throws CSPDependencyException {
		try {
			ServicesConnection conn=new ServicesConnection(base_url);
			addChild("collection-object",new ServicesCollectionObjectStorage(conn));
			addChild("intake",new ConfiguredRecordStorage(spec.getRecord("intake"),conn));
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

	public void configure(Rules rules) throws CSPDependencyException {
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
		BootstrapConfigController bootstrap=(BootstrapConfigController)ctx.getConfigRoot().getRoot(BootstrapCSP.BOOTSTRAP_ROOT);
		if(bootstrap!=null) {
			String boot_root=bootstrap.getOption("store-url");
			if(boot_root!=null)
				base_url=boot_root;
		}
		Spec spec=(Spec)ctx.getConfigRoot().getRoot(Spec.SPEC_ROOT);
		if(spec==null)
			throw new CSPDependencyException("Could not load spec");
		real_init(spec);
	}
}
