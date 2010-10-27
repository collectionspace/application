/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.dom4j.DocumentException;

public class ServicesVocabStorage extends GenericVocabStorage {
	private static Map<String,String> vocabs=new HashMap<String,String>();
	
	static {
		vocabs.put("xxx","Temporary Vocab whilst none exist");
	}

	public ServicesVocabStorage(ServicesConnection conn) throws InvalidXTmplException, DocumentException {
		super(conn,"urn:cspace:org.collectionspace.demo:vocabulary({vocab}):item({entry})'{display}'",
			  Pattern.compile("(.*?)/urn:cspace:org.collectionspace.demo:vocabulary\\((.*?)\\):item\\((.*?)\\)'(.*?)'"),
			  vocabs,"http://collectionspace.org/services/vocabulary","vocabularies","vocabularies_common","vocabularyitems_common",
			  "vocabularies-common-list/vocabulary-list-item","vocabularyitems-common-list/vocabularyitem_list_item",
			  "vocabularyitems_common/displayName","vocabularies_common","inVocabulary");
	}
	
	//allow for initialization of vocab from ServiceStorageGenerator
	public ServicesVocabStorage(ServicesConnection conn, Map<String,String> vocabs) throws InvalidXTmplException, DocumentException {
		super(conn,"urn:cspace:org.collectionspace.demo:vocabulary({vocab}):item({entry})'{display}'",
			  Pattern.compile("(.*?)/urn:cspace:org.collectionspace.demo:vocabulary\\((.*?)\\):item\\((.*?)\\)'(.*?)'"),
			  vocabs,"http://collectionspace.org/services/vocabulary","vocabularies","vocabularies_common","vocabularyitems_common",
			  "vocabularies-common-list/vocabulary-list-item","vocabularyitems-common-list/vocabularyitem_list_item",
			  "vocabularyitems_common/displayName","vocabularies_common","inVocabulary");
	}
}
