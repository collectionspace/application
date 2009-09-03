/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

/** Separate requests into three types. Returned by ChainRequest on the basis of various things in the request 
 * including path, parameters, etc.
 * 
 * STORE: store/retrieve of object data
 * SCHEMA: retrieve schema
 * LIST: list all members
 * LOGIN: login page
 */
public enum RequestType {
	STORE,SCHEMA,LIST,RESET,LOGIN;
}
