package org.collectionspace.csp.api.ui;

public interface UI {
	public void serviceRequest(UIRequest ui) throws UIException;
	public UIUmbrella createUmbrella();
}
