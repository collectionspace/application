package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.dom4j.DocumentException;

public class PersonVocabStorage extends GenericVocabStorage {
	private static Map<String,String> vocabs=new HashMap<String,String>();
	
	static {
		vocabs.put("name","Default Name Authority");
	}
	
	public PersonVocabStorage(ServicesConnection conn) throws InvalidXTmplException, DocumentException {
		super(conn,"urn:cspace.org.collectionspace.demo.personauthority:name({vocab}):person:name({entry})'{display}'",
			  Pattern.compile("(.*?)/urn:cspace.org.collectionspace.demo.personauthority:name\\((.*?)\\):person:name\\((.*?)\\)'(.*?)'"),
			  vocabs, // OK
			  "personauthorities", // OK
			  "personauthorities_common", // OK
			  "persons_common", // OK
			  "personauthorities-common-list/personauthority-list-item", // OK
			  "persons-common-list/person_list_item", // OK
			  "persons_common/displayName"); // OK
	}
}
