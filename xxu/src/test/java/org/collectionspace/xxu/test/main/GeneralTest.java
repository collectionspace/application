package org.collectionspace.xxu.test.main;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.*;

public class GeneralTest {
	private static final Parser parser = new Parser();
	
	static {
		parser.addSomeRules(new SomeRules(null),null);
		parser.addSomeRules(new SomeRules("org/collectionspace/xxu/test/validation/rules.xml"),"collection-space/field");
	}
	
	@Test public void testGeneral() throws Exception {
		InputStream data=SomeRules.class.getClassLoader().getResourceAsStream("org/collectionspace/xxu/test/main/test1.xml");
		CSpace cs=parser.parse(data);
		System.err.println(cs.dump());
		assertTrue(cs.getField("test1").validate("P-12345"));
		assertFalse(cs.getField("test1").validate("Q-12345"));
		assertTrue(cs.getField("test2").validate("cheese"));
		assertFalse(cs.getField("test2").validate("zhiufer"));
		String[] sug=cs.getField("test2").suggest("india",10);
		for(String s1 : sug)
			System.err.println(s1);
	}
}
