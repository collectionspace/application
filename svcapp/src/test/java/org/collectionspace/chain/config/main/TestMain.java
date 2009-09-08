package org.collectionspace.chain.config.main;

import org.collectionspace.chain.config.main.impl.MainConfigFactoryImpl;
import org.junit.Test;
import org.xml.sax.InputSource;

public class TestMain {
	private InputSource getSource(String file) {
		String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+file;
		return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
	}
	
	@Test public void testMain() throws Exception {
		InputSource src=getSource("test.xml");
		MainConfigFactory mcf=new MainConfigFactoryImpl(null); // XXX test messages arg
		mcf.parseConfig(src,null); // XXX test not null
	}
}
