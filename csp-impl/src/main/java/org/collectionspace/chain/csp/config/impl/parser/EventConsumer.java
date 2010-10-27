/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.parser;

/* Like SAX, but simpler. You get reported when you enter and leave a tag, and any text in a tag (or attribute).
 * Attributes are treated as tags which begin with @, immediately within the tag where they appear. 
 * CDATA sections are collapsed into text. PIs are stripped.
 * 
 * You get the complete tag stack each time.
 */

public interface EventConsumer {
	public void start(String tag);
	public void end();
	public void text(String text);
}
