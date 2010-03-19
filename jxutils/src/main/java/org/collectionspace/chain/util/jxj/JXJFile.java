/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.jxj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A JXJ control file from which transformers can be extracted.
 * 
 */
public class JXJFile {
	private static final Logger log=LoggerFactory.getLogger(JXJFile.class);
	private Map<String,JXJTransformer> transformers=new HashMap<String,JXJTransformer>();
	
	public static JXJFile compile(Document in) throws InvalidJXJException {
		return new JXJFile(in);
	}
	
	@SuppressWarnings("unchecked")
	private JXJFile(Document in) throws InvalidJXJException {
		for(Node n : (List<Node>)in.selectNodes("/translations/translation")) {
			String key=((Element)n).attributeValue("type");
			if(key==null)
				throw new InvalidJXJException("Missing type attribute in JXJ file");
			log.info("== "+key+" ==");
			transformers.put(key,new JXJTransformer(key,n));
		}
	}
	
	public JXJTransformer getTransformer(String key) {
		return transformers.get(key);
	}
}
