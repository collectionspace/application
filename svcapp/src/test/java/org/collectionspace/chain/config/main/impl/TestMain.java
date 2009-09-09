package org.collectionspace.chain.config.main.impl;

import static org.junit.Assert.*;

import org.collectionspace.chain.config.main.impl.MainConfigFactoryImpl;
import org.junit.Test;
import org.xml.sax.InputSource;

public class TestMain {

	private static final String eventlist=
		"0 start aaa/\n"+
		"1 start aaa/bbb/\n"+
		"2 start aaa/bbb/ccc/\n"+
		"3 end aaa/bbb/ccc/\n"+
		"4 start aaa/bbb/ddd/\n"+
		"5 start aaa/bbb/ddd/@xxx/\n"+
		"6 text {yyy} aaa/bbb/ddd/@xxx/\n"+
		"7 end aaa/bbb/ddd/@xxx/\n"+
		"8 text {eee} aaa/bbb/ddd/\n"+
		"9 end aaa/bbb/ddd/\n"+
		"10 end aaa/bbb/\n"+
		"11 end aaa/\n";


	private InputSource getSource(String file) {
		String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+file;
		return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
	}

	@Test public void testMain() throws Exception {
		InputSource src=getSource("test.xml");
		MainConfigFactoryImpl mcf=new MainConfigFactoryImpl(null); // XXX test messages arg
		StringXMLEventConsumer consumer=new StringXMLEventConsumer();
		mcf.setConsumer(consumer);
		mcf.parseConfig(src,null); // XXX test not null
		assertEquals(eventlist,consumer.toString());
	}
}
