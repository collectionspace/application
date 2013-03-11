package org.collectionspace.chain.controller;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.file.FileStorage;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.chain.csp.schema.AdminData;
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

/**
 * This is the servlet that all calls that should be directly handled by App code should go through
 * This was separated from TenantUIServlet which deals with all the calls that used to go directly to the UI
 * e.g. http://nightly.collectionspace.org/collectionspace/ui/core/html/components/footer.html
 * vs. http://nightly.collectionspace.org/collectionspace/tenant/core/loginstatus
 * 
 * if /WEB-INF/web.xml you will find
 *   <servlet>
 *    <servlet-name>TenantServlet</servlet-name>
 *    <servlet-class>org.collectionspace.chain.controller.TenantServlet</servlet-class>
 *  </servlet>
 *  <servlet>
 *    <servlet-name>TenantUIServlet</servlet-name>
 *    <servlet-class>org.collectionspace.chain.controller.TenantUIServlet</servlet-class>
 *  </servlet>
 *
 *  <servlet-mapping>
 *    <servlet-name>TenantUIServlet</servlet-name>
 *    <url-pattern>/ui/*</url-pattern>
 *  </servlet-mapping>
 *  <servlet-mapping>
 *    <servlet-name>TenantServlet</servlet-name>
 *    <url-pattern>/tenant/*</url-pattern>
 *  </servlet-mapping>
 *  <servlet-mapping>
 *    <servlet-name>TenantServlet</servlet-name>
 *    <url-pattern>/chain/*</url-pattern>
 *  </servlet-mapping>
 *
 * @author csm22
 *
 */
public class TenantServlet extends HttpServlet {
	private static final Logger log=LoggerFactory.getLogger(TenantServlet.class);
	protected static final String COOKIENAME="CSPACESESSID";
	private static final long serialVersionUID = -4343156244448081917L;
	private static final String SERVER_HOME_PROPERTY = "catalina.home";
	private static final String PUBLISHED_DIR = "/cspace/published";
	protected Map<String, CSPManagerImpl> tenantCSPM = new HashMap<String, CSPManagerImpl>();
	protected Map<String, Boolean> tenantInit = new HashMap<String, Boolean>();
	protected Map<String, UIUmbrella> tenantUmbrella = new HashMap<String, UIUmbrella>();
	
	protected final String MIME_AUDIO = "audio/";
	protected final String MIME_VIDIO = "vidio/";
	protected final String MIME_IMAGE = "image/";
	protected final String MIME_JSON = "text/javascript";
	protected final String MIME_HTML = "text/html";
	protected final String MIME_CSS = "text/css";
	protected final String MIME_PLAIN = "text/plain";
	protected final String SUFFIX_PROPS = ".properties";
	protected final String SUFFIX_JSON = ".json";

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
	
	/**
	 * Guess the tenant by looking at a cookie set during login
	 * @param servlet_request
	 * @return
	 */
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
	/**
	 * retrieve the correct config file based on the tenantId
	 * @param ctx
	 * @param tenantId
	 * @throws CSPDependencyException
	 */
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
	
	/**
	 * setup CSPManagerImpl, load config etc if required.
	 * @param tenantId
	 * @throws BadRequestException
	 */
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
	
	/**
	 * If you know the tenant id then
	 * Check for global issues and if not run set up and then test if this is a init request, a composite request or a single request and 
	 * do the right thing
	 * @param tenantid
	 * @param pathparts
	 * @param initcheck
	 * @param servlet_request
	 * @param servlet_response
	 * @throws ServletException
	 * @throws IOException
	 * @throws BadRequestException
	 */
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
		if(perhapsServeFixedContent(servlet_request,servlet_response)) {
			return;
		}
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
	
	/**
	 * This is a entry point. 
	 * First check is for the tenant Id
	 */
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
		
