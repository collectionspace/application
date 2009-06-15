package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang.StringEscapeUtils;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.json.JSONObject;

public abstract class LabelledField implements Fragment {
	private String label;

	public LabelledField(String label) {
		this.label=label;
	}

	protected abstract void writeControl(HTMLPage page,JSONObject data,String mode) throws FactoryException;

	public void writePage(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			Writer out=page.getBody();
			out.append("<div class='entry'>");
			out.append("<span class='label'>");
			out.append(StringEscapeUtils.escapeHtml(label));
			out.append("</span>");
			writeControl(page,data,mode);
			out.append("</div>");
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}
}
