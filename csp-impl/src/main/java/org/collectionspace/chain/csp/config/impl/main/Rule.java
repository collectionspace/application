/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.main;

import java.util.List;

import org.collectionspace.chain.csp.config.SectionGenerator;
import org.collectionspace.chain.csp.config.RuleTarget;

class Rule {
	private String start, end;
	private String[] path;
	private SectionGenerator step;
	private RuleTarget target;

	Rule(String start, String[] path, String end, SectionGenerator step,
			RuleTarget target) {
		this.start = start;
		this.end = end;
		this.path = path;
		this.step = step;
		this.target = target;
		if (this.step == null && this.target != null) {
			this.step = new DefaultStep();
		}
	}

	boolean match(String start, List<String> path) {
		if (!start.equals(this.start))
			return false;
		
		if (path.size() != this.path.length)
			return false;
		
		for (int i = 0; i < this.path.length; i++) {
			if (!this.path[i].equals(path.get(i))) {
				return false;
			}
		}
		
		return true;
	}

	int getLength() {
		return path.length;
	}

	String destName() {
		return end;
	}

	SectionGenerator getStep() {
		return step;
	}

	RuleTarget getTarget() {
		return target;
	}
}
