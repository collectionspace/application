package org.collectionspace.chain.csp.schema;

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
}
