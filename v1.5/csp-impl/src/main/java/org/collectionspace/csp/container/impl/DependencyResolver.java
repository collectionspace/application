/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.container.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.collectionspace.csp.api.core.CSPDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyResolver {
	private String task_name;
	private List<Dependable> jobs=new ArrayList<Dependable>();
	private static final Logger log=LoggerFactory.getLogger(DependencyResolver.class);
	
	public DependencyResolver(String task_name) { this.task_name=task_name; }
	
	public void addRunnable(Dependable r) { jobs.add(r); }
	
	public void go() throws CSPDependencyException {
		Set<Dependable> success=new HashSet<Dependable>();
		Set<CSPDependencyException> errors=new HashSet<CSPDependencyException>();
		boolean anything=true;
		int count=0;
		int max=jobs.size();
		while(anything && count<max) {
			errors=new HashSet<CSPDependencyException>();
			anything=false;
			for(Dependable r : jobs) {
				if(success.contains(r))
					continue;
				try {
					r.run();
					success.add(r);
					anything=true;
					log.debug("Dynamic dependency task '"+task_name+"' CSP("+r.getName()+") loaded successfully");
					count++;
				} catch(CSPDependencyException x) {
					log.debug("Dynamic dependency task '"+task_name+"' could not load CSP("+r.getName()+") yet: "+x.getMessage());
					errors.add(x);
				}
			}
		}
		if(count<max) {
			for(CSPDependencyException x : errors) {
				log.error("Unresolved CSP Exception in dependency task '"+task_name+"' "+x.getMessage());
			}
			throw new CSPDependencyException("Multiple dependency Resolution exceptions",
					(CSPDependencyException[])(errors.toArray(new CSPDependencyException[0])));
		}
	}
}
