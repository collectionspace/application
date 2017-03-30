package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.services.structureddate.Certainty;
import org.collectionspace.services.structureddate.Date;
import org.collectionspace.services.structureddate.Era;
import org.collectionspace.services.structureddate.QualifierType;
import org.collectionspace.services.structureddate.QualifierUnit;
import org.collectionspace.services.structureddate.StructuredDate;
import org.collectionspace.services.structureddate.StructuredDateFormatException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple web service that accepts a date string, parses it into a
 * structured date, and returns the structured date in JSON format.
 *
 */
public class StructuredDateParser implements WebMethod {

	@Override
	public void configure(WebUI ui, Spec spec) {
		// Intentionally left blank.
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		UIRequest request = ((Request) in).getUIRequest();
		String displayDate = request.getRequestArgument("displayDate");

		JSONObject output = new JSONObject();
		StructuredDate structuredDate = null;
		StructuredDateFormatException formatException = null;
		
		try {
			structuredDate = StructuredDate.parse(displayDate);
		}
		catch(StructuredDateFormatException e) {
			formatException = e;
		}
		
		try {
			if (formatException != null) {
				// The convention in app layer error responses appears to be to
				// send a boolean isError, and an array of error messages.
				output.put("isError", true);
				output.put("messages", new String[] {"Unrecognized date format", formatException.getMessage()});
			}
			
			if (structuredDate != null) {
				output.put("structuredDate", structuredDateToJSON(structuredDate));
			}
		}
		catch(JSONException e) {
			throw new UIException("Error building JSON", e);
		}
		
		request.sendJSONResponse(output);
	}
	
	private JSONObject structuredDateToJSON(StructuredDate structuredDate) throws JSONException {
		JSONObject json = new JSONObject();
		
		String displayDate = structuredDate.getDisplayDate();
		String association = structuredDate.getAssociation();
		String period = structuredDate.getPeriod();
		String note = structuredDate.getNote();
		Date earliestSingleDate = structuredDate.getEarliestSingleDate();
		Date latestDate = structuredDate.getLatestDate();
		Boolean scalarValuesComputed = structuredDate.areScalarValuesComputed();
		
		json.put("dateDisplayDate", (displayDate != null) ? displayDate : "");
		json.put("dateAssociation", (association != null) ? association : "");
		json.put("datePeriod", (period != null) ? period : "");
		json.put("dateNote", (note != null) ? note : "");
		addDateFieldsToJSON(json, earliestSingleDate, "dateEarliestSingle");
		addDateFieldsToJSON(json, latestDate, "dateLatest");
		json.put("scalarValuesComputed", (scalarValuesComputed != null) ? scalarValuesComputed : false);
		
		return json;
	}
	
	private void addDateFieldsToJSON(JSONObject json, Date date, String prefix) throws JSONException {
		Integer year = null;
		Integer month = null;
		Integer day = null;
		Era era = null;
		Certainty certainty = null;
		QualifierType qualifierType = null;
		Integer qualifierValue = null;
		QualifierUnit qualifierUnit = null;
		
		if (date != null) {
			year = date.getYear();
			month = date.getMonth();
			day = date.getDay();
			era = date.getEra();
			certainty = date.getCertainty();
			qualifierType = date.getQualifierType();
			qualifierValue = date.getQualifierValue();
			qualifierUnit = date.getQualifierUnit();
		}
		
		json.put(prefix + "Year", (year != null) ? year.toString() : "");
		json.put(prefix + "Month", (month != null) ? month.toString() : "");
		json.put(prefix + "Day", (day != null) ? day.toString() : "");
		json.put(prefix + "Era", (era != null) ? era.toString() : "");
		json.put(prefix + "Certainty", (certainty != null) ? certainty.toString() : "");
		json.put(prefix + "Qualifier", (qualifierType != null) ? qualifierType.toString() : "");
		json.put(prefix + "QualifierValue", (qualifierValue != null) ? qualifierValue.toString() : "");
		json.put(prefix + "QualifierUnit", (qualifierUnit != null) ? qualifierUnit.toString() : "");
	}
}
