package org.collectionspace.chain.util.xtmpl;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestXTmpl {
	private static final Logger log=LoggerFactory.getLogger(TestXTmpl.class);
	
	private Document getDocument(String in) throws DocumentException, IOException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		assertNotNull(stream);
		SAXReader reader=new SAXReader();
		Document doc=reader.read(stream);
		stream.close();
		return doc;
	}
	
	@SuppressWarnings("unchecked")
	@Test public void testBasic() throws Exception {
		Document doc=getDocument("tmpl1.xml");
		XTmplTmpl template=XTmplTmpl.compile(doc);
		XTmplDocument document=template.makeDocument();
		document.setText("c","hello-c");
		document.setContents("d",getDocument("tmpl2.xml").getRootElement());
		document.setTexts("x",new String[]{"y","z"});
		log.info(document.getDocument().asXML());
		assertEquals("hi",document.getDocument().selectSingleNode("/a/b/d/e/f").getText());
		assertEquals("hello-c",document.getDocument().selectSingleNode("/a/b/c").getText());
		List<Node> x=document.getDocument().selectNodes("/a/b/w/x");
		assertEquals(2,x.size());
		assertEquals("y",x.get(0).getText());
		assertEquals("z",x.get(1).getText());
	}
}
