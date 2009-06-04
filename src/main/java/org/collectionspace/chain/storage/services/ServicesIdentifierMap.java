package org.collectionspace.chain.storage.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.dom4j.Node;

// XXX this is a hack to deal with the ID late/early generation dilemma. It will certainly need to be addressed
// more intelligently before there are things other than CollectionObject
class ServicesIdentifierMap {
	private ServicesConnection conn;
	
	private Map<String,String> cache=new HashMap<String,String>();
	
	ServicesIdentifierMap(ServicesConnection conn) {
		this.conn=conn;
	}
	
	// XXX objectNumber in lists is incorrect?
	private String toObjNum(String csid) throws BadRequestException {
		ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,"collectionobjects/"+csid);
		if(all.getStatus()!=200)
			throw new BadRequestException("Bad request during identifier cache map update: status not 200");
		return all.getDocument().selectSingleNode("collection-object/objectNumber").getText();
	}
	
	// XXX horribly inefficient, but no search.
	@SuppressWarnings("unchecked")
	private synchronized void updateCache() throws BadRequestException {
		try {
			ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,"collectionobjects/");
			if(all.getStatus()!=200)
				throw new BadRequestException("Bad request during identifier cache map update: status not 200");
			List<Node> objects=all.getDocument().selectNodes("collection-object-list/collection-object-list-item");
			Set<String> present=new HashSet<String>();
			for(Node object : objects) {
				String csid=object.selectSingleNode("csid").getText();
				present.add(csid);
				if(cache.get(csid)==null)
					cache.put(toObjNum(csid),csid);
			}
			
		} catch (BadRequestException e) {
			throw new BadRequestException("Bad request during identifier cache map update",e);
		}
	}
	
	String getCSID(String object_number) throws BadRequestException {
		String csid=cache.get(object_number);
		if(csid==null)
			updateCache();
		return cache.get(object_number);
	}
}
