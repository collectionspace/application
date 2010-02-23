package org.collectionspace.chain.csp.webui.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapCSP;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
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
	String getUISpecPath() { return uispec_path; }
	String getLoginDest() { return login_dest; }
	String getLoginFailedDest() { return login_failed_dest; }
	
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
		for(Record r : spec.getAllRecords()) {
			addMethod(Operation.READ,new String[]{r.getWebURL(),"__auto"},0,new WebAuto());
			addMethod(Operation.READ,new String[]{r.getWebURL(),"autocomplete"},0,new WebAutoComplete());
			addMethod(Operation.READ,new String[]{r.getWebURL(),"search"},0,new WebSearchList(r,true));
			addMethod(Operation.READ,new String[]{r.getWebURL()},0,new WebSearchList(r,false));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"uispec"},0,new WebUISpec(r.getID()));
			addMethod(Operation.READ,new String[]{r.getWebURL(),"schema"},0,new WebUISpec(r.getID()));
			addMethod(Operation.READ,new String[]{r.getWebURL()},1,new WebRead(r));
			addMethod(Operation.DELETE,new String[]{r.getWebURL()},1,new WebDelete(r.getID()));
			addMethod(Operation.CREATE,new String[]{r.getWebURL()},0,new WebCreateUpdate(r,true));
			addMethod(Operation.UPDATE,new String[]{r.getWebURL()},1,new WebCreateUpdate(r,false));
		}
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
			tries.get(ui.getRequestedOperation()).call(path,r);
		} catch(UIException e) {
			throw e;
		} catch (Exception e) {
			throw new UIException("Error in read",e);
		}
	}
}
