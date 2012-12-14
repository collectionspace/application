package org.collectionspace.chain.csp.schema;


public abstract class FieldSetImpl implements FieldSet {
	
	protected SchemaUtils utils = new SchemaUtils();
	
	@Override
	public Boolean isInServices() {
		return utils.getBoolean("@exists-in-services");
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
	
	@Override
	public String getServicesType(boolean namespaceQualified) {
		String result = utils.getString("services-type");
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
	public String getServicesType() {
		return getServicesType(true /* default to getting namespace qualified */);
	}
	
	@Override
	public void setServicesType(String servicesType) {
		utils.setString("services-type", servicesType);
	}
	
}
