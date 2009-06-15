package org.collectionspace.toronto1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.toronto1.store.Store;
import org.collectionspace.toronto1.widgets.PageCreator;
import org.json.JSONException;
import org.json.JSONObject;

public class AccordionSidebar {
	private static int PER_PAGE=10;
	private Store store;
	private PageCreator pc;
	private int page=-1,total_pages;
	private HttpServletRequest req;
	private String[] select_ids=null;
	private boolean clearable=false;
	private String type;
	
	public AccordionSidebar(HttpServletRequest req,Store store,PageCreator pc,String type) {
		this.store=store;
		this.pc=pc;
		this.req=req;
		this.type=type;
	}
	
	public void setIds(String[] in) { select_ids=in; }
	public void setClearable() { clearable=true; }
	
	private String servletRelativePath(String in) { // XXX configurable
		if(!in.startsWith("/"))
			in="/"+in;
		return req.getContextPath()+in;
	}
	
	static String defaultString(String in,String def) {
		if(StringUtils.isBlank(in))
			return def;
		return in;
	}
	
	private Map<String,Object> modelForItem(String type,String id) throws IOException, JSONException {
		JSONObject data=store.getEntry(id);
		Map<String,Object> out=new HashMap<String,Object>();
		out.put("id",id);
		out.put("url",servletRelativePath("/main/edit/"+type+"/"+id));
		out.put("title",AccordionSidebar.defaultString(pc.summarize(type,"title",data),"Untitled"));
		out.put("brief",AccordionSidebar.defaultString(pc.summarize(type,"brief",data),"No decsription"));
		return out;
	}
	
	private List<String> getIDsInOrder(String type,String our_id) throws IOException, JSONException {
		// Get full list
		String[] ids=select_ids;
		if(select_ids==null) {
			Map<String,String> typed=new HashMap<String,String>();
			typed.put("__type",type);		
			ids=store.search(typed);
		}		
		final Map<String,String> titles=new HashMap<String,String>();		
		for(String id : ids) {
			JSONObject entry=store.getEntry(id);
			titles.put(id,defaultString(pc.summarize(type,"title",entry),"Untitled"));
		}
		// Sort by title
		List<String> order=new ArrayList<String>(titles.keySet());
		Collections.sort(order,new Comparator<String>() {
			public int compare(String id1, String id2) {
				return titles.get(id1).toLowerCase().compareTo(titles.get(id2).toLowerCase());
			}
		});
		// If no page has been set, base it on us
		if(page==-1) {
			// Find our index
			int our_index=0;
			int i=0;
			for(String r : order) {
				if(r.equals(our_id))
					our_index=i;
				i++;
			}
			// Work out which page to display
			page=our_index/PER_PAGE;
		}
		// Return page		
		int finish=(page+1)*PER_PAGE;
		if(finish>order.size())
			finish=order.size();
		total_pages=(order.size()-1)/PER_PAGE+1;
		return new ArrayList<String>(order.subList(page*PER_PAGE,finish));
	}
	
	private List<Object> getItems(String type,String our_id) throws IOException, JSONException {
		List<String> ids=getIDsInOrder(type,our_id);
		List<Object> items=new ArrayList<Object>();
		for(String id : ids) {
			items.add(modelForItem(type,id));
		}
		return items;
	}
	
	public Map<String,Object> generateModel(Map<String,Object> model,String type,String our_id) throws IOException, JSONException {
		List<Object> items=getItems(type,our_id);
		model.put("items",items);
		if(page>0)
			model.put("prev",page-1);
		if(page<total_pages-1)
			model.put("next",page+1);		
		model.put("clearable",clearable);
		return model;
	}

	public void setPage(int page) { this.page=page; }
}
