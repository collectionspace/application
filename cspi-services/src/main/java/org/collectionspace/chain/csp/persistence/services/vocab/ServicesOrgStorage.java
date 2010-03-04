package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.dom4j.DocumentException;

public class ServicesOrgStorage extends GenericVocabStorage {
	private static Map<String,String> vocabs=new HashMap<String,String>();
	
	static {
		vocabs.put("orgs","Default Organization Name Authority");
	}
	
	public ServicesOrgStorage(ServicesConnection conn) throws InvalidXTmplException, DocumentException {
		super(conn,"urn:cspace.org.collectionspace.demo.orgauthority:name({vocab}):organization:name({entry})'{display}'",
			  Pattern.compile("(.*?)/urn:cspace.org.collectionspace.demo.orgauthority:name\\((.*?)\\):organization:name\\((.*?)\\)'(.*?)'"),
			  vocabs,"http://collectionspace.org/services/organization","orgauthorities",
			  "orgauthorities_common","organizations_common","orgauthorities-common-list/orgauthority-list-item",
			  "organizations-common-list/organization_list_item","organizations_common/displayName","organizations_common","inAuthority");
	}
}
