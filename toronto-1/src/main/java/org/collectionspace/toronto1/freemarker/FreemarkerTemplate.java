package org.collectionspace.toronto1.freemarker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

class FreemarkerTemplate {
	private InputStream stream;
	private static final String FREEMARKER_BASE="org/collectionspace/toronto1/freemarker";
	
	public FreemarkerTemplate(String name) {
		String path=FREEMARKER_BASE+"/"+name;
		stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	
	public void close() throws IOException {
		if(stream!=null)
		stream.close();
	}
	
	public Reader get() throws IOException {
		return new InputStreamReader(stream,"UTF-8");
	}
}
