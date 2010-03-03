package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Util {
	public static String getStringOrDefault(ReadOnlySection section,String path,String dfault) {
		String out=(String)section.getValue(path);
		if(out==null)
			return dfault;
		return out;
	}
}
