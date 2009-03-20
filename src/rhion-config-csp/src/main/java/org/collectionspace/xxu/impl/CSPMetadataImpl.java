package org.collectionspace.xxu.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.xxu.api.CSPDependency;
import org.collectionspace.xxu.api.CSPMetadata;
import org.collectionspace.xxu.api.CSPProvider;
import org.collectionspace.xxu.api.ConfigLoadingException;
import org.collectionspace.xxu.api.CSPProviderFactory;
import org.dom4j.Document;
import org.dom4j.Node;

// XXX CSPs create providers
public class CSPMetadataImpl implements CSPMetadata {
	private static final String VERSION="0.0";
	private static final Pattern version_pattern=Pattern.compile("^(\\d+)\\.(\\d+)$");
	
	private String identity,human_id,title,author,author_url,csp_url,description;
	private int major,minor;
	private CSPDependency[][] dependencies;
	private List<CSPProvider> provides=new ArrayList<CSPProvider>();
	private CSPImpl csp;
		
	private void notNull(Object in,String name) throws ConfigLoadingException {
		if(in!=null)
			return;
		throw new ConfigLoadingException(name+" is missing from metadata file");
	}
	
	private void string(Object in,String name) throws ConfigLoadingException {
		if(!(in instanceof String))
			throw new ConfigLoadingException(name+" must be an alphanumeric string");		
	}
	
	private void alnum_string(Object in,String name) throws ConfigLoadingException {
		notNull(in,name);
		string(in,name);
		for(char c : ((String)in).toCharArray())
			if(!Character.isLetter(c) && !Character.isDigit(c) && c!='-')
				throw new ConfigLoadingException(name+" must be alphanumeric or - throughout");
	}
	
	private void parseVersion(String in) throws ConfigLoadingException {
		Matcher m=version_pattern.matcher(in);
		if(!m.matches())
			throw new ConfigLoadingException("Badly formatted version string, must be [0-9]+.[0-9]+");
		major=Integer.parseInt(m.group(1));
		minor=Integer.parseInt(m.group(2));
	}
	
	private CSPDependency parseDependency(Node n) throws ConfigLoadingException {
		String identity=n.valueOf("@identity");
		alnum_string(identity,"dependency identity");
		String version=n.valueOf("@version");
		string(version,"version");
		Matcher m=version_pattern.matcher(version);
		if(!m.matches())
			throw new ConfigLoadingException("Badly formatted dependency version string, must be [0-9]+.[0-9]+");
		int maj=Integer.parseInt(m.group(1));
		int min=Integer.parseInt(m.group(2));
		return new CSPDependencyImpl(identity,maj,min);
	}
	
	@SuppressWarnings("unchecked")
	public CSPMetadataImpl(CSPImpl csp,Document doc) throws ConfigLoadingException {
		this.csp=csp;
		// Metadata
		String csp_version=doc.valueOf("csp/@version");
		notNull(csp_version,"version");
		if(!VERSION.equals(csp_version))
			throw new ConfigLoadingException("Unrecognised CSP version "+csp_version);
		identity=doc.valueOf("csp/identity");
		alnum_string(identity,"identity");
		String version=doc.valueOf("csp/version");
		string(version,"version");
		parseVersion(version);
		human_id=doc.valueOf("csp/short-name");
		string(human_id,"short-name");
		title=doc.valueOf("csp/title");
		author=doc.valueOf("csp/author");
		author_url=doc.valueOf("csp/author-url");
		csp_url=doc.valueOf("csp/url");
		description=doc.valueOf("csp/description");
		List<CSPDependency[]> deps=new ArrayList<CSPDependency[]>();
		for(Node dep : (List<Node>)doc.selectNodes("csp/dependencies/dependency")) {
			CSPDependency[] d=new CSPDependency[1];
			d[0]=parseDependency(dep);
			deps.add(d);
		}
		for(Node alts : (List<Node>)doc.selectNodes("csp/dependencies/alternatives")) {
			List<Node> d_in=(List<Node>)alts.selectNodes("dependency");
			CSPDependency[] d_out=new CSPDependency[d_in.size()];
			for(int i=0;i<d_out.length;i++)
				d_out[i]=parseDependency(d_in.get(i));
			deps.add(d_out);
		}
		dependencies=(CSPDependency[][])deps.toArray(new CSPDependency[0][]);
		// Providers (delegate)
		CSPProviderFactory[] pfacts=csp.getLoader().getProviderFactories();
		for(Node provs : (List<Node>)doc.selectNodes("csp/provides/*")) {
			for(CSPProviderFactory pf : pfacts) {
				CSPProvider pv=pf.process(csp,provs);
				if(pv!=null)
					provides.add(pv);
			}
		}
		// XXX priority		
	}
	
	void act(CSPImpl csp,ConfigLoaderImpl in) throws ConfigLoadingException {
		for(CSPProvider p : provides)
			p.act(in);
	}
	
	public String getAuthor() { return author; }
	public String getAuthorURL() { return author_url; }
	public String getCSPURL() { return csp_url; }
	public String getHumanDescription() { return description; }
	public String getHumanID() { return human_id; }
	public String getHumanTitle() { return title; }
	public String getIdentity() { return identity; }
	public int getMajorVersion() { return major; }
	public int getMinorVersion() { return minor; }
	public CSPDependency[][] getDependencies() { return dependencies; }
	public CSPProvider[] getProvider() { return provides.toArray(new CSPProvider[0]); }
}
