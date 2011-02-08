/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.parser;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXResult;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.impl.main.ParseRun;
import org.collectionspace.chain.csp.config.impl.main.RulesImpl;
import org.collectionspace.chain.csp.config.impl.main.SectionImpl;
import org.collectionspace.chain.csp.config.impl.main.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class ConfigParser {
	private static final Logger log=LoggerFactory.getLogger(ConfigParser.class);
	private ConfigLoadingMessages messages=new ConfigLoadingMessagesImpl();
	private SAXParserFactory factory;
	private RulesImpl rules;
	
	private static final class Resolver implements EntityResolver {
		@Override public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+systemId;
			InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			return new InputSource(in);
		}			
	}
	
	public ConfigParser(RulesImpl rules) throws ConfigException {
		factory = SAXParserFactory.newInstance();
		log.debug("Factoryclass",factory.getClass());
		factory.setNamespaceAware(true);
		messages=new ConfigLoadingMessagesImpl(); // In the end we probably want to pass this in
		this.rules=rules;
	}
	
	public void parse(InputSource src,String url) throws ConfigException {
		ConfigErrorHandler errors=new ConfigErrorHandler(messages);
		try {
			ParseRun handler=new ParseRun();
			ContentHandler content_handler=new MainConfigHandler(handler);
			if(url!=null)
				src.setSystemId(url);
			AssemblingParser p=new AssemblingParser(new Resolver(),src);
			p.parse(new SAXResult(content_handler));
			TreeNode tree=handler.getTree();
			TreeNode tree_root=TreeNode.create_tag("ROOT");
			tree_root.addChild(tree);
			tree_root.claim(rules,"ROOT",null,null);
			SectionImpl ms_root=new SectionImpl(null,"ROOT",null);
			tree_root.run_all(ms_root);
			tree_root.dump();
			ms_root.buildTargets(null);
			ms_root.dump();
		} catch(Throwable t) {
			errors.any_error(t);
			errors.fail_if_necessary();
		}
		errors.fail_if_necessary();
	}
}
