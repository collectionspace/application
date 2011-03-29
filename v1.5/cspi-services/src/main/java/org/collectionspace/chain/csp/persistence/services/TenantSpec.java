package org.collectionspace.chain.csp.persistence.services;
import java.util.HashSet;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.schema.Util;

/**
 * 
    <persistence>
        <service>
            <url>http://nightly.collectionspace.org:8180/cspace-services</url>
			<tenant>
				<name>hearstmuseum.berkeley.edu</name>
				<version>0.1</version>
				<display-name>Phoebe A. Hearst Museum of Anthropology</display-name>
			</tenant>
			<repository>
				<domain>pahma-domain</domain>
				<client>nuxeo-java</client>
				<dateformats>
					<pattern>MM/dd/yyyy</pattern>
					<pattern>dd.MM.yyyy</pattern>
				</dateformats>
				<languages>
					<language>en</language>
				</languages>
			</repository>
        </service>
    </persistence>
 * 
 * 
 * @author csm22
 *
 */
public class TenantSpec {
	private String tenant, tenantDisplay, tenantVersion;
	private String repoDomain, repoClient;
	private String defaultDomain;
	private Set<String> languages = new HashSet<String>();
	private Set<String> dateformats = new HashSet<String>();
	private Set<String> defaultlanguages = new HashSet<String>();
	private Set<String> defaultdateformats = new HashSet<String>();
	
	
	public TenantSpec(ReadOnlySection section) {
		defaultDomain = "default-domain";
		tenant = Util.getStringOrDefault(section,"/tenant/name","collectionspace.org");
		tenantDisplay = Util.getStringOrDefault(section,"/tenant/display-name","CollectionSpace Demo");
		tenantVersion = Util.getStringOrDefault(section,"/tenant/version","1.0");
		repoDomain = Util.getStringOrDefault(section,"/repository/domain",defaultDomain);
		repoClient = Util.getStringOrDefault(section,"/repository/client","nuxeo-java");
		defaultlanguages.add("en");
		defaultdateformats.add("MM/dd/yyyy");
		defaultdateformats.add("MMM dd, yyyy");
		defaultdateformats.add("dd.MM.yyyy");
	}

	public void addLanguage(String lang){
		languages.add(lang);
	}
	
	public void addFormat(String format){
		dateformats.add(format);
	}

	public String[] getLanguages(){
		if(languages.size()>0){
			return languages.toArray(new String[0]);
		}
		return defaultlanguages.toArray(new String[0]);
	}

	public Boolean isDefaultDate(){
		if(dateformats.size()>0){
			return false;
		}
		return true;		
	}

	public Boolean isDefaultLanguage(){
		if(languages.size()>0){
			return false;
		}
		return true;		
	}
	
	public String[] getDateFormats(){
		if(dateformats.size()>0){
			return dateformats.toArray(new String[0]);
		}
		return defaultdateformats.toArray(new String[0]);
	}
	
	public String getDefaultDomain(){
		return defaultDomain;
	}

	public String getRepoClient(){
		return repoClient;
	}

	public String getRepoDomain(){
		return repoDomain;
	}

	public String getTenant(){
		return tenant;
	}

	public String getTenantVersion(){
		return tenantVersion;
	}

	public String getTenantDisplay(){
		return tenantDisplay;
	}
	
	public TenantSpec getTenantData() {
		return this;
	}
}
