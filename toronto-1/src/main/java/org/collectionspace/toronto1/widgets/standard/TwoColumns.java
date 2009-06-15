package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.io.Writer;

import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.json.JSONObject;

public class TwoColumns implements Fragment,Group {
	private Fragment col1,col2;
	private boolean second_col=false;

	public TwoColumns() {
	}

	public void writePage(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			page.addCSSFile("css/toronto.css");
			Writer out=page.getBody();
			out.append("<div class='colmask'><div class='colleft'><div class='col1'><div class='col1inner'>");
			col1.writePage(page,data,mode);
			out.append("</div></div><div class='col2'><div class='col2inner'>");
			col2.writePage(page,data,mode);
			out.append("</div></div></div></div>");
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}

	public void addHint(String hint) {}

	public void addMember(String title, Fragment content) {
		if(second_col) {
			col2=content;
		} else {
			col1=content;
			second_col=true;
		}
	}

	public void getSummary(StringBuffer out,String part,JSONObject data) throws FactoryException {
		col1.getSummary(out,part,data);
		col2.getSummary(out,part,data);
	}
}
