package org.collectionspace.toronto1.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.collectionspace.toronto1.freemarker.Freemarker;
import org.collectionspace.toronto1.freemarker.FreemarkerException;
import org.json.JSONObject;

public class HTMLPage {
	private class WrapTemplate { String template; Object model; }
	
	private List<String> js=new ArrayList<String>();
	private List<String> css=new ArrayList<String>();
	private Set<String> js_set=new HashSet<String>();
	private Set<String> css_set=new HashSet<String>();
	private StringBuffer head=new StringBuffer();
	private StringBuffer body_script=new StringBuffer();
	private StringBuffer json=new StringBuffer();
	private StringBuffer late_body_script=new StringBuffer();
	private StringWriter body=new StringWriter();
	private List<WrapTemplate> wrap_template=new ArrayList<WrapTemplate>();
	private Map<String,StringWriter> acolytes=new HashMap<String,StringWriter>();
	private HttpServletRequest request;
	private Freemarker freemarker;
	private int seq=1;
	
	public HTMLPage(HttpServletRequest request,Freemarker freemarker) {
		this.request=request;
		this.freemarker=freemarker;
	}
	
	public Writer getBody() { return body; }
	
	public int nextInt() {
		return seq++;
	}
	public void addJSFile(String file) {
		if(js_set.contains(file))
			return;
		js.add(file);
		js_set.add(file);
	}
	public void addCSSFile(String file) {
		if(css_set.contains(file))
			return;
		css.add(file);
		css_set.add(file);
	}

	public static String servletRelativePath(HttpServletRequest r,String in) { // XXX configurable
		if(!in.startsWith("/"))
			in="/"+in;
		return r.getContextPath()+in;
	}

	
	private String servletRelativePath(String in) { // XXX configurable
		return servletRelativePath(request,in);
	}
	
	public Writer getAcolyte(String key) {
		StringWriter w=acolytes.get(key);
		if(w==null) {
			w=new StringWriter();
			acolytes.put(key,w);
		}
		return w;
	}
	
	public void addBodyScript(String in) {
		body_script.append("<script type='text/javascript'>\n");
		body_script.append(in);
		body_script.append("\n</script>");
	}
	
	public void addLateBodyScript(String in) {
		body_script.append("<script type='text/javascript'>\n");
		body_script.append(in);
		body_script.append("\n</script>");
	}
		
	public void addWrapTemplate(String template,Object model) {
		WrapTemplate w=new WrapTemplate();
		w.template=template;
		w.model=model;
		wrap_template.add(w);
	}
	
	// XXX do it more efficiently
	private String applyWrapTemplate(WrapTemplate w,String inner) throws FreemarkerException {
		Map<String,Object> data=new HashMap<String,Object>();
		data.put("model",w.model);
		data.put("inner",inner);
		Map<String,String> sidebar_data=new HashMap<String,String>();
		for(Map.Entry<String,StringWriter> e : acolytes.entrySet()) {
			sidebar_data.put(e.getKey(),e.getValue().toString());
		}
		data.put("acolytes",sidebar_data);
		StringWriter out=new StringWriter();
		freemarker.applyTemplate(w.template,data,out);
		return out.toString();
	}

	private String applyAllWrapTemplates(String data) throws FreemarkerException {
		int len=wrap_template.size();
		for(int i=0;i<len;i++)
			data=applyWrapTemplate(wrap_template.get(len-i-1),data);
		return data;
	}
	
	public void applyTemplate(String template,Object model,Writer writer) throws FreemarkerException {
		freemarker.applyTemplate(template,model,writer);
	}
	
	public void addJSON(String id,JSONObject data) {
		json.append("<div id='"+id+"' style='display: none;'>"+StringEscapeUtils.escapeHtml(data.toString())+"</div>");
	}

	public void addString(String id,String data) {
		json.append("<div id='"+id+"' style='display: none;'>"+StringEscapeUtils.escapeHtml(data)+"</div>");
	}
	
	public void writePage(HttpServletResponse res) throws IOException, FreemarkerException {
		PrintWriter out=res.getWriter();
		out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		out.println("<html><head>");
		for(String css_e : css) {
			out.println("<link rel='stylesheet' type='text/css' href='"+StringEscapeUtils.escapeHtml(servletRelativePath(css_e))+"' />");
		}
		for(String js_e : js) {
			out.println("<script type='text/javascript' src='"+StringEscapeUtils.escapeHtml(servletRelativePath(js_e))+"'></script>");
		}
		out.append(head.toString());
		out.println("</head><body>");
		out.append(json.toString());
		out.append(applyAllWrapTemplates(body.toString()));
		out.append(body_script);
		out.append(late_body_script);
		out.println("</body></html>");
	}
}
