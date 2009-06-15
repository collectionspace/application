package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class NamedField extends LabelledField {
	private String name;
	private String[] summaries;
	
	public NamedField(String label,String name,String[] summaries) {
		super(label);
		this.name=name;
		this.summaries=summaries;
	}

	protected String getName() { return name; }

	protected String nameHTML() {
		if(StringUtils.isBlank(name))
			return "";
		return " name='"+StringEscapeUtils.escapeHtml(name)+"' ";
	}

	public String valueString(JSONObject data) throws FactoryException {
		try {
			if(data==null || "".equals(data) || name==null || "".equals(name))
				return null;
			return data.getString(name);
		} catch (JSONException e) {
			throw new FactoryException("JSONException reading data",e);
		}
	}
	
	public String valueHTML(JSONObject data) throws FactoryException {
		String value=valueString(data);
		if(value==null)
			return "";
		return " value='"+StringEscapeUtils.escapeHtml(value)+"' ";
	}
	
	public void getSummary(StringBuffer out,String part,JSONObject data) throws FactoryException {
		if(summaries==null)
			return;
		for(String summary : summaries) {
			if(part.equals(summary))
				out.append(valueString(data)+" ");
		}
	}
}
