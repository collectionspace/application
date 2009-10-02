/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Node;

// XXX This will go
// XXX this is a hack to deal with the ID late/early generation dilemma. It will certainly need to be addressed
// more intelligently before there are things other than CollectionObject
/** ServicesIdentifierMap is a class designed to deal with the "when are csid's generated" issue and will go when 
 * that's resolved.
 * 
 */
class ServicesIdentifierMap {
	private ServicesConnection conn;
	private String prefix,entry_xpath;
	private int cache_hits=0,cache_misses=0,cache_loadsteps=0;
	
	private Map<String,String> cache=new HashMap<String,String>();
	private Map<String,String> back_cache=new HashMap<String,String>();
	
	ServicesIdentifierMap(ServicesConnection conn,String prefix,String entry_xpath) {
		this.conn=conn;
		this.prefix=prefix;
		this.entry_xpath=entry_xpath;
	}
	
	// XXX objectNumber in lists is incorrect?
	private String toObjNum(String csid) throws ConnectionException {
		ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,prefix+"/"+csid);
		if(all.getStatus()!=200)
			throw new ConnectionException("Bad request during identifier cache map update: status not 200 for "+prefix+"/"+csid);
		return all.getDocument().selectSingleNode("collection-object/objectNumber").getText();
	}
	
	// XXX horribly inefficient, but no search.
	@SuppressWarnings("unchecked")
	private synchronized void updateCache() throws ConnectionException {
		try {
			cache_misses++;
			ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,prefix+"/");
			if(all.getStatus()!=200)
				throw new ConnectionException("Bad request during identifier cache map update: status not 200 for "+prefix+"/");
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
			
		} catch (ConnectionException e) {
			throw new ConnectionException("Bad request during identifier cache map update",e);
		}
	}
	
	String getCSID(String object_number) throws ConnectionException {
		String csid=cache.get(object_number);
		if(csid==null) {
			updateCache();
		} else {
			cache_hits++;
		}
		return cache.get(object_number);
	}

	void blastCache() {
		cache.clear();
		back_cache.clear();
	}
	
	String fromCSID(String csid) throws ConnectionException {
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
