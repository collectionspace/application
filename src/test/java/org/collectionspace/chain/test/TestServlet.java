package org.collectionspace.chain.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class TestServlet extends HttpServlet {
	private static final long serialVersionUID = -2656639341409647602L;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(request.getPathInfo().equals("/reflect")) {
			IOUtils.copy(request.getInputStream(),response.getOutputStream());
		}
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path=request.getPathInfo();
		if(path.startsWith("/"))
			path=path.substring(1);
		path=getClass().getPackage().getName().replaceAll("\\.","/")+"/get-"+path;
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if(stream==null) {
			response.setStatus(404);
			return;
		}
		String data=IOUtils.toString(stream);
		stream.close();
		OutputStream out=response.getOutputStream();
		out.write(data.getBytes("UTF-8"));
	}
}
