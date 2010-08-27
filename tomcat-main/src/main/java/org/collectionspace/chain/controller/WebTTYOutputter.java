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
