package org.collectionspace.csp.api.config;

public interface Barb {
	public BarbWirer getManager();
	public void attach(BarbWirer manager,String root);
}
