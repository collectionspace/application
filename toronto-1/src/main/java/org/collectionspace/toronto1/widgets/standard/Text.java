package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;

import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.json.JSONObject;

public class Text implements Fragment {
	private String content;

	public Text(String in) { content=in; }

	public void writePage(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			page.getBody().append(content);
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}

	public void getSummary(StringBuffer out, String part, JSONObject data) throws FactoryException {}
}
