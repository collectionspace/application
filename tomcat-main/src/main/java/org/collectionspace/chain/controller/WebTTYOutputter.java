/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;

public class WebTTYOutputter implements TTYOutputter {
	private PrintWriter pw;
	
	WebTTYOutputter(HttpServletResponse res) throws IOException {
		pw=res.getWriter();
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/plain");
	}
	
	WebTTYOutputter(PrintWriter pw) {
		this.pw=pw;
	}
	
	public void flush() throws UIException { pw.flush(); }
	public void line(String text) throws UIException { pw.println(text); }
	
	public PrintWriter getWriter() { return pw; }
}
