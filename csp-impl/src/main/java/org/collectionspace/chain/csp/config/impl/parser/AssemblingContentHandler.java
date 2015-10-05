package org.collectionspace.chain.csp.config.impl.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.services.common.api.FileTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import ch.elca.el4j.services.xmlmerge.AbstractXmlMergeException;
import ch.elca.el4j.services.xmlmerge.Configurer;
import ch.elca.el4j.services.xmlmerge.config.ConfigurableXmlMerge;
import ch.elca.el4j.services.xmlmerge.config.PropertyXPathConfigurer;

// XXX namespaces in XSLT
public class AssemblingContentHandler extends DefaultHandler implements ContentHandler {

    private static final class XSLTTag {
        private String src, root;
    }

    private static final class IncludeTag {
        private String src;
        private String merge;
        private boolean strip;
    }

    private static final Logger logger = LoggerFactory.getLogger(AssemblingContentHandler.class);
    private static final String XMLMERGE_DEFAULT_PROPS_STR = "matcher.default=ID\n";
    private static final String XSLT_TAG = "xslt";
    private static final String INCLUDE_TAG = "include";
    private static final String DEFINE_TAG = "define";
	protected static File tempDirectory = setTempDirectory(); // Keep this static so all the XMLMerge files (for each run, that is) end up in the same place

    private AssemblingContentHandler resolve_parent;
    private SAXParserFactory factory;
    private ContentHandler delegated, up;
    private String delegated_root;
    private String tenantname;
    private int delegated_depth = -1, depth = 0;
    private SAXTransformerFactory transfactory;
    private boolean outer, strip;
    private AssemblingParser parser;
    private Map<String, XSLTTag> xslt_tags = new HashMap<String, XSLTTag>();
    private Map<String, IncludeTag> include_tags = new HashMap<String, IncludeTag>();

    private static File setTempDirectory() {
    	File result = null;
    	
    	try {
    		result = FileTools.createTmpDir("merged-app-config-");
    	} catch (IOException x) {
    		logger.debug("Could not create a temp directory in which to store the XMLMerge results of the app config files.", x);
    	}
    	
    	return result;
    }
    
    AssemblingContentHandler(AssemblingParser parser, ContentHandler r) throws ConfigException, IOException {
        this(parser, r, true, false, null);
    }

	private AssemblingContentHandler(AssemblingParser parser, ContentHandler r,
			boolean outer, boolean strip, AssemblingContentHandler rp)
			throws ConfigException, IOException {
		up = r;
		factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		TransformerFactory tf = TransformerFactory.newInstance();
		if (!tf.getFeature(SAXSource.FEATURE)
				|| !tf.getFeature(SAXResult.FEATURE)) {
			throw new ConfigException("XSLT not supported");
		}
		transfactory = (SAXTransformerFactory) tf;
		this.outer = outer;
		this.strip = strip;
		this.resolve_parent = rp;
		if (rp != null) {
			this.tenantname = rp.tenantname;
		}
		this.parser = parser;
	}

    private void apply_xslt(InputSource xslt, String root) throws SAXException {
        try {
            ContentHandler inner = new AssemblingContentHandler(parser, up, false, false, this);
            TransformerHandler transformer = transfactory.newTransformerHandler(new StreamSource(xslt.getByteStream()));
            transformer.setResult(new SAXResult(inner));
            delegated = transformer;
            delegated.startDocument();
            if (root != null) {
                delegated.startElement("", root, root, new AttributesImpl());
            }
            delegated_root = root;
            delegated_depth = 1;
        } catch (TransformerConfigurationException e) {
            throw new SAXException("Could not create inner parser", e);
        } catch (ConfigException e) {
            throw new SAXException("Could not create inner parser", e);
        } catch (IOException e) {
			//logger.debug("Could not create);
		}

    }

