package org.collectionspace.toronto1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.collectionspace.toronto1.freemarker.Freemarker;
import org.collectionspace.toronto1.freemarker.FreemarkerException;
import org.collectionspace.toronto1.store.Store;
import org.collectionspace.toronto1.widgets.PageCreator;
import org.dom4j.DocumentException;
import org.json.JSONException;

public class DataServlet extends HttpServlet {
	private static Store store=new Store(); // XXX eugh!
	private PageCreator pc;
	private Freemarker freemarker=new Freemarker();
	
	public DataServlet() throws DocumentException, FileNotFoundException {
		pc=new PageCreator(this,freemarker);
	}
	
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException {
		try {
			String name=req.getPathInfo();
			if(name.startsWith("/"))
				name=name.substring(1);
			String[] parts=name.split("/");
			if("accordion".equals(parts[0])) {
				if(parts.length<3)
					throw new ServletException("path too shoet");
				AccordionSidebar as=new AccordionSidebar(req,store,pc,parts[1]);
				as.setPage(Integer.parseInt(parts[2]));
				String[] ids=Utils.getAccordion(req,parts[1]);
				if(ids!=null) {
					as.setIds(ids);
					as.setClearable();
				}
				Map<String,Object> model=new HashMap<String,Object>();
				as.generateModel(model,parts[1],null);
				freemarker.applyTemplate("accordion.ftl",model,res.getWriter());
			}
			if("clear-accordion".equals(parts[0])) {
				Utils.removeAccordion(req);
				res.getWriter().append("ok");
			}
		} catch (IOException e) {
			throw new ServletException("IOException",e);
		} catch (JSONException e) {
			throw new ServletException("JSONException",e);
		} catch (FreemarkerException e) {
			throw new ServletException("FreemarkerException",e);
		}
	}
}
