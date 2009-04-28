package org.collectionspace.xxu.test.main;

import java.io.*;

import org.xml.sax.*;
import org.apache.commons.digester.*;
import org.apache.commons.digester.xmlrules.*;

public class SomeRules {
	private static final String RULES_FILE="org/collectionspace/xxu/test/main/rules.xml";
	private FromXmlRuleSet rules;
	
	public SomeRules(String rules_path) {
		rules=new FromXmlRuleSet(new InputSource(getRules(rules_path)));
	}

	void addRulesToDigester(Digester d,String base) {
		if(base!=null)
			rules.addRuleInstances(d,base);
		else
			rules.addRuleInstances(d);
	}
	
	private InputStream getRules(String rules_path) {
		if(rules_path==null)
			rules_path=RULES_FILE;
		InputStream rules=SomeRules.class.getClassLoader().getResourceAsStream(rules_path);
		if(rules==null) {
			throw new RuntimeException("Cannot load "+RULES_FILE+" from classpath");
		}
		return rules;
	}
}
