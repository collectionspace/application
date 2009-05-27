package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class ChainRequest {
	private final static String SCHEMA_REF = "/objects/schema";
	private final static String STORE_REF = "/objects";
	
	private static final String usage="You must structure the requests like so: \n" +
		"GET "+SCHEMA_REF+"/%path-to-file-with-name% \n" +
		"GET "+STORE_REF+"/%path-to-file-with-name% \n" +
		"POST "+STORE_REF+"/%path-to-file-with-name% - note that data in body must be JSON \n";

	private HttpServletRequest req;
	private HttpServletResponse res;
	private boolean is_get;
	private RequestType type;
	private String rest,body=null;
	private boolean create_not_overwrite=false,found=false;
	
	private void perhapsStartsWith(String what,RequestType rq,String path) throws BadRequestException {
		if(!path.startsWith(what))
			return; // Nope, it doesn't
		// Yes it does
		type=rq;
		rest=path.substring(what.length());
		if("".equals(rest))
			throw new BadRequestException("No file path supplied after " + what);
		// Capture body
		if(!is_get) {
			try {
				body=IOUtils.toString(req.getReader());
			} catch (IOException e) {
				throw new BadRequestException("Cannot capture request body");
			}
		}
		found=true;
	}
	
	public String getStoreURL(String which) {
		return STORE_REF+"/"+which;
	}
	
	/** Wrapper for requests for chain
	 * 
	 * @param req the servlet request
	 * @param res the servlet response
	 * @throws BadRequestException cannot build valid chain request from servlet request
	 */
	public ChainRequest(HttpServletRequest req,HttpServletResponse res) throws BadRequestException {
		this.req=req;
		this.res=res;
		String path = req.getPathInfo();
		// Regular URLs
		if(!found)
			perhapsStartsWith(SCHEMA_REF,RequestType.SCHEMA,path);
		if(!found)
			perhapsStartsWith(STORE_REF,RequestType.STORE,path);
		String method=req.getMethod();
		// Allow method to be overridden by params for testing
		String p_method=req.getParameter("method");
		if(!StringUtils.isBlank(p_method)) {
			method=p_method;
		}
		is_get="GET".equals(method);
		if("POST".equals(method)) {
			create_not_overwrite=true;
		}
		// Mmm. Perhaps it's a non-get request with stuff in parameters.
		if(!"GET".equals(method)) {
			String qp_path=req.getParameter("storage");
			if(qp_path!=null && qp_path.startsWith(STORE_REF)) {
				rest=qp_path.substring(STORE_REF.length());
				type=RequestType.STORE;
				body=req.getParameter("json_str");
				return;
			}
		}
		if(!found)
			throw new BadRequestException("Invalid path "+path);
		if(path==null || "".equals(path))
			throw new BadRequestException(usage);
	}
	
	/** What overall type is the request? ie controller selection.
	 * 
	 * @return the type
	 */
	public RequestType getType() { return type; }
	
	/** What's the trailing path of the request?
	 * 
	 * @return the trailing path, ie after controller selection.
	 */
	public String getPathTail() { return rest; }

	/** What's the request body? Either the real body, or the fake one from the query parameter.
	 * 
	 * @return the body
	 */
	public String getBody() { return body; }
	
	/** Returns a printwriter for some JSON, having set up mime-type, etc, correctly.
	 * 
	 * @return
	 * @throws IOException 
	 */
	public PrintWriter getJSONWriter() throws IOException {
		// Set response type to JSON
		res.setCharacterEncoding("UTF-8");
		res.setContentType("application/json");
		// Return JSON
		
		return res.getWriter();
	}
	
	/** Method/params indicate data should be created at path, not updated
	 * 
	 * @return
	 */
	public boolean isCreateNotOverwrite() {
		return create_not_overwrite;
	}
}
