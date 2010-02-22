package org.collectionspace.csp.helper.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigListener;
import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.config.Evaluator;
import org.collectionspace.csp.api.config.EventContext;
import org.collectionspace.csp.api.config.EventConsumer;

public class SimpleConfigProviderBarbWirer extends LeafBarbWirer 
	implements BarbWirer, ConfigProvider {

	private static class ConfigValue {
		private String[] key;
		private String value;
	}

	private static class StringCSPConfigEvaluator implements Evaluator {
		private String value;
		
		StringCSPConfigEvaluator(String in) { value=in; }
		public String getValue() { return value; }
	}
	
	private List<ConfigValue> values=new ArrayList<ConfigValue>();
	private Object[] prefix;
	
	public class LiteralConfigAccumulator implements EventConsumer {
		private boolean empty;
		
		public void start(int ev, EventContext context) { empty=true; }
		public void end(int ev, EventContext context) {
			if(empty)
				text(ev,context,"");
		}

		public void text(int ev, EventContext context, String text) {
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
			empty=false;
		}

		public String getName() {
			return "literal-config-accumulator"; // XXX
		}
	}
	
	public SimpleConfigProviderBarbWirer(Object[] prefix) { 
		super.setConsumer(new LiteralConfigAccumulator());
		this.prefix=prefix;
	}

	public void provide(ConfigListener response) {
		for(ConfigValue v : values) {
			response.addConfig(v.key,new StringCSPConfigEvaluator(v.value),true);
		}
	}
}
