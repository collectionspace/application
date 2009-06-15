package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang.StringEscapeUtils;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.json.JSONObject;

public class Dropdown extends NamedField {
	private String[] values;

	public Dropdown(String label,String name,String[] values,String[] summaries) {
		super(label,name,summaries);
		this.values=values;
	}

	@Override
	protected void writeControl(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			String value=null;
			if(data!=null) {
				value=valueString(data);
			}
			Writer out=page.getBody();
			out.append("<select class='autowidth data' "+nameHTML()+">");
			if("search".equals(mode)) {
				out.append("<option value=''>--unspecified--</option>");
			}
			for(String option : values) {
				String selected="";
				if(value!=null && option.equals(value))
					selected=" selected='selected' ";
				out.append("<option value='"+StringEscapeUtils.escapeHtml(option)+"'"+selected+">"+option+"</option>");
			}
			out.append("</select>");	
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}
}
