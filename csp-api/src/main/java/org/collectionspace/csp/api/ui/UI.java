package org.collectionspace.csp.api.ui;

import org.collectionspace.csp.api.persistence.Storage;

public interface UI {
	public void serviceRequest(UIRequest ui) throws UIException;
}
