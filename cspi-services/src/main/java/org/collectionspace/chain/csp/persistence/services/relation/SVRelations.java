package org.collectionspace.chain.csp.persistence.services.relation;

public class SVRelations {
	private Relation[] relations;
	
	public SVRelations(Relation[] in) {
		relations=in;
	}
	
	public Relation[] getRelations() { return relations; }
}
