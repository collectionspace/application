package org.collectionspace.chain.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.input.TeeInputStream;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionUtils;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.external.UIMapping;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UI;

public class ChainUIServlet extends ChainServlet {
	private static final Logger log = LoggerFactory
			.getLogger(ChainUIServlet.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void service(HttpServletRequest servlet_request,
			HttpServletResponse servlet_response) throws ServletException,
			IOException {

		try {

			String pathinfo = servlet_request.getPathInfo();

			if (pathinfo.equals("/html") || pathinfo.equals("/html/")) {
				servlet_response
						.sendRedirect("/collectionspace/ui/html/index.html");
			}

			if (!inited)
				setup();

			// should we redirect this url or just do the normal stuff

			// Setup our request object
			UI web = cspm.getUI("web");
			WebUI webui = (WebUI) web;
			UIMapping[] allmappings = webui.getAllMappings();

			ConfigRoot root = cspm.getConfigRoot();
			Spec spec = (Spec) root.getRoot(Spec.SPEC_ROOT);

			ServletContext sc = null;
			sc = getServletContext().getContext("/cspace-ui");
			if(sc==null){
				servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing servlet context cspace-ui");
			}

			Boolean doMetaConfig = false;
			String path = pathinfo;
			UIMapping validmap = doMapping(pathinfo, allmappings, spec);
			if (null != validmap) {
				path = validmap.getFile();
				if (validmap.hasMetaConfig() && doMetaConfig) {
					InputStream is = sc.getResourceAsStream(path);
					
					SAXReader reader = new SAXReader();
					reader.setEntityResolver(new NullResolver());
					Document xmlfile = null;

					ByteArrayOutputStream dump = new ByteArrayOutputStream();
					xmlfile = reader.read(new TeeInputStream(is, dump));
				//	log.info(dump.toString("UTF-8"));

					for (String metafield : validmap.getAllMetaConfigs()) {
						String xPath = "//html/head/" + metafield;
						List<Node> nodes = xmlfile.selectNodes(xPath);

						if (!nodes.isEmpty()) {
							String fieldval = validmap.getMetaConfig(metafield)
									.getValue();
							nodes.get(0).setText(fieldval);
						}
					}
					InputStream source = documentToStream(xmlfile);
					serveContent(servlet_response, source);

					is.close();
					return;
				}
			}

			if (serverFixedExternalContent(servlet_request, servlet_response,
					sc, path)) {
				return;
			}

		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					getStackTrace(x));
		} catch (ConnectionException e) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					getStackTrace(e));
		} catch (DocumentException e) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					getStackTrace(e));
		}
	}
	/* borrowed from ContectionUtils and tweaked e.g. set newline = true*/
	static InputStream serializetoXML(Document doc) throws IOException {
		return new ByteArrayInputStream(serializeToBytes(doc));
	}

	static byte[] serializeToBytes(Document doc) throws IOException {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		outformat.setExpandEmptyElements(true);
		outformat.setNewlines(true);
		outformat.setIndent(false);
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(doc);
		writer.flush();
		out.close();
		return out.toByteArray();
	}
	
	public static InputStream documentToStream(Document in) throws ConnectionException {
		if(in!=null) {
			try {
				return serializetoXML(in);
			} catch (IOException e) {
				throw new ConnectionException("Could not connect "+e.getLocalizedMessage(),e);
			}
		}
		return null;
	}

	private UIMapping doMapping(String pathinfo, UIMapping[] allmappings,
			Spec spec) {
		for (UIMapping map : allmappings) {

			if (map.hasUrl()) {
				if (map.getUrl().equals(pathinfo)) {
					return map;
				}
			} else if (map.hasType()) {
				for (Record r : spec.getAllRecords()) {
					if (r.isType(map.getType())) {
						if(r.isType("authority")){
							for(Instance ins: r.getAllInstances()){
								if (pathinfo.equals("/html/" + ins.getUIURL())) {
									return map;
								}
							}
						}
						if (pathinfo.equals("/html/" + r.getUIURL())) {
							return map;
						}
					}
				}
			}
		}
		return null;
	}

}

