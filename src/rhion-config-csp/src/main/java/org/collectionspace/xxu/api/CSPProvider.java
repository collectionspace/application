package org.collectionspace.xxu.api;

/* Built in:
 * 
 * 1. XSLT transform FINISHED (TEST IT)
 * 2. Add attachment point
 * 3. Attach javascript
 * 4. Attach java
 * 5. Implement library in java
 * 6. Implement library in javascript
 * 7. Add provider
 */
public interface CSPProvider {
	public void act(ConfigLoader in) throws ConfigLoadingException;
}
