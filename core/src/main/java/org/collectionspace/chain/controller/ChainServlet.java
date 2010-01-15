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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.config.main.impl.BootstrapCSP;
import org.collectionspace.chain.csp.config.CoreConfig;
import org.collectionspace.chain.csp.persistence.file.FileStorage;
import org.collectionspace.chain.csp.persistence.services.ServicesStorage;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.chain.uispec.StubSchemaStore;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.core.RequestCache;
import org.dom4j.DocumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;

/** This is the servlet proper for the current interface between the App and UI layers. It is a repository of
 * random junk which needs to be swept away as it becomes parameterised. We use ChainRequest to encapsulate the
 * servlet request and response and present it in a more project-focused way.
 */
public class ChainServlet extends HttpServlet  {	
	private static final long serialVersionUID = -4343156244448081917L;
	private Config config=null;
	private boolean inited=false;
	private ControllerGlobal global;
	private Map<String,RecordController> controllers=new HashMap<String,RecordController>();
	private static final Set<String> record_controller_types=new HashSet<String>();
	private static final Set<String> other_controller_types=new HashSet<String>();
	private static final Set<String> users=new HashSet<String>();
	
	static {
		record_controller_types.add("collection-object");
		record_controller_types.add("intake");
		record_controller_types.add("acquisition");
		other_controller_types.add("id");

		users.add("guest");
		users.add("curator");
		users.add("admin");
	}

	/* Not in the constructor because errors during construction of servlets tend to get lost in a mess of startup.
	 * Better present it on first request.
	 */

	private RecordController getController(ChainRequest request) throws BadRequestException {
		String record=request.getRecordType();
		if(record==null)
			throw new BadRequestException("No such record type");
		RecordController controller=controllers.get(record);
		if(controller==null)
			throw new BadRequestException("Could not find controller");
		return controller;
	}

	// XXX refactor
	private JSONObject getJSONResource(String in) throws IOException, JSONException {	
		return new JSONObject(getResource(in));
	}

	// XXX refactor
	private String getResource(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		String data=IOUtils.toString(stream);
		stream.close();		
		return data;
	}

	private void register_csps() throws IOException, InvalidJXJException, DocumentException {
		CSPManager cspm=global.getCSPManager();
		cspm.register(new CoreConfig());
		cspm.register(new FileStorage());
		cspm.register(new ServicesStorageGenerator());
		cspm.register(new BootstrapCSP(config.getController()));
	}

	private void load_config() throws BootstrapConfigLoadFailedException, CSPDependencyException {
		try {
			CSPManager cspm=global.getCSPManager();
			InputStream stream=new ByteArrayInputStream(config.getMainConfigFileLocation().getBytes("UTF-8"));
			cspm.configure(new InputSource(stream),null); // XXX not null
		} catch (UnsupportedEncodingException e) {
			throw new BootstrapConfigLoadFailedException("Config has bad character encoding",e);
		}
	}

