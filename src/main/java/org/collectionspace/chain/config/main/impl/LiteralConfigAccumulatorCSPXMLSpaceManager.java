package org.collectionspace.chain.config.main.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.config.main.XMLEventConsumer;
import org.collectionspace.chain.config.main.csp.CSPConfigEvaluator;
import org.collectionspace.chain.config.main.csp.CSPConfigProvider;
import org.collectionspace.chain.config.main.csp.CSPRConfigResponse;
import org.collectionspace.chain.config.main.csp.CSPXMLSpaceManager;

public class LiteralConfigAccumulatorCSPXMLSpaceManager extends LeafCSPXMLSpaceManager 
	implements CSPXMLSpaceManager, CSPConfigProvider {

	private static class ConfigValue {
		private String[] key;
		private String value;
	}

	private static class StringCSPConfigEvaluator implements CSPConfigEvaluator {
		private String value;
		
		StringCSPConfigEvaluator(String in) { value=in; }
		public String getValue() { return value; }
	}
	
	private List<ConfigValue> values=new ArrayList<ConfigValue>();
	private Object[] prefix;
	
	public class LiteralConfigAccumulator implements XMLEventConsumer {

		public void start(int ev, XMLEventContext context) {}
		public void end(int ev, XMLEventContext context) {}

		public void text(int ev, XMLEventContext context, String text) {
			if(StringUtils.isWhitespace(text))
				return;
			text=StringUtils.strip(text);
			ConfigValue cv=new ConfigValue();
			String[] stack=context.getStack();
			cv.key=new String[prefix.length+stack.length];
			if(prefix.length>0)
				System.arraycopy(prefix,0,cv.key,0,prefix.length);
			if(stack.length>0)
				System.arraycopy(stack,0,cv.key,prefix.length,stack.length);
			cv.value=text;
			values.add(cv);
		}

		public String getName() {
			return "literal-config-accumulator"; // XXX
		}
	}
	
	public LiteralConfigAccumulatorCSPXMLSpaceManager(Object[] prefix) { 
		super.setConsumer(new LiteralConfigAccumulator());
		this.prefix=prefix;
	}

	public void provide(CSPRConfigResponse response) {
		for(ConfigValue v : values) {
			response.addConfig(v.key,new StringCSPConfigEvaluator(v.value),true);
		}
	}
}
