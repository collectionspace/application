package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.AJAXRequest;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Task;
import org.collectionspace.toronto1.widgets.Actionable;
import org.collectionspace.toronto1.widgets.Fragment;
import org.collectionspace.toronto1.widgets.FragmentEconomy;
import org.collectionspace.toronto1.widgets.Page;
import org.json.JSONArray;
import org.json.JSONObject;

public class Buttons implements Fragment {
	private class Button implements Actionable {
		private String title;
		private Task act;
		private int num;

		private Button(String title,int num,Task act) {
			this.title=title;
			this.act=act;
			this.num=num;
			page.registerAction(seq+"-"+num,this);
		}

		public void writePage(HTMLPage html,JSONObject data,String mode) throws FactoryException {
			try {
				html.getBody().append("<input type='button' class='button' name='"+seq+"-"+num+"' value='"+StringEscapeUtils.escapeHtml(title)+"'/>");
			} catch (IOException e) {
				throw new FactoryException("IOException writing page",e);
			}
		}

		public void act(AJAXRequest request) { 
			Page.executeTask(act,request);
		}

		public void getSummary(StringBuffer out, String part, JSONObject data) throws FactoryException {}
	}

	private List<Button> buttons=new ArrayList<Button>();
	private int seq;
	private Page page;
	private Set<String> modes=new HashSet<String>();
	private boolean default_mode=false;

	public Buttons(Page page,String mode) {
		this.page=page;
		if(mode==null) {
			default_mode=true;
		} else {
			for(String v : mode.split(",")) {
				if(mode==null || "DEFAULT".equals(mode))
					default_mode=true;
				else
					modes.add(v);
			}
		}
		seq=page.nextInt();
	}

	public void addButton(String title,Task action) {
		Button b=new Button(title,buttons.size(),action);
		buttons.add(b);
	}

	private boolean thisMode(String requested_mode) {
		if(page.hasExplicitMode("buttons",requested_mode)) {
			// there is an explicit <buttons/> for this, is it this one?
			return modes.contains(requested_mode);
		} else {
			// no explicit buttons, this must be the implicit one
			return default_mode;
		}
	}

	public void writePage(HTMLPage html,JSONObject data,String mode) throws FactoryException {
		if(!thisMode(mode))
			return;
		try {
			html.getBody().append("<div class='buttons'>");
			for(Button b : buttons) {
				b.writePage(html,data,mode);
			}
			html.getBody().append("</div>");
		} catch (IOException e) {
			throw new FactoryException("IOException writing page",e);
		}

	}

	public void getSummary(StringBuffer out, String part, JSONObject data) throws FactoryException {}
}
