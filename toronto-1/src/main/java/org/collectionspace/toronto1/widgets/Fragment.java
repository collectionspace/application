package org.collectionspace.toronto1.widgets;

import java.io.IOException;

import org.collectionspace.toronto1.html.HTMLPage;
import org.json.JSONObject;

public interface Fragment {
	public void writePage(HTMLPage page,JSONObject data,String mode) throws FactoryException;
	public void getSummary(StringBuffer out,String part,JSONObject data) throws FactoryException;
}
