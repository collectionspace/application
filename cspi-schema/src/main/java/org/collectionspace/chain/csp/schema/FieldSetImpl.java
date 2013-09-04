package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class FieldSetImpl implements FieldSet {
	
	private static final Logger logger = LoggerFactory.getLogger(FieldSetImpl.class);
	
	protected SchemaUtils utils = new SchemaUtils();
	
	protected void initialiseVariables(ReadOnlySection section, String tempid) {
		utils.initBoolean(section,"@services-list-exclude", false);
		utils.initBoolean(section,"@services-derived", false);
		utils.initBoolean(section,"@services-refnameDisplayName", false);
		utils.initStrings(section,"@services-setter", null);
		utils.initBoolean(section,"@services-schema-qualify", false);
		utils.initBoolean(section,"@services-type-anonymous", true); // generate an embedded anonymous type instead of a standalone complex type
		if (section != null) {
			String servicesType = (String)section.getValue("/@services-type");
			if (servicesType != null && servicesType.isEmpty() == false) {
				this.setServicesType(servicesType);
			}
			String servicesGroupType = (String)section.getValue("/@services-group-type");
			if (servicesGroupType != null && servicesGroupType.isEmpty() == false) {
				this.setServicesGroupType(servicesGroupType);
			}
		}
		
	}
	
	@Override
	public Boolean excludeFromServicesList() {
		return utils.getBoolean("@services-list-exclude");
	}
	
	@Override
	public boolean isServicesDerived() {
		boolean result = utils.getBoolean("@services-derived");
		if (result == true) {
			logger.trace(String.format("Found a Services derived field named '%s'", this.getID()));
		}
		return result;
	}
	
	@Override
	public boolean isServicesRefnameDisplayName() {
		return utils.getBoolean("@services-refnameDisplayName");
	}
	
	@Override
	public String getServicesSetter() {
		return utils.getString("@services-setter");
	}
	
	@Override
	public boolean shouldSchemaQualify() {
		return utils.getBoolean("@services-schema-qualify");
	}	
	
	@Override
	public String getServiceFieldAlias() {
		return utils.getString("@service-field-alias");
	}
	
	@Override
	public Boolean isAuthRefInServices() {
		return utils.getBoolean("@authref-in-services");
	}
	
	@Override
	public Boolean isInServices() {
		return utils.getBoolean("@exists-in-services");
	}	
	
	@Override
	public Boolean shouldIndex() {
		return utils.getBoolean("@services-should-index");
	}
	
	@Override
	public Boolean isServicesAnonymousType() {
		return utils.getBoolean("@services-type-anonymous");
	}
	
	@Override
	public Boolean isAGroupField() {
		return this.getUIType().startsWith("groupfield");
	}
	
	@Override
	public Boolean isAStructureDate() {
		return this.getUIType().startsWith("groupfield") && this.getUIType().contains("structureddate");
	}	
		
	@Override
	public String getID() {
		return  utils.getString("@id");
	}
	
	@Override
	public String getParentID() {
		return utils.getString("parentID");
	}
	
	private String getServicesType(String attributeName, boolean namespaceQualified) {
		String result = utils.getString(attributeName);
		String nsPrefix = NS;

		if (result == null) {
			if (this.isAGroupField() == true) {
				result = this.getUIType().split("/")[1]; // returns "bar" from something like "foo/bar"
			} else {
				String datatype = utils.getString("@datatype");
				if (datatype != null && !datatype.isEmpty()) {
					nsPrefix = XS;
					if (datatype.equalsIgnoreCase(DATATYPE_FLOAT)) {
						datatype = DATATYPE_DECIMAL;
					} else if (datatype.equalsIgnoreCase(DATATYPE_LARGETEXT)) {
						datatype = DATATYPE_STRING;
					}
					result = datatype;
				}
			}
		}
		
		if (result != null && namespaceQualified == true) {
			result = nsPrefix + result;
		}
		
		return result;
	}
	
	@Override
	public String getServicesGroupType(boolean namespaceQualified) {
		return getServicesType("services-group-type", namespaceQualified);
	}
	
	@Override
	public void setServicesGroupType(String servicesGroupType) {
		utils.setString("services-group-type", servicesGroupType);
	}
		
	@Override
	public String getServicesType(boolean namespaceQualified) {
		return getServicesType("services-type", namespaceQualified);
	}
	
	@Override
	public String getServiceTableName(boolean isAuthority) {
		String result = null;
		//
		// If the parent is a Repeat instance then the enclosing type is the
		// table name.  Otherwise, the table name is either the section name
		// or the authority schema name if we're dealing with an authority.
		//
		FieldParent parent = this.getParent();
		if (parent instanceof Repeat) {
			Repeat fieldParent = (Repeat)parent;
			result = fieldParent.getServicesTag().toLowerCase();
		} else if (parent instanceof Record) {
			Record record = (Record)parent;
			if (isAuthority == true) {
				Record proxy = record.getLastAuthorityProxy(); // Since all the authorities share the same "baseAuthority" record, we need to find the actual authority record; e.g., Person, Organization, etc...
				result = proxy.getAuthoritySchemaName();
			} else {
				result = record.getServicesSchemaName(this.getSection());
			}
		}

		return result;
	}
	
	@Override
	public String getServicesType() {
		return getServicesType(true /* default to getting namespace qualified */);
	}
	
	@Override
	public void setServicesType(String servicesType) {
		utils.setString("services-type", servicesType);
	}
	
}
