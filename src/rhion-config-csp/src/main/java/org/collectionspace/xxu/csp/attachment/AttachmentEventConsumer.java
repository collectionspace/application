package org.collectionspace.xxu.csp.attachment;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.CSPConfig;
import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;
import org.collectionspace.xxu.impl.ConfigLoaderImpl;
import org.collectionspace.xxu.impl.DelegatingAttachment;

public class AttachmentEventConsumer implements XMLEventConsumer {
	private ConfigLoader cfg;
	private DelegatingAttachment delegate;
	private XMLEventConsumer vanilla;
	
	public AttachmentEventConsumer(ConfigLoader cfg) { 
		this.cfg=cfg; 
		delegate=new DelegatingAttachment((ConfigLoaderImpl)cfg);
		vanilla=new VanillaEventConsumer(cfg);
	}
	
	private String stackPrint(String[] stack) {
		StringBuffer out=new StringBuffer();
		for(String s : stack) {
			out.append(s);
			out.append(' ');
		}
		return out.toString();
	}
	
	private void start() {
		cfg.pushCSPConfig();
		System.err.println("Starting new delegation");
	}

	private void callFactory(CSPConfig parent,CSPConfig child,String tag) {
		System.err.println("parent="+parent+" child="+child+" tag="+tag);
	}
	
	private void stop(String tag) {
		System.err.println("Ending delegation");
		CSPConfig sub=cfg.popCSPConfig();
		callFactory(cfg.getCSPConfig(),sub,tag);
	}
	
	public void end(int ev,XMLEventContext context) {
		System.err.println("("+ev+") end "+stackPrint(context.getStack()));
		String[] stack=context.getStack();
		if(delegate.delegate_end(ev,context))
			return;
		if(stack.length==1)
			stop(stack[0]);
		vanilla.end(ev,context);
	}

	public void start(int ev,XMLEventContext context) {
		System.err.println("("+ev+") start "+stackPrint(context.getStack()));
		String[] stack=context.getStack();
		if(stack.length==1)
			start();
		if(delegate.delegate_start(ev,context))
			return;
		vanilla.start(ev,context);
	}

	public void text(int ev,XMLEventContext context, String text) {
		System.err.println("("+ev+") text "+stackPrint(context.getStack())+" : "+text);
		if(delegate.delegate_text(ev,context,text))
			return;
		vanilla.text(ev,context,text);
	}
}
