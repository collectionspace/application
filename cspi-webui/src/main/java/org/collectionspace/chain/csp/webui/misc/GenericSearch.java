package org.collectionspace.chain.csp.webui.misc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.util.ArrayList;
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
/**
 * @author pschmitz
 *
 */
public class GenericSearch {
	private static final Logger log=LoggerFactory.getLogger(GenericSearch.class);
        
	final static String UNESCAPED_PREVIOUS_CHAR_PATTERN = "(?<!\\\\)";
	final static String DOUBLE_QUOTE_PATTERN = "([\\\"])";
	final static String PERCENT_SIGN_PATTERN = "([\\%])";

	final static String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	final static String SERVICES_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	final static int    RANGE_START_SUFFIX_LEN = Record.RANGE_START_SUFFIX.length();
	final static int    RANGE_END_SUFFIX_LEN   = Record.RANGE_END_SUFFIX.length();
	final static String TIMESTAMP_SUFFIX       = "T00:00:00Z";
	final static String EQUALS                 = " = ";
	final static String GTE                    = " >= ";
	final static String LTE                    = " <= ";
	final static String ILIKE_COMPARATOR       = " ILIKE ";
	final static String NOT_SPECIFIER          = " NOT ";
	final static String DATE_CAST              = " DATE ";
	final static String TIMESTAMP_CAST         = " TIMESTAMP ";
	public final static String SEARCH_RELATED_TO_CSID_AS_SUBJECT = "rtSbj";
	public final static String SEARCH_ALL_GROUP = "searchAllGroup";

	private static class RangeInfo {
		private final Logger logger = LoggerFactory.getLogger(RangeInfo.class);
		public String rangeStartValue = null;
		public String rangeEndValue = null;
		public String rangeStartOrSingleField = null;
		public String rangeEndField = null;				// If rangeEndField not null, then do interval compare
		
