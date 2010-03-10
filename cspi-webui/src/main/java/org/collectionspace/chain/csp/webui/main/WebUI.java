package org.collectionspace.chain.csp.webui.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapCSP;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.authorities.AuthoritiesVocabulariesSearchList;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesCreateUpdate;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesDelete;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesRead;
import org.collectionspace.chain.csp.webui.misc.WebAuto;
import org.collectionspace.chain.csp.webui.misc.WebAutoComplete;
import org.collectionspace.chain.csp.webui.misc.WebLogin;
import org.collectionspace.chain.csp.webui.misc.WebReset;
import org.collectionspace.chain.csp.webui.misc.WebUISpec;
import org.collectionspace.chain.csp.webui.nuispec.UISpec;
import org.collectionspace.chain.csp.webui.nuispec.FindEditUISpec;
import org.collectionspace.chain.csp.webui.nuispec.TabUISpec;
import org.collectionspace.chain.csp.webui.record.RecordCreateUpdate;
import org.collectionspace.chain.csp.webui.record.RecordDelete;
import org.collectionspace.chain.csp.webui.record.RecordRead;
import org.collectionspace.chain.csp.webui.record.RecordSearchList;
import org.collectionspace.chain.csp.webui.relate.RelateCreateUpdate;
import org.collectionspace.chain.csp.webui.relate.RelateDelete;
import org.collectionspace.chain.csp.webui.relate.RelateRead;
import org.collectionspace.chain.csp.webui.relate.RelateSearchList;
import org.collectionspace.chain.pathtrie.Trie;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.helper.core.RequestCache;

public class WebUI implements CSP, UI, Configurable {
	public static String SECTION_PREFIX="org.collectionspace.app.config.ui.web.";
	public static String WEBUI_ROOT=SECTION_PREFIX+"web";

	private Map<Operation,Trie> tries=new HashMap<Operation,Trie>();
	private List<WebMethod> all_methods=new ArrayList<WebMethod>();
	private CSPContext ctx;
	private StorageGenerator xxx_storage;
	private String uispec_path;
	private String login_dest,login_failed_dest;

	public String getName() { return "ui.webui"; }
	public String getUISpecPath() { return uispec_path; }
	public String getLoginDest() { return login_dest; }
	public String getLoginFailedDest() { return login_failed_dest; }
	
