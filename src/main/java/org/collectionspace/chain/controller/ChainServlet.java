/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.config.ConfigLoadFailedException;
import org.collectionspace.chain.schema.SchemaStore;
import org.collectionspace.chain.schema.StubSchemaStore;
import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.Storage;
import org.collectionspace.chain.storage.StorageRegistry;
import org.collectionspace.chain.storage.UnderlyingStorageException;
import org.collectionspace.chain.storage.UnimplementedException;
import org.collectionspace.chain.storage.file.FileStorage;
import org.collectionspace.chain.storage.services.ServicesStorage;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.dom4j.DocumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** This is the servlet proper for the current interface between the App and UI layers. It is a repository of
 * random junk which needs to be swept away as it becomes parameterised. We use ChainRequest to encapsulate the
 * servlet request and response and present it in a more project-focused way.
 */
public class ChainServlet extends HttpServlet 
{	
	private static final long serialVersionUID = -4343156244448081917L;
	private Config config=null;
	private boolean inited=false;
	private ControllerGlobal global;
	
	/* Not in the constructor because errors during construction of servlets tend to get lost in a mess of startup.
	 * Better present it on first request.
	 */

	private Storage getStorage() throws InvalidJXJException, DocumentException, IOException {
		// Complexity here is to allow for refactoring when storages can come from plugins
		StorageRegistry storage_registry=new StorageRegistry();
		storage_registry.addStorage("file",new FileStorage(config.getPathToStore()));
		storage_registry.addStorage("service",new ServicesStorage(config.getServicesBaseURL()));
		return storage_registry.getStorage(config.getStorageType());
	}

	private synchronized void setup() throws BadRequestException {
		if(inited)
			return;
		try {
			config=new Config(getServletContext());
			Storage store=getStorage();
			SchemaStore schema=new StubSchemaStore(config.getPathToSchemaDocs());
			global=new ControllerGlobal(store,schema);
		} catch (IOException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (ConfigLoadFailedException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (InvalidJXJException e) { // XXX better exception handling in ServicesStorage constructor
			throw new BadRequestException("Cannot load backend"+e,e);
		} catch (DocumentException e) {
			throw new BadRequestException("Cannot load backend"+e,e);
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
			String record=request.getRecordType();
			if(record==null)
				throw new BadRequestException("No such record type");
			new RecordController(global,record).doGet(request,request.getPathTail());
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());
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
			String record=request.getRecordType();
			if(record==null)
				throw new BadRequestException("No such record type");
			new RecordController(global,record).send(request,request.getPathTail());
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
			String record=request.getRecordType();
			if(record==null)
				throw new BadRequestException("No such record type");
			new RecordController(global,record).doDelete(request,request.getPathTail());
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());			
		}
	}
}
