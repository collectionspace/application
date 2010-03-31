package org.collectionspace.chain.storage;

//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

// Modified for characterset safety at CARET. Same licence applies.

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.servlet.http.Cookie;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.ByteArrayEndPoint;
import org.mortbay.io.SimpleBuffers;
import org.mortbay.io.View;
import org.mortbay.io.bio.StringEndPoint;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpGenerator;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpParser;
import org.mortbay.jetty.HttpVersions;
import org.mortbay.jetty.testing.ServletTester;
import org.mortbay.util.ByteArrayOutputStream2;


public class UTF8SafeHttpTester {
    private String version,reason;
    private int status;
    private HttpFields fields=new HttpFields();
    private ByteArrayOutputStream content=new ByteArrayOutputStream();

    public String getHeader(String name) { return fields.getStringField(name); }
    public int getStatus() { return status; }
    public String getContent() { 
    	try {
			return content.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 always defined
			return null;
		}
    }
    
    private class PH extends HttpParser.EventHandler
    {
        public void startRequest(Buffer method, Buffer url, Buffer version) throws IOException {}
        public void startResponse(Buffer version, int status, Buffer reason) throws IOException  {
        	UTF8SafeHttpTester.this.version=version.toString();
            UTF8SafeHttpTester.this.status=status;
            UTF8SafeHttpTester.this.reason=reason.toString();
        }
        
        public void parsedHeader(Buffer name, Buffer value) throws IOException {
        	UTF8SafeHttpTester.this.fields.add(name,value);
        }

        public void headerComplete() throws IOException {}

        public void messageComplete(long contextLength) throws IOException {}
        
        public void content(Buffer ref) throws IOException
        {
        	content.write(ref.asArray());
        }
    }
	
	public void request(ServletTester tester,String method,String path,String data_str,String cookie) throws Exception {
		byte[] data=null;
		if(data_str!=null)
			data=data_str.getBytes("UTF-8");
		Buffer bb=new ByteArrayBuffer(32*1024 + (data!=null?data.length:0));
		Buffer sb=new ByteArrayBuffer(4*1024);
		ByteArrayEndPoint endp = new ByteArrayEndPoint(new byte[]{},1);
		endp.setGrowOutput(true);
		HttpGenerator generator = new HttpGenerator(new SimpleBuffers(new Buffer[]{sb,bb}),endp, sb.capacity(), bb.capacity());
		generator.setRequest(method,path);
		generator.setVersion(HttpVersions.HTTP_1_0_ORDINAL);
		HttpFields fields=new HttpFields();
		fields.put("Host","tester");
		fields.put(HttpHeaders.CONTENT_TYPE,"text/plain; charset=utf-8");
		fields.put(HttpHeaders.COOKIE,cookie);
		if(data!=null)
			fields.putLongField(HttpHeaders.CONTENT_LENGTH,data.length);
		generator.completeHeader(fields,false);
		if(data!=null)
			generator.addContent(new ByteArrayBuffer(data),false);
		generator.complete();
		generator.flush();
		ByteArrayBuffer res=tester.getResponses(endp.getOut());
		HttpParser parser = new HttpParser(res,new PH());
		parser.parse();
	}
}
