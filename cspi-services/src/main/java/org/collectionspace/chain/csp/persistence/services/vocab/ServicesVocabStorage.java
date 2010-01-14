package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.dom4j.DocumentException;

public class ServicesVocabStorage extends GenericVocabStorage {
	private static Map<String,String> vocabs=new HashMap<String,String>();
	
	static {
		vocabs.put("name","Default Name Authority");
	}
	
	public ServicesVocabStorage(ServicesConnection conn) throws InvalidXTmplException, DocumentException {
		super(conn,"urn:cspace:org.collectionspace.demo:vocabulary({vocab}):item({entry})'{display}'",
			  Pattern.compile("(.*?)/urn:cspace:org.collectionspace.demo:vocabulary\\((.*?)\\):item\\((.*?)\\)'(.*?)'"),
			  vocabs,"vocabularies","vocabularies_common","vocabularyitems_common",
			  "vocabularies-common-list/vocabulary-list-item","vocabularyitems-common-list/vocabularyitem_list_item",
			  "vocabularyitems_common/displayName");
	}
}
