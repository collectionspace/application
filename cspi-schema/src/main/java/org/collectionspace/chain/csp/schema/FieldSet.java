/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.List;

public interface FieldSet {
	public static final String NS = "ns:"; // Namespace prefix for all XML Schema .xsd files
	public static final String XS = "xs:";
	
	public static final String QUERY_BEHAVIOR_NORMAL = "normal";
	public static final String QUERY_BEHAVIOR_IGNORE = "ignore";
	public static final String DATATYPE_STRING = "string";
	public static final String DATATYPE_LARGETEXT = "largetext";	
	public static final String DATATYPE_INT = "integer";
	public static final String DATATYPE_FLOAT = "float";
	public static final String DATATYPE_DECIMAL = "decimal";
	public static final String DATATYPE_BOOLEAN = "boolean";
	
	public String getID();
	public String[] getIDPath();
	
	public SchemaUtils getUtils();
	public FieldParent getParent();
	public String getParentID();
	public void setParent(FieldParent fp);
	public Record getRecord();

	/*
	 * Methods specific to Service artifact generation
	 */
	public boolean isServicesDerived();
	public boolean isServicesRefnameDisplayName();
	public String getServicesSetter();
	public boolean shouldSchemaQualify();
	public String getServicesTag();
	public String getServiceTableName(boolean isAuthority);
	public String getServicesUrl();
	public String[] getServicesParent();
	public boolean hasServicesParent();
	public String getServiceFieldAlias();
	public Boolean isInServices();
	public Boolean isServicesAnonymousType();
	public Boolean isAuthRefInServices();
	public Boolean excludeFromServicesList();
	
	public String getSection();	
	public boolean hasFieldPerm(String k);
	public String[] getAllFieldOperations();

	public void config_finish(Spec spec);
	
	//subrecords
	public String getSelectorAffix();
	public String getUISpecPrefix();
	public Boolean getUISpecInherit();
	public String getUILabelSelector();
	public String getLabelAffix();
	public String getContainerSelector();
	public String getDecoratorSelector();
	public String getPreContainerSelector();
	public String getTitleSelector();
	public String getPreTitleSelector();
	public String getSelector();
	public String getPreSelector();
	public String getLabel();
	//used in generateGroupField in uispec for elpaths
	public String getPrimaryKey();
	public Boolean usesRecord();
	public String usesRecordValidator();
	public Record usesRecordId();
	/**
	 * UI specific marking: YURA said: 
	 * these are renderer decorators that do their own rendering so need some sub nesting
	 * @param fs
	 * @return
	 */
	final static String SELFRENDERER = "selfrenderer";
	public boolean isASelfRenderer();
	public Record getSelfRendererRecord();
	
	//
	public Boolean isAGroupField();
	public Boolean isAStructureDate();	
	public Boolean hasAutocompleteInstance();
	public Boolean hasMergeData();
	public List<String> getAllMerge();
	public boolean isExpander();
	public boolean isConditionExpander();
	public boolean isRepeatSubRecord();
	public boolean isReadOnly();
	public boolean isServicesReadOnly();
	public void setRepeatSubRecord(Boolean var);
	public String getUIType();
	public String getQueryBehavior();
	
	public String getUIFunc();
	public String getWithCSID();
	public String getSearchType();
	
	public void setServicesType(String servicesType);
	public String getServicesType();
	public String getServicesType(boolean namespaceQualified);
	
	public String getServicesGroupType(boolean namespaceQualified);
	public void setServicesGroupType(String servicesGroupType);
	public Boolean shouldIndex();
}
