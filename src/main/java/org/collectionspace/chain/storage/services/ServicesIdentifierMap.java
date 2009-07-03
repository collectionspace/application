package org.collectionspace.chain.storage.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.dom4j.Node;

// XXX This will go
// XXX this is a hack to deal with the ID late/early generation dilemma. It will certainly need to be addressed
// more intelligently before there are things other than CollectionObject
class ServicesIdentifierMap {
	private ServicesConnection conn;
	private int cache_hits=0,cache_misses=0,cache_loadsteps=0;
	
	private Map<String,String> cache=new HashMap<String,String>();
	private Map<String,String> back_cache=new HashMap<String,String>();
	
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
			cache_misses++;
			ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,"collectionobjects/");
			if(all.getStatus()!=200)
				throw new BadRequestException("Bad request during identifier cache map update: status not 200");
			List<Node> objects=all.getDocument().selectNodes("collection-object-list/collection-object-list-item");
			Set<String> present=new HashSet<String>();
			for(Node object : objects) {
				String csid=object.selectSingleNode("csid").getText();
				present.add(csid);
				if(cache.get(csid)==null) {
					cache_loadsteps++;
					String objnum=toObjNum(csid);
					cache.put(objnum,csid);
					back_cache.put(csid,objnum);
				}
			}
			
		} catch (BadRequestException e) {
			throw new BadRequestException("Bad request during identifier cache map update",e);
		}
	}
	
	String getCSID(String object_number) throws BadRequestException {
		String csid=cache.get(object_number);
		if(csid==null) {
			updateCache();
		} else {
			cache_hits++;
		}
		return cache.get(object_number);
	}

	String fromCSID(String csid) throws BadRequestException {
		String object_number=back_cache.get(csid);
		if(object_number==null) {
			updateCache();
		} else {
			cache_hits++;
		}
		return back_cache.get(csid);
	}
	
	int getNumberHits() { return cache_hits; }
	int getNumberMisses() { return cache_misses; }
	int getLoadSteps() { return cache_loadsteps; }
}
