package org.collectionspace.toronto1.widgets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.collectionspace.toronto1.freemarker.Freemarker;
import org.collectionspace.toronto1.freemarker.FreemarkerException;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.html.ResultPage;
import org.collectionspace.toronto1.widgets.standard.StandardFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONObject;

public class PageCreator {
	private FragmentEconomy fe=new FragmentEconomy();
	private Document control;
	private Map<String,Page> pages=new HashMap<String,Page>();
	private ServletConfig servlet;
	private Freemarker freemarker;
	
	@SuppressWarnings("unchecked")
	public PageCreator(ServletConfig servlet,Freemarker freemarker) throws DocumentException, FileNotFoundException {
		this.servlet=servlet;
		this.freemarker=freemarker;
		new StandardFactory(fe);
		control=getControlFile();
		List<Element> page_nodes=control.selectNodes("pages/page");
		for(Element page : page_nodes) {
			try {
				pages.put(page.attributeValue("id"),new Page(fe,page));
			} catch (FactoryException e) {
				throw new DocumentException("Cannot construct page",e);
			}
		}
	}

	public String[] listPageTypes() { return pages.keySet().toArray(new String[0]); }
	
	private Document getControlFile() throws DocumentException, FileNotFoundException {
		File f=new File(System.getProperty("user.home"),"test1.xml");
		InputStream control_stream=null;
		if(f.exists())
			control_stream=new FileInputStream(f);
		if(control_stream==null)	
		control_stream=Thread.currentThread().getContextClassLoader().getResourceAsStream("test1.xml");
		SAXReader r=new SAXReader();
		return r.read(control_stream);		
	}

	private ResultPage createPageBase(HttpServletRequest request) {
		HTMLPage html=new HTMLPage(request,freemarker);
		ResultPage out=new ResultPage(html);
		html.addJSFile("js/jquery-1.3.2.min.js");
		html.addJSFile("js/jquery-ui-1.7.2.custom.min.js");
		html.addJSFile("js/jquery.json-1.3.js");
		html.addJSFile("js/cookies.js");
		html.addJSFile("js/toronto.js");
		html.addJSFile("js/equalcolumns.js");
		html.addCSSFile("css/hot-sneaks/jquery-ui-1.7.2.custom.css");
		html.addCSSFile("css/frame.css");
		html.addCSSFile("css/toronto.css");
		html.addWrapTemplate("frame.ftl",null);
		html.addLateBodyScript("$(function() { fin(); });");
		return out;
	}
	
	public ResultPage createTemplatedPage(HttpServletRequest request,String template,Object model) throws FreemarkerException {
		ResultPage out=createPageBase(request);
		freemarker.applyTemplate(template,model,out.getHTML().getBody());
		return out;
	}
	
	public ResultPage createPage(HttpServletRequest request,String type,String key,JSONObject data,String mode) throws DocumentException, FactoryException, IOException {
		ResultPage out=createPageBase(request);
		HTMLPage html=out.getHTML();
		html.addString("__type",type);
		if(key!=null)
			html.addString("__key",key);
		Fragment root=pages.get(type).generatePage();
		if(root==null)
			throw new FactoryException("No such page type "+type);
		root.writePage(html,data,mode);
		return out;
	}
	
	public void act(String key,String control,AJAXRequest request) {
		Page page=pages.get(key);
		if(page==null)
			return;
		page.act(control,request);
	}
	
	public String summarize(String key,String part,JSONObject data) {
		try {
			Page page=pages.get(key);
			if(page==null)
				return null;
			return page.summarize(part,data);
		} catch (FactoryException e) {
			return null;
		}
	}
}
