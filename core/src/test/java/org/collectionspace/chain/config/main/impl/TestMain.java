package org.collectionspace.chain.config.main.impl;

import static org.junit.Assert.*;

import org.collectionspace.chain.config.main.ConfigRoot;
import org.collectionspace.chain.config.main.impl.MainConfigFactoryImpl;
import org.collectionspace.chain.csp.config.CoreConfig;
import org.collectionspace.csp.helper.config.LeafBarbWirer;
import org.collectionspace.csp.helper.config.SimpleBarbWirer;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;
import org.collectionspace.csp.impl.core.CSPManagerImpl;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.junit.Test;
import org.xml.sax.InputSource;

public class TestMain {

	private static final String eventlist=
		"0 start aaa\n"+
		"1 start aaa/bbb\n"+
		"2 start aaa/bbb/ccc\n"+
		"3 end aaa/bbb/ccc\n"+
		"4 start aaa/bbb/ddd\n"+
		"5 start aaa/bbb/ddd/@xxx\n"+
		"6 text {yyy} aaa/bbb/ddd/@xxx\n"+
		"7 end aaa/bbb/ddd/@xxx\n"+
		"8 text {eee} aaa/bbb/ddd\n"+
		"9 end aaa/bbb/ddd\n"+
		"10 end aaa/bbb\n"+
		"11 end aaa\n";
	
	private static String eventlist_attached=eventlist;
	
	static {
		eventlist_attached=eventlist_attached.replaceAll(" aaa/"," ");
		eventlist_attached=eventlist_attached.replaceAll(" aaa\n"," \n");
		eventlist_attached=eventlist_attached.replaceAll("\n",":::aaa\n");
	}
	
	private static final String doubleeventlist=
		"4 start :::bbb/ddd:::aaa\n"+
		"5 start @xxx:::bbb/ddd:::aaa\n"+
		"6 text {yyy} @xxx:::bbb/ddd:::aaa\n"+
		"7 end @xxx:::bbb/ddd:::aaa\n"+
		"8 text {eee} :::bbb/ddd:::aaa\n"+
		"9 end :::bbb/ddd:::aaa\n";
		
	private InputSource getSource(String file) {
		String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+file;
		return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
	}

	@Test public void testMain() throws Exception {
		InputSource src=getSource("test.xml");
		CSPManagerImpl csp=new CSPManagerImpl();
		MainConfigFactoryImpl mcf=new MainConfigFactoryImpl(csp); // XXX test messages arg
		StringXMLEventConsumer consumer=new StringXMLEventConsumer();
		mcf.setConsumer(consumer);
		mcf.parseConfig(src,null); // XXX test not null
		assertEquals(eventlist,consumer.toString());
	}
	
	@Test public void testRootAttachment() throws Exception {
		InputSource src=getSource("test.xml");
		CSPManagerImpl csp=new CSPManagerImpl();
		MainConfigFactoryImpl mcf=new MainConfigFactoryImpl(csp); // XXX test messages arg
		StringXMLEventConsumer consumer=new StringXMLEventConsumer();
		mcf.getRootBarbWirer().getBarb("root").attach(new LeafBarbWirer(consumer),"aaa");
		mcf.parseConfig(src,null); // XXX test not null
		assertEquals(eventlist_attached,consumer.toString());	
	}

	@Test public void testDoubleAttachment() throws Exception {
		InputSource src=getSource("test.xml");
		CSPManagerImpl csp=new CSPManagerImpl();
		MainConfigFactoryImpl mcf=new MainConfigFactoryImpl(csp); // XXX test messages arg
		StringXMLEventConsumer consumer=new StringXMLEventConsumer();
		SimpleBarbWirer att1=new SimpleBarbWirer("att1");
		att1.addAttachmentPoint("second",new String[]{"bbb"});
		att1.getBarb("second").attach(new LeafBarbWirer(consumer),"ddd");		
		mcf.getRootBarbWirer().getBarb("root").attach(att1,"aaa");
		mcf.parseConfig(src,null); // XXX test not null
		assertEquals(doubleeventlist,consumer.toString());	
	}
	
	@Test public void testLiteralConfig() throws Exception {
		InputSource src=getSource("test.xml");
		CSPManagerImpl csp=new CSPManagerImpl();
		SimpleConfigProviderBarbWirer ca=new SimpleConfigProviderBarbWirer(new Object[]{"root"});
		csp.addConfigProvider(ca);
		MainConfigFactoryImpl mcf=new MainConfigFactoryImpl(csp); // XXX test messages arg
		SimpleBarbWirer att1=new SimpleBarbWirer("att1");
		att1.addAttachmentPoint("second",new String[]{"bbb"});
		mcf.getRootBarbWirer().getBarb("root").attach(ca,"aaa");
		ConfigRoot cfg=mcf.parseConfig(src,null);
		assertEquals("eee",cfg.getValue(new Object[]{"root","bbb","ddd"}));
		assertEquals("yyy",cfg.getValue(new Object[]{"root","bbb","ddd","@xxx"}));		
	}
	
	@Test public void testCoreConfig() throws Exception { // XXX this is not a test!
		BootstrapConfigController bootstrap=new BootstrapConfigController(null);
		bootstrap.addSearchSuffix("test-config-loader.xml");
		bootstrap.go();
		CSPManagerImpl csp=new CSPManagerImpl();
		csp.register(new CoreConfig());
		csp.register(new BootstrapCSP(bootstrap));
		csp.go();
		InputSource src=getSource("test2.xml");
		MainConfigFactoryImpl mcf=new MainConfigFactoryImpl(csp); // XXX test messages arg
		ConfigRoot cfg=mcf.parseConfig(src,null); // XXX test not null
		System.err.println(cfg.dump());
	}
}
