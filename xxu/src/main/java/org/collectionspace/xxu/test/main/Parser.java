package org.collectionspace.xxu.test.main;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class Parser {
	private Digester digester=new Digester();
	
	public void addSomeRules(SomeRules rules,String base) {
		rules.addRulesToDigester(digester,base);
	}
	
	public CSpace parse(InputStream data) throws InvalidConfigException {
		try {
			return (CSpace)digester.parse(data);
		} catch (IOException x) {
			throw new InvalidConfigException("Could not parse config",x);
		} catch (SAXException x) {
			throw new InvalidConfigException("Could not parse config",x);
		}
	}
}