    private void apply_include(InputSource src, boolean strip) throws SAXException {
        try {
            SAXParser sp = factory.newSAXParser();
            DefaultHandler inner = new AssemblingContentHandler(parser, up, false, strip, this);
            sp.parse(src, inner);
        } catch (ParserConfigurationException e) {
            throw new SAXException("Could not create inner parser", e);
        } catch (ConfigException e) {
            throw new SAXException("Could not create inner parser", e);
        } catch (IOException e) {
            throw new SAXException("Could not create inner parser", e);
        }
    }

    private InputStream merge(String loggingPrefix, Properties xmlmergeProps, InputStream[] inputStreams) throws AbstractXmlMergeException {
        InputStream result = null;
        //
        // If we have an XMLMerge properties file then use it -otherwise, use a default set of props.
        //
        Configurer configurer = null;
        if (xmlmergeProps != null) {
            configurer = new PropertyXPathConfigurer(xmlmergeProps);
        } else {
            configurer = new PropertyXPathConfigurer(XMLMERGE_DEFAULT_PROPS_STR);
        }
        result = new ConfigurableXmlMerge(configurer).merge(inputStreams);

        /*
         * Save the merge results for debugging purposes.
         */
        if (logger.isInfoEnabled() == true) {
            try {
                String outputFileNamePrefix = "/merged-" + loggingPrefix + "-";
                if (xmlmergeProps != null) {
                    File mergePropertiesFile = new File(AssemblingContentHandler.tempDirectory, outputFileNamePrefix + ".properties"); //make a copy of the XMLMerge properties used for the merge
                    ByteArrayInputStream propertiesStream = new ByteArrayInputStream(xmlmergeProps.toString().getBytes());
                    FileUtils.copyInputStreamToFile(propertiesStream, mergePropertiesFile);
                }
                File mergedOutFile = new File(AssemblingContentHandler.tempDirectory, outputFileNamePrefix + ".xml"); //make a copy of the merge results
                FileUtils.copyInputStreamToFile(result, mergedOutFile);
                result.reset();
                logger.info(String.format("XMLMerge results were written to file: %s", mergedOutFile.getAbsolutePath()));
            } catch (IOException e) {
                logger.info(String.format("Could not write XMLMerge results to directory: %s",
                		AssemblingContentHandler.tempDirectory.getAbsolutePath()), e);
            }
        }

        return result;
    }

    //
    // Use our standard method for finding resources to get the named XMLMerge properties file
    //
    private Properties getXMLMergeProperties(String resourceName) {
        Properties result = null;

        try {
            InputSource inputSource = this.find_entity(resourceName);
            if (inputSource != null && inputSource.getByteStream() != null) {
                result = new Properties();
                result.load(inputSource.getByteStream());
            } // FIXME: REM - Add an 'else' clause with a warning log entry here -i.e., the stream is empty the input source is null
        } catch (Exception e) {
            logger.debug("Could not find the XMLMerge properties file named: " + resourceName);
        }

        return result;
    }

    //
    // XMLMerge needs an InputStream[].  This method converts our ArrayList<InputSource> to an InputStream[] array.
    // This method also filters out any empty/blank InputSources
    //
    private InputStream[] toArray(ArrayList<InputSource> inputSources) {
        for (InputSource inputSource : inputSources) {
            try {
                if (inputSource.getByteStream().available() == 0) { //filter out any and all the empty streams
                    inputSources.remove(inputSource);
                }
            } catch (IOException e) {
                logger.warn("Encountered an exception while converting InputSource array to InputStream array", e);
            }
        }
        InputStream[] inputStreams = new InputStream[inputSources.size()]; //now create the InputStream[] array for XMLMerge
        for (int i = 0; i < inputSources.size(); i++) {
            inputStreams[i] = inputSources.get(i).getByteStream();
        }

        return inputStreams;
    }

