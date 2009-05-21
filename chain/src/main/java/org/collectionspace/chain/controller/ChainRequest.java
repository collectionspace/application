package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class ChainRequest {
	private final static String SCHEMA_REF = "/schema";
	private final static String STORE_REF = "/store/object";
	
	private static final String usage="You must structure the requests like so: \n" +
		"GET /schema/%path-to-file-with-name% \n" +
		"GET /store/object/%path-to-file-with-name% \n" +
		"POST /store/object/%path-to-file-with-name% - note that data in body must be JSON \n";

	@SuppressWarnings("unused") // I'm guessing we will use it pretty soon
	private HttpServletRequest req;
	private HttpServletResponse res;
	private boolean is_get;
	private RequestType type;
	private String rest,body=null;
	
	private boolean perhapsStartsWith(String what,RequestType rq,String path) throws BadRequestException {
		if(!path.startsWith(what))
			return false; // Nope, it doesn't
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
		return true;
	}
	
	/** Wrapper for requests for chain
	 * 
	 * @param req the servlet request
	 * @param res the servlet response
	 * @throws BadRequestException cannot build valid chain request from servlet request
	 */
	public ChainRequest(HttpServletRequest req,HttpServletResponse res,boolean is_get) throws BadRequestException {
		this.req=req;
		this.res=res;
		this.is_get=is_get;
		String path = req.getPathInfo();
		// Regular URLs
		if(perhapsStartsWith(SCHEMA_REF,RequestType.SCHEMA,path))
			return;
		if(perhapsStartsWith(STORE_REF,RequestType.STORE,path))
			return;
		// Mmm. Perhaps it's a non-get request with stuff in parameters.
		if(!is_get) {
			String qp_path=req.getParameter("storage");
			if(qp_path.startsWith(STORE_REF)) {
				rest=qp_path.substring(STORE_REF.length());
				type=RequestType.STORE;
				body=req.getParameter("json_str");
				return;
			}
		}
		if(path==null || "".equals(path))
			throw new BadRequestException(usage);
		throw new BadRequestException("Invalid path "+path);
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
	
	/** Returns a printwirter for some JSON, having set up mime-type, etc, correctly.
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
}
