package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.ServicesBaseClass;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.core.RequestCache;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TestVocab extends ServicesBaseClass {
	private static Pattern urn=Pattern.compile("urn:cspace:org.collectionspace.demo:vocabulary\\((.*?)\\):item\\((.*?)\\)'(.*?)'");
	
	// XXX refactor
	private static Storage makeServicesStorage(String path) throws CSPDependencyException {
		return new ServicesStorageGenerator(path).getStorage(new RequestCache());
	}

	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}
	
	@Test public void testVocab() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		// Create
		JSONObject data=new JSONObject();
		data.put("name","TEST");
		String id=ss.autocreateJSON("/vocab/name",data);
		Matcher m=urn.matcher(id);
		assertTrue(m.matches());
		assertEquals("TEST",m.group(3));
		// Read
		JSONObject out=ss.retrieveJSON("/vocab/name/"+id);
		assertEquals("TEST",out.getString("name"));
		// Update
		data.remove("name");
		data.put("name","TEST2");
		ss.updateJSON("/vocab/name/"+id,data);
		out=ss.retrieveJSON("/vocab/name/"+id);
		assertEquals("TEST2",out.getString("name"));
		String id3=out.getString("csid");
		// List
		data.remove("name");
		data.put("name","TEST3");
		String id2=ss.autocreateJSON("/vocab/name",data);
		out=ss.retrieveJSON("/vocab/name/"+id2);
		assertEquals("TEST3",out.getString("name"));		
		boolean found1=false,found2=false;
		for(String u : ss.getPaths("/vocab/name",null)) {
			System.err.println(u);
			if(id3.equals(u))
				found1=true;
			if(id2.equals(u))
				found2=true;
		}
		System.err.println("id2="+id2+" f="+found2);
		System.err.println("id3="+id3+" f="+found1);
		assertTrue(found1);
		assertTrue(found2);
		// Delete
		ss.deleteJSON("/vocab/name/"+id);
		try {
			out=ss.retrieveJSON("/vocab/name/"+id);		
			assertTrue(false);
		} catch(ExistException x) {}
	}
}