			// /chain is now deprecated and should not appear in URLs - all urls should be tenant
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
				if (pathbits[0].equals("tenant")) { //somehow we have come across a url which isn't nested as we hoped and we need to remove the first item and re-analyze it 
					servlet_response.sendRedirect(pathinfo);
					return;
				}
				p.remove(0);
				tenant = pathbits[0];
				if(tenant.equals("html")){ // this was a early mistake in urls where html was used instead of tenant - hopefully this code is never fired
					tenant = getTenantByCookie(servlet_request);
				}
				checkinit = pathbits[1];
			}
			
			serviceWTenant(tenant, p, checkinit, servlet_request, servlet_response);
			
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, getStackTrace(x));
		}
	}
	

	/**
	 * Iterate through a composite request sequentially.
	 * We do all this very sequentially rather than in one big loop to avoid the fear of weird races and to fail early on parse errors
	 * @param ui
	 * @param req
	 * @throws UIException
	 */
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
	
	/**
	 * output the content to the user
	 * @param servlet_response
	 * @param is
	 * @return
	 * @throws IOException
	 */
	protected boolean serveContent(HttpServletResponse servlet_response,InputStream is) throws IOException{

		if(is==null)
			return false; // Not for us

		IOUtils.copy(is,servlet_response.getOutputStream());
		return true;
	}
	
	/**
	 * Helper function to create a list of all possible valid paths for a certain tenant/path combination
	 * Used to help fall through
	 * @param path
	 * @param tenant
	 * @param withdefaults
	 * @return
	 */
	protected List<String> possiblepaths(String path, String tenant, Boolean withdefaults){
		List<String> testpaths =new ArrayList<String>();
		testpaths.add("/"+tenant+path);
		testpaths.add(path);
		
		if(path.startsWith("/"+tenant+"/")){
			path = path.replace("/"+tenant+"/", "/");
		}
		testpaths.add("/tenants/"+tenant+"/html"+path);
		testpaths.add("/tenants/"+tenant+""+path);
		if(withdefaults){
			testpaths.add("/defaults/html"+path);
			testpaths.add("/defaults"+path);
		}
		return testpaths;
	}
	
	protected void setCacheAge(String tenant, String mimetype, String path, HttpServletResponse servlet_response) {
		ConfigRoot root=tenantCSPM.get(tenant).getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		AdminData adminData = spec.getAdminData();
		int cacheAge = 0;	// The default value
		if(MIME_HTML.equals(mimetype)) {
			cacheAge = adminData.getUiStaticHTMLResourcesCacheAge();
		} else if(MIME_CSS.equals(mimetype)) {
			cacheAge = adminData.getUiStaticCSSResourcesCacheAge();
		} else if(MIME_JSON.equals(mimetype)) {
			cacheAge = adminData.getUiStaticJSResourcesCacheAge();
		} else if(MIME_PLAIN.equals(mimetype) || mimetype==null) {
			// try to refine from extension
			if(path.endsWith(SUFFIX_PROPS)) {
				cacheAge = adminData.getUiStaticPropertiesResourcesCacheAge();
			} else if(path.endsWith(SUFFIX_JSON)) {
				cacheAge = adminData.getUiStaticJSResourcesCacheAge();
			}
		} else if(mimetype!=null) {
			if(mimetype.startsWith(MIME_IMAGE)
				|| mimetype.startsWith(MIME_AUDIO)
				|| mimetype.startsWith(MIME_VIDIO)) {
				cacheAge = adminData.getUiStaticMediaResourcesCacheAge();
			}
		}
		if(cacheAge>0) {
			// Create a cache header per the timeout requested (usu. by the individual request handler)
			servlet_response.addHeader("Cache-Control","max-age="+Integer.toString(cacheAge));
		}
	}
	
	/**
	 * Output fixed content to the User
	 * @param sc
	 * @param path
	 * @param tenant
	 * @param servlet_response
	 * @return
	 * @throws IOException
	 */
	protected boolean serveFixedContent(ServletContext sc,String path, String tenant, HttpServletResponse servlet_response) throws IOException{
		List<String> testpaths =possiblepaths(path, tenant, true);
		InputStream is = null;
		String matchedPath = null;
		String mimetype = null;
		while( is == null && testpaths.size()> 0){
			String pathToTry = testpaths.remove(0);
			is=sc.getResourceAsStream(pathToTry);
			if(is!=null) {
				matchedPath = pathToTry;
			}
		}
		if(is==null)
			return false; // Not for us
		mimetype = sc.getMimeType(matchedPath);
		if(mimetype == null) {
			if(matchedPath.endsWith(".appcache")){
				mimetype = "text/cache-manifest";
			} else if(matchedPath.endsWith(".json")){
				mimetype = MIME_JSON;
			} 
		}
		servlet_response.setContentType(mimetype);

		setCacheAge(tenant, mimetype, matchedPath, servlet_response);
		
		ServletOutputStream out = servlet_response.getOutputStream();
		IOUtils.copy(is,out);
		return true;
	}

	/**
	 * loop through all possible paths for a tenant / path combo to find the resource
	 * Has a fall through methodology
	 * @param sc
	 * @param path
	 * @param tenant
	 * @return
	 */
	protected InputStream getFixedContent(ServletContext sc,String path, String tenant){
		List<String> testpaths =possiblepaths(path, tenant, true);
		InputStream is = null;
		while( is == null && testpaths.size()> 0){
			is=sc.getResourceAsStream(testpaths.remove(0));
		}
        return is;
		
	}


	/**
	 * naive function to simply aggregate language files that are simple key/values
	 * and then write the file so it is cached
	 * TODO have a flag to force recreation of the asset when required
	 * @param servlet_request
	 * @param servlet_response
	 * @param sc
	 * @param path
	 * @param tenant
	 * @return
	 * @throws IOException
	 */
	protected boolean serverCreateMergedExternalContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response,ServletContext sc,String path, String tenant) throws IOException{
		//is there a tenant specific file instead of an overlay
		String origfile = path;
		List<String> testpaths_orig =possiblepaths(origfile, tenant, false);
		InputStream is_default = null;
		InputStream is = null;
		String matchedPath = "";
		String mimetype = "";
		while( is == null && testpaths_orig.size()> 0){
			matchedPath = testpaths_orig.remove(0);
			is=sc.getResourceAsStream(matchedPath);
		}
		if(is != null){
			mimetype = sc.getMimeType(matchedPath);
			servlet_response.setContentType(mimetype);
			setCacheAge(tenant, mimetype, matchedPath, servlet_response);
			ServletOutputStream out = servlet_response.getOutputStream();
			IOUtils.copy(is,out);
			return true;
		}
		//no tenant specific - so lets look for an overlay file
		String defaultfile = path;
		String overlayext = "-overlay";
		path = path + overlayext;
		List<String> testpaths =possiblepaths(path, tenant, false);
		List<String> testpathswdefault =possiblepaths(defaultfile, tenant, true);
		String tenantpath = "";
		while( is == null && testpaths.size()> 0){
			tenantpath = testpaths.remove(0);
			is=sc.getResourceAsStream(tenantpath);
		}
		
		{
			String pt = null;
			while( is_default == null && testpathswdefault.size()> 0){
				pt = testpathswdefault.remove(0);
				is_default=sc.getResourceAsStream(pt);
			}
			if(is_default == null)
				return false; //no file to use at all
			mimetype = sc.getMimeType(pt);
		}
		if(is!=null){
			//file to be written
			tenantpath = tenantpath.substring(0, tenantpath.length() - overlayext.length());
			
			Map<String, String> allStrings = new HashMap<String, String>();
			StringWriter writer2 = new StringWriter();
			IOUtils.copy(is_default, writer2, "UTF-8");
			String theString2 = writer2.toString();

			String[] temp = theString2.split("\n");
			for(int i =0; i < temp.length ; i++){
				String[] temp2 = temp[i].split(":");
				if(temp2.length ==2){
					allStrings.put(temp2[0], temp2[1]);
				}
				else if(temp2.length ==1){
					allStrings.put(temp[i], "");
				}
				else{
					allStrings.put(temp[i], "");
				}
			}

			StringWriter writer = new StringWriter();
			IOUtils.copy(is, writer, "UTF-8");
			String theString = writer.toString();
			
	
			String[] temp3 = theString.split("\n");
			for(int i =0; i < temp3.length ; i++){
				String[] temp2 = temp3[i].split(":");
				if(temp2.length ==2){
					allStrings.put(temp2[0], temp2[1]);
				}
				else if(temp2.length ==1){
					allStrings.put(temp3[i], "");
				}
				else{
					allStrings.put(temp3[i], "");
				}
			}
			

			String theStringNew = "";
			for (String key : allStrings.keySet()) {
				theStringNew += key;
				if(!allStrings.get(key).equals("")){
					theStringNew += ":"+allStrings.get(key);
				}
				theStringNew += "\n";
			}
			is_default = new ByteArrayInputStream(theStringNew.getBytes("UTF-8"));
			String test = sc.getRealPath(tenantpath);
			FileWriter fstream = new FileWriter(test);
			BufferedWriter outer = new BufferedWriter(fstream);
			outer.write(theStringNew);
			//Close the output stream
			outer.close();
		}

		ServletOutputStream out = servlet_response.getOutputStream();
		servlet_response.setContentType(mimetype);
		IOUtils.copy(is_default,out);
		setCacheAge(tenant, mimetype, tenantpath, servlet_response);
			
		return true;
	}
	
	/**
	 * Wrapper function for serving fixed content 
	 * where tenant id provided
	 * @param servlet_request
	 * @param servlet_response
	 * @param sc
	 * @param path
	 * @param tenant
	 * @return
	 * @throws IOException
	 */
	protected boolean serverFixedExternalContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response,ServletContext sc,String path, String tenant) throws IOException{
		//InputStream is = getFixedContent( sc, path,  tenant);
        return serveFixedContent(sc, path,  tenant, servlet_response);
	}
	/**
	 * Wrapper function for fixed content where no tenant passed
	 * @param servlet_request
	 * @param servlet_response
	 * @param sc
	 * @param path
	 * @return
	 * @throws IOException
	 */
	protected boolean serverFixedExternalContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response,ServletContext sc,String path) throws IOException{

        InputStream is=sc.getResourceAsStream(path);
        return serveContent(servlet_response,is);
	}
	
	/**
	 * Test is request is a composite request or not
	 * @param req
	 * @return
	 * @throws UIException
	 */
	protected boolean is_composite(WebUIRequest req) throws UIException {
		String[] path=req.getPrincipalPath();
		if(path.length!=1)
			return false;
		return "composite".equals(path[0]);
	}
	
	/**
	 * allow a stack trace to be returned
	 * @param aThrowable
	 * @return
	 */
	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}
	
	private InputStream getPublishedResource(HttpServletRequest servlet_request) {
		InputStream result = null;
				
		String serverRootDir = System.getProperty(SERVER_HOME_PROPERTY);
		String webAppContext = servlet_request.getContextPath();
		String pathInfo = servlet_request.getPathInfo();
		
		try {
			StringBuffer requestedResource = new StringBuffer(serverRootDir);
			requestedResource.append(PUBLISHED_DIR);
			requestedResource.append(pathInfo);
			FileInputStream fis = new FileInputStream(requestedResource.toString());
			result = fis;
		} catch (Exception e) {
			if (log.isDebugEnabled() == true) {
				log.debug(String.format("Could not find published resource for request: %s", servlet_request.getPathInfo()), e);
			}
		}
		
		return result;
	}
	
	/**
	 * attempts to create the outputstream or returns false.
	 * @param servlet_request
	 * @param servlet_response
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	protected boolean perhapsServeFixedContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		boolean result = false;
		
		String pathinfo=servlet_request.getPathInfo();
		if (pathinfo.startsWith("/")) {
			pathinfo=pathinfo.substring(1); // remove the leading '/' character
		}
		// First check to see if the current class loader can find it
		InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(pathinfo);
		// If we didn't find it there, look in the "published" resources area.
		if (is == null) {
			is = getPublishedResource(servlet_request);
		}
		
		if (is != null) {
			// Serve fixed content
			IOUtils.copy(is,servlet_response.getOutputStream());
			result = true;
		}
		
		return result;
	}
}

