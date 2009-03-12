package org.collectionspace.xxu.rhinotest;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.collectionspace.xxu.js.api.JavascriptContext;
import org.collectionspace.xxu.js.api.JavascriptExecution;
import org.collectionspace.xxu.js.api.JavascriptLibrary;
import org.collectionspace.xxu.js.api.JavascriptScript;
import org.collectionspace.xxu.js.api.JavascriptSystem;
import org.collectionspace.xxu.js.impl.rhino.RhinoSystem;
import org.junit.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class GeneralTest {
	
	// Good smoke test
	@Test public void testGeneral() throws Exception {
		JavascriptSystem js=new RhinoSystem();
		JavascriptContext ctx=js.createContext();
		JavascriptScript script=js.createScript();
		script.setScript("function hello(whom) { return 'Hello, '+whom; }");
		ctx.addScript(script);
		JavascriptExecution exec=ctx.createExecution("hello");
		Object out=exec.execute(new Object[]{"World!"});
		assertEquals("Hello, World!",out);
	}
	
	@Test public void testDistinctContexts() throws Exception {
		JavascriptSystem js=new RhinoSystem();
		JavascriptContext ctx1=js.createContext();
		JavascriptContext ctx2=js.createContext();
		JavascriptScript script1=js.createScript();
		JavascriptScript script2=js.createScript();
		script1.setScript("function hello(whom) { return 'Hello, '+whom; }");
		script2.setScript("function hello(whom) { return 'Goodbye, '+whom; }");
		ctx1.addScript(script1);
		ctx2.addScript(script2);
		JavascriptExecution exec1=ctx1.createExecution("hello");
		JavascriptExecution exec2=ctx2.createExecution("hello");
		Object out1=exec1.execute(new Object[]{"World!"});
		Object out2=exec2.execute(new Object[]{"World!"});
		assertEquals("Hello, World!",out1);
		assertEquals("Goodbye, World!",out2);		
	}
	
	private String runJavascript(String in,String method,Object[] args) throws Exception {
		JavascriptSystem js=new RhinoSystem();
		JavascriptContext ctx=js.createContext();
		JavascriptScript script=js.createScript();
		script.setScript(in);
		ctx.addScript(script);
		JavascriptExecution exec=ctx.createExecution(method);
		Object out=exec.execute(args);
		if(!(out instanceof String))
			throw new Exception(out+" is not a string");
		return (String)out;
	}
	
	@Test public void testClassShutter() throws Exception {
		String out=runJavascript("function error() { try { x = new java.lang.Number(); return 'no'; } catch(e) { return 'yes'; } }",
								 "error",new Object[]{});
		assertEquals("yes",out);
	}
	
	@Test public void testNewInstanceBug() throws Exception {
		String out=runJavascript("function error() { try { x = new Packages.org.collecitonspace.xxu.js.impl.rhino.RhinoRegistry().getClass(); return 'no'; } catch(e) { return 'yes'; } }",
				 "error",new Object[]{});
		assertEquals("yes",out);
	}

	@Test public void testSys() throws Exception {
		String out=runJavascript("function test() { return sys+''; }","test",new Object[]{});
		assertEquals("sys",out);
	}
	
	@Test public void testJava() throws Exception {
		JavascriptSystem js=new RhinoSystem();
		JavascriptContext ctx=js.createContext();
		JavascriptLibrary lib=js.createLibrary();
		lib.addJavaClass("key","value");
		ctx.addLibrary(lib);
		JavascriptScript script=js.createScript();
		script.setScript("function test() { return sys.key; }");
		ctx.addScript(script);
		JavascriptExecution exec=ctx.createExecution("test");
		Object out=exec.execute(new Object[]{});
		assertEquals("value",out);
	}
	
	@Test public void testJavascript() throws Exception {
		JavascriptSystem js=new RhinoSystem();
		JavascriptContext ctx=js.createContext();
		JavascriptScript def=js.createScript();
		def.setScript("function a(x) { return x+42; }");
		ctx.addScript(def);
		JavascriptExecution fn=ctx.createExecution("a");
		JavascriptLibrary lib=js.createLibrary();
		lib.addJavascriptExecution("test",fn);
		ctx.addLibrary(lib);
		JavascriptScript script=js.createScript();
		script.setScript("function test() { return sys.test(3)==45; }");
		ctx.addScript(script);
		JavascriptExecution exec=ctx.createExecution("test");
		Object out=exec.execute(new Object[]{});
		assertEquals(Boolean.valueOf(true),out);
	}
	
	private Object annotationTesting(Object attach,String s) throws Exception {
		JavascriptSystem js=new RhinoSystem();
		JavascriptContext ctx=js.createContext();
		JavascriptLibrary lib=js.createLibrary();
		lib.addJavaClass("key",attach);
		ctx.addLibrary(lib);
		JavascriptScript script=js.createScript();
		script.setScript(s);
		ctx.addScript(script);
		JavascriptExecution exec=ctx.createExecution("test");
		return exec.execute(new Object[]{});	
	}
	
	@Test public void testNoUnannotatedBindings() throws Exception {
		Object out=annotationTesting(new Unannotated(),"function test() { return sys.key!=undefined; }");
		assertEquals(Boolean.valueOf(false),out);		
	}

	@Test public void testAnnotatedBindings() throws Exception {
		Object out=annotationTesting(new Annotated(),"function test() { return sys.key!=undefined; }");
		assertEquals(Boolean.valueOf(true),out);
	}

	@Test public void testGetField() throws Exception {
		Object out=annotationTesting(new Annotated(),"function test() { return sys.key.field; }");
		assertEquals(42,out);
	}

	@Test public void testUnannotatedGetField() throws Exception {
		Object out=annotationTesting(new Annotated(),"function test() { return sys.key.unannotated!=undefined; }");
		assertEquals(Boolean.valueOf(false),out);		
	}

	@Test public void testPutField() throws Exception {
		Annotated a=new Annotated();
		annotationTesting(a,"function test() { sys.key.field=99; }");
		assertEquals(99,a.field);
	}

	@Test public void testUnannotatedPutField() throws Exception {
		Annotated a=new Annotated();
		annotationTesting(a,"function test() { sys.key.unannotated=99; }");
		assertFalse(a.unannotated==99);
	}
	
	@Test public void testMethod() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.method!=undefined; }");
		assertEquals(Boolean.valueOf(true),out);
	}

	@Test public void testUnannotatedMethod() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.unmethod!=undefined; }");
		assertEquals(Boolean.valueOf(false),out);
	}	

	@Test public void testMethodInstanceOf() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.method instanceof Function && sys.key.method instanceof Object; }");
		assertEquals(Boolean.valueOf(true),out);
	}

	@Test public void testMethodToString() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.method.toString(); }");
		assertEquals("[XXU sys method]",out);
	}
	
	@Test public void testMethodCall() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.method(7); }");
		assertEquals(13,out);
	}

	@Test public void testMethodWrapping() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.extract(sys.key.another()); }");
		assertEquals(84,out);
	}
	
	@Test public void testGetArray() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.array; }");
		assertTrue(out instanceof int[]);
		assertEquals(2,((int[])out)[0]);
	}

	@Test public void testGetArrayLength() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.array.length; }");
		assertTrue(out instanceof Integer);
		assertEquals(4,((Integer)out));
	}

	@Test public void testSetArray() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { sys.key.array[0]=7; return sys.key.array[0]; }");
		assertTrue(out instanceof Integer);
		assertEquals(7,((Integer)out));
	}
		
	@Test public void testGetList() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.list; }");
		assertTrue(out instanceof List);
		assertEquals(3,((List)out).size());	
	}
	
	@Test public void testGetListLength() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.list.length; }");
		assertTrue(out instanceof Integer);
		assertEquals(3,((Integer)out));
	}
	
	@Test public void testSetList() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { sys.key.list[2]=7; return sys.key.list[2]; }");
		assertTrue(out instanceof Integer);
		assertEquals(7,((Integer)out));
	}


	@Test public void testTruncateList() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { sys.key.list.length=2; return sys.key.list; }");
		assertTrue(out instanceof List);
		List v=(List)out;
		assertEquals(2,v.size());
		assertEquals(11,v.get(0));
		assertEquals(13,v.get(1));
	}
	
	@Test public void testMapGet() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.map.apple; }");
		assertEquals("banana",out);
	}
	
	@Test public void testMapGetAll() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.map; }");
		assertTrue(out instanceof Map);
		assertEquals(2,((Map)out).size());
	}
	
	@Test public void testMapSet() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { sys.key.map.carrot='dundee'; return sys.key.map; }");
		assertTrue(out instanceof Map);
		assertEquals(3,((Map)out).size());
		assertEquals("dundee",((Map)out).get("carrot"));
	}
	
	@Test public void testMapDelete() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { delete sys.key.map.apple; return sys.key.map; }");
		assertTrue(out instanceof Map);
		assertEquals(1,((Map)out).size());
	}
	
	@Test public void testMapGetNoEvil() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.evil_map.bad; }");
		assertEquals(null,out);
	}

	@Test public void testMapGetHoly() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.evil_map.good; }");
		assertTrue(out instanceof Annotated);
	}

	@Test public void testListGetNoEvil() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.evil_list[0]; }");
		assertEquals(null,out);
	}

	@Test public void testListGetHoly() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.evil_list[1]; }");
		assertTrue(out instanceof Annotated);
	}	
	
	@Test public void testMultidimensionalArray1() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.twod[0][0]; }");
		assertEquals(1,out);
	}
	
	@Test public void testMultidimensionalArray2() throws Exception {
		Annotated a=new Annotated();
		Object out=annotationTesting(a,"function test() { return sys.key.twod[1]; }");
		assertTrue(out instanceof int[]);
		assertEquals(2,((int[])out).length);
	}

	@Test public void testLoggingContext() throws Exception {
		JavascriptSystem js=new RhinoSystem();
		JavascriptContext ctx=js.createContext();
		JavascriptLibrary lib=js.createLibrary();
		lib.addJavaClass("key",new Unannotated());
		Logger logger=new Logger();
		ctx.setMessages(logger);
		ctx.addLibrary(lib);
		JavascriptScript script=js.createScript();
		script.setScript("function test() { return sys.key; }");
		ctx.addScript(script);
		JavascriptExecution exec=ctx.createExecution("test");
		exec.execute(new Object[]{});
		String[] msgs=logger.getAll();
		assertEquals(1,msgs.length);
	}
}
