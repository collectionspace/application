/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.nuispec;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.schema.UISpecRunContext;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.record.RecordCreateUpdate;
import org.collectionspace.chain.csp.webui.relate.RelateCreateUpdate;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes the cspace-config.xml record structures and creates a record 
 * that can be saved and used for test data 
 * @author csm22
 *
 */
public class DataGenerator  extends UISpec {
	private static final Logger log=LoggerFactory.getLogger(DataGenerator.class);
	private String dataprefix;
	private String repeataffix = "";
	private String extraprefix="";
	private Integer repeatnum = 3;
	private Integer quant = 1;
	private Integer startvalue = 0;
	private Integer maxrecords = 20; //how many records will we set relations on
	
	private Integer authoritylimit = 50; // 0 = nolimit return all authority items for each authority

	private BigDecimal minValue = new BigDecimal("0");
	private BigDecimal maxValue = new BigDecimal("1423453127");
	Random random = new Random();
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private RecordCreateUpdate writer;
	private RelateCreateUpdate relater;
	private Spec spec;
	private Boolean doall  = false;
	

	private TTYOutputter tty;


	public DataGenerator(Spec spec) {
		super(spec);
		this.doall = true;
		this.spec = spec;
	}

	public DataGenerator(Record record, String structureview) {
		super(record, structureview);
		this.doall = false;
		this.writer=new RecordCreateUpdate(record,true);
	}
	
	private JSONObject datagenerator(Storage storage,UIRequest ui) throws UIException {
		this.storage = storage;
		maxValue = new BigDecimal(dateToLong("2030-01-01"));
		minValue = new BigDecimal(dateToLong("1970-01-01"));
		log.info("initialize params");
		tty.line("initialize params");
		JSONObject out=new JSONObject();
		Structure s = record.getStructure(this.structureview);
		//how many records do we want to create
		if(ui.getRequestArgument("quantity")!=null){
			String quantity =ui.getRequestArgument("quantity");
			 this.quant = Integer.parseInt(quantity);
		}
		//how many time do we want the repeatable group to repeat
		if(ui.getRequestArgument("repeats")!=null){
			String rstring = ui.getRequestArgument("repeats");
			Integer rnum = Integer.parseInt(rstring);
			this.repeatnum = rnum;
		}
		//how many records will we set relationships on
		if(ui.getRequestArgument("maxrelationships")!=null){
			String mxstring = ui.getRequestArgument("maxrelationships");
			Integer mxnum = Integer.parseInt(mxstring);
			this.maxrecords = mxnum;
		}
		//how many records will we set relationships on
		if(ui.getRequestArgument("startvalue")!=null){
			String startvalue = ui.getRequestArgument("startvalue");
			Integer stnum = Integer.parseInt(startvalue);
			this.startvalue = stnum;
		}
		//how many records will we set relationships on
		if(ui.getRequestArgument("extraprefix")!=null){
			String exstring = ui.getRequestArgument("extraprefix");
			this.extraprefix = exstring;
		}
		//do we really want to randomly choose authority item from all the authoritys
		//setting this will speed up the initialization of this script
		if(ui.getRequestArgument("authoritylimit")!=null){
			String astring = ui.getRequestArgument("authoritylimit");
			Integer anum = Integer.parseInt(astring);
			this.authoritylimit = anum;
		}

		log.info("Creating "+quant.toString()+" records of type "+record.getWebURL());
		tty.line("Creating "+quant.toString()+" records of type "+record.getWebURL());
		tty.flush();
		for(Integer i=0; i<quant;i++){
			try {
				if((i % 10)==0){
					log.info("So far up to number: "+i.toString());
					tty.line("So far up to number: "+i.toString());
					tty.flush();
				}
				out.put(i.toString(), makedata(i));
			} catch (JSONException e) {
				throw new UIException("Cannot generate UISpec due to JSONException",e);
			}
		}
		
		return out;
	}

	private JSONObject makedata(Integer num) throws JSONException{

		UISpecRunContext context= new UISpecRunContext();
		num = num + this.startvalue;
		
		this.dataprefix = this.extraprefix + num.toString();
		JSONObject out=new JSONObject();
		out = generateDataEntrySection(context);
		return out;
	}
	
