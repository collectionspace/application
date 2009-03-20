package org.collectionspace.xxu.impl;

import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;

// XXX more efficient for resolve and truncate when deep
public class DelegatingAttachment implements XMLEventConsumer {
	private ConfigLoaderImpl loader;
	
	public DelegatingAttachment(ConfigLoaderImpl loader) {
		this.loader=loader;
	}
	
	public boolean delegate_end(int ev,XMLEventContext context) {
		XMLEventContext orig=new XMLEventRouter(context);
		AttachmentConsumer[] consumers=loader.resolveAndTruncate(context);
		String[] stack=context.getStack();
		if(consumers!=null) {
			for(AttachmentConsumer c : consumers) {
				String tag=c.getName();
				if(tag==null || stack.length==0 || !tag.equals(stack[0]))
					continue;
				c.getEventConsumer().end(ev,context);
				return true;				
			}
		}
		context.restore(orig);
		return false;
	}

	public boolean delegate_start(int ev,XMLEventContext context) {
		XMLEventContext orig=new XMLEventRouter(context);
		AttachmentConsumer[] consumers=loader.resolveAndTruncate(context);
		String[] stack=context.getStack();
		if(consumers!=null) {
			for(AttachmentConsumer c : consumers) {
				String tag=c.getName();
				if(tag==null || stack.length==0 || !tag.equals(stack[0]))
					continue;
				c.getEventConsumer().start(ev,context);
				return true;
			}
		}		
		context.restore(orig);
		return false;
	}

	public boolean delegate_text(int ev,XMLEventContext context, String text) {
		XMLEventContext orig=new XMLEventRouter(context);
		AttachmentConsumer[] consumers=loader.resolveAndTruncate(context);
		String[] stack=context.getStack();
		if(consumers!=null) {
			for(AttachmentConsumer c : consumers) {
				String tag=c.getName();
				if(tag==null || stack.length==0 || !tag.equals(stack[0]))
					continue;
				c.getEventConsumer().text(ev,context,text);
				return true;
				
			}
		}
		context.restore(orig);
		return false;
	}

	public void end(int ev, XMLEventContext context) { delegate_end(ev,context); }
	public void start(int ev, XMLEventContext context) { delegate_start(ev,context); }
	public void text(int ev, XMLEventContext context, String text) { delegate_text(ev,context,text); }
}
