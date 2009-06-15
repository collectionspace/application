package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.collectionspace.toronto1.html.HTMLPage;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.json.JSONObject;

public class Sequence implements Fragment, Group {
	private List<Fragment> seq=new ArrayList<Fragment>();

	public Sequence() {}
	public Sequence(Fragment[] frags) { seq.addAll(Arrays.asList(frags)); }

	public void addMember(String title,Fragment content) { seq.add(content); }

	public void writePage(HTMLPage page,JSONObject data,String mode) throws FactoryException {
		for(Fragment f : seq) {
			f.writePage(page,data,mode);
		}
	}

	public void addHint(String hint) {}
	
	public void getSummary(StringBuffer out, String part, JSONObject data) throws FactoryException {
		for(Fragment f : seq)
			f.getSummary(out,part,data);
	}
}
