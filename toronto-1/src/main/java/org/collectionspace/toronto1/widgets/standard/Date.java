package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;

import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.json.JSONObject;

public class Date extends NamedField {

	public Date(String label,String name,String[] summaries) { super(label,name,summaries); }

	@Override
	protected void writeControl(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			int seq=page.nextInt();
			page.getBody().append("<input type='text' "+nameHTML()+valueHTML(data)+" id='date-"+seq+"' class='autowidth data'/>");
			page.addBodyScript("$(function() { $(\"#date-"+seq+"\").datepicker(); });");
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}
}
