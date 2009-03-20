package org.collectionspace.xxu.rhinotest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.xxu.js.api.JavascriptVisible;

public @JavascriptVisible class Annotated {
	public @JavascriptVisible int field=42;
	public @JavascriptVisible int[] array=new int[]{2,4,6,8};
	public @JavascriptVisible List<Integer> list=new ArrayList<Integer>();
	public @JavascriptVisible Map<String,String> map=new HashMap<String,String>();
	public @JavascriptVisible Map<String,Object> evil_map=new HashMap<String,Object>();
	public @JavascriptVisible List<Object> evil_list=new ArrayList<Object>();
	public @JavascriptVisible int[][] twod=new int[][]{new int[]{1},new int[]{2,3}};
	
	public int unannotated;
	
	public Annotated() {
		this(true);
	}
	
	public Annotated(boolean rec) {
		list.add(11);
		list.add(13);
		list.add(15);
		map.put("apple","banana");
		map.put("yoga","zebra");
		evil_map.put("bad",new Unannotated());
		evil_list.add(new Unannotated());
		if(rec) {
			evil_map.put("good",new Annotated(false));
			evil_list.add(new Annotated(false));
		}
	}
	
	public @JavascriptVisible int method(int i) { return i+6; }
	public @JavascriptVisible Annotated another() { return new Annotated(); }
	public @JavascriptVisible int extract(Annotated a) { return a.field*2; }
	public int unmethod(int i) { return i+4; }
}
