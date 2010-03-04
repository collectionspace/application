package org.collectionspace.chain.csp.schema;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Util {
	public static String getStringOrDefault(ReadOnlySection section,String path,String dfault) {
		String out=(String)section.getValue(path);
		if(out==null)
			return dfault;
		return out;
	}

	public static boolean getBooleanOrDefault(ReadOnlySection section,String path,boolean dfault) {
		String out=(String)section.getValue(path);
		if(out==null)
			return dfault;
		return "1".equals(out) || "yes".equals(out.toLowerCase());
	}
	
	public static Set<String> getSetOrDefault(ReadOnlySection section,String path,String[] dfault) {
		String values=(String)section.getValue(path);
		String[] data=dfault;
		if(values!=null)
			data=values.split(",");
		return new HashSet<String>(Arrays.asList(data));
	}
}