    private InputSource apply_merge(IncludeTag includeTag) throws SAXException, IOException {
        InputSource result = null;

        ArrayList<InputSource> inputSources = this.find_all_entities(includeTag.src); // try to find all the files that are part of the "src" attribute
        if (inputSources != null && inputSources.isEmpty() == false) {
            InputStream mergedStream = null;
            try {
                Properties xmlmergeProps = getXMLMergeProperties(includeTag.merge); //the "merge" attribute contains the name of the XMLMerge properties file
                mergedStream = merge(includeTag.src.replace(',', '_'), // peform the merge
                        xmlmergeProps, toArray(inputSources));
            } catch (AbstractXmlMergeException e) {
                String msg = "Could not merge the include files: " + includeTag.src;
                logger.warn(msg);
                throw new IOException(msg, e);
            }
            result = new InputSource(mergedStream);
        }

        return result;
    }

    /*
     * "Includes" can be either a merge of all the "src" files or just an inline include of the first-found "src" file.
     */
    private void apply_include(IncludeTag includeTag) throws SAXException, IOException {
        InputSource includeSrc = null;

        if (includeTag.merge != null && !includeTag.merge.isEmpty()) { //FIXME: includeTag.merge could be null so an NPE might be thrown
            includeSrc = apply_merge(includeTag); //merges all the "src" files
        } else {
            includeSrc = find_first_entity(includeTag.src); //returns the first found "src" file
        }

        apply_include(includeSrc, includeTag.strip);
    }

    XSLTTag resolveXSLTTag(String name) {
        XSLTTag out = xslt_tags.get(name);
        if (out != null) {
            return out;
        }
        if (resolve_parent != null) {
            return resolve_parent.resolveXSLTTag(name);
        }
        return null;
    }

    IncludeTag resolveIncludeTag(String name) {
        IncludeTag out = include_tags.get(name);
        if (out != null) {
            return out;
        }
        if (resolve_parent != null) {
            return resolve_parent.resolveIncludeTag(name);
        }
        return null;
    }

    private XSLTTag isXSLTTag(String localName, Attributes attributes) throws SAXException {
        XSLTTag out = resolveXSLTTag(localName);
        if (out != null) {
            return out;
        }
        if (XSLT_TAG.equals(localName)) {
            out = new XSLTTag();
            out.src = attributes.getValue("src");
            out.root = attributes.getValue("root");
            return out;
        } else {
            return null;
        }
    }

    private IncludeTag isIncludeTag(String localName, Attributes attributes) throws SAXException {
        IncludeTag out = resolveIncludeTag(localName);
        if (out != null) {
            return out;
        }
        if (INCLUDE_TAG.equals(localName)) {
            out = new IncludeTag();
            out.src = attributes.getValue("src");
            out.merge = attributes.getValue("merge");
            out.strip = stringToBoolean(attributes.getValue("strip-root"));
            return out;
        } else {
            return null;
        }
    }

    private static boolean stringToBoolean(String in) {
        if (in != null) {
            in = in.toLowerCase();
            return (in.equals("yes") || in.equals("true"));
        }
        return false;
    }

    private void processXSLTDefine(Attributes attr) throws SAXException {
        XSLTTag tag = new XSLTTag();
        tag.src = attr.getValue("src");
        tag.root = attr.getValue("root");
        String name = attr.getValue("tag");
        if (name == null || "".equals(name)) {
            throw new SAXException("Tag has no name in definition");
        }
        xslt_tags.put(name, tag);
    }

    private void processIncludeDefine(Attributes attr) throws SAXException {
        IncludeTag tag = new IncludeTag();
        tag.src = attr.getValue("src");
        tag.strip = stringToBoolean(attr.getValue("strip-root"));
        String name = attr.getValue("tag");
        if (name == null || "".equals(name)) {
            throw new SAXException("Tag has no name in definition");
        }
        include_tags.put(name, tag);
    }

    private boolean processDefines(String localName, Attributes attributes) throws SAXException {
        if (!DEFINE_TAG.equals(localName)) {
            return false;
        }
        if (resolve_parent != null && stringToBoolean(attributes.getValue("global"))) {
            return resolve_parent.processDefines(localName, attributes);
        }
        String mode = attributes.getValue("mode");
        if ("xslt".equals(mode)) {
            processXSLTDefine(attributes);
        } else if ("include".equals(mode)) {
            processIncludeDefine(attributes);
        } else {
            throw new SAXException("Bad or missing include tag in define");
        }
        return true;
    }

