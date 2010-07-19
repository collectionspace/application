package org.collectionspace.chain.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
@BenchmarkMethodChart(filePrefix = "benchmark-authority")
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20)
public class TestAuthority  extends AbstractBenchmark{

	private static final Logger log=LoggerFactory.getLogger(TestAuthority.class);
	private static String cookie;
	private static ServletTester jetty;
	private static Stack<String> uid = new Stack<String>();
	private static Stack<String> update_uid = new Stack<String>();
	private static Stack<String> del_uid = new Stack<String>();
	
	
	// XXX refactor
	protected InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	
	private static void login(ServletTester tester) throws IOException, Exception {
		HttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
		log.info("Got cookie "+cookie);
	}
	
	// XXX refactor
	private static HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");		
		if(data!=null)
			request.setContent(data);
		if(cookie!=null)
			request.addHeader(HttpHeaders.COOKIE,cookie);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	
	// XXX refactor into other copy of this method
	private static ServletTester setupJetty() throws Exception {
		BootstrapConfigController config_controller=new BootstrapConfigController(null);
		config_controller.addSearchSuffix("test-config-loader2.xml");
		config_controller.go();
		String base=config_controller.getOption("services-url");		
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("storage","service");
		tester.setAttribute("store-url",base+"/cspace-services/");	
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		login(tester);
		return tester;
	}

	@BeforeClass public static void reset() throws Exception {
		jetty=setupJetty();
		//test if need to reset data - only reset it org auth are null
		HttpTester out=jettyDo(jetty,"GET","/chain/authorities/person/",null);
		if(out.getStatus()<299){
			JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");
			if(results.length()==0){
				jettyDo(jetty,"GET","/chain/reset",null);
			}
		}			
	}
	
	@Test
	public void Create(){

		// Create
		JSONObject data;
		try {
			data = new JSONObject("{'fields':{'displayName':'XXXTESTFred Bloggs'}}");
			HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
			assertTrue(out.getStatus()<300);
			uid.add(out.getHeader("Location"));
		} catch (JSONException e) {
			assertTrue(false);
		} catch (IOException e) {
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(false);
		}
	}
	@Test public void Read(){
		// Read

    	String url = uid.pop();
    	update_uid.add(url);
		HttpTester out;
		try {
			out = jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
			assertTrue(out.getStatus()<299);
			JSONObject data=new JSONObject(out.getContent()).getJSONObject("fields");
			assertEquals(data.getString("csid"),url.split("/")[2]);
			assertEquals("XXXTESTFred Bloggs",data.getString("displayName"));
		} catch (JSONException e) {
			assertTrue(false);
		} catch (IOException e) {
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(false);
		}
	}
	@Test public void Update(){
		// Update
    	String url = update_uid.pop();
    	del_uid.add(url);
    	JSONObject data;
		try {
			data = new JSONObject("{'fields':{'displayName':'XXXTESTOwain Glyndwr'}}");
	    	HttpTester out=jettyDo(jetty,"PUT","/chain/vocabularies"+url,data.toString());		
			assertTrue(out.getStatus()<300);
		} catch (JSONException e) {
			assertTrue(false);
		} catch (IOException e) {
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(false);
		}
	}
	
	@Test public void Autocomplete10Matches(){
		// Now test
		try {
			HttpTester out=jettyDo(jetty,"GET","/chain/intake/autocomplete/depositor?q=XXXTESTOwain",null);
			assertTrue(out.getStatus()<299);
			String[] testData=out.getContent().split("\n");
			for(int i=0;i<testData.length;i++) {
				JSONObject entry=new JSONObject(testData[i]);
				assertTrue(entry.getString("label").toLowerCase().contains("xxxtestowain"));
				assertTrue(entry.has("urn"));
			}
		} catch (JSONException e) {
			assertTrue(false);
		} catch (IOException e) {
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(false);
		}
		
	}

	@Test public void Autocomplete0Matches(){
		// Now test
		try {
			HttpTester out=jettyDo(jetty,"GET","/chain/intake/autocomplete/depositor?q=XXXTESTBob",null);
			assertTrue(out.getStatus()<299);
			assertTrue(out.getContent()==null);
		} catch (JSONException e) {
			assertTrue(false);
		} catch (IOException e) {
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(false);
		}
		
	}
	
	@Test public void Delete(){
		// Delete
    	String url = del_uid.pop();
		try {
	    	HttpTester out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
			assertTrue(out.getStatus()<299);
			out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
			assertEquals(400,out.getStatus());		
		} catch (JSONException e) {
			assertTrue(false);
		} catch (IOException e) {
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(false);
		}
	}
}
