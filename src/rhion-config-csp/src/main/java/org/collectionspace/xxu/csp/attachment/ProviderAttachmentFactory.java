package org.collectionspace.xxu.csp.attachment;

import java.util.List;
import org.collectionspace.xxu.api.CSP;
import org.collectionspace.xxu.api.CSPProvider;
import org.collectionspace.xxu.api.CSPProviderFactory;
import org.collectionspace.xxu.api.ConfigLoadingException;
import org.dom4j.Node;

public class ProviderAttachmentFactory implements CSPProviderFactory {

	public CSPProvider process(CSP csp, Node in) throws ConfigLoadingException {
		if(!"attachment".equals(in.getName()))
			return null;
		String point=in.valueOf("@point");
		if(point==null || "".equals(point))
			throw new ConfigLoadingException("Must supply point attribute to attachment");
		String tag=in.valueOf("@tag");
		if(tag==null || "".equals(tag))
			throw new ConfigLoadingException("Must supply tag attribute to attachment");
		ProviderAttachmentImpl out=new ProviderAttachmentImpl(point,tag);
		for(Node node : (List<Node>)in.selectNodes("point"))
			out.addPoint(node.valueOf("@name"),node.valueOf("@path"));
		return out;
	}
}
