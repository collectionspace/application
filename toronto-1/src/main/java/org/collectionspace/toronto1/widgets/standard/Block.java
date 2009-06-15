package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.json.JSONObject;

public class Block implements Fragment, Group {
	private Sequence seq=new Sequence();
	private String title=null;

	public Block(String title) { this.title=title; }

	public void writePage(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			page.addCSSFile("css/toronto.css");
			if(title!=null)
				page.getBody().append("<div class='block-head'>"+StringEscapeUtils.escapeHtml(title)+"</div>");
			page.getBody().append("<div class='block'>");
			seq.writePage(page,data,mode);
			page.getBody().append("</div>");
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}

	public void addHint(String hint) {}

	public void addMember(String title, Fragment content) {
		seq.addMember(title,content);
	}

	public void getSummary(StringBuffer out,String part,JSONObject data) throws FactoryException {
		seq.getSummary(out,part,data);
	}
}
