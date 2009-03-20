package org.collectionspace.xxu.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;

public class XMLEventTester implements XMLEventConsumer {
	private List<String> events;
	
	private static final Pattern p=Pattern.compile("^(.*?):(.*?)(?::(.*))?");
	
	public XMLEventTester(String[] in) { events=new ArrayList(Arrays.asList(in)); }
	
	private String stackPrint(String[] stack) {
		StringBuffer out=new StringBuffer();
		for(String s : stack) {
			out.append(s);
			out.append(' ');
		}
		return out.toString();
	}
	
	private String shift(String name,String[] stack) { 
		String next=events.remove(0);
		Matcher m=p.matcher(next);
		assertTrue(m.matches());
		assertEquals(name,m.group(1));
		String[] parts=m.group(2).split(",");
		assertEquals(parts.length,stack.length);
		for(int i=0;i<parts.length;i++)
			assertEquals(parts[i],stack[i]);
		return m.group(3);
	}
	
	public void end(int ev,XMLEventContext context) {
		shift("end",context.getStack());
	}

	public void start(int ev,XMLEventContext context) {
		shift("start",context.getStack());
	}

	public void text(int ev,XMLEventContext context, String text) {
		String t=shift("text",context.getStack());
		assertEquals(t,text);
	}
}