		public boolean isValid() {
			// One of the values, and at least the start/Single field spec must be set.
			return ((rangeStartValue != null) ||  (rangeEndValue != null))
				&& (rangeStartOrSingleField != null);
			// Okay for rangeEndField to be null
		}
	}

	
	/**
	 * Returns the per field search structure needed by the service layer
	 * replaces UI wild cards * with service wild cards %
	 * 
	 * @param r
	 * @param fieldname
	 * @param value
	 * @param operator
	 * @param comparator
	 * @return
	 */
	public static String getAdvancedSearch(Record r, String fieldname, FieldSet fieldSet, 
			String value, String wrapChar, String cast, String comparator){
		if(!value.equals("")){
			try{
				// Escape various unescaped characters in the advanced search string
				value = escapeUnescapedChars(value, DOUBLE_QUOTE_PATTERN, "\"", "\\\"");
				value = escapeUnescapedChars(value, PERCENT_SIGN_PATTERN, "%", "\\%");
				
				// Replace user wildcards with service-legal wildcards
				if(value.contains("*")){
					value = value.replace("*", "%");
				}
				String fieldSpecifier = getSchemaQualifiedSearchSpecifierForField(r, fieldname, fieldSet);
				log.debug("Built XPath specifier for field: " + fieldname + " is: "+fieldSpecifier);
				
				return "("+fieldSpecifier+comparator+cast+wrapChar+value+wrapChar+")";
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
	 * Returns the schema-qualified field path
	 * 
	 * @param r
	 * @param fieldname
	 * @param value
	 * @param operator
	 * @param comparator
	 * @return
	 */
	public static String getSchemaQualifiedSearchSpecifierForField(Record r, String fieldname, FieldSet fieldSet){
		String section = fieldSet.getSection(); 	// Get the payload part
		String spath=r.getServicesRecordPath(section);
		String[] parts=spath.split(":",2);

		String fieldSpecifier = getSearchSpecifierForField(fieldSet, false);

		return parts[0]+":"+fieldSpecifier;
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
					if (restrict.equals(WebMethod.PAGE_SIZE_PARAM)
							|| restrict.equals(WebMethod.PAGE_NUM_PARAM)
							|| restrict.equals(WebMethod.MARK_RELATED_QUERY_PARAM)
							|| restrict.equals("keywords")
							|| restrict.equals("sortDir")) {
						restriction.put(restrict, value);
					} else if(restrict.equals("sortKey")) {////"summarylist.updatedAt"//movements_common:locationDate
						if (r.isType("searchall")) {
							log.debug("Ignoring sortKey (nor supported on searchall record):"+value);
						} else {
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
								fs = r.getFieldFullList(fieldname); // CSPACE-4909: Getting null with fieldname = "movements_common:locationDate"
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
	 * @param r Current record spec to search on
	 * @param params Passed payload from caller
	 * @param restriction Services search restriction object into which to put query params
	 * @throws JSONException
	 */
	public static void buildQuery(Record r, JSONObject params, JSONObject restriction)
			throws JSONException {
		Map<String, String> dates = new HashMap<String, String>();

		String operation = params.getString("operation").toUpperCase();	// How to combine clauses
		String clauseCombiner = " "+operation.trim()+" ";	// slightly anal, but produces nice NXQL
		JSONObject fields = params.getJSONObject("fields");
		log.debug("Advanced Search on fields: "+fields.toString());

		HashMap<String,RangeInfo> rangeSpecs = new HashMap<String,RangeInfo>();
		StringBuilder queryStringBuilder = new StringBuilder(); 
		boolean fClauseAdded = false;
		Iterator outerListIter=fields.keys();
		while(outerListIter.hasNext()) {	// For each field specified
			String fieldName=(String)outerListIter.next();
			Object item = fields.get(fieldName);

			if(item instanceof JSONArray){ // this is a repeatable
				JSONArray itemarray = (JSONArray)item;
				for(int j=0;j<itemarray.length();j++){
					JSONObject innerList = itemarray.getJSONObject(j);
					Iterator innerListIter=innerList.keys();
					while(innerListIter.hasNext()){
						String innerListFieldName=(String)innerListIter.next();
						if(!innerListFieldName.equals("_primary")){
							Object innerItem = innerList.get(innerListFieldName); 
							String clause = buildQueryClauseForItem(r, innerListFieldName, innerItem, rangeSpecs);
							if(!clause.isEmpty()) {
								if(fClauseAdded) {
									queryStringBuilder.append(clauseCombiner);
								} else {
									fClauseAdded = true;
								}
								queryStringBuilder.append(clause);
							}
						}
					}
				}
			} else {
				String clause = buildQueryClauseForItem(r, fieldName, item, rangeSpecs);
				if(!clause.isEmpty()) {
					if(fClauseAdded) {
						queryStringBuilder.append(clauseCombiner);
					} else {
						fClauseAdded = true;
					}
					queryStringBuilder.append(clause);
				}
			}
		}
		// Now, we have to handle all the ranges they have specified. 
		Set<String> rangeSpecsKeys=rangeSpecs.keySet();
		for(String rangeFieldName:rangeSpecsKeys) {	// For each rangeSpec, by field name
			RangeInfo rangeSpec = rangeSpecs.get(rangeFieldName);
			String clause = null;
			if(!(rangeSpec.isValid())) {
				log.error("buildQuery only got partial spec for range on field: "+rangeFieldName);
			} else if(rangeSpec.rangeEndField == null) {
				// build query clause for simple range spec
				StringBuilder sb = new StringBuilder();
				sb.append("(");
				if(rangeSpec.rangeStartValue != null) {
					sb.append(rangeSpec.rangeStartOrSingleField);
					sb.append(GTE);
					sb.append(rangeSpec.rangeStartValue);
				}
				if(rangeSpec.rangeEndValue != null) {
					if(rangeSpec.rangeStartValue != null) {
						sb.append(" AND ");
					}
					sb.append(rangeSpec.rangeStartOrSingleField);
					sb.append(LTE);
					sb.append(rangeSpec.rangeEndValue);
				}
				sb.append(")");
				clause = sb.toString();
			} else {
				// build query clause for interval range spec
				// Note for intervals (a,b) and (c,d), overlap is defined as
				// a <= d && b >= c
				StringBuilder sb = new StringBuilder();
				sb.append("(");
				if(rangeSpec.rangeStartValue != null) {
					sb.append(rangeSpec.rangeEndField);
					sb.append(GTE);
					sb.append(rangeSpec.rangeStartValue);
				}
				if(rangeSpec.rangeEndValue != null) {
					if(rangeSpec.rangeStartValue != null) {
						sb.append(" AND ");
					}
					sb.append(rangeSpec.rangeStartOrSingleField);
					sb.append(LTE);
					sb.append(rangeSpec.rangeEndValue);
				}
				sb.append(")");
				clause = sb.toString();
			}
			if(clause != null) {
				if(fClauseAdded) {
					queryStringBuilder.append(clauseCombiner);
				} else {
					fClauseAdded = true;
				}
				queryStringBuilder.append(clause);
			}
		}
		
		String queryString = queryStringBuilder.toString().trim();
		if(!queryString.isEmpty()){
			String asquery = "( "+queryString+" )";
			restriction.put("advancedsearch", asquery);
		}
	}

	private static String timeValToTimestampQueryString(String value) {
		return TIMESTAMP_CAST + "'" + value + "'";
	}
	
	/**
	 * @param r The current record spec we are searching on 
	 * @param fieldName The name of the field from UI (may have range suffix)
	 * @param item The JSON object passed as the value
	 * @param rangeSpecs a map of field-name to String-Pairs, specifying begin and end
	 * @return
	 * @throws JSONException
	 */
	private static String buildQueryClauseForItem(Record r, String fieldName, Object item,
			HashMap<String,RangeInfo> rangeSpecs) throws JSONException {
		
		log.debug("buildQueryClauseForItem: "+fieldName);

		boolean isRangeStart = false;
		boolean isRangeEnd = false;
		RangeInfo rangeInfo = null;
		
		String queryClause = ""; 

		if(fieldName.endsWith(Record.RANGE_START_SUFFIX)) {
			fieldName = fieldName.substring(0, (fieldName.length() - RANGE_START_SUFFIX_LEN));
			isRangeStart = true;
		} else if(fieldName.endsWith(Record.RANGE_END_SUFFIX)) {
			fieldName = fieldName.substring(0, (fieldName.length() - RANGE_END_SUFFIX_LEN));
			isRangeEnd = true;
		}
		if(isRangeStart || isRangeEnd) {
			rangeInfo = rangeSpecs.get(fieldName);
			if(rangeInfo==null)
				rangeInfo = new RangeInfo();
		}
		
		//FieldSet fieldSet = r.getSearchFieldFullList(fieldName); This seems like it shoudl work, but many fields are not in this map!
		FieldSet fieldSet = r.getFieldFullList(fieldName);
		if(fieldSet==null) {
			log.error("buildQueryClauseForItem: fieldName does not map to a FieldSet:"+fieldName);
			return queryClause;
		}

		// Used, e.g., when a base entry field is used to compute the actual search field;
		// do not want to build query term for the base entry field 
		if(Field.QUERY_BEHAVIOR_IGNORE.equals(fieldSet.getQueryBehavior())) {
			log.error("buildQueryClauseForItem: QB_IGNORE on fieldName:"+fieldName);
			return queryClause;
		}
		
		if(item instanceof JSONArray || item instanceof JSONObject) { // Don't know how to handle this
			log.warn("GenericSearch.buildQuery ignoring unexpected field type for field: "
						+ fieldName + " [" + item.getClass().getName() + "]");
			// Leave queryClause empty and fall through
		} else if(fieldSet.getUIType().equals("date")) {	// UI returns String
			if(!(item instanceof String)) {
				log.error("GenericSearch.buildQuery field of type date not passed String value: "
						+ fieldName + " / value is: [" + item.getClass().getName() + "]");
				return queryClause;
			}
			String value = (String)item;
			if(!value.isEmpty()) {
	            value = utcToLocalTZ(value + TIMESTAMP_SUFFIX);
				if(isRangeStart) {
					rangeInfo.rangeStartValue = timeValToTimestampQueryString(value);
				} else if(isRangeEnd) {
					rangeInfo.rangeEndValue = timeValToTimestampQueryString(value);
				} else {	// Straight equals a date. May not be used currently
					queryClause = getAdvancedSearch(r,fieldName,fieldSet, value, "\"", 
													TIMESTAMP_CAST, EQUALS);
				}
				// Single field, not an interval
	            if(rangeInfo!=null) {
					if(rangeInfo.rangeStartOrSingleField==null) {
		            	rangeInfo.rangeStartOrSingleField = getSchemaQualifiedSearchSpecifierForField(r, fieldName, fieldSet); 
		            	rangeInfo.rangeEndField = null;
					}
					rangeSpecs.put(fieldName, rangeInfo);
					// Leave queryClause empty and fall through
	            }
			}
		} else if(fieldSet.getUIType().equals("groupfield/structureddate")) {	// UI returns String
			if(!(item instanceof String)) {
				log.error("GenericSearch.buildQuery field of type structured date not passed String value: "
						+ fieldName + " / value is: [" + item.getClass().getName() + "]");
				return queryClause;
			}
			String value = (String)item;
			if(!value.isEmpty()) {
	            value = utcToLocalTZ(value + TIMESTAMP_SUFFIX);
				if(isRangeStart) {
					rangeInfo.rangeStartValue = timeValToTimestampQueryString(value);
				} else if(isRangeEnd) {
					rangeInfo.rangeEndValue = timeValToTimestampQueryString(value);
				} else {	// Cannot do direct compare on structured date.
					log.error("GenericSearch.buildQuery field of type structured date not passed Range value: "
							+ fieldName );
					return queryClause;
				}
				// Specify interval values
	            if(rangeInfo!=null) {
					if(rangeInfo.rangeStartOrSingleField==null) {
						String searchSpec = getSchemaQualifiedSearchSpecifierForField(r, fieldName, fieldSet);
		            	rangeInfo.rangeStartOrSingleField = searchSpec+"/dateEarliestScalarValue";
						rangeInfo.rangeEndField = searchSpec+"/dateLatestScalarValue";
					}
					rangeSpecs.put(fieldName, rangeInfo);
					// Leave queryClause empty and fall through
	            }
			}
		} else if(item instanceof String) {	// Includes fields of types String, int, float, authRefs
			String value = (String)item;
			String wrapChar = getQueryValueWrapChar(fieldSet);
			if(!value.isEmpty()) {
				if(isRangeStart) {
					rangeInfo.rangeStartValue = wrapChar+value+wrapChar;
				} else if(isRangeEnd) {
					rangeInfo.rangeEndValue = wrapChar+value+wrapChar;
				} else {
					queryClause = getAdvancedSearch(r,fieldName,fieldSet, value, 
											getQueryValueWrapChar(fieldSet), "", ILIKE_COMPARATOR);
				}
				// These fields are all single - no intervals on basic fields
	            if(rangeInfo!=null) {
					if(rangeInfo.rangeStartOrSingleField==null) {
		            	rangeInfo.rangeStartOrSingleField = getSchemaQualifiedSearchSpecifierForField(r, fieldName, fieldSet); 
		            	rangeInfo.rangeEndField = null;
					}
					rangeSpecs.put(fieldName, rangeInfo);
					// Leave queryClause empty and fall through
	            }
			}
		} else if(item instanceof Boolean) { 
			boolean value = ((Boolean)item).booleanValue(); 
			queryClause = "(" + getSchemaQualifiedSearchSpecifierForField(r, fieldName, fieldSet)
							  + (value ? " = 1": " = 0") + ")";
		}
		
		return queryClause;
	}
	
	private static String getQueryValueWrapChar(FieldSet fs) {
		String datatype = "";
		if(fs != null && fs instanceof Field) {
			datatype = ((Field)fs).getDataType();
		}
		if(FieldSet.DATATYPE_INT.equals(datatype)
			|| FieldSet.DATATYPE_FLOAT.equals(datatype)) {	// Numeric - no quotes
			return "";
		}
		// Expand to handle boolean...
		// Assume String.
		return "\"";
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
