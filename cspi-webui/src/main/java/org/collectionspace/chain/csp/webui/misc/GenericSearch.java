package org.collectionspace.chain.csp.webui.misc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract search functionality
 * 
 * Abstract class to unify some of the generic features of search used in procedures and Authorities
 * 
 * @author csm22
 *
 */
public class GenericSearch {
	private static final Logger log=LoggerFactory.getLogger(GenericSearch.class);
        
        final static String UNESCAPED_PREVIOUS_CHAR_PATTERN = "(?<!\\\\)";
        final static String DOUBLE_QUOTE_PATTERN = "([\\\"])";
        final static String PERCENT_SIGN_PATTERN = "([\\%])";
        
        final static String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        final static String SERVICES_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * Returns the per field search structure needed by the service layer
	 * replaces UI wild cards * with service wild cards %
	 * 
	 * @param r
	 * @param fieldname
	 * @param value
	 * @param operator
	 * @param join
	 * @return
	 */
	public static String getAdvancedSearch(Record r, String fieldname, String value, String operator, String join, String affix){
		if(!value.equals("")){
			try{
				FieldSet fieldSet = r.getFieldFullList(fieldname);
				String section = fieldSet.getSection(); 	// Get the payload part
				String spath=r.getServicesRecordPath(section);
				String[] parts=spath.split(":",2);
                                
				// Escape various unescaped characters in the advanced search string
				value = escapeUnescapedChars(value, DOUBLE_QUOTE_PATTERN, "\"", "\\\"");
				value = escapeUnescapedChars(value, PERCENT_SIGN_PATTERN, "%", "\\%");
				
				// Replace user wildcards with service-legal wildcards
				if(value.contains("*")){
					value = value.replace("*", "%");
					join = " ilike ";
				}
				String fieldSpecifier = getSearchSpecifierForField(fieldSet, false);
				log.debug("Built XPath specifier for field: " + fieldname + " is: "+fieldSpecifier);
				
				return parts[0]+":"+fieldSpecifier+affix+join+"\""+value +"\""+ " " + operator+ " ";
			}
			catch(Exception e){
				log.error("Problem creating advanced search specifier for field: "+fieldname);
				log.error(e.getLocalizedMessage());
				return "";
			}
		}
		return "";
		
	}
        
