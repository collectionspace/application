package org.collectionspace.chain.csp.persistence.services.vocab;

import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.GenericRecordStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.chain.util.xtmpl.XTmplDocument;
import org.collectionspace.chain.util.xtmpl.XTmplTmpl;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONException;
import org.json.JSONObject;

public class ServicesVocabStorage implements ContextualisedStorage {
	private ServicesConnection conn;
	private Map<String,String> csids=new HashMap<String,String>();
	private XTmplTmpl create_vocab,create_entry;

	private static Map<String,String> vocabs=new ConcurrentHashMap<String,String>();
	private static Pattern urn_syntax=Pattern.compile("(.*?)/urn:cspace:org.collectionspace.demo:vocabulary\\((.*?)\\):item\\((.*?)\\)'(.*?)'");

	
	static {
		vocabs.put("name","Default Name Authority");
	}

	// XXX refactor
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	// XXX refactor
	private Document getDocument(String name) throws DocumentException {
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		return reader.read(getResource(name));
	}

	public ServicesVocabStorage(ServicesConnection conn) throws InvalidXTmplException, DocumentException {
		this.conn=conn;
		create_vocab=XTmplTmpl.compile(getDocument("create_vocab.xtmpl"));
		create_entry=XTmplTmpl.compile(getDocument("create_entry.xtmpl"));
	}

	private String confound(String name) throws ExistException {
		if(!vocabs.containsKey(name))
			throw new ExistException("No such vocab "+name);
		return vocabs.get(name)+" ("+name+")";
	}
	
	private String unconfound(String name) {
		if(name==null)
			return null;
		if(!name.endsWith(")"))
			return null;
		int pos=name.lastIndexOf('(');
		if(pos==-1)
			return null;
		String rest=name.substring(pos+1);
		return rest.substring(0,rest.length()-1);
	}
	
	// Only called if doesn't exist
	private synchronized void createVocabulary(CSPRequestCache cache,String id) throws ConnectionException, UnderlyingStorageException, ExistException {
		XTmplDocument doc=create_vocab.makeDocument();
		doc.setText("name",confound(id));
		Map<String,Document> body=new HashMap<String,Document>();
		body.put("vocabularies_common",doc.getDocument());
		ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/vocabularies/",body);
		if(out.getStatus()>299)
			throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
		csids.put(id,out.getURLTail());
	}

	@SuppressWarnings("unchecked")
	private void buildVocabularies(CSPRequestCache cache) throws ConnectionException, UnderlyingStorageException {
		ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/vocabularies/",null);
		Document doc=data.getDocument();
		if(doc==null)
			throw new UnderlyingStorageException("Could not retrieve vocabularies");
		List<Node> objects=doc.getDocument().selectNodes("vocabularies-common-list/vocabulary-list-item");
		for(Node object : objects) {
			String name=object.selectSingleNode("displayName").getText();
			String base=unconfound(name);
			if(base==null)
				continue;
			if(!vocabs.containsKey(base))
				continue;
			csids.put(base,object.selectSingleNode("csid").getText());
		}
	}
	
	private String getVocabularyId(CSPRequestCache cache,String id) throws ConnectionException, UnderlyingStorageException, ExistException {
		if(csids.containsKey(id))
			return csids.get(id);
		synchronized(getClass()) {
			buildVocabularies(cache);
			if(csids.containsKey(id))
				return csids.get(id);
			createVocabulary(cache,id);
			if(csids.containsKey(id))
				return csids.get(id);
			throw new UnderlyingStorageException("Bad vocabulary "+id);
		}
	}
	
	private String constructURN(CSPRequestCache cache,String vocab_id,String entry_id,String display) throws UnderlyingStorageException, ConnectionException, ExistException {
		try {
			return "urn:cspace:org.collectionspace.demo:vocabulary("+URLEncoder.encode(vocab_id,"UTF-8")+"):item("+
				URLEncoder.encode(entry_id,"UTF-8")+")'"+URLEncoder.encode(display,"UTF-8")+"'";
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?");
		}
	}
	
	private String[] deconstructURN(CSPRequestCache cache,String urn) throws ExistException {
		Matcher m=urn_syntax.matcher(urn);
		if(!m.matches())
			throw new ExistException("Bad URN, does not exist");
		String[] out=new String[3];
		return new String[]{m.group(1),m.group(2),m.group(3),m.group(4)};
	}
	
	public String autocreateJSON(CSPRequestCache cache,String filePath,JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			if(!jsonObject.has("name"))
				throw new UnderlyingStorageException("Missing name argument to data");
			String name=jsonObject.getString("name");
			String vocab=getVocabularyId(cache,filePath);
			XTmplDocument doc=create_entry.makeDocument();
			doc.setText("name",name);
			doc.setText("vocab",vocab);
			Map<String,Document> body=new HashMap<String,Document>();
			body.put("vocabularyitems_common",doc.getDocument());
			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/vocabularies/"+vocab+"/items",body);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
			return constructURN(cache,vocab,out.getURLTail(),name);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON",e);
		}
	}

	// XXX harness
	public void createJSON(CSPRequestCache cache, String filePath,JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnderlyingStorageException("Cannot create at named path");
	}

	public void deleteJSON(CSPRequestCache cache, String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		// TODO Auto-generated method stub

	}

	public String[] getPaths(CSPRequestCache cache,String rootPath,JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	private String URNtoURL(CSPRequestCache cache,String path) throws ExistException, ConnectionException, UnderlyingStorageException {
		String[] parts=deconstructURN(cache,path);
		String vocab=getVocabularyId(cache,parts[0]);
		if(!vocab.equals(parts[1]))
			throw new ExistException("Not in this vocabulary");
		return "/vocabularies/"+parts[1]+"/items/"+parts[2];
	}

	private String URNtoVocab(CSPRequestCache cache,String path) throws ExistException, ConnectionException, UnderlyingStorageException {
		String[] parts=deconstructURN(cache,path);
		return getVocabularyId(cache,parts[0]);
	}
	
	public JSONObject retrieveJSON(CSPRequestCache cache, String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {			
			ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,URNtoURL(cache,filePath),null);
			if(doc.getStatus()>299)
				throw new UnderlyingStorageException("Could not retrieve vocabulary status="+doc.getStatus());
			JSONObject out=new JSONObject();
			out.put("name",doc.getDocument("vocabularyitems_common").selectSingleNode("vocabularyitems_common/displayName").getText());
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot generate JSON",e);
		}
	}

	public void updateJSON(CSPRequestCache cache,String filePath,JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			if(!jsonObject.has("name"))
				throw new UnderlyingStorageException("Missing name argument to data");
			String name=jsonObject.getString("name");
			XTmplDocument doc=create_entry.makeDocument();
			doc.setText("name",name);
			doc.setText("vocab",URNtoVocab(cache,filePath));
			Map<String,Document> body=new HashMap<String,Document>();
			body.put("vocabularyitems_common",doc.getDocument());
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,URNtoURL(cache,filePath),body);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON",e);
		}
	}
}
