package org.collectionspace.chain.controller;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.external.UIMapping;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UI;
import org.collectionspace.csp.api.ui.UIException;

/**
 * TenantUIServlet extends TenantServlet as it just adds the extra functionality specific to rendering the UI pages
 * 
 * @author csm22
 *
 */
public class TenantUIServlet extends TenantServlet {
	private static final Logger log = LoggerFactory.getLogger(TenantUIServlet.class);

	private static final long serialVersionUID = 1L;

	/**
	 * UI specific logic to work out the file needed from the server based on the url
	 * fromthe cspace-ui servlet context 
	 * @param tenant
	 * @param servlet_request
	 * @param servlet_response
	 * @throws BadRequestException
	 * @throws ConnectionException
	 * @throws DocumentException
	 * @throws IOException
	 */
	protected void serviceUIWTenant(String tenant, HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws BadRequestException,ConnectionException,DocumentException, IOException{

		String pathinfo = servlet_request.getPathInfo();
		String[] pathbits = pathinfo.substring(1).split("/");
		String urlredirect = "/collectionspace/ui/"+tenant+"/html/index.html";
		
		//clean up bad links - hopefully these links are never found any more...
		if (pathinfo.equals("/html") || pathinfo.equals("/html/")) {
			servlet_response.sendRedirect(urlredirect);
		}
		else if (pathinfo.equals("/"+tenant) || pathinfo.equals("/"+tenant+"/html") || pathinfo.equals("/"+tenant+"/")) {
			servlet_response.sendRedirect(urlredirect);
		}
		else if (pathinfo.equals("/"+tenant+"/index.html") || pathinfo.equals("/"+tenant+"/html/")) {
			servlet_response.sendRedirect(urlredirect);
		}
		else if (pathinfo.equals("/"+tenant+"/html") || pathinfo.equals("/"+tenant+"/html/")) {
			servlet_response.sendRedirect(urlredirect);
		}

		ServletContext sc = null;
		sc = getServletContext().getContext("/cspace-ui");
		if(sc==null){
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing servlet context cspace-ui");
		}
		//work out what to do with the item based on it's type
		if(pathbits[0].equals("css") || pathbits[0].equals("js") || pathbits[0].equals("lib") || pathbits[0].equals("images") ){
			String tenantposs = getTenantByCookie(servlet_request);
			if (serverFixedExternalContent(servlet_request, servlet_response,
					sc, pathinfo, tenantposs)) {
				return;
			}
		}
		if(pathbits[0].equals("bundle")){
			String tenantposs = getTenantByCookie(servlet_request);
			if(serverCreateMergedExternalContent(servlet_request, servlet_response,
					sc, pathinfo, tenantposs)){
				return;
			}
		}

		if(pathbits[0].equals("config")||pathbits[1].equals("config")){
//			tenant = getTenantByCookie(servlet_request);
		}
		
		if(!tenantInit.containsKey(tenant) || !tenantInit.get(tenant))
			setup(tenant);
		/**
		 * Support composite requests in ui calls as well as app direct calls
		 */
		if(is_composite(pathinfo)) {
			List<String> p=new ArrayList<String>();
			for(String part : servlet_request.getPathInfo().split("/")) {
				if("".equals(part))
					continue;
				p.add(part);
			}		
			p.remove(0);
			ConfigRoot root=tenantCSPM.get(tenant).getConfigRoot();
			Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
			WebUIRequest req;
			
			UI web=tenantCSPM.get(tenant).getUI("web");
			if(!tenantUmbrella.containsKey(tenant)){
				synchronized(getClass()) {
					if(!tenantUmbrella.containsKey(tenant)) {
						tenantUmbrella.put(tenant, new WebUIUmbrella((WebUI)web));
					}
				}
			}
			
			try {
				req = new WebUIRequest(tenantUmbrella.get(tenant),servlet_request,servlet_response,spec.getAdminData().getCookieLife(),p);
				serveComposite(tenant, req, sc);
			} catch (UIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		} else {
			serveSingle(tenant, servlet_request, servlet_response, pathinfo,
					pathbits, sc);
		}
		
	}

	/**
	 * test if item is composite based only on the path info
	 * Could be merged with is_composite in TenantSevlet with minimal work 
	 * so only one call
	 * @param pathinfo
	 * @return
	 */
	private boolean is_composite(String pathinfo){
		String[] path = pathinfo.substring(1).split("/");
		if(path.length!=2)
			return false;
		return "composite".equals(path[1]);
		
	}
	

	/**
	 * Iterate through a composite request sequentially.
	 * We do all this very sequentially rather than in one big loop to avoid the fear of weird races and to fail early on parse errors
	 * @param tenant
	 * @param req
	 * @param sc
	 * @throws UIException
	 * @throws IOException
	 */
	private void serveComposite(String tenant, WebUIRequest req, ServletContext sc) throws UIException, IOException{
		
		try {
			// Extract JSON request payload
			JSONObject in=req.getJSONBody();
			// Build composite object for each subrequest
			Map <String,CompositeWebUIRequestPart> subrequests=new HashMap<String,CompositeWebUIRequestPart>();
			Iterator<?> ki=in.keys();
			while(ki.hasNext()) {
				String key=(String)ki.next();
				JSONObject value=in.getJSONObject(key);
				//need to add tenant into path
				String path = value.getString("path");
				if(path.startsWith("./")){
					path = path.replace("./", "/"+tenant+"/");
				}
				value.put("path", path);
				CompositeWebUIRequestPart sub=new CompositeWebUIRequestPart(req,value);
				subrequests.put(key,sub);
			}
			// Build a place for results
			JSONObject out=new JSONObject();
			// Execute each composite object
			for(String key : subrequests.keySet()) {
				CompositeWebUIRequestPart sub=subrequests.get(key);
				serveSingle(tenant, sub, sc); //NEED TO TEST IF IT WORKS
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
	 * Return a single request from the cspace-ui servlet context
	 * @param tenant
	 * @param sub
	 * @param sc
	 * @throws UIException
	 * @throws IOException
	 */
	private void serveSingle(String tenant, CompositeWebUIRequestPart sub, ServletContext sc) throws UIException, IOException{// Setup our request object
		UI web = tenantCSPM.get(tenant).getUI("web");
		WebUI webui = (WebUI) web;
		UIMapping[] allmappings = webui.getAllMappings();

		ConfigRoot root = tenantCSPM.get(tenant).getConfigRoot();
		Spec spec = (Spec) root.getRoot(Spec.SPEC_ROOT);


		Boolean doMetaConfig = true;// this doesn't seem to work yet
		String[] pathbits = sub.getPrincipalPath();
		String pathinfo = StringUtils.join(pathbits,"/");
		String path = pathinfo;
		Map<String,Object> validmap = doMapping(pathinfo, allmappings, spec);
		if (null != validmap) {
			path = (String)validmap.get("File");
			if (((UIMapping)validmap.get("map")).hasMetaConfig() && doMetaConfig) {
				
				InputStream is = getFixedContent( sc, path,  tenant);
				

				StringWriter writer = new StringWriter();
				IOUtils.copy(is, writer, "UTF-8");
				String theString = writer.toString();
				
				
				/*
				SAXReader reader = new SAXReader();
				reader.setEntityResolver(new NullResolver());
				Document xmlfile = null;

				ByteArrayOutputStream dump = new ByteArrayOutputStream();
				xmlfile = reader.read(new TeeInputStream(is, dump));
			//	log.info(dump.toString("UTF-8"));
			//	log.info(xmlfile.asXML());<tr></tr<.>
			 
			 TODO I want this to work... it just doesn't yet as I haven\'t had time to work on it
			 this is all about replace items in the title etc of the universal files so they become more specific
*/
				for (String metafield : ((UIMapping)validmap.get("map")).getAllMetaConfigs()) {
					
					String rtext = ((UIMapping)validmap.get("map")).getMetaConfig(metafield).parseValue((Record)validmap.get("record"), (Instance)validmap.get("instance"));
					//try as json
					String regex = "\""+metafield+"\": \"[^\"]*\"";
					String replacement = "\""+metafield+"\": \""+rtext+"\"";

					theString = theString.replaceFirst(regex, replacement);
					//try as xml
					String regex2 = "<"+metafield+">[^<]*</"+metafield+">";
					String replacement2 = "<"+metafield+">"+rtext+"</"+metafield+">";

					theString = theString.replaceFirst(regex2, replacement2);
					
				}

				InputStream is2 = new ByteArrayInputStream(theString.getBytes("UTF-8"));
				serveExternalContent(sub, sc, is2, path);
				is.close();
				return;
			}
		}

		if(pathbits[1].equals("bundle")){

			if(serverCreateMergedExternalContent(sub, sc, pathinfo, tenant)){
				return;
			}
		}
		if (serverFixedExternalContent(sub, sc)) {
			return;
		}
	}

	/**
	 * serve content with tweaking of mime types if important
	 * 
	 * @param sub
	 * @param sc
	 * @param is
	 * @param path
	 * @return
	 * @throws UIException
	 * @throws IOException
	 */
	private boolean serveExternalContent(CompositeWebUIRequestPart sub, ServletContext sc, InputStream is, String path) throws UIException, IOException{

		if(is==null)
			return false; // Not for us

		byte[] bytebody;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        IOUtils.copy(is,byteOut);
        new TeeInputStream(is,byteOut);
        bytebody = byteOut.toByteArray();
        
		String mimetype = sc.getMimeType(path);
		if(mimetype == null && path.endsWith(".appcache")){
			mimetype = "text/cache-manifest";
		}
        
        sub.sendUnknown(bytebody, mimetype, null);
		if(bytebody==null)
			return false; // Not for us
		
        
		return true;
	
	}
	/**
	 * when given a tenant work out all the possible paths this resource might have
	 *  and do some fall through to return the most relevant file
	 * @param sub
	 * @param sc
	 * @param path
	 * @param tenant
	 * @return
	 * @throws IOException
	 * @throws UIException
	 */
	protected boolean serverCreateMergedExternalContent(CompositeWebUIRequestPart sub, ServletContext sc, String path, String tenant) throws IOException, UIException{
		//is there a tenant specific file instead of an overlay
		String origfile = path;
		List<String> testpaths_orig =possiblepaths(origfile, tenant, false);
		InputStream is_default = null;
		InputStream is = null;
		String path2 = "";
		String mimetype = "";
		while( is == null && testpaths_orig.size()> 0){
			path2 = testpaths_orig.remove(0);
			is=sc.getResourceAsStream(path2);
		}
		if(is != null){
			serveExternalContent(sub, sc, is, path2);
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
		

		while( is_default == null && testpathswdefault.size()> 0){
			String pt = testpathswdefault.remove(0);
			is_default=sc.getResourceAsStream(pt);
			mimetype = sc.getMimeType(pt);
		}
		if(is_default == null)
			return false; //no file to use at all
		
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

		return serveExternalContent(sub, sc, is_default, path);
		
	}
		
	/**
	 * Wrapper function for serving fixed content from a composite request
	 * where no tenant passed
	 * @param sub
	 * @param sc
	 * @return
	 * @throws UIException
	 * @throws IOException
	 */
	private boolean serverFixedExternalContent(CompositeWebUIRequestPart sub, ServletContext sc) throws UIException, IOException{
		//
		//sub.getPrincipalPath()
		String path = StringUtils.join(sub.getPrincipalPath(),"/");
        InputStream is=sc.getResourceAsStream(path);
        return serveExternalContent( sub,  sc,  is, path);
		
	}
	
	/**
	 * abstraction for returning a single item.
	 * probably could be simplified with other serveSingle function
	 * @param tenant
	 * @param servlet_request
	 * @param servlet_response
	 * @param pathinfo
	 * @param pathbits
	 * @param sc
	 * @throws IOException
	 * @throws BadRequestException
	 * @throws UnsupportedEncodingException
	 */
	private void serveSingle(String tenant, HttpServletRequest servlet_request,
			HttpServletResponse servlet_response, String pathinfo,
			String[] pathbits, ServletContext sc) throws IOException,
			BadRequestException, UnsupportedEncodingException {
		

		// should we redirect this url or just do the normal stuff

		// Setup our request object
		UI web = tenantCSPM.get(tenant).getUI("web");
		WebUI webui = (WebUI) web;
		UIMapping[] allmappings = webui.getAllMappings();

		ConfigRoot root = tenantCSPM.get(tenant).getConfigRoot();
		Spec spec = (Spec) root.getRoot(Spec.SPEC_ROOT);


		Boolean doMetaConfig = true;// this doesn't seem to work yet
		String path = pathinfo;
		Map<String,Object> validmap = doMapping(pathinfo, allmappings, spec);
		if (null != validmap) {
			path = (String)validmap.get("File");
			if (((UIMapping)validmap.get("map")).hasMetaConfig() && doMetaConfig) {
				
				InputStream is = getFixedContent( sc, path,  tenant);
				

				StringWriter writer = new StringWriter();
				IOUtils.copy(is, writer, "UTF-8");
				String theString = writer.toString();
				
				
				/*
				SAXReader reader = new SAXReader();
				reader.setEntityResolver(new NullResolver());
				Document xmlfile = null;

				ByteArrayOutputStream dump = new ByteArrayOutputStream();
				xmlfile = reader.read(new TeeInputStream(is, dump));
			//	log.info(dump.toString("UTF-8"));
			//	log.info(xmlfile.asXML());<tr></tr<.>
			 TODO I want this to work... it just doesn't yet as I haven\'t had time to work on it
			 this is all about replace items in the title etc of the universal files so they become more specific
*/
				for (String metafield : ((UIMapping)validmap.get("map")).getAllMetaConfigs()) {
					
					String rtext = ((UIMapping)validmap.get("map")).getMetaConfig(metafield).parseValue((Record)validmap.get("record"), (Instance)validmap.get("instance"));
					//try as json
					String regex = "\""+metafield+"\": \"[^\"]*\"";
					String replacement = "\""+metafield+"\": \""+rtext+"\"";

					theString = theString.replaceFirst(regex, replacement);
					//try as xml
					String regex2 = "<"+metafield+">[^<]*</"+metafield+">";
					String replacement2 = "<"+metafield+">"+rtext+"</"+metafield+">";

					theString = theString.replaceFirst(regex2, replacement2);
					
				}

				InputStream is2 = new ByteArrayInputStream(theString.getBytes("UTF-8"));
				serveContent(servlet_response, is2);
				is.close();
				return;
			}
		}

		if(pathbits[1].equals("bundle")){
			if(serverCreateMergedExternalContent(servlet_request, servlet_response,
					sc, pathinfo, tenant)){
				return;
			}
		}
		if (serverFixedExternalContent(servlet_request, servlet_response,
				sc, path, tenant)) {
			return;
		}
	}
	
	public void service(HttpServletRequest servlet_request,
			HttpServletResponse servlet_response) throws ServletException,
			IOException {

		try {
			String pathinfo = servlet_request.getPathInfo();
			String[] pathbits = pathinfo.substring(1).split("/");
			String tenant = pathbits[0];
			
			serviceUIWTenant(tenant,servlet_request, servlet_response);
			
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					getStackTrace(x));
		} catch (ConnectionException e) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					getStackTrace(e));
		} catch (DocumentException e) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					getStackTrace(e));
		}
	}

	/**
	 * Simple serialization helper
	 * borrowed from ContectionUtils and tweaked e.g. set newline = true
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	static InputStream serializetoXML(Document doc) throws IOException {
		return new ByteArrayInputStream(serializeToBytes(doc));
	}

	/**
	 * Simple serializer helper
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	static byte[] serializeToBytes(Document doc) throws IOException {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		outformat.setExpandEmptyElements(true);
		outformat.setNewlines(true);
		outformat.setIndent(false);
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(doc);
		writer.flush();
		out.close();
		return out.toByteArray();
	}
	
	/**
	 * Convert document to stream
	 * @param in
	 * @return
	 * @throws ConnectionException
	 */
	public static InputStream documentToStream(Document in) throws ConnectionException {
		if(in!=null) {
			try {
				return serializetoXML(in);
			} catch (IOException e) {
				throw new ConnectionException("Could not connect "+e.getLocalizedMessage(),e);
			}
		}
		return null;
	}

	/**
	 * Some simple mappings to help with the unfinished functionality to 
	 * allow changing of content on templates based on returned item type
	 * @param pathinfo
	 * @param allmappings
	 * @param spec
	 * @return
	 */
	private Map<String,Object> doMapping(String pathinfo, UIMapping[] allmappings,
			Spec spec) {
		

		Map<String,Object> out=new HashMap<String,Object>();
		for (UIMapping map : allmappings) {
			out.put("map", map);
			out.put("isConfig", false);
			out.put("isRecord", false);
			out.put("instance", null);
			out.put("record", null);
			out.put("RecordFile", map.getFile());
			out.put("ConfigFile", "");
			out.put("File", "");

			if (map.hasUrl()) {
				if (map.getUrl().equals(pathinfo)) {
					return out;
				}
			} else if (map.hasType()) {
				for (Record r : spec.getAllRecords()) {
					if (r.isType(map.getType())) {
						if(r.isType("authority")){
							for(Instance ins: r.getAllInstances()){
								//config
								String tenantInstanceconfig = "/"+ spec.getAdminData().getTenantName() +"/config/" + ins.getWebURL()+ ".json";
								String tenantAuthconfig = "/"+ spec.getAdminData().getTenantName() +"/config/" + r.getWebURL()+ ".json";
								if(pathinfo.equals(tenantInstanceconfig)){
									out.put("instance", ins);
									out.put("record", r);
									out.put("isConfig", true);
									out.put("ConfigFile", tenantAuthconfig);
									out.put("File", tenantAuthconfig);
									return out;
								}
								else if(pathinfo.equals("/config/" + ins.getWebURL()+ ".json")){
									out.put("instance", ins);
									out.put("record", r);
									out.put("isConfig", true);
									out.put("ConfigFile", tenantAuthconfig);
									out.put("File", tenantAuthconfig);
									return out;
								}
								
								//record
								if (pathinfo.equals("/"+ spec.getAdminData().getTenantName() +"/" + ins.getUIURL())) {
									out.put("record", r);
									out.put("isRecord", true);
									out.put("instance", ins);
									out.put("File", map.getFile());
									return out;
								}
								if (pathinfo.equals("/"+ spec.getAdminData().getTenantName() +"/html/" + ins.getUIURL())) {
									out.put("record", r);
									out.put("isRecord", true);
									out.put("instance", ins);
									out.put("File", map.getFile());
									return out;
								}
							}
						}
						String test = "/"+spec.getAdminData().getTenantName()+"/html/" + r.getUIURL();
						if (pathinfo.equals(test)) {
							out.put("record", r);
							out.put("isRecord", true);
							out.put("File", map.getFile());
							return out;
						}
					}
				}
			}
		}
		return null;
	}

}
/**
 * Null Resolver
 * @author csm22
 *
 */
class NullResolver implements EntityResolver {
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		return new InputSource(new StringReader(""));
	}
}