package org.collectionspace.chain.csp.nconfig.impl.main;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.SectionGenerator;
import org.collectionspace.chain.csp.nconfig.Target;
import org.collectionspace.chain.csp.nconfig.impl.main.Rule;

public class RulesImpl implements Rules {
	private List<Rule> rules=new ArrayList<Rule>();
	
	public void addRule(String start,String[] path,String end,SectionGenerator step,Target target) { rules.add(new Rule(start,path,end,step,target)); }
	
	Rule matchRules(String name,List<String> path) {
		System.err.println("Looking for rules with base milestone '"+name+"' and path "+StringUtils.join(path,"/"));
		for(Rule r : rules)
			if(r.match(name,path))
				return r;
		return null;
	}
}
