package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.json.JSONObject;

public class Tabs implements Fragment, Group {
	private static class Tab { String title; Fragment content; int seq; }

	private List<Tab> tabs=new ArrayList<Tab>();

	public void addMember(String title,Fragment content) {
		Tab tab=new Tab();
		if(StringUtils.isBlank(title))
			title="untitled";
		tab.title=title;
		tab.content=content;
		tabs.add(tab);
	}

	public void writePage(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		try {
			Writer out=page.getBody();
			int seq=page.nextInt();
			out.append("<div class='tabs' id='tabs-"+seq+"'><ul>");
			for(Tab t : tabs) {
				t.seq=page.nextInt();
			}
			for(Tab t : tabs) {
				out.append("<li><a href=\"#tab-"+t.seq+"\">"+StringEscapeUtils.escapeHtml(t.title)+"</a></li>");
			}
			out.append("</ul>");
			for(Tab t : tabs) {
				out.append("<div id=\"tab-"+t.seq+"\">");
				t.content.writePage(page,data,mode);
				out.append("</div>");
			}
			out.append("</div>");
			page.addBodyScript("$(function() { $(\"#tabs-"+seq+"\").tabs(); });");
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}
	}

	public void addHint(String hint) {}

	public void getSummary(StringBuffer out, String part, JSONObject data) throws FactoryException {
		for(Tab t : tabs) {
			t.content.getSummary(out,part,data);
		}
	}
}
