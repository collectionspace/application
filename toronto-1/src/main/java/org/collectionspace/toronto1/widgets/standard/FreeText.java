package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.io.Writer;

import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.json.JSONObject;

public class FreeText extends NamedField {

	public FreeText(String label,String name,String[] summaries) {
		super(label,name,summaries);
	}

	public void writeControl(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			Writer out=page.getBody();
			out.append("<input type='text' "+nameHTML()+valueHTML(data)+" class='autowidth data'/>");
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}
}
