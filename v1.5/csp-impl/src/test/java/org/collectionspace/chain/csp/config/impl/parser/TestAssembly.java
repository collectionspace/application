package org.collectionspace.chain.csp.config.impl.parser;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;


import javax.xml.transform.stream.StreamResult;


import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@RunWith(Enclosed.class)
public class TestAssembly {
	private static final Logger log=LoggerFactory.getLogger(TestAssembly.class);

	private static final String assembly1="<?xmlversion=\"1.0\"encoding=\"UTF-8\"?><collection-space><core></core><tag2><tag5><tag6>tag6</tag6></tag5><tag7>tag7</tag7><tag16/><tag18/></tag2></collection-space>";

	
	//@Test 
	public void testAssembly() throws Exception {
		StringWriter sbos=new StringWriter();
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/test1.xml";
		InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		AssemblingParser p=new AssemblingParser(new CheckingResolver(),new InputSource(in));		
		p.setRootFile("test-root.xml");
		p.parse(new StreamResult(sbos));
		log.info(sbos.toString().replaceAll("\\s",""));
		assertEquals(assembly1,sbos.toString().replaceAll("\\s",""));
	}
	
}