	private Date randomDate(){
		BigDecimal retValue = null;
		BigDecimal length = maxValue.subtract(minValue);
		BigDecimal factor = new BigDecimal(random.nextDouble());
		retValue = length.multiply(factor).add(minValue);
		
		return new Date(retValue.toBigInteger().longValue());
		
	}   
	private long dateToLong(String dateStr) {
		java.util.Date d;
		try {
			d = DATE_FORMAT.parse(dateStr);
		} catch (ParseException e) {
			d = new java.util.Date();
		}
		return d.getTime();
	}
	protected JSONObject createAllRecords(Storage storage,UIRequest ui) throws UIException {

		log.info("Lets make some records");
		tty.line("Lets make some records");
		tty.flush();
		JSONObject returnData = new JSONObject();
		try{
			for(Record r : spec.getAllRecords()) {
				if(r.isType("authority") || r.isType("authorizationdata") || r.isType("id") || r.isType("userdata")){
					//don't do these yet (if ever)
				}
				else if (r.getID().equals("structureddate") ||r.getID().equals("media") ||r.getID().equals("hierarchy") ||r.getID().equals("blobs") || r.getID().equals("dimension")|| r.getID().equals("contacts")){
					//and ignore these
				}
				else if (r.getID().equals("termlist") ||r.getID().equals("termlistitem")){
					//and ignore these
				}
				else{
					this.record = r;
					this.structureview="screen";
					this.writer=new RecordCreateUpdate(r,true);
					JSONObject items = createRecords(storage,ui);
					returnData.put(r.getID(), items.getJSONObject(r.getID()));
				}
			}
			
		//lets create some relationships
			log.info("Initializing relationships");
			tty.line("Initializing relationships");
		tty.flush();
		createDataSetRelationships(returnData);
		
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: ",x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: ",x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: ",x);
		} catch (UnderlyingStorageException x) {
			throw new UIException("Problem storing: "+x.getLocalizedMessage(),x.getStatus(),x.getUrl(),x);
		}
		
		return returnData;
	}
	//lets create some relationships
	protected JSONObject createDataSetRelationships(JSONObject dataset) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException, UIException{
		JSONObject out = new JSONObject();
		this.relater = new RelateCreateUpdate(true);
		this.relater.configure(spec);
//dataset: {"intake":{"1":"528ba269-c6df-49ea-a3da","0":"b2efd56f-a6f9-4a99-8870"},"loanin":{"1":
		//how many records are there
		Integer recordnums = this.quant;
		Integer maxrecords = this.maxrecords;

		Integer tempmax = recordnums;
		if(recordnums>maxrecords){tempmax = maxrecords;}
		
		//do something here
		Iterator rit=dataset.keys();
		JSONArray allrecordtypes = new JSONArray();
		while(rit.hasNext()) {
			String recordtype=(String)rit.next();
			allrecordtypes.put(recordtype);
		}
		
		Integer numOfRecords = allrecordtypes.length();
		for(int j=0;j<numOfRecords;j++){
			String sourceType = spec.getRecord(allrecordtypes.getString(j)).getWebURL();
			for(int k=0;k<numOfRecords;k++){
				String dstType = spec.getRecord(allrecordtypes.getString(k)).getWebURL();
				if(!dstType.equals(sourceType)){
					//make a 1way relationship

					Integer currentrecord = tempmax - 1;
					while(currentrecord >=0){
						String srcCsid =  dataset.getJSONObject(allrecordtypes.getString(j)).getString(currentrecord.toString());

						Integer temp = tempmax - currentrecord;
						for(Integer m=currentrecord;m>=temp;m--){
							String dstCsid =  dataset.getJSONObject(allrecordtypes.getString(k)).getString(m.toString());

							//log.info(currentrecord.toString()+":"+m.toString());

							tty.line(currentrecord.toString()+":"+m.toString()+">>"+sourceType+":"+srcCsid+ " associated with "+dstType+":"+dstCsid);
							log.info(currentrecord.toString()+":"+m.toString()+">>"+sourceType+":"+srcCsid+ " associated with "+dstType+":"+dstCsid);
							//log.info(sourceType+":"+srcCsid+ " associated with "+dstType+":"+dstCsid);
							JSONObject relatedata = createRelation(sourceType,srcCsid,"affects",dstType,dstCsid,false);

							relater.sendJSONOne(storage,null,relatedata,false);
							
							//one way 2-way? I think I need to comment this one out...
							relater.sendJSONOne(storage,null,relatedata,true);
							
						}
						tty.flush();
						currentrecord--;
					}
					tty.flush();
				}
			}
		}
		
	
		
		return out;
	}
	
