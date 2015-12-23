/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
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
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Relationship;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.services.common.api.RefName;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* /relate/main/      POST ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst} ::: id
 * /relate/main/<id>  PUT ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst} :::
 * /relate/main/<id>  DELETE ::: :::
 * /relate/main/<id>  GET ::: ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst}
 * 
 */

// XXX some hacks here because services don't seem to support multiple search criteria on relationships. 
// XXX need to implement CSPACE-1080

public class ServicesRelationStorage implements ContextualisedStorage { 
	private static final Logger log=LoggerFactory.getLogger(ServicesRelationStorage.class);
	private ServicesConnection conn;
	private Spec spec;
	private RelationFactory factory;
	private Map<String,String> type_to_surl=new HashMap<String,String>();
	private Map<String,String> surl_to_type=new HashMap<String,String>();

	private static Map<String,Relationship> types=new HashMap<String,Relationship>();
	
	static {
		//needs to be set thr CSPACE-2557
	//	types.add("affects");
	//	types.add("new"); // XXX Only one type is bad for testing. remove when there's a second real one
	}
	
	public ServicesRelationStorage(ServicesConnection conn,Spec spec) throws JaxenException, InvalidXTmplException, DocumentException, IOException {
		this.conn=conn;
		this.spec = spec;
		for(Relationship rel : spec.getAllRelations()){
			types.put(rel.getID(),rel);
		}

		for(Record r : spec.getAllRecords()) {
			type_to_surl.put(r.getID(),r.getServicesURL());
			surl_to_type.put(r.getServicesURL(), r.getID());
		}
		factory=new RelationFactory();
	}

	private String[] splitTypeFromId(String path) throws UnderlyingStorageException {
		String[] out=path.split("/");
		if(out[0].equals("")){
			path = path.substring(1);
			out=path.split("/");
		}
		if(out.length!=2)
			throw new UnderlyingStorageException("Path must be two components, not "+path);
		return out;
	}
	
	private Relation dataToRelation(CSPRequestCache cache,String id,JSONObject data) throws JSONException, UnderlyingStorageException {
		String[] src=splitTypeFromId(data.getString("src"));
		String[] dst=splitTypeFromId(data.getString("dst"));
		if(type_to_surl.containsKey(src[0])){
			src[0] = type_to_surl.get(src[0]);
		}
		if(type_to_surl.containsKey(dst[0])){
			dst[0] = type_to_surl.get(dst[0]);
		}
		String type=data.getString("type");
		if(types.containsKey(type)){
			Relationship rel = types.get(type);
			if(!rel.hasDestinationType(dst[0]) && !rel.hasDestinationType("all") ){
				throw new UnderlyingStorageException("type "+type+" is undefined for destination:"+dst[0]);
			}
			if(!rel.hasSourceType("all") && !rel.hasSourceType(src[0]) ){
				throw new UnderlyingStorageException("type "+type+" is undefined for source: "+src[0]);
			}
		}
		else{
			throw new UnderlyingStorageException("type "+type+" is undefined");
		}
		return factory.create(id,src[0],src[1],type,dst[0],dst[1]);
	}

	private JSONObject relationToData(CSPRequestCache cache,Relation r) throws JSONException {
		JSONObject out=new JSONObject();
		String srct = r.getSourceType();
		String dstt = r.getDestinationType();
		if(surl_to_type.containsKey(r.getSourceType())){
			srct = surl_to_type.get(r.getSourceType());
		}
		if(surl_to_type.containsKey(r.getDestinationType())){
			dstt = surl_to_type.get(r.getDestinationType());
		}
		out.put("src",srct+"/"+r.getSourceId());
		out.put("dst",dstt+"/"+r.getDestinationId());
		out.put("type",r.getRelationshipType());
		out.put("csid",r.getID());
		return out;
	}

