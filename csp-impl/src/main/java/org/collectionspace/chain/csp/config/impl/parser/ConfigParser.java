package org.collectionspace.chain.csp.config.impl.parser;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.impl.main.ParseRun;
import org.collectionspace.chain.csp.config.impl.main.RulesImpl;
import org.collectionspace.chain.csp.config.impl.main.SectionImpl;
import org.collectionspace.chain.csp.config.impl.main.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class ConfigParser {
	private static final Logger log=LoggerFactory.getLogger(ConfigParser.class);
	private ConfigLoadingMessages messages=new ConfigLoadingMessagesImpl();
	private List<byte[]> xslts=new ArrayList<byte[]>();
	private SAXTransformerFactory transfactory;
	private SAXParserFactory factory;
	private RulesImpl rules;
	
	public ConfigParser(RulesImpl rules) throws ConfigException {
		factory = SAXParserFactory.newInstance();
		log.info("Factoryclass",factory.getClass());
		factory.setNamespaceAware(true);
		TransformerFactory tf=TransformerFactory.newInstance();
		if (!tf.getFeature(SAXSource.FEATURE) || !tf.getFeature(SAXResult.FEATURE))
			throw new ConfigException("XSLT transformer doesn't support SAX!");
		transfactory=(SAXTransformerFactory)tf;
		messages=new ConfigLoadingMessagesImpl(); // In the end we probably want to pass this in
		this.rules=rules;
	}
	
	public void parse(InputSource src,String url) throws ConfigException {
		ConfigErrorHandler errors=new ConfigErrorHandler(messages);
		try {
			TransformerHandler[] xform=new TransformerHandler[xslts.size()];
			for(int i=0;i<xform.length;i++) {
				xform[i]=transfactory.newTransformerHandler(new StreamSource(new ByteArrayInputStream(xslts.get(i))));
			}
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			ParseRun handler=new ParseRun();
			ContentHandler content_handler=new MainConfigHandler(handler);
			ContentHandler first=content_handler;
			if(xform.length>0)
				first=xform[0];
			reader.setContentHandler(first);
			reader.setErrorHandler(errors);
			for(int i=1;i<xform.length;i++)
				xform[i-1].setResult(new SAXResult(xform[i]));
			if(xform.length>0)
				xform[xform.length-1].setResult(new SAXResult(content_handler));
			if(url!=null)
				src.setSystemId(url);
			reader.parse(src); // <-- The important line which kicks off the whole parse process
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
