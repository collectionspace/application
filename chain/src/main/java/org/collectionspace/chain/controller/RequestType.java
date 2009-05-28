package org.collectionspace.chain.controller;

/** Separate request into two types depending on path suffix:
 * 
 * STORE: store/retrieve of object data
 * SCHEMA: retrieve schema
 * 
 * @author dan
 *
 */
public enum RequestType {
	STORE,SCHEMA,LIST;
}
