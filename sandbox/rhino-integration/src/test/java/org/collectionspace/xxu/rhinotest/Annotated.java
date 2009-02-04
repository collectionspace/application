package org.collectionspace.xxu.rhinotest;

import org.collectionspace.xxu.js.api.JavascriptVisible;

public @JavascriptVisible class Annotated {
	public @JavascriptVisible int field=42;
	public int unannotated;
	
	public @JavascriptVisible int method(int i) { return i+6; }
	public @JavascriptVisible Annotated another() { return new Annotated(); }
	public @JavascriptVisible int extract(Annotated a) { return a.field*2; }
	public int unmethod(int i) { return i+4; }
}
