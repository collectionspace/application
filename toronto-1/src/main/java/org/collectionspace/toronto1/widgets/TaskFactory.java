package org.collectionspace.toronto1.widgets;

import org.dom4j.Element;

public interface TaskFactory {
	public Task createTask(Page e,Element n) throws FactoryException;
}
