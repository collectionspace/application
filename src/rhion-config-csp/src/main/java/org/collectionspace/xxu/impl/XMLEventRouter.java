package org.collectionspace.xxu.impl;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.collectionspace.xxu.api.XMLEventContext;

// XXX do it efficiently
public class XMLEventRouter implements XMLEventContext {
	private String[] stack,prestack;
	
	public XMLEventRouter(String[] stack) { this.stack=stack; prestack=new String[0]; }
	public XMLEventRouter(XMLEventContext in) { stack=in.getStack(); prestack=in.getPreStack(); }
	
	public String[] getStack() { return stack; }
	public String[] getPreStack() { return prestack; }
	
	public void truncate(int num) {
		if(num>=stack.length) {
			stack=new String[0];
			return;
		}
		String[] out=new String[stack.length-num];
		String[] preout=new String[prestack.length+num];
		System.arraycopy(prestack,0,preout,0,prestack.length);
		System.arraycopy(stack,0,preout,prestack.length,num);
		System.arraycopy(stack,num,out,0,stack.length-num);
		stack=out;
		prestack=preout;
	}
	
	public void restore(XMLEventContext in) { stack=in.getStack(); prestack=in.getPreStack(); }
}
