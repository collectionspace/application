/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.container;

import java.io.File;

import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/*
 * I wonder what "CSP" stands for? Maybe CollectionSpace (P)ersistence manager?
 */
public interface CSPManager extends CSPContext {
	public void register(CSP in);
	public void go() throws CSPDependencyException;
	public void configure(InputSource in, EntityResolver er, boolean forXsdGeneration) throws CSPDependencyException;
	public void setConfigBase(File file);
	public File getConfigBase();
}
