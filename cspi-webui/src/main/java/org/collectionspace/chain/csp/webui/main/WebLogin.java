package org.collectionspace.chain.csp.webui.main;

import java.util.HashSet;
import java.util.Set;

import org.collectionspace.chain.pathtrie.TrieMethod;
import org.collectionspace.csp.api.config.ConfigException;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;

public class WebLogin implements WebMethod {
	private static final Set<String> users=new HashSet<String>();
	private String login_dest,login_failed_dest;
	
	static {
		users.add("guest");
		users.add("curator");
		users.add("admin");
	}
	
	private void login(UIRequest request) throws UIException { // Temporary hack for Mars
		String username=request.getRequestArgument("userid");
		String password=request.getRequestArgument("password");
		if(username!=null && username.equals(password) && users.contains(username)) {
			request.setRedirectPath(login_dest.split("/"));
		} else {
			request.setRedirectPath(login_failed_dest.split("/"));
			request.setRedirectArgument("result","fail");
		}
	}
	
	public void configure(ConfigRoot config) throws ConfigException {
		login_dest=config.getString(new String[]{"ui","web","login-dest"});
		login_failed_dest=config.getString(new String[]{"ui","web","login-failed-dest"});
	}
	
	public void run(Object in,String[] tail) throws UIException {
		login(((Request)in).getUIRequest());
	}
}
