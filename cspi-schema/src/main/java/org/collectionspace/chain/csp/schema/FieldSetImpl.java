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
	public String getID() {
		return  utils.getString("@id");
	}
	
	@Override
	public String getParentID() {
		return utils.getString("parentID");
	}
	
	@Override
	public String getServicesType() {
		String result = utils.getString("services-type");

		if (result == null) {
			if (this.isAGroupField() == true) {
				result = this.getUIType().split("/")[1]; // returns "bar" from something like "foo/bar"
			} else {
				String datatype = utils.getString("@datatype");
				if (datatype != null && !datatype.isEmpty()) {
					if (datatype.equalsIgnoreCase(DATATYPE_FLOAT)) {
						datatype = DATATYPE_DECIMAL;
					}
					result = "xs:" + datatype;
				}
			}
		}
		
		return result;
	}
	
	@Override
	public void setServicesType(String servicesType) {
		utils.setString("services-type", servicesType);
	}
	
}
