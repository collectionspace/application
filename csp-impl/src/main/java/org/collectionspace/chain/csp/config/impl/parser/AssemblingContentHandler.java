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

    private static final Logger logger = LoggerFactory.getLogger(AssemblingContentHandler.class);

    private static final class XSLTTag {
        private String src, root;
    }

    private static final class IncludeTag {
    	Attributes attributes;
    	private boolean required = true;
        private String src;
        private String merge;
        private boolean strip;
        
        /**
         * Create a new IncludeTag instance initialized with an Attributes instance.
         * 
         * @param attributes
         * @throws IOException
         */
        IncludeTag(Attributes attrs) throws IOException {
        	attributes = attrs;
            required = false;
            strip = false;
            
            if (attributes.getLength() > 0) {
                required = stringToBoolean(attributes.getValue("required"), true);
                strip = stringToBoolean(attributes.getValue("strip-root"));
                merge = attributes.getValue("merge");
                src = attributes.getValue("src");
                if (src == null || src.length() == 0) {
                	String msg = String.format("Attempted to create a new IncludeTag that contained an empty 'src' attribute/tag.",
                			attributes.toString());
                	throw new IOException(msg);
                }            	
            }
        }
        
        boolean isRequired() {
        	return required;
        }
        
        @Override
		public
        String toString() {
        	String result = "Include tag with no attributes.";
        	
        	if (attributes.getLength() > 0) {
	        	result = String.format("merge:'%s' required:%s src:'%s' strip:%s", 
	        			attributes.getValue("merge"), 
	        			attributes.getValue("required"),
	        			attributes.getValue("src"),
	        			attributes.getValue("strip-root"));
        	}
        	
        	return result;
        }
    }

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
	private String srcFileName = null;

	public String getSrcFileName() {
		if (this.srcFileName == null) {
			logger.debug("AssemblingContentHandler.getSrcFileName() is returning null.");
		}
		return this.srcFileName;
	}
	
    private static File setTempDirectory() {
    	File result = null;
    	
    	try {
    		result = FileTools.createTmpDir("merged-app-config-");
    	} catch (IOException x) {
    		logger.debug("Config generation: Could not create a temp directory in which to store the XMLMerge results of the app config files.", x);
    	}
    	
    	return result;
    }
    
    public static File getTempDirectory() {
    	return tempDirectory;
    }
    
    public static File getTempDirectory(String leafDir) throws IOException {
    	File result = null;
    	
    	String newDirStr = getTempDirectory().getPath() + File.separatorChar + leafDir.split("\\.")[0];
    	result = new File(newDirStr);
    	if (result.exists() == false) {
    		if (result.mkdir() == false) {
    			throw new IOException(String.format("Could not create a new directory named: %s.", 
    					newDirStr));
    		}
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
		if (parser.getMain().getPublicId() != null) {
			this.srcFileName = parser.getMain().getPublicId();
		} else {
			logger.warn("Could not find source file name.");
		}
		this.parser = parser;
	}

    private void apply_xslt(InputSource xslt, String root) throws SAXException {
    	String errMsg = String.format("Config Generation: '%s' - Could not create inner parser.",
    			getSrcFileName());
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
            throw new SAXException(errMsg, e);
        } catch (ConfigException e) {
            throw new SAXException(errMsg, e);
        } catch (IOException e) {
            throw new SAXException(errMsg, e);
		}
    }

    private void apply_include(InputSource src, boolean strip) throws SAXException {
    	String errMsg = String.format("Config Generation: '%s' - Could not create inner parser.",
    			getSrcFileName());

        try {
            SAXParser sp = factory.newSAXParser();
            DefaultHandler inner = new AssemblingContentHandler(parser, up, false, strip, this);
            sp.parse(src, inner);
        } catch (ParserConfigurationException e) {
            throw new SAXException(errMsg, e);
        } catch (ConfigException e) {
            throw new SAXException(errMsg, e);
        } catch (IOException e) {
            throw new SAXException(errMsg, e);
        }
    }

    private InputStream merge(String loggingPrefix, Properties xmlmergeProps, InputStream[] inputStreams) throws AbstractXmlMergeException, IOException {
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
                    File mergePropertiesFile = new File(getTempDirectory(getSrcFileName()), 
                    		outputFileNamePrefix + ".properties"); //make a copy of the XMLMerge properties used for the merge
                    ByteArrayInputStream propertiesStream = new ByteArrayInputStream(xmlmergeProps.toString().getBytes());
                    FileUtils.copyInputStreamToFile(propertiesStream, mergePropertiesFile);
                }
                File mergedOutFile = new File(getTempDirectory(getSrcFileName()), outputFileNamePrefix + ".xml"); //make a copy of the merge results
                FileUtils.copyInputStreamToFile(result, mergedOutFile);
                result.reset();
                logger.trace(String.format("Config Generation: '%s' - XMLMerge results were written to file: %s", getSrcFileName(), mergedOutFile.getAbsolutePath()));
            } catch (IOException e) {
                logger.info(String.format("Config Generation: '%s' - Could not write XMLMerge results to directory: %s",
                		getSrcFileName(), getTempDirectory(getSrcFileName()).getAbsolutePath()), e);
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
        	String msg = String.format("Config Generation: '%s' - Could not find the XMLMerge properties file named: %s.", 
        			getSrcFileName(), resourceName);
            logger.debug(msg);
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
            	String msg = String.format("Config Generation: '%s' - Encountered an exception while converting InputSource array to InputStream array.",
            			getSrcFileName());
                logger.warn(msg, e);
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

        ArrayList<InputSource> inputSources = this.find_all_entities(includeTag); // try to find all the files that are part of the "src" attribute
        if (inputSources != null && inputSources.isEmpty() == false) {
            InputStream mergedStream = null;
            try {
                Properties xmlmergeProps = getXMLMergeProperties(includeTag.merge); //the "merge" attribute contains the name of the XMLMerge properties file
                String src = includeTag.src.replaceAll("\\s+","");
                mergedStream = merge(src.replace(',', '_'), // perform the merge
                        xmlmergeProps, toArray(inputSources));
            } catch (Throwable e) {
                String msg = String.format("Config Generation: '%s' - Error while processing %s.  Check the included files for syntax errors. : Could not merge the include files: '%s'.",
                		e.getMessage(), getSrcFileName(), includeTag.src);
                logger.error(msg);
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

    private IncludeTag isIncludeTag(String localName, Attributes attributes) throws SAXException, IOException {
        IncludeTag out = resolveIncludeTag(localName);
        if (out != null) {
            return out;
        }
        
        if (INCLUDE_TAG.equals(localName)) {
            out = new IncludeTag(attributes);
            return out;
        } else {
            return null;
        }
    }

    private static boolean stringToBoolean(String in) {
        return stringToBoolean(in, false);
    }
    
    /**
     * Returns the 'defaultValue' if not set (i.e., is NULL)
     * @param in
     * @param defaultValue
     * @return
     */
    private static boolean stringToBoolean(String in, boolean defaultValue) {
        if (in != null) {
            in = in.toLowerCase().trim();
            return (in.equals("yes") || in.equals("true"));
        }
        return defaultValue;
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

    private void processIncludeDefine(Attributes attr) throws SAXException, IOException {
        IncludeTag includeTag = new IncludeTag(attr);
                
        String name = attr.getValue("tag");
        if (name == null || "".equals(name)) {
            throw new SAXException("Tag has no name in definition");
        }
        include_tags.put(name, includeTag);
    }

    private boolean processDefines(String localName, Attributes attributes) throws SAXException, IOException {
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
        	src = src.trim();
            result = find_entity(src);
            if (result != null) {
                return result;
            }
        }

        throw new SAXException(all + " file(s) not found.");
    }

    private ArrayList<InputSource> find_all_entities(IncludeTag includeTag) throws SAXException, IOException {
    	String all = includeTag.src;
        ArrayList<InputSource> result = new ArrayList<InputSource>();
        ArrayList<String> missingFiles = new ArrayList<String>();
        
        InputSource inputSource = null;
        for (String src : all.split(",")) {
        	src = src.trim();
            inputSource = find_entity(src);
            if (inputSource != null) {
                result.add(inputSource);
            } else {
            	String msg = String.format("Config Generation: '%s' - Source file for \"Include\" tag could not be found: '%s'.", 
            			getSrcFileName(), src);
                logger.warn(msg);
                missingFiles.add(src);
            }
        }

        if (result.isEmpty() == true  || missingFiles.isEmpty() == false) {
        	String msg = String.format("Config Generation: '%s' - These source files for an \"Include\" tag could not be found: '%s'.",
        			getSrcFileName(), missingFiles.toString());
            logger.warn(msg);
            if (includeTag.isRequired() == true) {
            	throw new IOException(msg);
            }
        }

        return result;
    }

    @Override
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
        	IncludeTag includeTag = null;
            try {
                if (processDefines(localName, attributes)) {
                    return;
                }
                XSLTTag xslt = isXSLTTag(localName, attributes);
                includeTag = isIncludeTag(localName, attributes);
                if (localName.equals("cspace-config")) {
                    this.tenantname = attributes.getValue("tenantname");
                }
                if (xslt != null) {
                    apply_xslt(resolveEntity(null, xslt.src), xslt.root);
                } else if (includeTag != null) {
                    if ("@".equals(includeTag.src)) {
                        apply_include(parser.getMain(), includeTag.strip);
                    } else //apply_include(find_entity(include.src),include.strip); //put merge code here when "merge" REM
                    {
                        apply_include(includeTag);
                    }
                    logger.debug("Including: " + includeTag.src);
                } else {
                    up.startElement(uri, localName, qName, attributes);
                }
            } catch (IOException x) {
            	logger.error(x.getMessage());
                throw new SAXException("Could not load source from Include tag: " + includeTag);
            }
        }
    }

    @Override
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
        IncludeTag include;
		try {
			include = isIncludeTag(localName, new AttributesImpl());
		} catch (IOException e) {
			throw new SAXException(e);
		}
		
        if (include != null || xslt != null || DEFINE_TAG.equals(localName)) {
            return;
        }
        
        up.endElement(uri, localName, qName);
    }

    @Override
	public void characters(char[] ch, int start, int length) throws SAXException {
        if (delegated_depth > 0) {
            delegated.characters(ch, start, length);
        } else {
            up.characters(ch, start, length);
        }
    }

    @Override
	public void startDocument() throws SAXException {
        if (outer) {
            up.startDocument();
        }
    }

    @Override
	public void endDocument() throws SAXException {
        if (outer) {
            up.endDocument();
        }
    }

    @Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return parser.getEntityResolver().resolveEntity(publicId, systemId);
    }
}
