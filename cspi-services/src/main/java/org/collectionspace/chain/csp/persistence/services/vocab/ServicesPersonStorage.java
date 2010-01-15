package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.dom4j.DocumentException;

public class ServicesPersonStorage extends GenericVocabStorage {
	private static Map<String,String> vocabs=new HashMap<String,String>();
	
	static {
		vocabs.put("person","Default Person's Name Authority");
	}
	
	public ServicesPersonStorage(ServicesConnection conn) throws InvalidXTmplException, DocumentException {
		super(conn,"urn:cspace.org.collectionspace.demo.personauthority:name({vocab}):person:name({entry})'{display}'",
			  Pattern.compile("(.*?)/urn:cspace.org.collectionspace.demo.personauthority:name\\((.*?)\\):person:name\\((.*?)\\)'(.*?)'"),
			  vocabs,"http://collectionspace.org/services/person","personauthorities",
			  "personauthorities_common","persons_common","personauthorities-common-list/personauthority-list-item",
			  "persons-common-list/person_list_item","persons_common/displayName","personauthorities_common","inAuthority");
	}
}
