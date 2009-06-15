package org.collectionspace.toronto1.freemarker;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class Freemarker {
	private Configuration cfg;
	
	public Freemarker() {
		cfg=new Configuration();
		cfg.setLocalizedLookup(false);
		cfg.setTemplateLoader(new FreemarkerLoader());
		cfg.setObjectWrapper(new DefaultObjectWrapper());		
	}
	
	public void applyTemplate(String template,Object model,Writer writer) throws FreemarkerException {
		try {
			Template tmpl = cfg.getTemplate(template);
			if(tmpl==null)
				throw new FreemarkerException("Cannot find template "+template);
			tmpl.process(model,writer);
		} catch (IOException e) {
			throw new FreemarkerException("Cannot load template "+template,e);
		} catch (TemplateException e) {
			throw new FreemarkerException("Cannot process template "+template,e);
		}
	}
}
