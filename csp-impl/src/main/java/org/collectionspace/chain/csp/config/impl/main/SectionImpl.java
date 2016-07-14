/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Section;
import org.collectionspace.chain.csp.config.RuleTarget;
import org.collectionspace.services.common.api.Tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionImpl implements Section {
	private static final Logger log = LoggerFactory.getLogger(SectionImpl.class);
	private ReadOnlySection parent;
	private String name;
	private Map<String, Object> map = new HashMap<String, Object>();
	private List<SectionImpl> children = new ArrayList<SectionImpl>();
	private RuleTarget target;

	public SectionImpl(ReadOnlySection parent, String name, RuleTarget target) {
		this.parent = parent;
		this.name = name;
		this.target = target;
	}

	@Override
	public ReadOnlySection getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void addValue(String key, Object value) {
		map.put(key, value);
	}

	@Override
	public Object getRawValue(String key) {
		return map.get(key);
	}
	
	@Override
	public Object getValue(String key) {
		Object result = map.get(key);
		
		if (result != null && result instanceof String) {
			//
			// See if the value is actually a property variable (i.e., of the form ${foo}) that we need to substitute with an environment variable value
			try {
				String newValue = Tools.getValueFromEnv((String)result);
				if (newValue != null) {
					result = newValue;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
		
		return result;
	}

	void addChild(SectionImpl m) {
		children.add(m);
	}

	public void buildTargets(Object payload) throws Exception {
		if (target != null)
			payload = target.populate(payload, this);
		for (SectionImpl m : children)
			m.buildTargets(payload);
	}

	public void dump() {
		log.debug("Dumping milestone type " + name);
		for (Map.Entry<String, Object> e : map.entrySet()) {
			log.debug(" " + e.getKey() + "=" + e.getValue());
		}
		for (SectionImpl m : children)
			m.dump();
	}

	@Override
	public ReadOnlySection[] getChildren() {
		return children.toArray(new ReadOnlySection[0]);
	}
}
