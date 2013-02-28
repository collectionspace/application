package org.collectionspace.chain.csp.webui.record;

import org.collectionspace.chain.csp.webui.main.WebMethodWithOps;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportUtils {
	private static final Logger log = LoggerFactory.getLogger(ReportUtils.class);
	private static final String PUBLISH_URL_SUFFIX = "publish";

	/*
	 * Set a field in the payloadOut object with a value from fieldSrc or if not present there then from dataSrc
	 */
	private static boolean setPayloadField(String fieldName, JSONObject payloadOut, JSONObject fieldsSrc, JSONObject dataSrc) throws JSONException {
		return setPayloadField(fieldName, payloadOut, fieldsSrc, dataSrc, null);
	}
	
	private static boolean setPayloadField(String fieldName, JSONObject payloadOut, JSONObject fieldsSrc, JSONObject dataSrc, String defaultValue) throws JSONException {
		boolean result = true;
		
		if (fieldsSrc != null && fieldsSrc.has(fieldName)) {
			payloadOut.put(fieldName, fieldsSrc.getString(fieldName));
		} else if (dataSrc != null && dataSrc.has(fieldName)) {
			payloadOut.put(fieldName, dataSrc.getString(fieldName));
		} else if (defaultValue != null) {
			payloadOut.put(fieldName, defaultValue);
		} else {
			result = false;
		}
		
		return result;
	}	
		
	static private void extractParamsFromJSON(UIRequest request, JSONObject payloadOut) throws Exception {
		JSONObject payloadIn = null;
		try {
			// Look for a JSON payload from the request
			payloadIn = request.getJSONBody();
		} catch (UIException e) {
			log.trace(e.getMessage());
		}

		if (payloadIn != null) {
			JSONObject fields = payloadIn.optJSONObject("fields"); // incoming query params
			setPayloadField("mode", payloadOut, fields, payloadIn, "single"); // default value set to "single"
			setPayloadField("docType", payloadOut, fields, payloadIn);
			//
			// If mode is 'single' then look for a 'singleCSID' param, otherwise if mode is 'group' look for a 'groupCSID'.
			// If <mode>CSID param is missing then try to use 'csid' from the query params (from the 'fields' var).
			//
			String exceptionMsg = null;
			if (payloadOut.getString("mode").equals("single")) {
				if (setPayloadField("singleCSID", payloadOut, fields, payloadIn) == false) {
					if (fields != null && fields.getString("csid").trim().isEmpty() == false) {
						payloadOut.put("singleCSID", fields.getString("csid"));
					} else {
						exceptionMsg = String.format("Report invocation context specified '%s' mode but did not provide a '%s' param.",
								"single", "singleCSID");
					}
				}
			} else if (payloadOut.getString("mode").equals("group")) {
				if (setPayloadField("groupCSID", payloadOut, fields, payloadIn) == false) {
					if (fields != null && fields.getString("csid").trim().isEmpty() == false) {
						payloadOut.put("groupCSID", fields.getString("csid"));
					} else {
						exceptionMsg = String.format("Report invocation context specified '%s' mode but did not provide a '%s' param.",
								"group", "groupCSID");
					}
				}
			} else {
				exceptionMsg = String.format("The Report invocation mode '%s' is unknown.", payloadOut.getString("mode"));
			}
	
			if (exceptionMsg != null) {
				throw new UIException(exceptionMsg);
			}
		}
	}
		
	/* Package Protected */	
	static void invokeReport(WebMethodWithOps webMethod, Storage storage, UIRequest request, String path) throws Exception {
		JSONObject payloadOut = new JSONObject();
		boolean publish = path.endsWith(PUBLISH_URL_SUFFIX);
		//
		// First look for the invocation context params from the incoming path/url of the form
		// {CSID_OF_REPORT}/{docType}/{singleCSID}
		//
		String[] bits = path.split("/");
		if (bits.length > 2 && !bits[1].equals("output")) {
			//
			// Create the "InvocationContext" for the report -right now this is hard coded.
			//
			path = bits[0] + (publish ? ("/" + PUBLISH_URL_SUFFIX) : "");							// bit[0] = report CSID
			String type = webMethod.getSpec().getRecordByWebUrl(bits[1]).getServicesTenantSg();		// bit[1] = document type
			payloadOut.put("singleCSID", bits[2]);													// bit[2] = document CSID
			payloadOut.put("docType", type);
			payloadOut.put("mode", "single");
		}
		
		//
		// Next look for invocation content parms from incoming payload.
		// The values from payload params will override any params set in the above code from the path/URL
		//
		extractParamsFromJSON(request, payloadOut);
		//
		// Make the report invocation.
		//
		JSONObject out=storage.retrieveJSON(webMethod.getBase() + "/" + path, payloadOut);
		
		if (publish == true) {
			// If they are asking the report to be published to the PublicItems service, there will be no response body.  The new public item URL gets returned in the reponse header.					
			request.sendURLReponse((String)out.get("Location"));
		} else {
			// They've asked for the report back, so we need to build up a response containing the report					
			byte[] data_array = (byte[])out.get("getByteBody");
			String contentDisp = out.has("contentdisposition")?out.getString("contentdisposition"):null;
			request.sendUnknown(data_array,out.getString("contenttype"), contentDisp);
			request.setCacheMaxAgeSeconds(0);	// Ensure we do not cache report output.
			//request.sendJSONResponse(out);
		}
		
		request.setOperationPerformed(webMethod.getOperation());
	}
}
