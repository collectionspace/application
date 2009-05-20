package org.collectionspace;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ChainRequest {
	private final static String SCHEMA_REF = "/schema";
	private final static String STORE_REF = "/store/object";
	
	private HttpServletRequest req;
	private HttpServletResponse res;
	private RequestType type;
	private String rest;
	
	private boolean perhapsStartsWith(String what,RequestType rq,String path) throws BadRequestException {
		if(!path.startsWith(what))
			return false; // Nope, it doesn't
		// Yes it does
		type=rq;
		rest=path.substring(what.length());
		if("".equals(rest))
			throw new BadRequestException("No file path supplied after " + what);
		return true;
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
		if(perhapsStartsWith(SCHEMA_REF,RequestType.SCHEMA,path))
			return;
		if(perhapsStartsWith(STORE_REF,RequestType.STORE,path))
			return;
		throw new BadRequestException("Invalid path");
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
}
