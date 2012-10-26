/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Util {
	public static String getStringOrDefault(ReadOnlySection section,String path,String dfault) {
		String out = null;
		if(section != null){
			out=(String)section.getValue(path);
		}
		if(out==null)
			return dfault;
		return out;
	}

	public static boolean getBooleanOrDefault(ReadOnlySection section,String path,boolean dfault) {
		String out = null;
		if(section != null){
			out=(String)section.getValue(path);
		}
		if(out==null)
			return dfault;
		return "1".equals(out) || "yes".equals(out.toLowerCase()) || "true".equals(out.toLowerCase());
	}

	public static Set<String> getSetOrDefault(ReadOnlySection section,String path,String[] dfault) {
		String out = null;
		if(section != null){
			out=(String)section.getValue(path);
		}
		return getSetOrDefault(out, path, dfault);
	}
	public static Set<String> getSetOrDefault(String values,String path,String[] dfault) {
		String[] data=dfault;
		if(values!=null)
			data=values.split(",");
		return new LinkedHashSet<String>(Arrays.asList(data));
	}
	
	
}
