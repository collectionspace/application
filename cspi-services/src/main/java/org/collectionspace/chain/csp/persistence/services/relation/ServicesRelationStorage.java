package org.collectionspace.chain.csp.persistence.services.relation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.json.JSONException;
import org.json.JSONObject;

/* /relate/main/      POST ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst} ::: id
 * /relate/main/<id>  PUT ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst} :::
 * /relate/main/<id>  DELETE ::: :::
 * /relate/main/<id>  GET ::: ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst}
 * 
 */

public class ServicesRelationStorage implements ContextualisedStorage {
	private ServicesConnection conn;
	private RelationFactory factory;

	private static Set<String> types=new HashSet<String>();
	
	static {
		types.add("affects");
	}
	
	public ServicesRelationStorage(ServicesConnection conn) throws JaxenException, InvalidXTmplException, DocumentException, IOException {
		this.conn=conn;
		factory=new RelationFactory();
	}

	private String[] splitTypeFromId(String path) throws UnderlyingStorageException {
		String[] out=path.split("/");
		if(out.length!=2)
			throw new UnderlyingStorageException("Path must be two components, not "+path);
		return out;
	}
	
	private Relation dataToRelation(CSPRequestCache cache,String id,JSONObject data) throws JSONException, UnderlyingStorageException {
		String[] src=splitTypeFromId(data.getString("src"));
		String[] dst=splitTypeFromId(data.getString("dst"));
		String type=data.getString("type");
		if(!types.contains(type))
			throw new UnderlyingStorageException("type "+type+" is undefined");
		return factory.create(id,src[0],src[1],type,dst[0],dst[1]);
	}

	private JSONObject relationToData(CSPRequestCache cache,Relation r) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("src",r.getSourceType()+"/"+r.getSourceId());
		out.put("dst",r.getDestinationType()+"/"+r.getDestinationId());
		out.put("type",r.getRelationshipType());
		out.put("csid",r.getID());
		return out;
	}

	// XXX refactor
	private String[] extractPaths(String in,String[] prefixes,int var) throws UnderlyingStorageException {
		if(in==null) 
			throw new UnderlyingStorageException("null is not a path");
		if(in.startsWith("/"))
			in=in.substring(1);
		if(in.endsWith("/"))
			in=in.substring(0,in.length()-1);
		String[] split=in.split("/");
		if(split.length!=prefixes.length+var)
			throw new UnderlyingStorageException("Path is incorrect length (should be "+(prefixes.length+var)+" but is "+split.length);
		for(int i=0;i<prefixes.length;i++)
			if(!prefixes[i].equals(split[i]))
				throw new UnderlyingStorageException("Path component "+i+" must be "+prefixes[i]+" but is "+split[i]);
		if(var==0)
			return new String[0];
		String[] ret=new String[var];
		System.arraycopy(split,prefixes.length,ret,0,var);
		return ret;
	}

	private String[] extractPathsOrNull(String in,String[] prefixes,int var) {
		try {
			return extractPaths(in,prefixes,var);
		} catch (UnderlyingStorageException e) {
			return null;
		}
	}

	public String autocreateJSON(CSPRequestCache cache, String filePath, JSONObject data)
	throws ExistException,UnimplementedException, UnderlyingStorageException {
		try {
			extractPaths(filePath,new String[]{"main"},0);
			Map<String,Document> in=new HashMap<String,Document>();
			in.put("relations_common",dataToRelation(cache,null,data).toDocument());
			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/relations/",in);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not add relation status="+out.getStatus());
			return out.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not add relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve data",e);
		}
	}

	public void createJSON(CSPRequestCache cache, String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot create relations to path");
	}

	public void deleteJSON(CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			int status=conn.getNone(RequestMethod.DELETE,"/relations/"+parts[0],null);
			if(status>299)
				throw new UnderlyingStorageException("Could not delete relation, status="+status);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not delete relation",e);
		}
	}

	private String searchPath(JSONObject in) throws UnderlyingStorageException, JSONException {
		if(in==null)
			return "";
		StringBuffer out=new StringBuffer();
		if(in.has("src")) {
			String[] src=splitTypeFromId(in.getString("src"));
			out.append("/subject/"+src[1]);
		}
		if(in.has("dst")) {
			String[] dst=splitTypeFromId(in.getString("dst"));
			out.append("/object/"+dst[1]);
		}
		if(in.has("type")) {
			out.append("/type/"+in.getString("type"));
		}
		String ret=out.toString();
		if(ret.startsWith("/"))
			ret=ret.substring(1);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public String[] getPaths(CSPRequestCache cache, String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		extractPaths(rootPath,new String[]{"main"},0);
		try {
			List<String> out=new ArrayList<String>();
			ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/relations/"+searchPath(restrictions),null);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
			List<Node> objects=doc.getDocument().selectNodes("relations-common-list/relation-list-item");
			for(Node object : objects) {
				out.add(object.selectSingleNode("csid").getText());
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		}
	}

	public JSONObject retrieveJSON(CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.GET,"/relations/"+parts[0],null);
			if(out.getStatus()==404)
				throw new ExistException("Not found");
			Document doc=out.getDocument("relations_common");
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
			return relationToData(cache,factory.load(parts[0],doc));
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		} catch (JaxenException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		}
	}

	public void updateJSON(CSPRequestCache cache, String filePath,JSONObject data) 
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			Map<String,Document> in=new HashMap<String,Document>();
			in.put("relations_common",dataToRelation(cache,parts[0],data).toDocument());
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,"/relations/"+parts[0],in);
			if(out.getStatus()==404)
				throw new ExistException("Not found");
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not update relation, status="+out.getStatus());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not update relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve data",e);
		}
	}
}
