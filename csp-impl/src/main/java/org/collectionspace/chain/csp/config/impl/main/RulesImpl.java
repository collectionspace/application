/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.main;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.SectionGenerator;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.config.impl.main.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesImpl implements Rules {
	private static final Logger log=LoggerFactory.getLogger(RulesImpl.class);
	private List<Rule> rules=new ArrayList<Rule>();
	
	public void addRule(String start,String[] path,String end,SectionGenerator step,Target target) { rules.add(new Rule(start,path,end,step,target)); }
	
	Rule matchRules(String name,List<String> path) {
		log.debug("Looking for rules with base milestone '"+name+"' and path "+StringUtils.join(path,"/"));
		for(Rule r : rules)
			if(r.match(name,path))
				return r;
		return null;
	}
}
