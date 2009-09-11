package org.collectionspace.csp.api.config;



/* Like SAX, but simpler. You get reported when you enter and leave a tag, and any text in a tag (or attribute).
 * Attributes are treated as tags which begin with @, immediately within the tag where they appear. 
 * CDATA sections are collapsed into text. PIs are stripped.
 * 
 * You get the complete tag stack each time.
 */

public interface EventConsumer {
	public void start(int ev,EventContext context);
	public void end(int ev,EventContext context);
	public void text(int ev,EventContext context,String text);
	public String getName(); // Used in debugging: simple string corresponding to consumer's name
}
