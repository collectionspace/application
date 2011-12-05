package org.collectionspace.chain.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.external.UIMapping;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UI;

public class TenantUIServlet extends TenantServlet {
	private static final Logger log = LoggerFactory
			.getLogger(TenantUIServlet.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void serviceUIWTenant(String tenant, HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws BadRequestException,ConnectionException,DocumentException, IOException{

		String pathinfo = servlet_request.getPathInfo();
		String[] pathbits = pathinfo.substring(1).split("/");
		String urlredirect = "/collectionspace/ui/"+tenant+"/html/index.html";
		
		//clean up bad links
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
		
		if("composite".equals(pathbits[0])) {
		//	serve_composite(web,req);
		} else {
			
		}
		

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
	/* borrowed from ContectionUtils and tweaked e.g. set newline = true*/
	static InputStream serializetoXML(Document doc) throws IOException {
		return new ByteArrayInputStream(serializeToBytes(doc));
	}

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

class NullResolver implements EntityResolver {
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		return new InputSource(new StringReader(""));
	}
}