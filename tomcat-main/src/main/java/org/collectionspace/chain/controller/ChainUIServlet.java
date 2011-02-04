package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.external.UIMapping;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UI;



public class ChainUIServlet extends ChainServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void service(HttpServletRequest servlet_request,
			HttpServletResponse servlet_response) throws ServletException,
			IOException {

		try {
			if (!inited)
				setup();

			String pathinfo = servlet_request.getPathInfo();

			// should we redirect this url or just do the normal stuff

			// Setup our request object
			UI web=cspm.getUI("web");
			WebUI webui = (WebUI)web;
			UIMapping[] allmappings = webui.getAllMappings();
			
			ConfigRoot root = cspm.getConfigRoot();
			Spec spec = (Spec) root.getRoot(Spec.SPEC_ROOT);
			

	        if(pathinfo.equals("/html")){
	        	pathinfo = "/html/index.html";
	        }
			String msg = "";
			String path = pathinfo;
			for(UIMapping map : allmappings){
				if(map.hasUrl()){
					//does pathinfo match url
					if(map.getUrl().equals(pathinfo)){
						msg += "this is the path of doom"+map.getUrl()+":"+map.getFile()+":"+pathinfo;
						path = map.getFile();
					}
				}
				else if(map.hasType()){
					for(Record r : spec.getAllRecords()) {
						//msg += "MAPTYPE: "+map.getType()+":";
						if(r.isType(map.getType())){
							//msg += r.getUIURL()+":";
							if(pathinfo.equals("/html/"+r.getUIURL())){
								msg += "this is the path of doom"+map.getType()+":"+r.getUIURL()+":"+map.getFile()+":"+pathinfo;
								path = map.getFile();
							}
						}
					}
				}
			}
			
			ServletContext sc=null;  
            
	        sc=getServletContext().getContext("/cspace-ui");
	        	
	        	if(serverFixedExternalContent(servlet_request,servlet_response,sc,path)){
	        		return;
	        	}  
	        //InputStream is=sc.getResourceAsStream("/html/record.html");
	        //.getClass().getClassLoader().getResourceAsStream("/html/record.html");

			//IOUtils.copy(is,servlet_response.getOutputStream());


			//servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			//		"Chris has taken over the servlet world waa haaa haaaaa: "
			//				+ msg);
	

		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					getStackTrace(x));
		}
	}
}