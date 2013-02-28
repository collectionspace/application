package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.ui.Operation;

public interface WebMethodWithOps extends WebMethod {
	public Operation getOperation();
	public String getBase();
	public Spec getSpec();
}
