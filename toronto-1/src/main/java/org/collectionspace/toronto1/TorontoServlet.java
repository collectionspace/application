package org.collectionspace.toronto1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.toronto1.freemarker.Freemarker;
import org.collectionspace.toronto1.freemarker.FreemarkerException;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.html.ResultPage;
import org.collectionspace.toronto1.store.Store;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.collectionspace.toronto1.widgets.FragmentEconomy;
import org.collectionspace.toronto1.widgets.PageCreator;
import org.collectionspace.toronto1.widgets.standard.Block;
import org.collectionspace.toronto1.widgets.standard.Date;
import org.collectionspace.toronto1.widgets.standard.Dropdown;
import org.collectionspace.toronto1.widgets.standard.FreeText;
import org.collectionspace.toronto1.widgets.standard.Sequence;
import org.collectionspace.toronto1.widgets.standard.StandardFactory;
import org.collectionspace.toronto1.widgets.standard.Tabs;
import org.collectionspace.toronto1.widgets.standard.Text;
import org.collectionspace.toronto1.widgets.standard.TwoColumns;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.json.JSONException;
import org.json.JSONObject;

public class TorontoServlet extends HttpServlet {
	private static Store store=new Store(); // XXX eugh!
	private PageCreator pc;
	private Freemarker freemarker=new Freemarker();
	
	public TorontoServlet() throws DocumentException, FileNotFoundException {
		pc=new PageCreator(this,freemarker);
	}
				
	private void applyAcolytes(HttpServletRequest req,String type,ResultPage page,String key,boolean use_accordion,String[] ids)
		throws FreemarkerException, IOException, JSONException {
		// Load all the data XXX eugh!
		Map<String,Object> model=new HashMap<String,Object>();
		if(use_accordion) {
			AccordionSidebar as=new AccordionSidebar(req,store,pc,type);
			if(ids==null) {
				ids=Utils.getAccordion(req,type);
			}
			if(ids!=null) {
				as.setIds(ids);
				as.setClearable();
			}
			as.generateModel(model,type,key);
			model.put("accordion",true);
		}
		model.put("home",HTMLPage.servletRelativePath(req,"/main/home"));
		HTMLPage html=page.getHTML();
		html.applyTemplate("sidebar.ftl",model,html.getAcolyte("sidebar"));
		html.addJSFile("js/sidebar.js");
		html.addBodyScript("$(function() { sidebar_accordion_load(); });");
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,String> makeConditions(HttpServletRequest req,String type) {
		Map<String,String> out=new HashMap<String,String>();
		Map<String,String[]> in=req.getParameterMap();
		for(Map.Entry<String,String[]> vs : in.entrySet()) {
			out.put(vs.getKey(),vs.getValue()[0]);
		}
		out.put("__type",type);
		return out;
	}
	
	private Map<String,Object> listType(HttpServletRequest request,String type) {
		Map<String,Object> out=new HashMap<String,Object>();
		out.put("name",type);
		out.put("createurl",HTMLPage.servletRelativePath(request,"/main/create/"+type));
		out.put("searchurl",HTMLPage.servletRelativePath(request,"/main/search/"+type));
		out.put("allurl",HTMLPage.servletRelativePath(request,"/main/results/"+type));
		return out;
	}
	
	private List<Map<String,Object>> listTypes(HttpServletRequest request) {
		List<Map<String,Object>> out=new ArrayList<Map<String,Object>>();
		for(String type : pc.listPageTypes()) {
			out.add(listType(request,type));
		}
		return out;
	}
	
	public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException {
		try {
			String name=req.getPathInfo();
			if(name.startsWith("/"))
				name=name.substring(1);
			String[] parts=name.split("/");
			ResultPage page=null;
			String key=null;
			boolean use_accordion=true;
			String[] accordion_ids=null;
			String type=null;
			if("create".equals(parts[0])) {
				// Create a page
				// XXX exceptions
				if(parts.length<2)
					throw new ServletException("Request too short");
				page=pc.createPage(req,parts[1],null,null,"create");
				use_accordion=false;
				Utils.removeAccordion(req);
				type=parts[1];
			}
			if("search".equals(parts[0])) {
				if(parts.length<2)
					throw new ServletException("Request too short");
				Utils.removeAccordion(req);
				page=pc.createPage(req,parts[1],null,null,"search");
				use_accordion=false;
				type=parts[1];
			}
			if("results".equals(parts[0])) {
				if(parts.length<2)
					throw new ServletException("Request too short");
				accordion_ids=store.search(makeConditions(req,parts[1]));
				Map<String,Object> model=new HashMap<String,Object>();
				model.put("total",accordion_ids.length);
				page=pc.createTemplatedPage(req,"results.ftl",model);
				page.getHTML().addString("__type",parts[1]);
				Utils.setAccordion(req,parts[1],accordion_ids);
				type=parts[1];
			}
			if("edit".equals(parts[0])) {
				// Edit a page
				if(parts.length<3)
					throw new ServletException("Request too short");
				// XXX exceptions
				key=parts[2];
				JSONObject data=store.getEntry(parts[2]);
				page=pc.createPage(req,parts[1],parts[2],data,"edit");
				type=parts[1];
			}
			if("home".equals(parts[0])) {
				Map<String,Object> model=new HashMap<String,Object>();
				Utils.removeAccordion(req);
				model.put("types",listTypes(req));
				page=pc.createTemplatedPage(req,"home.ftl",model);	
				use_accordion=false;
			}
			applyAcolytes(req,type,page,key,use_accordion,accordion_ids);
			page.getHTML().writePage(res);
		} catch (IOException e) {
			throw new ServletException("IOException writing response",e);
		} catch (DocumentException e) {
			throw new ServletException("DocumentException writing response",e);
		} catch (FactoryException e) {
			throw new ServletException("Bad config",e);			
		} catch (JSONException e) {
			throw new ServletException("JSONException writing response",e);
		} catch (FreemarkerException e) {
			throw new ServletException("FreemarkerException writing response",e);
		}
	}
}
