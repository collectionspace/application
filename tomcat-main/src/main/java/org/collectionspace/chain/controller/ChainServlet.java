/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapCSP;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
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
import org.dom4j.DocumentException;
import org.xml.sax.InputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is the servlet proper for the current interface between the App and UI layers. It is a repository of
 * random junk which needs to be swept away as it becomes parameterised. We use ChainRequest to encapsulate the
 * servlet request and response and present it in a more project-focused way.
 */
public class ChainServlet extends HttpServlet  {	
	private static final Logger log=LoggerFactory.getLogger(ChainServlet.class);
	private static final long serialVersionUID = -4343156244448081917L;
	private boolean inited=false;
	private CSPManager cspm=new CSPManagerImpl();
	private BootstrapConfigController bootstrap;
	private String locked_down=null;
	private UIUmbrella umbrella;
	
	/* Not in the constructor because errors during construction of servlets tend to get lost in a mess of startup.
	 * Better present it on first request.
	 */

	private void register_csps() throws IOException, DocumentException {
		cspm.register(new CoreConfig());
		cspm.register(new FileStorage());
		cspm.register(new ServicesStorageGenerator());
		cspm.register(new BootstrapCSP(bootstrap));
		cspm.register(new WebUI());
		cspm.register(new Spec());
	}

	private void load_config(ServletContext ctx) throws BootstrapConfigLoadFailedException, CSPDependencyException {
		try {
			InputStream cfg_stream=ConfigFinder.getConfig(ctx);
			if(cfg_stream==null) {
				locked_down="Cannot find cspace config xml file";
			} else {
				cspm.configure(new InputSource(cfg_stream),null); // XXX not null
			}
		} catch (UnsupportedEncodingException e) {
			throw new BootstrapConfigLoadFailedException("Config has bad character encoding",e);
		} catch (IOException e) {
			throw new BootstrapConfigLoadFailedException("Cannot load config",e);			
		}
	}

	private synchronized void setup() throws BadRequestException {
		if(inited)
			return;
		try {
			bootstrap=new BootstrapConfigController(getServletContext());
			bootstrap.go();
			// Register csps
			register_csps();
			cspm.go(); // Start up CSPs
			load_config(getServletContext());
		} catch (IOException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (BootstrapConfigLoadFailedException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (DocumentException e) {
			throw new BadRequestException("Cannot load backend"+e,e);
		} catch (CSPDependencyException e) {
			throw new BadRequestException("Cannot initialise CSPs"+e,e);
		}
		inited=true;
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

	/**
	 * Responding to a request. The request is assumed to consist of a path to a requested JSON object.
	 * The response returns the object in string form (or an empty string if not found).
	 */
	@Override
	public void service(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {
			if(locked_down!=null) {
				servlet_response.getWriter().append("Servlet is locked down in a hard fail because of fatal error: "+locked_down);
				return;
			}
			if(!inited)
				setup();
			if(locked_down!=null) {
				servlet_response.getWriter().append("Servlet is locked down in a hard fail because of fatal error: "+locked_down);
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
				WebUIRequest req=new WebUIRequest(umbrella,servlet_request,servlet_response);
				web.serviceRequest(req);
				req.solidify();
			} catch (UIException e) {
				throw new BadRequestException("UIException",e);
			}
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, getStackTrace(x));
		}
	}
}
