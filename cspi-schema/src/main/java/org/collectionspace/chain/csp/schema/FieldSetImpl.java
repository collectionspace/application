package org.collectionspace.chain.csp.schema;


public abstract class FieldSetImpl implements FieldSet {
	
	protected SchemaUtils utils = new SchemaUtils();
	
	@Override
	public String getID() {
		return  utils.getString("@id");
	}
	
	@Override
	public String getServicesType() {
		String result = utils.getString("services-type");

		if (result == null) {
			if (this.isAGroupField() == true) {
				result = this.getUIType().split("/")[1]; // returns "bar" from something like "foo/bar"
			}
		}
		
		return result;
	}
	
	@Override
	public void setServicesType(String servicesType) {
		utils.setString("services-type", servicesType);
	}
	
}
