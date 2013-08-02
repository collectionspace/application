/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.main;

import org.collectionspace.chain.csp.config.impl.parser.EventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseRun implements EventConsumer {
	private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

	private TreeNode root = null, here = null;

	@Override
	public void end() {
		here = here.getParent();
	}

	@Override
	public void start(String tag) {
		if (root == null) {
			root = TreeNode.create_tag(tag);
			here = root;
		} else {
			TreeNode next = TreeNode.create_tag(tag);
			here.addChild(next);
			here = next;
		}
	}

	@Override
	public void text(String text) {
		log.trace(text);
		here.addChild(TreeNode.create_text(text));
	}

	public TreeNode getTree() {
		return root;
	}
}
