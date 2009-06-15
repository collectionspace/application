package org.collectionspace.toronto1.widgets.standard;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.toronto1.widgets.FactoryException;
import org.collectionspace.toronto1.widgets.Fragment;
import org.collectionspace.toronto1.widgets.FragmentEconomy;
import org.collectionspace.toronto1.widgets.FragmentFactory;
import org.collectionspace.toronto1.widgets.Page;
import org.collectionspace.toronto1.widgets.Task;
import org.collectionspace.toronto1.widgets.TaskFactory;
import org.dom4j.Element;
import org.dom4j.Node;

public class StandardFactory implements FragmentFactory, TaskFactory {
	
	public StandardFactory(FragmentEconomy e) {
		e.registerFragmentFactory("free",this);
		e.registerFragmentFactory("date",this);
		e.registerFragmentFactory("dropdown",this);
		e.registerFragmentFactory("block",this);
		e.registerFragmentFactory("tabs",this);
		e.registerFragmentFactory("columns",this);
		e.registerFragmentFactory("buttons",this);
		e.registerFragmentFactory("page",this);
		
		e.registerTaskFactory("save",this);
		e.registerTaskFactory("search",this);
		e.registerTaskFactory("goto",this);
	}
	
	@SuppressWarnings("unchecked")
	private void addSequence(Page page,Group g,Element n) throws FactoryException {
		List<Element> children=(List<Element>)n.elements();
		for(Element child : children) {
			if("layout-hint".equals(child.getName())) {
				g.addHint(child.attributeValue("type"));
			} else {
				g.addMember(null,page.createFragment(child));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addMembers(Page page,Group g,Element n,String tag) throws FactoryException {
		List<Element> children=n.elements();
		for(Element child : children) {
			if(tag.equals(child.getName())) {
				String title=child.attributeValue("title");
				Sequence s=new Sequence();
				addSequence(page,s,child);
				g.addMember(title,s);
			} else if("layout-hint".equals(child.getName())) {
				g.addHint(child.attributeValue("type"));
			} else {
				g.addMember(null,page.createFragment(child));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public Fragment createFragment(Page page,Element n) throws FactoryException {
		String name=n.getName();
		if("block".equals(name)) {
			String title=n.attributeValue("title");
			Group g=new Block(title);
			addSequence(page,g,n);
			return g;			
		}
		if("tabs".equals(name)) {
			Tabs t=new Tabs();
			addMembers(page,t,n,"tab");			
			return t;
		}
		if("columns".equals(name)) {
			TwoColumns t=new TwoColumns();
			addMembers(page,t,n,"column");
			return t;
		}
		if("buttons".equals(name)) {
			String mode=n.attributeValue("mode");
			if(mode!=null)
				page.addExplicitMode("buttons",mode);
			Buttons b=new Buttons(page,mode);
			List<Element> children=n.elements();
			// IMPORTANT must always add all buttons (for routing)
			for(Element child : children) {
				Task task=page.createTask(child);
				b.addButton(child.attributeValue("title"),task);
			}
			return b;
		}
		if("page".equals(name)) {
			Sequence s=new Sequence();
			addSequence(page,s,n);
			return s;
		}
		String[] summaries=new String[0];
		String sstr=n.attributeValue("summary");
		if(!StringUtils.isBlank(sstr)) {
			summaries=sstr.split(",");
		}
		if("free".equals(name)) {
			return new FreeText(n.attributeValue("title"),n.attributeValue("name"),summaries);
		} else if("date".equals(name)) {
			return new Date(n.attributeValue("title"),n.attributeValue("name"),summaries);
		} else if("dropdown".equals(name)) {
			List<String> values=new ArrayList<String>();
			for(Node value : (List<Node>)n.selectNodes("value")) {
				values.add(value.getText());
			}
			return new Dropdown(n.attributeValue("title"),n.attributeValue("name"),values.toArray(new String[0]),summaries);
		}
		throw new FactoryException("Bad element "+name);
	}

	public Task createTask(Page e, Element n) throws FactoryException {
		String name=n.getName();
		if("save".equals(name))
			return new SaveTask();
		if("search".equals(name))
			return new SearchTask();
		if("goto".equals(name))
			return new GotoTask(n.attributeValue("url"));
		return null;
	}
}
