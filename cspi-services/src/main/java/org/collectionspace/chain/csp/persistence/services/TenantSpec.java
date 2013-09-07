package org.collectionspace.chain.csp.persistence.services;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
	private static final String DEFAULT_INDEX_HANDLER = "org.collectionspace.services.common.init.AddIndices";
	private String tenantId;
	private String tenant, tenantDisplay, tenantVersion;
	private String storageName;
	private String repositoryName;
	private String repositoryClient;
	private String repositoryDomain;
	private String indexHandler;
	private Set<String> languages = new LinkedHashSet<String>();
	private Set<String> dateformats = new LinkedHashSet<String>();
	private Set<String> defaultlanguages = new LinkedHashSet<String>();
	private Set<String> defaultdateformats = new LinkedHashSet<String>();	
	
	public TenantSpec(ReadOnlySection section) {
		repositoryDomain = "default-domain"; //FIXME:REM - This should not be hard coded.  Should be part of the App layer configuration
		tenantId = Util.getStringOrDefault(section,"/tenant/id","-1");
		tenant = Util.getStringOrDefault(section,"/tenant/name","collectionspace.org");
		tenantDisplay = Util.getStringOrDefault(section,"/tenant/display-name","CollectionSpace Demo");
		tenantVersion = Util.getStringOrDefault(section,"/tenant/version","1.0");
		storageName = Util.getStringOrDefault(section,"/repository/domain", repositoryDomain);
		repositoryName = Util.getStringOrDefault(section,"/repository/name", "");
		repositoryClient = Util.getStringOrDefault(section,"/repository/client", "nuxeo-java");
		indexHandler = Util.getStringOrDefault(section,"/repository/indexHandler", DEFAULT_INDEX_HANDLER);
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
	
	public String getRepositoryDomain(){
		return repositoryDomain;
	}
	
	public String getIndexHandler() {
		return indexHandler;
	}

	public String getRepositoryClient(){
		return repositoryClient;
	}

	public String getStorageName(){
		return storageName;
	}
        
        public String getRepositoryName(){
		return repositoryName;
	}

	public String getTenant(){
		return tenant;
	}

	public String getTenantId(){
		return tenantId;
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
