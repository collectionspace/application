/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.collectionspace.chain.config.api.ConfigLoadFailedException;
import org.collectionspace.chain.config.bootstrap.BootstrapConfigController;

/** Hides the answers to configuration questions from the source of those answers. The interface to this will
 * change soon, as we can't go on supporting an additional method for each question.
 */
public class Config {
	private BootstrapConfigController controller;
	
	public Config(ServletContext ctx) throws IOException, ConfigLoadFailedException {
		controller=new BootstrapConfigController(ctx);
		controller.go();
	}
	
	public BootstrapConfigController getController() { return controller; }
	
	public String getPathToSchemaDocs() {
		return controller.getOption("schema");
	}
	
	public String getStorageType() {
		return controller.getOption("storage-type");
	}
	
	public String getServicesBaseURL() {
		return controller.getOption("store-url");
	}
	
	public String getLoginDestination() {
		return controller.getOption("login-dest");
	}
	
	public String getMainConfigFileLocation() {
		return controller.getOption("main-config");
	}
}