	private void addMethod(Operation op,String[] path,int extra,WebMethod method) {
		tries.get(op).addMethod(path,extra,method);
		all_methods.add(method);
	}

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
		ctx.addUI("web",this);
		this.ctx=ctx;
	}

	public void configure(Rules rules) throws CSPDependencyException {
		/* MAIN/ui/web -> UI */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"ui","web"},SECTION_PREFIX+"web",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				((CoreConfig)parent).setRoot(WEBUI_ROOT,WebUI.this);
				if(section.getValue("/tmp-schema-path")!=null) {
					uispec_path=System.getProperty("java.io.tmpdir")+"/ju-cspace"; // XXX fix
				} else {
					uispec_path=(String)section.getValue("/schema-path");
				}
				login_dest=(String)section.getValue("/login-dest");
				login_failed_dest=(String)section.getValue("/login-failed-dest");
				return WebUI.this;
			}
		});	
	}

	private void configure_finish(Spec spec) {
		for(Operation op : Operation.values())
			tries.put(op,new Trie());		
		addMethod(Operation.READ,new String[]{"login"},0,new WebLogin());
		addMethod(Operation.READ,new String[]{"reset"},0,new WebReset(false));
		addMethod(Operation.READ,new String[]{"quick-reset"},0,new WebReset(true));
		addMethod(Operation.READ,new String[]{"find-edit","uispec"},0,new FindEditUISpec(spec.getAllRecords()));
		for(Record r : spec.getAllRecords()) {
			addMethod(Operation.READ,new String[]{r.getWebURL(),"ouispec"},0,new WebUISpec(r.getID()));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"uispec"},0,new UISpec(r));
			addMethod(Operation.READ,new String[]{r.getTabURL(),"uispec"},0,new TabUISpec(r));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"oschema"},0,new WebUISpec(r.getID()));
		}
		for(Record r : spec.getAllRecords()) {
			if(r.isType("authority"))
				continue;
			addMethod(Operation.READ,new String[]{r.getWebURL(),"__auto"},0,new WebAuto());
			addMethod(Operation.READ,new String[]{r.getWebURL(),"autocomplete"},0,new WebAutoComplete(spec.getRecord("person"))); // XXX
			addMethod(Operation.READ,new String[]{r.getWebURL(),"search"},0,new RecordSearchList(r,true));
			addMethod(Operation.READ,new String[]{r.getWebURL()},0,new RecordSearchList(r,false));
			addMethod(Operation.READ,new String[]{r.getWebURL()},1,new RecordRead(r));
			addMethod(Operation.DELETE,new String[]{r.getWebURL()},1,new RecordDelete(r.getID()));
			addMethod(Operation.CREATE,new String[]{r.getWebURL()},0,new RecordCreateUpdate(r,true));
			addMethod(Operation.UPDATE,new String[]{r.getWebURL()},1,new RecordCreateUpdate(r,false));
		}
		for(Record r : spec.getAllRecords()) {
			if(!r.isType("authority"))
				continue;
			addMethod(Operation.READ,new String[]{"authorities",r.getWebURL()},0,new AuthoritiesVocabulariesSearchList(r,false));
			addMethod(Operation.READ,new String[]{"authorities",r.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(r,true));
			for(Instance n : r.getAllInstances()) {
				addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL()},0,new AuthoritiesVocabulariesSearchList(n,false));
				addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL(),"search"},0,new AuthoritiesVocabulariesSearchList(n,true));				
				addMethod(Operation.READ,new String[]{"vocabularies",n.getWebURL()},1,new VocabulariesRead(n));
				addMethod(Operation.CREATE,new String[]{"vocabularies",n.getWebURL()},0,new VocabulariesCreateUpdate(n,true));
				addMethod(Operation.UPDATE,new String[]{"vocabularies",n.getWebURL()},1,new VocabulariesCreateUpdate(n,false));
				addMethod(Operation.DELETE,new String[]{"vocabularies",n.getWebURL()},0,new VocabulariesDelete(n));
			}
		}
		addMethod(Operation.CREATE,new String[]{"relationships"},0,new RelateCreateUpdate(true));
		addMethod(Operation.UPDATE,new String[]{"relationships"},1,new RelateCreateUpdate(false));
		addMethod(Operation.READ,new String[]{"relationships"},1,new RelateRead());
		addMethod(Operation.DELETE,new String[]{"relationships"},1,new RelateDelete(false));
		addMethod(Operation.DELETE,new String[]{"relationships","one-way"},1,new RelateDelete(true));
		addMethod(Operation.READ,new String[]{"relationships","search"},0,new RelateSearchList(true));
		addMethod(Operation.READ,new String[]{"relationships"},0,new RelateSearchList(false));
	}
	
	public void config_finish() throws CSPDependencyException {
		Spec spec=(Spec)ctx.getConfigRoot().getRoot(Spec.SPEC_ROOT);
		if(spec==null)
			throw new CSPDependencyException("Could not load spec");
		configure_finish(spec);
		for(WebMethod m : all_methods)
			m.configure(this,spec);		
		BootstrapConfigController bootstrap=(BootstrapConfigController)ctx.getConfigRoot().getRoot(BootstrapCSP.BOOTSTRAP_ROOT);
		xxx_storage=ctx.getStorage(bootstrap.getOption("storage-type"));
	}

	public void serviceRequest(UIRequest ui) throws UIException {		
		CSPRequestCache cache=new RequestCache(); // XXX
		Storage storage=xxx_storage.getStorage(cache); // XXX
		String[] path=ui.getPrincipalPath();
		Request r=new Request(cache,storage,ui);
		System.err.println(StringUtils.join(path,"/"));
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
