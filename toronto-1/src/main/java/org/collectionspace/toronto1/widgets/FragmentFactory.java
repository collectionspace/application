package org.collectionspace.toronto1.widgets;

import org.dom4j.Element;

public interface FragmentFactory {
	public Fragment createFragment(Page e,Element n) throws FactoryException;
}