       /**
        * Escapes unescaped characters within the text of a services advanced search string.
        * 
        * For the match patterns and replacement algorithm, see;
        * http://stackoverflow.com/a/5937852 and
        * http://docs.oracle.com/javase/6/docs/api/java/util/regex/Matcher.html#quoteReplacement%28java.lang.String%29
        * 
        * @param value         the original text of the search string.
        * @param matchPattern  a regex pattern to be matched.  This pattern MUST contain exactly
        *                      one matching group; text will be found and replaced within that group.
        * @param findText      some text to find within the matching group of the search string.
        * @param replaceText   the replacement text for the text found, if any, in that group.
        * @return  the text of the search string, with any character escaping performed.
        */
        private static String escapeUnescapedChars(String value, String matchPattern, String findText, String replaceText) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            StringBuffer sb = new StringBuffer("");
            try {
                final Pattern pattern = Pattern.compile(UNESCAPED_PREVIOUS_CHAR_PATTERN + matchPattern);
                final Matcher matcher = pattern.matcher(value);
                int groupCount = matcher.groupCount();
                if (groupCount != 1) {
                    log.warn("Match pattern must contain exactly one matching group. Pattern " + matchPattern
                            + " contains " + groupCount + " matching groups.");
                    return value;
                }
                while (matcher.find()) {
                    if (matcher.groupCount() >= 1) {
                      matcher.appendReplacement(sb, matcher.group(1).replace(findText,Matcher.quoteReplacement(replaceText)));
                    }
                }
                matcher.appendTail(sb);
            } catch (PatternSyntaxException pse) {
                log.warn("Invalid regular expression pattern " + matchPattern + ": " + pse.getMessage());
            }
            return sb.toString();
        }

	/**
	 * Pivots from the UI restriction concept to what the services needs. Initialises valriables if needed
	 * 
	 * @param ui
	 * @param param
	 * @param pageNum
	 * @param pageSize
	 * @param search
	 * @param r
	 * @return
	 * @throws UIException
	 * @throws JSONException
	 */
	public static JSONObject setRestricted(UIRequest ui, String param, String pageNum, String pageSize, Boolean search, Record r) throws UIException, JSONException{
		JSONObject returndata = new JSONObject();
		JSONObject restriction=new JSONObject();
		String key="results";
		if(param==null){
			key = "items";
		}

		//restriction.put("pageNum","0"); //initialise
		
		Set<String> args = ui.getAllRequestArgument();
		for(String restrict : args){
			if(!restrict.equals("_")){
				if(ui.getRequestArgument(restrict)!=null){
					String value = ui.getRequestArgument(restrict);
					if(restrict.equals(WebMethod.SEARCH_QUERY_PARAM) && search){
						restrict = "keywords";
						key="results";
					}
					if(restrict.equals(WebMethod.PAGE_SIZE_PARAM)||restrict.equals(WebMethod.PAGE_NUM_PARAM)){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("keywords")){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("sortDir")){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("sortKey")){////"summarylist.updatedAt"//movements_common:locationDate
						String[] bits = value.split("\\.");
						String fieldname = value;
						if(bits.length>1){
							fieldname = bits[1];
						}
						FieldSet fs = null;
						if(fieldname.equals("number")){
							fs = r.getMiniNumber();
						}
						else if(fieldname.equals("summary")){
							fs = r.getMiniSummary();
						}
						else{
							//convert sortKey
							fs = r.getFieldFullList(fieldname);
						}
						if(fs.hasMergeData()){ //if this field is made up of multi merged fields in the UI then just pick the first field to sort on as services doesn't search on merged fields.
							Field f = (Field)fs;
							for(String fm : f.getAllMerge()){
								if(fm!=null){
									fs = r.getFieldFullList(fm);
									break;
								}
							}
						}
						fieldname = fs.getID();
						FieldSet tmp = fs;
						fieldname = getSearchSpecifierForField(fs, true);
						

						String tablebase = r.getServicesRecordPath(fs.getSection()).split(":",2)[0];
						String newvalue = tablebase+":"+fieldname;
						restriction.put(restrict,newvalue);
					}
					else if(restrict.equals("query")){
						//ignore - someone was doing something odd
					}
					else{
						//XXX I would so prefer not to restrict and just pass stuff up but I know it will cause issues later
						restriction.put("queryTerm",restrict);
						restriction.put("queryString",value);
					}
				}
			}
		}
		
		if(param!=null && !param.equals("")){
			restriction.put("queryTerm", "kw");
			restriction.put("queryString",param);
			//restriction.put(r.getDisplayNameField().getID(),param);
		}
		if(pageNum!=null){
			restriction.put("pageNum",pageNum);
		}
		if(pageSize!=null){
			restriction.put("pageSize",pageSize);
		}
		returndata.put("key", key);
		returndata.put("restriction", restriction);
		return returndata;
	}
	
	/**
	 * Gets a list of all teh fields required in advanced search and creates the services search structure based on field type
	 * 
	 * @param r
	 * @param params
	 * @param restriction
	 * @throws JSONException
	 */
	public static void buildQuery(Record r, JSONObject params, JSONObject restriction)
			throws JSONException {
		Map<String, String> dates = new HashMap<String, String>();

		String operation = params.getString("operation").toUpperCase();
		JSONObject fields = params.getJSONObject("fields");
		log.debug("Advanced Search on fields: "+fields.toString());

		String asq = ""; 
		Iterator rit=fields.keys();
		while(rit.hasNext()) {
			String join = " ILIKE "; //using ilike so we can have case insensitive searches
			String fieldname=(String)rit.next();
			Object item = fields.get(fieldname);

			String value = "";
			
			if(item instanceof JSONArray){ // this is a repeatable
				JSONArray itemarray = (JSONArray)item;
				for(int j=0;j<itemarray.length();j++){
					JSONObject jo = itemarray.getJSONObject(j);
					Iterator jit=jo.keys();
					while(jit.hasNext()){
						String jname=(String)jit.next();
						if(!jname.equals("_primary")){
							if(jo.get(jname) instanceof String || jo.get(jname) instanceof Boolean ){
								value = jo.getString(jname);
								asq += getAdvancedSearch(r,jname,value,operation,join,"");
							}
						}
					}
				}
				
			}
			else if(item instanceof JSONObject){ // no idea what this is
				
			}
			else if(item instanceof String){
				value = (String)item;
				String affix = "";
				if(!value.equals("")){
					String fieldid = fieldname;
					if(r.hasSearchField(fieldname) && (r.getSearchFieldFullList(fieldname).getUIType().equals("date") || r.getSearchFieldFullList(fieldname).getUIType().equals("groupfield/structureddate"))){
						String timestampAffix = "T00:00:00Z";
						if(fieldname.endsWith("Start")){
							fieldid = fieldname.substring(0, (fieldname.length() - 5));
							join = ">= TIMESTAMP ";
							if(r.getSearchFieldFullList(fieldname).getUIType().equals("groupfield/structureddate") && r.getSearchFieldFullList(fieldname).getUIType().equals("groupfield/structureddate")){
								affix = "/dateEarliestScalarValue";
							}
						}
						else if(fieldname.endsWith("End")){
							fieldid = fieldname.substring(0, (fieldname.length() - 3));
							join = "<= TIMESTAMP ";
							timestampAffix = "T23:59:59Z";
							if(r.getSearchFieldFullList(fieldname).getUIType().equals("groupfield/structureddate") && r.getSearchFieldFullList(fieldname).getUIType().equals("groupfield/structureddate")){
								affix = "/dateLatestScalarValue";
							}
						}
						value += timestampAffix;
                                                value = utcToLocalTZ(value);

						if(dates.containsKey(fieldid)){
							String temp = getAdvancedSearch(r,fieldid,value,"AND",join, affix);
							String get = dates.get(fieldid);
							dates.put(fieldid, temp + get);
						}
						else{
							String temp = getAdvancedSearch(r,fieldid,value,"",join,affix);
							dates.put(fieldid, temp);
						}
					}
					else{
						asq += getAdvancedSearch(r,fieldname,value,operation,join,affix);
					}
				}
			}				
		}
		if(!dates.isEmpty()){
			for (String keyed : dates.keySet()) {
				if(!dates.get(keyed).equals("")){
					asq += " ( "+dates.get(keyed)+" )  "+ operation;	
				}
			}
		}
		
		if(!asq.equals("")){
			asq = asq.substring(0, asq.length()-(operation.length() + 2));
		}
		asq = asq.trim();
		if(!asq.equals("")){
			String asquery = "( "+asq+" )";
			restriction.put("advancedsearch", asquery);
		}
	}
        
       /**
        * Converts a timestamp in UTC to a timestamp in the system local time zone.
        * 
        * @param utcTimestamp  a timestamp in UTC.
        * @return the timestamp converted to the system local time zone.
        */
        private static String utcToLocalTZ(String utcTimestamp) {
            if (utcTimestamp == null || utcTimestamp.isEmpty()) {
                return utcTimestamp;
            }
            
            // Some of the following can be statically initialized or cached.
            // Note thread-safe considerations related to SimpleDateFormat, as described in:
            // http://www.codefutures.com/weblog/andygrove/2007/10/simpledateformat-and-thread-safety.html

            // Create date formatters
            SimpleDateFormat iso8601Format;
            SimpleDateFormat iso8601FormatWithMillis;
            SimpleDateFormat servicesFormat;
            try {
                iso8601Format = new SimpleDateFormat(ISO_8601_FORMAT);
                servicesFormat = new SimpleDateFormat(SERVICES_TIMESTAMP_FORMAT);
            } catch (Exception e) {
                log.warn("Invalid or null date format pattern: " + e.getLocalizedMessage());
                return utcTimestamp;
            }
            
            String localTimestamp = utcTimestamp;
            
            final String UTC_TIMEZONE_ID = "Etc/UTC";
            TimeZone utcTz = TimeZone.getTimeZone(UTC_TIMEZONE_ID);
            iso8601Format.setTimeZone(utcTz);
             // Get system local time zone and set the output formatter to use it
            TimeZone localTz = Calendar.getInstance().getTimeZone();
            servicesFormat.setTimeZone(localTz);
             
            try {
                Date utcDate = iso8601Format.parse(utcTimestamp);
                if (utcDate != null) {
                    localTimestamp = servicesFormat.format(utcDate);
                }
            } catch (Exception e) {
                log.warn("Error parsing UTC timestamp or formatting timestamp in local time zone: " + e.getLocalizedMessage());
                return utcTimestamp;
            }
            
            return localTimestamp;
            
        }

	/**
	 * Abstract the process for creating the traverser record
	 * There should be enough information here to repeat the search and get the next/previous set of results.
	 * @param ui
	 * @param recordID
	 * @param instanceID
	 * @param results
	 * @param restriction
	 * @param key
	 * @param numInstances
	 * @throws JSONException
	 * @throws UIException
	 */
	public static void createTraverser(UIRequest ui, String recordID, String instanceID, JSONObject results,
			JSONObject restriction, String key, Integer numInstances) throws 
			UIException {
		try{
			JSONObject traverser = new JSONObject();
			traverser.put("restriction", restriction);
			traverser.put("record", recordID);
			traverser.put("instance", instanceID);//not auth so no instance info
			traverser.put("total", Integer.valueOf(results.getJSONObject("pagination").getString("totalItems")));
			traverser.put("pageNum", Integer.valueOf(results.getJSONObject("pagination").getString("pageNum")));
			traverser.put("pageSize", Integer.valueOf(results.getJSONObject("pagination").getString("pageSize")));
			traverser.put("itemsInPage", Integer.valueOf(results.getJSONObject("pagination").getString("itemsInPage")));
			
			traverser.put("numInstances", numInstances);
			traverser.put("results", results.getJSONArray(key));
			
			String vhash = Generic.createHash(results.getJSONObject("pagination").getJSONArray("separatelists").toString() + restriction.toString());
			ui.getSession().setValue(UISession.SEARCHTRAVERSER+""+vhash,traverser);
			results.getJSONObject("pagination").put("traverser", vhash);
		}
		catch(JSONException ex){
			//can't do traversal as something is wrong e..g pagination data missing
		}
	}

	/**
	 * Returns an NXQL-conformant string that specifies the full (X)path to this field.
	 * May recurse to handle nested fields.
	 * This should probably live in Field.java, not here.
	 * 
	 * @param fieldname the name of the field
	 * @param fieldSet the containing fieldSet
	 * @return NXQL conformant specifier.
	 **/
	public static String getSearchSpecifierForField(FieldSet fieldSet, Boolean isOrderNotSearch) {
		//String specifier = fieldname;	// default is just the simple field name
		String specifier = fieldSet.getServicesTag();
		//this should be service tag not ID
		
		// Check for a composite (fooGroupList/fooGroup). For these, the name is the 
		// leaf, and the first part is held in the "services parent"
		if(fieldSet.hasServicesParent()) {
			// Prepend the services parent field, and make the child a wildcard
			String [] svcsParent = fieldSet.getServicesParent();
			if(svcsParent[0] != null && !svcsParent[0].isEmpty()) {
				specifier = svcsParent[0] + (isOrderNotSearch?"/0":"/*");
			}
		}
		
		FieldParent parent = fieldSet.getParent();

		boolean isRootLevelField = false;			// Assume we are recursing until we see otherwise
		if(parent instanceof Record) {	// A simple reference to base field.
			isRootLevelField = true;
			log.debug("Specifier for root-level field: " + specifier + " is: "+specifier);
		} else {
			FieldSet parentFieldSet = (FieldSet)parent;
			// "repeator" marks things for some expansion - not handled here (?)
			if(parentFieldSet.getSearchType().equals("repeator")) {
				isRootLevelField = true;
			} else {
				// Otherwise, we're dealing with some amount of nesting.
				// First, recurse to get the fully qualified path to the parent.
				String parentID = parentFieldSet.getID();
				log.debug("Recursing for parent: " + parentID );
				specifier = getSearchSpecifierForField(parentFieldSet,isOrderNotSearch);
							
				// Is parent a scalar list or a complex list?
				Repeat rp = (Repeat)parentFieldSet;
				FieldSet[] children = rp.getChildren("");
				int size = children.length;
				// HACK - we should really mark a repeating scalar as such, 
				// or a complex schema from which only 1 field is used, will break this.
				if(size > 1){
					// The parent is a complex schema, not just a scalar repeat
					// Append the field name to build an XPath-like specifier.
					specifier += "/"+fieldSet.getServicesTag();
				}
				else if(isOrderNotSearch) {
					  specifier += "/0";
				} else{
					// Leave specifier as is. We just search on the parent name,
					// as the backend is smart about scalar lists. 
				}
			}
			log.debug("Specifier for non-leaf field: " + fieldSet.getServicesTag() + " is: "+specifier);
		}
		if(isRootLevelField) {
			// TODO - map leaf names like "titleGroupList/titleGroup" to "titleGroupList/*"
		}
		return specifier;
	}

}
