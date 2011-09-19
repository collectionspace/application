package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.file.FileStorage;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.ui.UI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIUmbrella;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.dom4j.DocumentException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TenantServlet extends HttpServlet {
	private static final Logger log=LoggerFactory.getLogger(TenantServlet.class);
	protected static final String COOKIENAME="CSPACESESSID";
	private static final long serialVersionUID = -4343156244448081917L;
	protected Map<String, CSPManagerImpl> tenantCSPM = new HashMap<String, CSPManagerImpl>();
	protected Map<String, Boolean> tenantInit = new HashMap<String, Boolean>();
	protected Map<String, UIUmbrella> tenantUmbrella = new HashMap<String, UIUmbrella>();

	protected String locked_down=null;
	
	/* Not in the constructor because errors during construction of servlets tend to get lost in a mess of startup.
	 * Better present it on first request.
	 */

	protected void register_csps(String tenantId) throws IOException, DocumentException {
		if(!tenantCSPM.containsKey(tenantId)){
			tenantCSPM.put(tenantId, new CSPManagerImpl());
		}
		tenantCSPM.get(tenantId).register(new CoreConfig());
		tenantCSPM.get(tenantId).register(new FileStorage());
		tenantCSPM.get(tenantId).register(new ServicesStorageGenerator());
		tenantCSPM.get(tenantId).register(new WebUI());
		tenantCSPM.get(tenantId).register(new Spec());
	}
	
	protected String getTenantByCookie(HttpServletRequest servlet_request){
		Cookie[] cookies = servlet_request.getCookies();
		if(cookies==null)
			cookies=new Cookie[0];
		for(Cookie cookie : cookies) {
			if(!COOKIENAME.equals(cookie.getName()))
				continue;
			
			//loop over all umbrellas and find the one we have the session for
			for(Map.Entry<String, UIUmbrella> entry: tenantUmbrella.entrySet()){
				String name = entry.getKey();
				UIUmbrella umb = entry.getValue();

				WebUISession session=((WebUIUmbrella) umb).getSession(cookie.getValue());
				if(session!=null)
					return name;
			}
		}
		return "";
	}
	protected void load_config(ServletContext ctx, String tenantId) throws CSPDependencyException {
		try {
			ConfigFinder cfg=new ConfigFinder(ctx);
			InputSource cfg_stream = cfg.resolveEntity("-//CSPACE//ROOT","cspace-config-"+tenantId+".xml");
			if(cfg_stream==null) {
				locked_down="Cannot find cspace config xml file";
			} else {
				tenantCSPM.get(tenantId).configure(cfg_stream,cfg);
			}
		} catch (UnsupportedEncodingException e) {
			throw new CSPDependencyException("Config has bad character encoding",e);
		} catch (IOException e) {
			throw new CSPDependencyException("Cannot load config",e);			
		} catch (SAXException e) {
			throw new CSPDependencyException("Cannot parse config file",e);			
		}
	}
	
	protected synchronized void setup(String tenantId) throws BadRequestException {
		if(tenantInit.containsKey(tenantId) && tenantInit.get(tenantId))
			return;
		try {
			// Register csps
			register_csps(tenantId);
			tenantCSPM.get(tenantId).go(); // Start up CSPs
			load_config(getServletContext(),tenantId);
		} catch (IOException e) {
			throw new BadRequestException("Cannot load config "+e,e);
		} catch (DocumentException e) {
			throw new BadRequestException("Cannot load backend "+e,e);
		} catch (CSPDependencyException e) {
			throw new BadRequestException("Cannot initialise CSPs "+e,e);
		}
		tenantInit.put(tenantId,true);
	}
	
	protected void serviceWTenant(String tenantid, List<String> pathparts, String initcheck, HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException, BadRequestException {
		

		if(locked_down!=null) {
			//this ended up with a status 200 hmmmm not great so changed it to return a 400... hopefully that wont break anythign else

			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Servlet is locked down in a hard fail because of fatal error: "+locked_down);
			//servlet_response.getWriter().append("Servlet is locked down in a hard fail because of fatal error: "+locked_down);
			return;
		}
		
		//reinit if url = /chain/init
		
		if(initcheck.equals("init")){
			tenantCSPM.put(tenantid, new CSPManagerImpl());
			tenantInit.put(tenantid, false);
			setup(tenantid);

			
			ConfigFinder cfg=new ConfigFinder(getServletContext());
			try {
				InputSource cfg_stream=cfg.resolveEntity("-//CSPACE//ROOT","cspace-config-"+tenantid+".xml");
				String test = IOUtils.toString(cfg_stream.getByteStream());
				//servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, "cspace-config re-loaded"+test);
				servlet_response.getWriter().append("cspace-config re-loaded"+test);
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, "cspace-config re-loadedfailed");
			}
			
			
			return;
		}
		
		if(!tenantInit.containsKey(tenantid) || !tenantInit.get(tenantid))
			setup(tenantid);
		
		if(locked_down!=null) {
			//this ended up with a status 200 hmmmm not great so changed it to return a 400... hopefully that wont break anythign else

			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Servlet is locked down in a hard fail because of fatal error: "+locked_down);
			//servlet_response.getWriter().append("Servlet is locked down in a hard fail because of fatal error: "+locked_down);
			return;
		}
		if(perhapsServeFixedContent(servlet_request,servlet_response))
			return;
		// Setup our request object
		UI web=tenantCSPM.get(tenantid).getUI("web");
		if(!tenantUmbrella.containsKey(tenantid)){
			synchronized(getClass()) {
				if(!tenantUmbrella.containsKey(tenantid)) {
					tenantUmbrella.put(tenantid, new WebUIUmbrella((WebUI)web));
				}
			}
		}
		try {
			ConfigRoot root=tenantCSPM.get(tenantid).getConfigRoot();
			Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
			WebUIRequest req=new WebUIRequest(tenantUmbrella.get(tenantid),servlet_request,servlet_response,spec.getAdminData().getCookieLife(),pathparts);
			if(is_composite(req)) {
				serve_composite(web,req);
			} else {
				web.serviceRequest(req);
				req.solidify(true);
			}
		} catch (UIException e) {
			throw new BadRequestException("UIException",e);
		}
		
	}
	public void service(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {
			String pathinfo = servlet_request.getPathInfo();
			String[] pathbits = pathinfo.substring(1).split("/");
			String test = servlet_request.getServletPath();
			String tenant = ""; 
			String checkinit = "";

			List<String> p=new ArrayList<String>();
			for(String part : servlet_request.getPathInfo().split("/")) {
				if("".equals(part))
					continue;
				p.add(part);
			}		
		
			if(test.equals("/chain")){
				if (pathbits[0].equals("chain")) {
					servlet_response.sendRedirect(pathinfo);
					return;
				}
				tenant = getTenantByCookie(servlet_request);
				checkinit = pathbits[0];
				if(tenant.equals("")){
					servlet_response.getWriter().append("Servlet is locked down in a hard fail because no tenant specified: ");
				}
			}
			else{
				if (pathbits[0].equals("tenant")) {
					servlet_response.sendRedirect(pathinfo);
					return;
				}
				p.remove(0);
				tenant = pathbits[0];
				if(tenant.equals("html")){
					tenant = getTenantByCookie(servlet_request);
				}
				checkinit = pathbits[1];
			}
			
			serviceWTenant(tenant, p, checkinit, servlet_request, servlet_response);
			
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, getStackTrace(x));
		}
	}
	
	/* We do all this very sequentially rather than in one big loop to avoid the fear of weird races and to fail early on parse errors */
	protected void serve_composite(UI ui,WebUIRequest req) throws UIException {
		try {
			// Extract JSON request payload
			JSONObject in=req.getJSONBody();
			// Build composite object for each subrequest
			Map <String,CompositeWebUIRequestPart> subrequests=new HashMap<String,CompositeWebUIRequestPart>();
			Iterator<?> ki=in.keys();
			while(ki.hasNext()) {
				String key=(String)ki.next();
				JSONObject value=in.getJSONObject(key);
				CompositeWebUIRequestPart sub=new CompositeWebUIRequestPart(req,value);
				subrequests.put(key,sub);
			}
			// Build a place for results
			JSONObject out=new JSONObject();
			// Execute each composite object
			for(String key : subrequests.keySet()) {
				CompositeWebUIRequestPart sub=subrequests.get(key);
				ui.serviceRequest(sub);
				JSONObject value=sub.solidify();
				out.put(key,value);
			}
			// Send result
			req.sendJSONResponse(out);
			req.setOperationPerformed(req.getRequestedOperation());
			req.solidify(true);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected boolean serveContent(HttpServletResponse servlet_response,InputStream is) throws IOException{

		if(is==null)
			return false; // Not for us

		IOUtils.copy(is,servlet_response.getOutputStream());
		return true;
	}
	
	protected boolean serveFixedContent(ServletContext sc,String path, String tenant, HttpServletResponse servlet_response) throws IOException{
		List<String> testpaths =new ArrayList<String>();
		testpaths.add("/"+tenant+path);
		testpaths.add(path);
		
		if(path.startsWith("/"+tenant+"/")){
			path = path.replace("/"+tenant+"/", "/");
		}
		testpaths.add("/tenants/"+tenant+"/html"+path);
		testpaths.add("/tenants/"+tenant+""+path);
		testpaths.add("/defaults/html"+path);
		testpaths.add("/defaults"+path);
		InputStream is = null;
		while( is == null && testpaths.size()> 0){
			is=sc.getResourceAsStream(testpaths.remove(0));
			String mimetype = sc.getMimeType(testpaths.remove(0));
			servlet_response.setContentType(mimetype);
		}
		if(is==null)
			return false; // Not for us

		IOUtils.copy(is,servlet_response.getOutputStream());
		return true;
	}

	protected InputStream getFixedContent(ServletContext sc,String path, String tenant){
		List<String> testpaths =new ArrayList<String>();
		testpaths.add("/"+tenant+path);
		testpaths.add(path);
		
		if(path.startsWith("/"+tenant+"/")){
			path = path.replace("/"+tenant+"/", "/");
		}
		testpaths.add("/tenants/"+tenant+"/html"+path);
		testpaths.add("/tenants/"+tenant+""+path);
		testpaths.add("/defaults/html"+path);
		testpaths.add("/defaults"+path);
		InputStream is = null;
		while( is == null && testpaths.size()> 0){
			is=sc.getResourceAsStream(testpaths.remove(0));
		}
        return is;
		
	}
	protected boolean serverFixedExternalContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response,ServletContext sc,String path, String tenant) throws IOException{
		//InputStream is = getFixedContent( sc, path,  tenant, servlet_response);
        return serveFixedContent(sc, path,  tenant, servlet_response);
	}
	protected boolean serverFixedExternalContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response,ServletContext sc,String path) throws IOException{

        InputStream is=sc.getResourceAsStream(path);
        return serveContent(servlet_response,is);
	}
	
	protected boolean is_composite(WebUIRequest req) throws UIException {
		String[] path=req.getPrincipalPath();
		if(path.length!=1)
			return false;
		return "composite".equals(path[0]);
	}
	
	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}
	
	protected boolean perhapsServeFixedContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		String pathinfo=servlet_request.getPathInfo();
		if(pathinfo.startsWith("/"))
			pathinfo=pathinfo.substring(1);
		InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(pathinfo);
		if(is==null)
			return false; // Not for us
		// Serve fixed content
		IOUtils.copy(is,servlet_response.getOutputStream());
		return true;
	}
}
