package org.collectionspace.chain.csp.nconfig.impl.main;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.nconfig.Section;
import org.collectionspace.chain.csp.nconfig.SectionGenerator;
import org.collectionspace.chain.csp.nconfig.Target;

public class Rule {
	private String start,end;
	private String[] path;
	private SectionGenerator step;
	private Target target;
	
	public Rule(String start,String[] path,String end,SectionGenerator step,Target target) {
		this.start=start;
		this.end=end;
		this.path=path;
		this.step=step;
		this.target=target;
		if(this.step==null && this.target!=null) {
			this.step=new DefaultStep();
		}
	}
	
	public boolean match(String start,List<String> path) {
		if(!start.equals(this.start))
			return false;
		if(path.size()!=this.path.length)
			return false;
		for(int i=0;i<this.path.length;i++)
			if(!this.path[i].equals(path.get(i)))
				return false;
		return true;
	}
	
	public int getLength() { return path.length; }
	
	public String destName() { return end; }
	
	public SectionGenerator getStep() { return step; }
	
	public Target getTarget() { return target; }
	
	public void end(Section m, List<String> path) {
		System.err.println("RULEEND Milestone "+m.getName()+" context="+StringUtils.join(path,"/"));	
	}

	public void start(Section m, List<String> path) {
		System.err.println("RULESTART Milestone "+m.getName()+" context="+StringUtils.join(path,"/"));	
	}

	public void text(Section m, List<String> path, String text) {
		System.err.println("RULETEXT Milestone "+m.getName()+" context="+StringUtils.join(path,"/")+" text=\""+text+"\"");			
	}
}