	private JSONObject createMini(String type,String id) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("csid",id);
		out.put("recordtype",type);
		return out;
	}
	private JSONObject createRelation(String src_type,String src,String type,String dst_type,String dst,boolean one_way) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("source",createMini(src_type,src));
		out.put("target",createMini(dst_type,dst));
		out.put("type",type);
		out.put("one-way",one_way);
		return out;
	}
	
	protected JSONObject createRecords(Storage storage,UIRequest ui) throws UIException {

		log.info("Making "+this.record.getID());
		tty.line("Making "+this.record.getID());
		tty.flush();
		
		JSONObject returnData = new JSONObject();
		JSONObject out= datagenerator(storage,ui);
		try{
		//make it a record
			JSONObject data = new JSONObject();

			Iterator rit=out.keys();
			JSONObject dataitems= new JSONObject();
			while(rit.hasNext()) {
				String key=(String)rit.next();
				data.put("fields",out.getJSONObject(key));

				String path=writer.sendJSON(storage,null,data);
				dataitems.put(key,path);
				//log.info(path);

				log.info("created "+this.record.getID()+" with csid of: "+path);
				tty.line("created "+this.record.getID()+" with csid of: "+path);
				tty.flush();
				
			}
			returnData.put(this.record.getID(),dataitems);

		} catch (JSONException x) {
			tty.line("JSONException(Failed to parse json: "+x);
			log.info("JSONException(Failed to parse json: "+x);
			throw new UIException("Failed to parse json: "+x,x);
		} catch (ExistException x) {
			log.info("ExistException(Existence exception: "+x);
			tty.line("ExistException(Existence exception: "+x);
			throw new UIException("Existence exception: "+x,x);
		} catch (UnimplementedException x) {
			tty.line("UnimplementedException(UnimplementedException: "+x);
			log.info("UnimplementedException(UnimplementedException: "+x);
			throw new UIException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			tty.line("UnderlyingStorageException(UnderlyingStorageException: "+x);
			log.info("UnderlyingStorageException(UnderlyingStorageException: "+x);
			throw new UIException("Problem storing: "+x.getLocalizedMessage(),x.getStatus(),x.getUrl(),x);
		}
		
		//return something to screen
		return returnData;
	}
	
	@Override
	public void configure(WebUI ui, Spec spec) {
	}

	private void initvariables(){
		random = new Random();
		
		dataprefix = "";
		repeataffix = "";
		extraprefix="";
		repeatnum = 3;
		quant = 1;
		startvalue = 0;
		maxrecords = 20; //how many records will we set relations on
		
		authoritylimit = 50; // 0 = nolimit return all authority items for each authority

		minValue = new BigDecimal("0");
		maxValue = new BigDecimal("1423453127");
	}
	
	@Override
	public void run(Object in, String[] tail) throws UIException {
		initvariables();
		Request q=(Request)in;
		ctl = new CacheTermList(q.getCache());
		JSONObject out = new JSONObject();
		tty=q.getUIRequest().getTTYOutputter();
		if(doall){
			out = createAllRecords(q.getStorage(),q.getUIRequest());
		}
		else{
			out= createRecords(q.getStorage(),q.getUIRequest());
			
		}
		q.getUIRequest().sendJSONResponse(out);
	}
	


	/* overrides UISpec */

	protected String getSelectorAffix(FieldSet fs){
		return "";
	}

	protected String getSelector(FieldSet fs, UISpecRunContext context){
		return fs.getID();
	}

	protected String veryplain(FieldSet f,UISpecRunContext context) {
		return this.dataprefix+" - " + repeataffix +f.getID();	
	}
	protected String veryplain(String f) {
		return f;	
	}
	protected String veryplainWithoutEnclosure(FieldSet f,UISpecRunContext context) {
		return this.dataprefix+" - " + repeataffix +f.getID();	
	}
	
	protected String plain(Field f, UISpecRunContext context) {
		if(f.getUIType().equals("date")){
			Date test = randomDate();
			return DATE_FORMAT.format(test);
		}
		if(f.getParent().isExpander()){
			return radio(f);
		}

		return this.dataprefix+" - " + repeataffix +f.getID();		
	}
	protected void generateFieldDataEntry_notrefactored(JSONObject out, UISpecRunContext context, Field f)
	throws JSONException {
		// Single field
		out.put(getSelector(f,context),generateDataEntryField(f,context));	
	}	

	private void repeatItem(JSONObject out, Repeat r, UISpecRunContext context) throws JSONException{
		String selector = getSelector(r,context);
		JSONArray arr = new JSONArray();
		for(Integer i=0;i<repeatnum;i++){
			repeataffix = i.toString()+" - ";
			JSONObject protoTree=new JSONObject();
			for(FieldSet child : r.getChildren("POST")) {
				generateDataEntry(protoTree,child, context);
			}
			arr.put(protoTree);
			repeataffix = "";
		}
		out.put(selector,arr);
	}
	protected void repeatNonSibling(JSONObject out, UISpecRunContext context,
			Repeat r) throws JSONException {
		repeatItem(out, r, context);
	}
	protected void repeatSibling(JSONObject out, UISpecRunContext context, Repeat r,
			JSONObject row, JSONArray children) throws JSONException {
		repeatItem(out, r, context);
	}
	
	protected void makeAuthorities(JSONObject out, UISpecRunContext context, Field f)
	throws JSONException {
		if("enum".equals(f.getUIType())){
			out.put(getSelector(f,context),generateDataEntryField(f,context));
		}
		else{
			//get authority
			Object data = generateAuthorityField(f,context);
			out.put(getSelector(f,context),data);
		}
	}
	
	protected Object generateAuthorityField(Field f,UISpecRunContext context) throws JSONException {

		String shortId="";
		JSONObject namedata = getAuthdata(f,context);
		if(namedata.length() != 0){
			if(namedata.has("refid") && !namedata.getString("refid").equals("")){
				shortId = namedata.getString("refid");
			}
			else if(namedata.has("shortIdentifier") && !namedata.getString("shortIdentifier").equals("")){
				shortId = namedata.getString("shortIdentifier");
				shortId.toLowerCase();
			}
			else{
				String name = namedata.getString("displayName");
				shortId = name.replaceAll("\\W","");	
				shortId.toLowerCase();				
			}
		}
		return shortId;
		
		
	}
	
	private JSONObject getAuthdata(Field f,UISpecRunContext context) throws JSONException {
		JSONObject dataitem= new JSONObject();

		JSONObject allnames = new JSONObject();
		Integer i =0;
		for(Instance type : f.getAllAutocompleteInstances()){
			String iid = type.getTitleRef();
			Record ir = type.getRecord();

			JSONArray thesenames = ctl.get(this.storage, iid,ir,authoritylimit);
			log.info("getting authority: "+type.getID()+":"+type.getRecord());
			try {
				tty.line("getting authority: "+type.getID()+":"+type.getRecord());
			} catch (UIException e) {
			}
			
			allnames.put(i.toString(), thesenames);
			i++;
		}

		Random objrandom = new Random();
		Integer pickobj = objrandom.nextInt(allnames.length());
		
		JSONArray getallnames = allnames.getJSONArray(pickobj.toString());
		
		//use random number to choose which item to use
		Random arrayrandom = new Random();
		if(getallnames.length() > 0){
			int pick = arrayrandom.nextInt(getallnames.length());
			dataitem = getallnames.getJSONObject(pick);
		}
		return dataitem;
	}
	protected Object generateENUMField(Field f,UISpecRunContext context) throws JSONException {
		String shortId="";
		JSONObject namedata = getAuthdata(f,context);
		if(namedata.has("shortIdentifier") && !namedata.getString("shortIdentifier").equals("")){
			shortId = namedata.getString("shortIdentifier");
		}
		else{
			String name = namedata.getString("displayName");
			shortId = name.replaceAll("\\W","");					
		}
		shortId.toLowerCase();
		return shortId;
	}
	
	protected Object generateOptionField(Field f,UISpecRunContext context) throws JSONException {
		
		Random optrandom = new Random();
		Integer pickopt = optrandom.nextInt(f.getAllOptions().length);
		return f.getAllOptions()[pickopt].getID();
	}
}
