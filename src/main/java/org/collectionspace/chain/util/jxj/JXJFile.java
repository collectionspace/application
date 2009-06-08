package org.collectionspace.chain.util.jxj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

public class JXJFile {
	private Map<String,JXJTransformer> transformers=new HashMap<String,JXJTransformer>();
	
	public static JXJFile compile(Document in) throws InvalidJXJException {
		return new JXJFile(in);
	}
	
	@SuppressWarnings("unchecked")
	private JXJFile(Document in) throws InvalidJXJException {
		for(Node n : (List<Node>)in.selectNodes("/translations/translation")) {
			String key=((Element)n).attributeValue("type");
			if(key==null)
				throw new InvalidJXJException("Missing type attribute in JXJ file");
			System.err.println("== "+key+" ==");
			transformers.put(key,new JXJTransformer(key,n));
		}
	}
	
	public JXJTransformer getTransformer(String key) {
		return transformers.get(key);
	}
}
