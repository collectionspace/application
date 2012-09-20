package org.collectionspace.chain.csp.persistence.services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RefName {

	/*
    public static final String HACK_VOCABULARIES = "Vocabularies"; //TODO: get rid of these.
    public static final String HACK_ORGANIZATIONS = "Organizations"; //TODO: get rid of these.
    public static final String HACK_ORGAUTHORITIES = "Orgauthorities";  //TODO: get rid of these.
    public static final String HACK_PERSONAUTHORITIES = "Personauthorities";  //TODO: get rid of these.
    public static final String HACK_LOCATIONAUTHORITIES = "Locationauthorities";  //TODO: get rid of these.
    */
    public static final String URN_PREFIX = "urn:cspace:";
    public static final String URN_NAME_PREFIX = "urn:cspace:name";
    public static final String REFNAME = "refName";
    public static final String AUTHORITY_REGEX = "urn:cspace:(.*):(.*):name\\((.*)\\)\\'?([^\\']*)\\'?";
    public static final String CSID_REFNAME_REGEX = "urn:cspace:(.*):(.*):id\\((.*)\\)\\'?([^\\']*)\\'?";
    public static final String AUTHORITY_ITEM_REGEX = "urn:cspace:(.*):(.*):name\\((.*)\\):item:name\\((.*)\\)\\'?([^\\']*)\\'?";
    public static final String NAME_SPECIFIER = "name";
    public static final String ID_SPECIFIER = "id";
    /*
    public static final String AUTHORITY_EXAMPLE = "urn:cspace:collectionspace.org:Loansin:name(shortID)'displayName'";
    public static final String AUTHORITY_EXAMPLE2 = "urn:cspace:collectionspace.org:Loansin:name(shortID)";
    public static final String AUTHORITY_ITEM_EXAMPLE = "urn:cspace:collectionspace.org:Loansin:name(shortID):item:name(itemShortID)'itemDisplayName'";
    public static final String EX_tenantName = "collectionspace.org";
    public static final String EX_resource = "Loansin";
    public static final String EX_shortIdentifier = "shortID";
    public static final String EX_displayName = "displayName";
    public static final String EX_itemShortIdentifier = "itemShortID";
    public static final String EX_itemDisplayName = "itemDisplayName";
     */

    public static class Authority {

        public String tenantName = "";
        public String resource = "";
        public String csid = "";
        public String shortIdentifier = "";
        public String displayName = "";

        public static Authority parse(String urn) {
            Authority info = new Authority();
            Pattern p = Pattern.compile(AUTHORITY_REGEX);
            Matcher m = p.matcher(urn);
            if (m.find()) {
                if (m.groupCount() < 4) {
                    return null;
                }
                info.tenantName = m.group(1);
                info.resource = m.group(2);
                info.shortIdentifier = m.group(3);
                info.displayName = m.group(4);
                return info;
            } else {
                p = Pattern.compile(CSID_REFNAME_REGEX);
                m = p.matcher(urn);
                if (m.find()) {
                    if (m.groupCount() < 4) {
                        return null;
                    }
                    info.tenantName = m.group(1);
                    info.resource = m.group(2);
                    info.csid = m.group(3);
                    info.displayName = m.group(4);
                    return info;
                }
            }
            return null;
        }
        
        public String getShortIdentifier() {
            return this.shortIdentifier;
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof Authority) {
                Authority ao = (Authority) other;
                return (this.tenantName.equals(ao.tenantName)
                        && this.resource.equals(ao.resource)
                        && this.shortIdentifier.equals(ao.shortIdentifier));
            } else {
                return false;
            }
        }

        public String getRelativeUri() {
            return "/" + resource + "/" + URN_NAME_PREFIX + "(" + shortIdentifier + ")";
        }

        public String toString() {
            String displaySuffix = (displayName != null && (!displayName.isEmpty())) ? '\'' + displayName + '\'' : "";
            //return URN_PREFIX + tenantName + ':' + resource + ":" + "name" + "(" + shortIdentifier + ")" + displaySuffix;
        	StringBuilder sb = new StringBuilder();
        	sb.append(URN_PREFIX);
        	sb.append(tenantName);
        	sb.append(':');
        	sb.append(resource);
        	sb.append(':');
        	if(csid!=null) {
            	sb.append(ID_SPECIFIER);
            	sb.append("(");
            	sb.append(csid);
            	sb.append(")");
        	} else if(shortIdentifier!= null) {
            	sb.append(NAME_SPECIFIER);
            	sb.append("(");
            	sb.append(shortIdentifier);
            	sb.append(")");
        	} else {
        		throw new NullPointerException("Authority has neither CSID nor shortID!");
        	}
        	sb.append(displaySuffix);
            return sb.toString();
        }
    }

    public static class AuthorityItem {

        public Authority inAuthority;
        public String shortIdentifier = "";
        public String displayName = "";

        public static AuthorityItem parse(String urn) {
            Authority info = new Authority();
            AuthorityItem termInfo = new AuthorityItem();
            termInfo.inAuthority = info;
            Pattern p = Pattern.compile(AUTHORITY_ITEM_REGEX);
            Matcher m = p.matcher(urn);
            if (m.find()) {
                if (m.groupCount() < 5) {
                    return null;
                }
                termInfo.inAuthority.tenantName = m.group(1);
                termInfo.inAuthority.resource = m.group(2);
                termInfo.inAuthority.shortIdentifier = m.group(3);
                termInfo.shortIdentifier = m.group(4);
                termInfo.displayName = m.group(5);
                return termInfo;
            }
            return null;
        }
        
        public String getParentShortIdentifier() {
            return this.inAuthority.shortIdentifier;
        }
        
        public String getShortIdentifier() {
            return this.shortIdentifier;
        }
    
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof AuthorityItem) {
                AuthorityItem aio = (AuthorityItem) other;
                boolean ok = true;
                ok = ok && aio.inAuthority != null;
                ok = ok && aio.inAuthority.equals(this.inAuthority);
                ok = ok && aio.shortIdentifier.equals(this.shortIdentifier);
                ok = ok && aio.displayName.equals(this.displayName);
                return ok;
            } else {
                return false;
            }
        }

        public String getRelativeUri() {
            return inAuthority.getRelativeUri() + "/items/" + URN_NAME_PREFIX + "(" + shortIdentifier + ")";
        }

        public String toString() {
            String displaySuffix = (displayName != null && (!displayName.isEmpty())) ? '\'' + displayName + '\'' : "";
            Authority ai = inAuthority;
            if (ai == null) {
                return URN_PREFIX + "ERROR:inAuthorityNotSet: (" + shortIdentifier + ")" + displaySuffix;
            } else {
                String base = URN_PREFIX + ai.tenantName + ':' + ai.resource + ":" + "name" + "(" + ai.shortIdentifier + ")";
                String refname = base + ":item:name(" + shortIdentifier + ")" + displaySuffix;
                return refname;
            }
        }
    }

    /* hack to play local to simplify development */
    public static class Tools {
        /** Handles null strings as empty.  */
        public static boolean notEmpty(String str){
            if (str==null) return false;
            if (str.length()==0) return false;
            return true;
        }
    }
    
    public static Authority buildAuthority(String tenantName, String serviceName, String authorityShortIdentifier, String authorityDisplayName) {
        Authority authority = new Authority();
        authority.tenantName = tenantName;
        authority.resource = serviceName;
        if (Tools.notEmpty(authority.resource)) {
            authority.resource = authority.resource.toLowerCase();
        }
        authority.shortIdentifier = authorityShortIdentifier;
        authority.displayName = authorityDisplayName;
        return authority;
    }

    public static AuthorityItem buildAuthorityItem(String tenantName, String serviceName, String authorityShortIdentifier,
            String itemShortIdentifier, String itemDisplayName) {
        Authority authority = buildAuthority(tenantName, serviceName, authorityShortIdentifier, "");
        return buildAuthorityItem(authority, itemShortIdentifier, itemDisplayName);
    }

    public static AuthorityItem buildAuthorityItem(String authorityRefName, String itemShortID, String itemDisplayName) {
        Authority authority = Authority.parse(authorityRefName);
        AuthorityItem item = buildAuthorityItem(authority, itemShortID, itemDisplayName);
        return item;
    }

    public static AuthorityItem buildAuthorityItem(Authority authority, String itemShortIdentifier, String itemDisplayName) {
        AuthorityItem item = new AuthorityItem();
        item.inAuthority = authority;
        item.shortIdentifier = itemShortIdentifier;
        item.displayName = itemDisplayName;
        return item;
    }

    /** Use this method to avoid formatting any urn's outside of this unit;
     * Caller passes in a shortId, such as "TestAuthority", and method returns
     * the correct urn path element, without any path delimiters such as '/'
     * so that calling shortIdToPath("TestAuthority") returns "urn:cspace:name(TestAuthority)", and
     * then this value may be put into a path, such as "/personauthorities/urn:cspace:name(TestAuthority)/items".
     */
    public static String shortIdToPath(String shortId) {
        return URN_NAME_PREFIX + '(' + shortId + ')';
    }
    
    public static String getDisplayName(String refName) {
    	Authority authority = Authority.parse(refName);
    	return authority==null?null:authority.displayName;
    }
}
