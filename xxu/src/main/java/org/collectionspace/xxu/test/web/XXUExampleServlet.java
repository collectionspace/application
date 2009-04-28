package org.collectionspace.xxu.test.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.xxu.test.main.CSpace;
import org.collectionspace.xxu.test.main.Field;
import org.collectionspace.xxu.test.main.Parser;
import org.collectionspace.xxu.test.main.SomeRules;

public class XXUExampleServlet extends HttpServlet {
	private static final Parser parser = new Parser();
	private static CSpace config=null;
	
	static {
		parser.addSomeRules(new SomeRules(null),null);
		parser.addSomeRules(new SomeRules("org/collectionspace/xxu/test/validation/rules.xml"),"collection-space/field");
	}

	private static InputStream getData() throws FileNotFoundException {
		File f=new File(System.getProperty("user.home"));
		return new FileInputStream(new File(f,"test-webapp.xml"));
	}
	
	private static void setData(String data) throws IOException {
		File f=new File(System.getProperty("user.home"));
		FileOutputStream out=new FileOutputStream(new File(f,"test-webapp.xml"));
		IOUtils.write(data,out);
		out.close();
	}
	
	
	private static CSpace getConfiguration() throws Exception {
		//if(config==null) {
			config=parser.parse(getData());
		//}
		return config;
	}
	
	protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException {
		try {
			if("/ajax".equals(request.getPathInfo())) {
				ajax(request,response);
				return;
			}
			if("/change".equals(request.getPathInfo())) {
				change_page(request,response);
				return;
			}
			response.setContentType("text/html");
			Map<String,Field> fs=getConfiguration().getFields();
			head(request,response);
			for(Field f : fs.values()) {
				doField(f,request,response);
			}
			tail(request,response);
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}
	
	protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException {
		if("/change".equals(request.getPathInfo())) {
			try {
				setData(request.getParameter("data"));
			} catch (IOException e) {
				throw new ServletException("Cannot set data",e);
			}
			response.addHeader("Location",request.getServletPath()+"/xxu/change");
			response.setStatus(302);
		}
	}
	
	private void ajax(HttpServletRequest request,HttpServletResponse response) throws Exception {
		Writer w=response.getWriter();
		getConfiguration();
		String action=request.getParameter("action");
		String field=request.getParameter("field");
		if("validate".equals(action)) {
			w.append(config.getField(field).validate(request.getParameter("value"))?"true":"false");
		} else if("suggest".equals(action)) {
			for(String s : config.getField(field).suggest(request.getParameter("q"),10)) {
				w.append(s);
				w.append("\n");
			}
		}
	}
		
	private void script(Writer w,String name) throws Exception {
		w.append("<script type='text/javascript' src='/xxu/"+name+"'></script>");
	}

	private void css(Writer w,String name) throws Exception {
		w.append("<link rel='stylesheet' href='/xxu/"+name+"'/>");
	}
	
	private void head(HttpServletRequest request,HttpServletResponse response) throws Exception {
		Writer w=response.getWriter();
		w.append("<html><head>");
		script(w,"jquery.js");
		script(w,"jquery.autocomplete.js");
		script(w,"demo.js");
		css(w,"jquery.autocomplete.css");
		w.append("</head><body><img src='/xxu/cslogo.jpg'/><h1>Field validation/Suggestion quick demo</h1><a href='"+request.getServletPath()+"/xxu/change'>to change page</a>");
	}
	
	private void tail(HttpServletRequest request,HttpServletResponse response) throws Exception {
		Writer w=response.getWriter();		
		w.append("</body></html>");
	}
	
	private void doField(Field f,HttpServletRequest request,HttpServletResponse response) throws Exception {
		Writer w=response.getWriter();
		w.append("<h2>"+f.getID()+"</h2>");
		w.append("<div class='target' id="+f.getID()+">");
		w.append("<input type='text'/>");
		w.append("<span class='good' style='display: none; color: green;'>good</span>");
		w.append("<span class='bad' style='display: none; color: red;'>bad</span>");
		w.append("</div>");
	}
	
	private void change_page(HttpServletRequest request,HttpServletResponse response) throws Exception {
		Writer w=response.getWriter();
		String config=IOUtils.toString(getData());
		w.append("<html><head>");
		w.append("</head><body><a href='"+request.getServletPath()+"/xxu'>to test page</a>");
		w.append("<form method='post' action='"+request.getServletPath()+"/xxu/change'><textarea name='data' cols=\"100\" rows=\"40\">"+StringEscapeUtils.escapeHtml(config)+"</textarea><br/><input type='submit'/></form>");
		w.append("</body></html>");
	}
}