	private synchronized void setup() throws BadRequestException {
		if(inited)
			return;
		try {
			config=new Config(getServletContext());
			SchemaStore schema=new StubSchemaStore(config.getPathToSchemaDocs());
			global=new ControllerGlobal(schema);
			// Register csps
			register_csps();
			global.getCSPManager().go(); // Start up CSPs
			load_config();
			StorageGenerator store=global.getCSPManager().getStorage(config.getStorageType());
			global.setStore(store);
			// Register controllers
			for(String type : record_controller_types) {
				controllers.put(type,new RecordController(global,type,true));
			}
			for(String type : other_controller_types) {
					controllers.put(type,new RecordController(global,type,false));
			}
		} catch (IOException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (BootstrapConfigLoadFailedException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (InvalidJXJException e) { // XXX better exception handling in ServicesStorage constructor
			throw new BadRequestException("Cannot load backend"+e,e);
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

	private void xxx_reset(ChainRequest request) throws IOException, BadRequestException { // Temporary hack to reset db
		CSPRequestCache cache=new RequestCache();
		Storage storage=global.getStore().getStorage(cache);
		try {
			PrintWriter pw=request.getPlainTextWriter();
			// Delete existing records
			for(String dir : storage.getPaths("/",null)) {
				if("relations".equals(dir) || "vocab".equals(dir) || "person".equals(dir))
					continue;
				pw.println("dir : "+dir);
				String[] paths=storage.getPaths(dir,null);
				for(int i=0;i<paths.length;i++) {
					pw.println("path : "+dir+"/"+paths[i]);
					try {
						storage.deleteJSON(dir+"/"+paths[i]);
					} catch (UnimplementedException e) {
						// Never mind
						pw.println("ux");
					} catch (UnderlyingStorageException e) {
						pw.println("use "+getStackTrace(e));
					}
					pw.println("ok");
					pw.flush();
				}					
			}
			// Create records anew
			String schedule=getResource("reset.txt");
			for(String line : schedule.split("\n")) {
				String[] parts=line.split(" +",2);
				pw.println("Creating "+parts[0]);
				storage.autocreateJSON(parts[0],getJSONResource(parts[1]));
				pw.flush();
			}
			// Delete existing vocab entries
			for(String urn : storage.getPaths("/person/person",null)) {
				pw.println("Deleting "+urn);
				storage.deleteJSON("/person/person/"+urn);
				pw.flush();
			}
			pw.println("Creating");
			pw.flush();
			// Create vocab entries
			String names=getResource("names.txt");
			int i=0;
			for(String line : names.split("\n")) {
				i++;
				JSONObject name=new JSONObject();
				name.put("name",line);
				storage.autocreateJSON("/person/person",name);
				pw.println("Created "+name);
				pw.flush();
			}
			pw.println("done");
		} catch (ExistException e) {
			throw new BadRequestException("Existence problem",e);
		} catch (UnimplementedException e) {
			throw new BadRequestException("Unimplemented ",e);
		} catch (UnderlyingStorageException e) {
			throw new BadRequestException("Problem storing",e);
		} catch (JSONException e) {
			throw new BadRequestException("Invalid JSON",e);
		}
	}

	private void xxx_login(ChainRequest request) { // Temporary hack for Mars
		String username=request.xxxGetUsername();
		String password=request.xxxGetPassword();
		if(username!=null && username.equals(password) && users.contains(username)) {
			// XXX temporary success
			request.redirect(config.getLoginDestination());
			request.setStatus(303);
		} else {
			// XXX temporary failure
			String dest=config.getFailedLoginDestination();
			dest=dest+((dest.contains("?"))?"&":"?")+"result=fail";
			request.redirect(dest);
			request.setStatus(303);
		}
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
	public void doGet(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {
			if(!inited)
				setup();
			if(perhapsServeFixedContent(servlet_request,servlet_response))
				return;
			// Setup our request object
			ChainRequest request;
			request = new ChainRequest(servlet_request,servlet_response);
			switch(request.getType()) {
			case RESET:
				xxx_reset(request);
				break;
			case LOGIN:
				xxx_login(request);
				break;
			case SCHEMA:
			case STORE:
			case LIST:
			case AUTO:
			case SEARCH:
			case AUTOCOMPLETE:
				getController(request).doGet(request,request.getPathTail());
				break;
			}
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, getStackTrace(x));
		}
	}

	@Override
	protected void doPost(HttpServletRequest servlet_request,HttpServletResponse servlet_response)
	throws ServletException, IOException {
		send(servlet_request,servlet_response);
	}

	/**
	 * Stores incoming JSON in a given location in the local storage. The location is derived from the incoming
	 * URL as the part following "/store/object/".
	 */
	@Override
	public void doPut(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		send(servlet_request,servlet_response);
	}

	private void send(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {
			if(!inited)
				setup();
			// Get various bits out of the request
			ChainRequest request=new ChainRequest(servlet_request,servlet_response);
			getController(request).send(request,request.getPathTail());
			// Created!
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());			
		}
	}

	public void doDelete(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {
			if(!inited)
				setup();
			// Get various bits out of the request
			ChainRequest request=new ChainRequest(servlet_request,servlet_response);
			getController(request).doDelete(request,request.getPathTail());
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());			
		}
	}
}