	private Boolean isPathType(String in,String[] prefixes,int var) throws UnderlyingStorageException {
		if(in==null) 
			return false;
		if(in.startsWith("/"))
			in=in.substring(1);
		if(in.endsWith("/"))
			in=in.substring(0,in.length()-1);
		String[] split=in.split("/");
		if(split.length!=prefixes.length+var)
			return false;
		for(int i=0;i<prefixes.length;i++){
			if(!prefixes[i].equals(split[i])){
				return false;
			}
		}
		return true;
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

	@Override
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject data, JSONObject restrictions)
	throws ExistException,UnimplementedException, UnderlyingStorageException {
		try {
			extractPaths(filePath,new String[]{"main"},0);
			Map<String,Document> in=new HashMap<String,Document>();
			Document datapath = dataToRelation(cache,null,data).toDocument();
			//log.info("AUTOCREATE"+datapath.asXML());
			in.put("relations_common",datapath);
			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/relations/",in,creds,cache);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not add relation status="+out.getStatus(),
						out.getStatus(), "/relations/");
			return out.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not add relation"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve data"+e.getLocalizedMessage());
		}
	}

	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot create relations to path");
	}

	public void transitionWorkflowJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, 
			String filePath, String workflowTransition) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Workflow not supported on relations");
	}
	
	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			//log.info("DELETE"+parts[0]);
			int status=conn.getNone(RequestMethod.DELETE,"/relations/"+parts[0],null,creds,cache);
			if(status>299)
				throw new UnderlyingStorageException("Could not delete relation, status="+status,status,"/relations/"+parts[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not delete relation"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
	}

	private String searchPath(JSONObject in) throws UnderlyingStorageException, JSONException {
		if(in==null)
			return "";
		StringBuffer out=new StringBuffer();
		if(in.has("src")) {
			String[] src=splitTypeFromId(in.getString("src"));
			out.append("&sbj="+src[1]);
		}
		if(in.has("srcType")) {
			String srctype=in.getString("srcType");
			out.append("&srcType="+srctype);
		}
		if(in.has("dst")) {
			String[] dst=splitTypeFromId(in.getString("dst"));
			out.append("&obj="+dst[1]);
		}
		if(in.has("dstType")) {
			String dsttype=in.getString("dstType");
			out.append("&objType="+dsttype);
		}
		if(in.has("type")) {
			out.append("&prd="+in.getString("type"));
		}
		String ret=out.toString();
		if(ret.startsWith("&"))
			ret=ret.substring(1);
		return ret;
	}
	
	// Needed because of CSPACE-1080
	//XXX is this still needed?CSPACE-1080 has been resolved...
	private boolean post_filter(CSPRequestCredentials creds,CSPRequestCache cache,JSONObject restrictions,Node candidate) throws ExistException, UnderlyingStorageException, ConnectionException, JSONException {
		if(restrictions==null)
			return true;
		// Subject
		String src_csid=candidate.selectSingleNode("subjectCsid").getText();
		String rest_src=restrictions.optString("src");
		if(rest_src!=null && !"".equals(rest_src)) {
			String[] data = rest_src.split("/");
			if(data[0].equals("")){
				rest_src = rest_src.substring(1);
				data = rest_src.split("/");
			}
			if(!src_csid.equals(rest_src.split("/")[1]))
				return false;
		}
		String dst_csid=candidate.selectSingleNode("objectCsid").getText();		
		String rest_dst=restrictions.optString("dst");
		if(rest_dst!=null && !"".equals(rest_dst)) {
			String[] data2 = rest_dst.split("/");
			if(data2[0].equals("")){
				rest_dst = rest_dst.substring(1);
				data2 = rest_dst.split("/");
			}
			if(!dst_csid.equals(rest_dst.split("/")[1]))
				return false;
		}
		// Retrieve the relation (CSPACE-1081)
		ReturnedMultipartDocument rel=conn.getMultipartXMLDocument(RequestMethod.GET,candidate.selectSingleNode("uri").getText(),null,creds,cache);
		if(rel.getStatus()==404)
			throw new ExistException("Not found");
		Document rel_doc=rel.getDocument("relations_common");
		if(rel_doc==null)
			throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
		String type=rel_doc.selectSingleNode("relations_common/relationshipType").getText();
		if(restrictions.has("type") && !type.equals(restrictions.optString("type")))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out = new JSONObject();
		Boolean isHierarchical = false;
		if(isPathType(rootPath,new String[]{"main"},0)){
			extractPaths(rootPath,new String[]{"main"},0);
		}
		else if (isPathType(rootPath,new String[]{"hierarchical"},0)){
			extractPaths(rootPath,new String[]{"hierarchical"},0);
			isHierarchical = true;
		}
		
		try {
			JSONObject moredata = new JSONObject();
			List<String> list=new ArrayList<String>();
			ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/relations?"+searchPath(restrictions),null,creds,cache);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
			JSONObject pagination = new JSONObject();
			String xmlroot = "relations-common-list";
			List<Node> nodes=doc.getDocument().selectNodes("/"+xmlroot+"/*");
			for(Node node : nodes){
				if("relation-list-item".equals(node.getName())){
					//if(post_filter(creds,cache,restrictions,node))
						list.add(node.selectSingleNode("csid").getText());
						if(isHierarchical){
							JSONObject hdata = new JSONObject();
							Node subjectNode = node.selectSingleNode("subject");
							Node objectNode = node.selectSingleNode("object");
							hdata.put("subjecturi", subjectNode.selectSingleNode("uri").getText());
							hdata.put("objecturi", objectNode.selectSingleNode("uri").getText());
							hdata.put("subjectcsid", subjectNode.selectSingleNode("csid").getText());
							hdata.put("objectcsid", objectNode.selectSingleNode("csid").getText());
							
							findNameUnderNode(hdata, "subjectname", "subjectrefname",subjectNode);
							findNameUnderNode(hdata, "objectname", "objectrefname",objectNode);
							
							hdata.put("type", node.selectSingleNode("predicate").getText());
							hdata.put("csid", node.selectSingleNode("csid").getText());
							moredata.put(node.selectSingleNode("csid").getText(), hdata);
						}
				}else{
					pagination.put(node.getName(), node.getText());
				}
			}
			
			out.put("pagination", pagination);
			out.put("listItems",list.toArray(new String[0]));
			out.put("moredata",moredata);
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation"+e.getLocalizedMessage(),e.getStatus(),e.getUrl());
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		}
	}
	
	private void findNameUnderNode(JSONObject out, String nameKey, String refNameKey,
						Node itemNode) throws JSONException {
		// Look for something to put into the subjectname. Start with refName,
		// then name, then number
		Node itemRefName = itemNode.selectSingleNode("refName");
		String nameValue = null;
		if(itemRefName!=null) {
			String refNameValue = itemRefName.getText();
                        out.put(refNameKey, refNameValue);
                        RefName.AuthorityItem item = RefName.AuthorityItem.parse(refNameValue);
                        if(item !=null) {
                                nameValue = item.displayName;
                        }else {
                                RefName.Authority authority = RefName.Authority.parse(refNameValue);
                                if(authority !=null) {
                                        nameValue = authority.displayName;
                                }  
                        }
		}
		// If no displayName from refName, then try name element
		if(nameValue==null) {
			Node itemNameNode = itemNode.selectSingleNode("name");
			if(itemNameNode!=null) {
				nameValue = itemNameNode.getText();
			}
		}
		// Still nothing? try number element
		if(nameValue==null) {
			Node itemNumberNode = itemNode.selectSingleNode("number");
			if(itemNumberNode!=null) {
				nameValue = itemNumberNode.getText();
			}
		}
		if(nameValue==null) {
			nameValue = "MISSING DATA";
		} 
		out.put(nameKey, nameValue);
	}

	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		extractPaths(rootPath,new String[]{"main"},0);
		try {
			List<String> out=new ArrayList<String>();
			ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/relations/"+searchPath(restrictions),null,creds,cache);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
			List<Node> objects=doc.getDocument().selectNodes("relations-common-list/relation-list-item");
			for(Node object : objects) {
				if(post_filter(creds,cache,restrictions,object))
					out.add(object.selectSingleNode("csid").getText());
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation"+e.getLocalizedMessage());
		}
	}

	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Boolean isHierarchical = false;
			String[] parts = null;
			if(isPathType(filePath,new String[]{"main"},1)){
				parts=extractPaths(filePath,new String[]{"main"},1);
			}
			else if (isPathType(filePath,new String[]{"hierarchical"},1)){
				parts=extractPaths(filePath,new String[]{"hierarchical"},1);
				isHierarchical = true;
			}
			
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.GET,"/relations/"+parts[0],null,creds,cache);
			if(out.getStatus()==404)
				throw new UnderlyingStorageException("Could not retrieve relation",out.getStatus(),"/relations/"+parts[0]);
			Document doc=out.getDocument("relations_common");
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common",out.getStatus(),"/relations/"+parts[0]);
			return relationToData(cache,factory.load(parts[0],doc));
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JaxenException e) {
			throw new UnderlyingStorageException("Could not retrieve relation"+e.getLocalizedMessage(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation"+e.getLocalizedMessage(),e);
		}
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath,JSONObject data, JSONObject restrictions) 
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			Map<String,Document> in=new HashMap<String,Document>();
			Document datapath = dataToRelation(cache,parts[0],data).toDocument();
			in.put("relations_common",datapath);
			//log.info("UPDATE"+datapath.asXML());
			//log.info("UPDATE"+"/relations/"+parts[0]);
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,"/relations/"+parts[0],in,creds,cache);
			if(out.getStatus()==404)
				throw new ExistException("Not found");
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not update relation",out.getStatus(),"/relations/"+parts[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not update relation"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve data"+e.getLocalizedMessage(),e);
		}
	}
}
