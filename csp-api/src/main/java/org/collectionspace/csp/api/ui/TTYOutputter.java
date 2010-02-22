package org.collectionspace.csp.api.ui;

/* As close as a given delivery method can get to that ole glowin' screen */
public interface TTYOutputter {
	public void line(String text) throws UIException;
	public void flush() throws UIException;
}
