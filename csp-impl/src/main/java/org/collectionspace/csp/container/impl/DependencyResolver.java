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
	private List<Dependable> jobList = new ArrayList<Dependable>();
	private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);
	
	public DependencyResolver(String task_name) {
		this.task_name=task_name;
	}
	
	public void addRunnable(Dependable r) {
		jobList.add(r); // Dependables are just proxies for running the "go" method of CSP instances -e.g., CoreConfig, FileStorage, Spec, etc
	}
	
	public void go() throws CSPDependencyException {
		Set<Dependable> successList = new HashSet<Dependable>(); // keep track of successful runs of Dependables
		Set<CSPDependencyException> exceptionList = null;
		
		boolean contineRunningJobs = true;
		int count = 0;
		int max = jobList.size();
		while (contineRunningJobs && count < max) { // FIXME: What's the logic here? Keep trying while all jobs haven't run successfully (i.e. count < max) and ???
			exceptionList = new HashSet<CSPDependencyException>(); // reset the list of errors/exceptions
			contineRunningJobs = false;
			for (Dependable job : jobList) {
				if (successList.contains(job) == false) { // test to see if we already successfully ran the job
					try {
						job.run();
						successList.add(job);
						contineRunningJobs = true;  // since we ran one successfully, we'll try rerunning the ones that may have failed earlier
						count++;
						log.debug("Dynamic dependency task '"+task_name+"' CSP("+job.getName()+") loaded successfully");
					} catch(CSPDependencyException x) {
						log.debug("Dynamic dependency task '"+task_name+"' could NOT load CSP("+job.getName()+") yet: "+x.getMessage());
						exceptionList.add(x);
					}
				}
			}
		}
		
		if (count < max) { // if we couln't run them all successfully then log the errors/exceptions and throw an exception
			for (CSPDependencyException x : exceptionList) {
				log.error("Unresolved CSP Exception in dependency task '"+task_name+"' "+x.getMessage());
			}
			throw new CSPDependencyException("Encountered multiple dependency resolution exceptions:",
					(CSPDependencyException[])(exceptionList.toArray(new CSPDependencyException[0])));
		}
	}
}
