package org.collectionspace.chain.csp.webui.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.config.main.impl.BootstrapCSP;
import org.collectionspace.chain.csp.config.CoreConfig;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.Target;
import org.collectionspace.chain.pathtrie.Trie;
import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigException;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.config.Configurable;
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
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;
import org.collectionspace.csp.helper.core.RequestCache;

public class WebUI implements CSP, UI, NConfigurable {
	public static String SECTION_PREFIX="org.collectionspace.app.config.ui.web.";
	public static String WEBUI_ROOT=SECTION_PREFIX+"web";


	private static final Map<String,String> url_to_type=new HashMap<String,String>();
	private static final Map<String,String> type_to_url=new HashMap<String,String>();
	private Map<Operation,Trie> tries=new HashMap<Operation,Trie>();
	private List<WebMethod> all_methods=new ArrayList<WebMethod>();
	private CSPContext ctx;
	private StorageGenerator xxx_storage;

	// XXX move mapping out
	static {
		url_to_type.put("objects","collection-object");
		url_to_type.put("intake","intake");
		url_to_type.put("acquisition","acquisition");
		url_to_type.put("id","id");

		for(Map.Entry<String,String> e : url_to_type.entrySet())
			type_to_url.put(e.getValue(),e.getKey());		
	}

	public static String convertTypeURLToType(String in) { return url_to_type.get(in); }
	public static String convertTypeToTypeURL(String in) { return type_to_url.get(in); }

	public String getName() { return "ui.webui"; }

	private void addMethod(Operation op,String[] path,int extra,WebMethod method) {
		tries.get(op).addMethod(path,extra,method);
		all_methods.add(method);
	}

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
		ctx.addUI("web",this);
		this.ctx=ctx;
		for(Operation op : Operation.values())
			tries.put(op,new Trie());		
		addMethod(Operation.READ,new String[]{"login"},0,new WebLogin());
		addMethod(Operation.READ,new String[]{"reset"},0,new WebReset(false));
		addMethod(Operation.READ,new String[]{"quick-reset"},0,new WebReset(true));
		for(Map.Entry<String,String> e : url_to_type.entrySet()) {
			addMethod(Operation.READ,new String[]{e.getKey(),"__auto"},0,new WebAuto());
			addMethod(Operation.READ,new String[]{e.getKey(),"autocomplete"},0,new WebAutoComplete());
			addMethod(Operation.READ,new String[]{e.getKey(),"search"},0,new WebSearchList(e.getValue(),true));
			addMethod(Operation.READ,new String[]{e.getKey()},0,new WebSearchList(e.getValue(),false));
			addMethod(Operation.READ,new String[]{e.getKey(),"uispec"},0,new WebUISpec(e.getValue()));
			addMethod(Operation.READ,new String[]{e.getKey(),"schema"},0,new WebUISpec(e.getValue()));
			addMethod(Operation.READ,new String[]{e.getKey()},1,new WebRead(e.getValue()));
			addMethod(Operation.DELETE,new String[]{e.getKey()},1,new WebDelete(e.getValue()));
			addMethod(Operation.CREATE,new String[]{e.getKey()},0,new WebCreateUpdate(e.getKey(),e.getValue(),true));
			addMethod(Operation.UPDATE,new String[]{e.getKey()},1,new WebCreateUpdate(e.getKey(),e.getValue(),false));
		}
	}

	public void nconfigure(Rules rules) throws CSPDependencyException {
		/* MAIN/ui/web -> UI */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"ui","web"},SECTION_PREFIX+"web",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				((CoreConfig)parent).setRoot(WEBUI_ROOT,WebUI.this);
				for(WebMethod m : all_methods)
					try {
						m.configure(milestone);
					} catch (ConfigException e) {
						// XXX throwable
					}
					return WebUI.this;
			}
		});	
	}

	public void config_finish() {
		for(WebMethod m : all_methods)
			m.configure_finish();		
		BootstrapConfigController bootstrap=(BootstrapConfigController)ctx.getNConfigRoot().getRoot(BootstrapCSP.BOOTSTRAP_ROOT);
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
