package org.collectionspace.toronto1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.AJAXRequest;
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
import org.json.JSONArray;

public class AJAXServlet extends HttpServlet {
	private PageCreator pc;

	public AJAXServlet() throws DocumentException, FileNotFoundException {
		pc=new PageCreator(this,null);
	}

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException {
		try {
			String name=req.getPathInfo();
			if(name.startsWith("/"))
				name=name.substring(1);
			String[] parts=name.split("/");
			if(parts.length<2)
				throw new ServletException("Request too short");
			String code=req.getParameter("code");
			AJAXRequest request=new AJAXRequest(req,parts[0],parts[1],code,(Map<String,String[]>)req.getParameterMap());
			pc.act(parts[0],code,request);
			res.getWriter().println(request.getResult().toString());
		} catch (IOException e) {
			throw new ServletException("IOException writing response",e);
		}
	}
}