    private InputSource find_entity(String src) throws SAXException,
            IOException { // this method returns when it finds the first item in
        // the "all" list
        try {
            InputSource out = resolveEntity(null, "tenants/" + this.tenantname //FIXME: this.tenantname might be null, so an NPE could be thrown
                    + "/" + src);
            if (out != null) {
                return out;
            }
        } catch (SAXException x) {
            // Quietly consume the exception
        }
        try {
            InputSource out = resolveEntity(null, "defaults/" + src);
            if (out != null) {
                return out;
            }
        } catch (SAXException x) {
            // Quietly consume the exception
        }
        try {
            InputSource out = resolveEntity(null, src);
            if (out != null) {
                return out;
            }
        } catch (SAXException x) {
            // Quietly consume the exception
        }

        return null;
    }

    private InputSource find_first_entity(String all) throws SAXException, IOException { // this method returns when it finds the first item in the "all" list
        InputSource result = null;
        for (String src : all.split(",")) {
            result = find_entity(src);
            if (result != null) {
                return result;
            }
        }

        throw new SAXException(all + " file(s) not found.");
    }

    private ArrayList<InputSource> find_all_entities(String all) throws SAXException, IOException {
        ArrayList<InputSource> result = new ArrayList<InputSource>();
        InputSource inputSource = null;
        for (String src : all.split(",")) {
            inputSource = find_entity(src);
            if (inputSource != null) {
                result.add(inputSource);
            } else {
                logger.warn("Source file for \"Include\" tag could not be found: " + src);
            }
        }

        if (result.isEmpty() == true) {
            logger.warn("All source files for \"Include\" tag could not be found: " + all);
        }

        return result;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        depth++;
        if (depth == 1 && strip) {
            return;
        }
        if (delegated_depth > 0) {
            delegated.startElement(uri, localName, qName, attributes);
            delegated_depth++;
            return;
        } else {
            try {
                if (processDefines(localName, attributes)) {
                    return;
                }
                XSLTTag xslt = isXSLTTag(localName, attributes);
                IncludeTag include = isIncludeTag(localName, attributes);
                if (localName.equals("cspace-config")) {
                    this.tenantname = attributes.getValue("tenantname");
                }
                if (xslt != null) {
                    apply_xslt(resolveEntity(null, xslt.src), xslt.root);
                } else if (include != null) {
                    if ("@".equals(include.src)) {
                        apply_include(parser.getMain(), include.strip);
                    } else //apply_include(find_entity(include.src),include.strip); //put merge code here when "merge" REM
                    {
                        apply_include(include);
                    }
                    logger.debug("Including: " + include.src);
                } else {
                    up.startElement(uri, localName, qName, attributes);
                }
            } catch (IOException x) {
            	logger.error(x.getMessage());
                throw new SAXException("Could not load source.");
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        depth--;
        if (depth == 0 && strip) {
            return;
        }
        if (delegated_depth > 0) {
            delegated_depth--;
            if (delegated_depth > 0) {
                delegated.endElement(uri, localName, qName);
            } else {
                if (delegated_root != null) {
                    delegated.endElement("", delegated_root, delegated_root);
                }
                delegated.endDocument();
            }
            return;
        }
        // XXX should check skipped tags instead, to make it more robust to internal definitions
        XSLTTag xslt = isXSLTTag(localName, new AttributesImpl());
        IncludeTag include = isIncludeTag(localName, new AttributesImpl());
        if (include != null || xslt != null || DEFINE_TAG.equals(localName)) {
            return;
        }
        up.endElement(uri, localName, qName);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (delegated_depth > 0) {
            delegated.characters(ch, start, length);
        } else {
            up.characters(ch, start, length);
        }
    }

    public void startDocument() throws SAXException {
        if (outer) {
            up.startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (outer) {
            up.endDocument();
        }
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return parser.getEntityResolver().resolveEntity(publicId, systemId);
    }
}
