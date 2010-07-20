package org.collectionspace.chain.benchmark;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.h2.AxisRange;
import com.carrotsearch.junitbenchmarks.h2.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.h2.BenchmarkMethodChart;
import com.carrotsearch.junitbenchmarks.h2.LabelType;


@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "benchmark-objects-nolist")
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20)
public class TestObject extends AbstractBenchmark {
	private static final Logger log=LoggerFactory.getLogger(AbstractBenchmark.class);

	private String recordType="objects";
	private final static String dataCreate = "{\"accessionNumber\":\"new OBJNUM\",\"description\":\"new DESCRIPTION\",\"descInscriptionInscriber\":\"new INSCRIBER\",\"objectNumber\":\"2\",\"objectTitle\":\"new TITLE\",\"comments\":\"new COMMENTS\",\"distinguishingFeatures\":\"new DISTFEATURES\",\"responsibleDepartments\":[{\"responsibleDepartment\":\"new DEPT\"}],\"objectName\":\"new OBJNAME\"}";
	
	private static String cookie;
	private static ServletTester jetty;
	private static String tmp=null;
	private static Stack<String> uid = new Stack<String>();
	private static Stack<String> read_uid = new Stack<String>();
	

	private static synchronized String tmpdir() {
		if(tmp==null) {
			tmp=System.getProperty("java.io.tmpdir");
		}
		return tmp;
	}

	private void rm_r(File in) {
		for(File f : in.listFiles()) {
			if(f.isDirectory())
				rm_r(f);
			f.delete();
		}
	}

	@Before public void setup() throws IOException, CSPDependencyException {
		File tmp=new File(tmpdir());
		File dir=new File(tmp,"ju-cspace");
		if(dir.exists())
			rm_r(dir);
		if(!dir.exists())
			dir.mkdir();
	}
	private static JSONObject makeRequest(JSONObject fields) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("fields",fields);
		return out;
	}
	
	private static String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in)).toString();
	}
	
	private String getFields(String in) throws JSONException {
		return getFields(new JSONObject(in)).toString();
	}

	private JSONObject getFields(JSONObject in) throws JSONException {
		in=in.getJSONObject("fields");
		in.remove("csid");
		return in;
	}
	private static void login(ServletTester tester) throws IOException, Exception {
		//HttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
		String test = "{\"userid\":\"test@collectionspace.org\",\"password\":\"testtest\"}";
		HttpTester out=jettyDo(tester,"POST","/chain/login/",test);
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
	}	
	private static HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");
		if(cookie!=null)
			request.addHeader(HttpHeaders.COOKIE,cookie);
		if(data!=null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	private static ServletTester setupJetty() throws Exception {
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		login(tester);
		return tester;
	}
 
    @BeforeClass
    public static void prepare() throws Exception
    {
    	jetty=setupJetty();
    }

    @Before
    public void init() throws Exception
    {
    	HttpTester out;
    	int i = 0;
    	while(i<30){
		//Create
		out = jettyDo(jetty,"POST","/chain/"+recordType,makeSimpleRequest(dataCreate));
		uid.add(out.getHeader("Location"));
		i++;
    	}
    }

    @After
    public void cleanup() throws Exception
    {
    	while(!uid.empty()){
        	String id = uid.pop();
        	jettyDo(jetty,"DELETE","/chain/"+id,null);
        }
    	
    }
    @Test
    public void testList20() throws Exception
    {
		//List
		int pgSz = 20;
		int pgNum = 0;
		jettyDo(jetty,"GET","/chain/"+recordType+"/search?pageNum="+pgNum+"&pageSize="+pgSz,null);
    	
    }
    @Test
    public void testList40() throws Exception
    {
		//List
		int pgSz = 40;
		int pgNum = 0;
		jettyDo(jetty,"GET","/chain/"+recordType+"/search?pageNum="+pgNum+"&pageSize="+pgSz,null);
    	
    }
    
    @Test
    public void testCreate() throws Exception
    {
    	HttpTester out;
		//Create
		out = jettyDo(jetty,"POST","/chain/"+recordType,makeSimpleRequest(dataCreate));
		uid.add(out.getHeader("Location"));
		assertEquals(201,out.getStatus());	
    }
    
    @Test
    public void testDelete() throws Exception
    {
    	if(!uid.empty()){
    		String id = uid.pop();
    		jettyDo(jetty,"DELETE","/chain/"+id,null);
    		log.info("testDelete"+id);
    	}
    	else{
    		log.info("delete failed");
    		assertTrue(false);
    	}
    }
    
	@Test public void UISpec() throws Exception {
		HttpTester out = jettyDo(jetty,"GET","/chain/"+recordType+"/uispec",null);
		log.info(out.getContent());
	}
	@Test public void TabUISpec() throws Exception {
		jettyDo(jetty,"GET","/chain/"+recordType+"-tab/uispec",null);
	}
    
}
