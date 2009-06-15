package org.collectionspace.toronto1.freemarker;

import java.io.IOException;
import java.io.Reader;


import freemarker.cache.TemplateLoader;

class FreemarkerLoader implements TemplateLoader {

	public Object findTemplateSource(String name) throws IOException {
		return new FreemarkerTemplate(name);
	}
	
	public void closeTemplateSource(Object tmpl) throws IOException {
		((FreemarkerTemplate)tmpl).close();
	}
	
	public long getLastModified(Object tmpl) { return -1; }

	public Reader getReader(Object tmpl, String encoding) throws IOException {
		return ((FreemarkerTemplate)tmpl).get();
	}
}
