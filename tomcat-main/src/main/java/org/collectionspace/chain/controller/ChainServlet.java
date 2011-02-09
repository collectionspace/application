/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
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
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.ui.UI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIUmbrella;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.dom4j.DocumentException;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is the servlet proper for the current interface between the App and UI layers. It is a repository of
 * random junk which needs to be swept away as it becomes parameterised. We use ChainRequest to encapsulate the
 * servlet request and response and present it in a more project-focused way.
 */
public class ChainServlet extends HttpServlet  {	
	private static final Logger log=LoggerFactory.getLogger(ChainServlet.class);
	private static final long serialVersionUID = -4343156244448081917L;
	protected boolean inited=false;
	protected CSPManager cspm=new CSPManagerImpl();
	private String locked_down=null;
	protected UIUmbrella umbrella;

	/* Not in the constructor because errors during construction of servlets tend to get lost in a mess of startup.
	 * Better present it on first request.
	 */

	protected void register_csps() throws IOException, DocumentException {
		cspm.register(new CoreConfig());
		cspm.register(new FileStorage());
		cspm.register(new ServicesStorageGenerator());
		cspm.register(new WebUI());
		cspm.register(new Spec());
	}


	protected void load_config(ServletContext ctx) throws CSPDependencyException {
		try {
			ConfigFinder cfg=new ConfigFinder(ctx);
			InputSource cfg_stream=cfg.resolveEntity(null,"cspace-config.xml");
			if(cfg_stream==null) {
				locked_down="Cannot find cspace config xml file";
			} else {
				cspm.configure(cfg_stream,cfg);
			}
		} catch (UnsupportedEncodingException e) {
			throw new CSPDependencyException("Config has bad character encoding",e);
		} catch (IOException e) {
			throw new CSPDependencyException("Cannot load config",e);			
		} catch (SAXException e) {
			throw new CSPDependencyException("Cannot parse config file",e);			
		}
	}

	protected synchronized void setup() throws BadRequestException {
		if(inited)
			return;
		try {
			// Register csps
			register_csps();
			cspm.go(); // Start up CSPs
			load_config(getServletContext());
		} catch (IOException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (DocumentException e) {
			throw new BadRequestException("Cannot load backend"+e,e);
		} catch (CSPDependencyException e) {
			throw new BadRequestException("Cannot initialise CSPs"+e,e);
		}
		inited=true;
	}

	protected boolean serveContent(HttpServletResponse servlet_response,InputStream is) throws IOException{

		if(is==null)
			return false; // Not for us

		IOUtils.copy(is,servlet_response.getOutputStream());
		return true;
	}
	
	protected boolean serverFixedExternalContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response,ServletContext sc,String path) throws IOException{

        InputStream is=sc.getResourceAsStream(path);
        return serveContent(servlet_response,is);
	}
	
	private boolean perhapsServeFixedContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
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

	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

	private boolean is_composite(WebUIRequest req) throws UIException {
		String[] path=req.getPrincipalPath();
		if(path.length!=1)
			return false;
		return "composite".equals(path[0]);
	}

	/* We do all this very sequentially rather than in one big loop to avoid the fear of weird races and to fail early on parse errors */
	private void serve_composite(UI ui,WebUIRequest req) throws UIException {
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
	 * Responding to a request. The request is assumed to consist of a path to a requested JSON object.
	 * The response returns the object in string form (or an empty string if not found).
	 */
	@Override
	public void service(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {

			String pathinfo = servlet_request.getPathInfo();
			String[] pathbits = pathinfo.substring(1).split("/");
			if (pathbits[0].equals("chain")) {
				servlet_response.sendRedirect(pathinfo);
				return;
			}
			if(locked_down!=null) {
				//this ended up with a status 200 hmmmm not great so changed it to return a 400... hopefully that wont break anythign else

				servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Servlet is locked down in a hard fail because of fatal error: "+locked_down);
				//servlet_response.getWriter().append("Servlet is locked down in a hard fail because of fatal error: "+locked_down);
				return;
			}
			if(!inited)
				setup();
			if(locked_down!=null) {
				//this ended up with a status 200 hmmmm not great so changed it to return a 400... hopefully that wont break anythign else

				servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Servlet is locked down in a hard fail because of fatal error: "+locked_down);
				//servlet_response.getWriter().append("Servlet is locked down in a hard fail because of fatal error: "+locked_down);
				return;
			}
			if(perhapsServeFixedContent(servlet_request,servlet_response))
				return;
			// Setup our request object
			UI web=cspm.getUI("web");
			if(umbrella==null) {
				synchronized(getClass()) {
					if(umbrella==null) {
						umbrella=new WebUIUmbrella((WebUI)web);
					}
				}
			}
			try {
				ConfigRoot root=cspm.getConfigRoot();
				Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
				WebUIRequest req=new WebUIRequest(umbrella,servlet_request,servlet_response,spec.getAdminData().getCookieLife());
				if(is_composite(req)) {
					serve_composite(web,req);
				} else {
					web.serviceRequest(req);
					req.solidify(true);
				}
			} catch (UIException e) {
				throw new BadRequestException("UIException",e);
			}
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, getStackTrace(x));
		}
	}
}
