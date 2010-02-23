package org.collectionspace.chain.csp.webui.main;

import java.util.HashSet;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.impl.main.ConfigException;
import org.collectionspace.chain.pathtrie.TrieMethod;
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
		
	public void run(Object in,String[] tail) throws UIException {
		login(((Request)in).getUIRequest());
	}

	public void configure(ReadOnlySection section) throws ConfigException {
		login_dest=(String)section.getValue("/login-dest");
		login_failed_dest=(String)section.getValue("/login-failed-dest");
	}

	public void configure_finish() {}
}
