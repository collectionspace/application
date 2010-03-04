package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.dom4j.DocumentException;

public class ServicesPersonStorage extends ConfiguredVocabStorage {
	private static Map<String,String> vocabs=new HashMap<String,String>();
	
	static {
		vocabs.put("person","Default Person's Name Authority");
	}
	
	public ServicesPersonStorage(Record r,ServicesConnection conn) throws InvalidXTmplException, DocumentException {
		super(r,conn,
			  vocabs,"http://collectionspace.org/services/person",
			  "personauthorities_common","personauthorities-common-list/personauthority-list-item",
			  "personauthorities_common");
	}
}
