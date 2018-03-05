/* Copyright 2010 University of Cambridge and UC Berkeley
 * Richard Millet
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.SectionGenerator;
import org.collectionspace.chain.csp.config.RuleTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeNode {
	private static final Logger log=LoggerFactory.getLogger(TreeNode.class);
	private TreeNode parent;
	private List<TreeNode> children=new ArrayList<TreeNode>();
	private String text=null;
	private boolean is_text=false,is_claimed;
	private SectionGenerator claim_step;
	private String claim_name;
	private RuleTarget claim_target;
	
	private TreeNode() {}
	
	public static TreeNode create_tag(String name) {
		TreeNode out=new TreeNode();
		out.is_text=false;
		out.text=name;
		return out;
	}
	
	public static TreeNode create_text(String text) {
		TreeNode out=new TreeNode();
		out.is_text=true;
		out.text=text;
		return out;
	}
	
	public void addChild(TreeNode child) {
		child.parent=this;
		// Merge text nodes
		if(children.size()>0 && child.is_text) {
			TreeNode youngest=children.get(children.size()-1);
			if(youngest.is_text) {
				youngest.text+=child.text;
				return;
			}
		}
		children.add(child);
	}
	
	String getName() { return text; }
	TreeNode getParent() { return parent; }

	private void match_all_children(RuleSetImpl rules,String name,List<String> path) {
		for(TreeNode child : children) {
			path.add(child.getName());
			child.match(rules,name,path);
			path.remove(path.size()-1);
		}
	}
	
	public void claim(RuleSetImpl rules,String name,SectionGenerator step,RuleTarget target) {
		log.debug("Node "+text+" claimed by "+name);
		this.claim_step=step;
		this.claim_name=name;
		this.claim_target=target;
		is_claimed=true;
		match_all_children(rules,name,new ArrayList<String>());
	}
	
	private void match(RuleSetImpl rules,String name,List<String> part) {
		Rule r=rules.matchRules(name,part);
		if(r==null) {
			log.debug("Node "+text+" is subsidiary claim of "+name+" with suffix "+StringUtils.join(part,"/"));
			is_claimed=false;
			match_all_children(rules,name,part);
		} else {
			claim(rules,r.destName(),r.getStep(),r.getTarget());
		}	
	}
	
	public void run_unclaimed(Map<String,String> data,String path) {
		if(is_text)
			data.put(path,text);
		else {
			if(!is_claimed)
				path+="/"+text;
			if(children.size()==0) {
				data.put(path,"");
			}
			for(TreeNode child : children) {
				if(!child.is_claimed)
					child.run_unclaimed(data,path);
			}
		}
	}
	
	public void run_all(SectionImpl m) {
		/* First we run our unclaimeds, to get a good Milestone */
		if(!is_text) {
			log.debug("Running claimed on milestone "+m.getName());
			for(TreeNode child : children) {
				if(child.is_claimed) {
					log.debug("Running unclaimed on milestone "+m.getName());		
					Map<String,String> data=new HashMap<String,String>();
					child.run_unclaimed(data,"");
					SectionImpl nxt=new SectionImpl(m,child.claim_name,child.claim_target);
					if(child.claim_step!=null)
						child.claim_step.step(nxt,data);
					child.run_all(nxt);
					m.addChild(nxt);
				} else {
					child.run_all(m);
				}
			}			
		}
	}
		
	void setContents(File aFile, String aContents)
			throws FileNotFoundException, IOException {
		if (aFile == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!aFile.exists()) {
			throw new FileNotFoundException("File does not exist: " + aFile);
		}
		if (!aFile.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: "
					+ aFile);
		}
		if (!aFile.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: "
					+ aFile);
		}

		// use buffering
		Writer output = new BufferedWriter(new FileWriter(aFile));
		try {
			// FileWriter always assumes default encoding is OK!
			output.write(aContents);
		} finally {
			output.close();
		}
	}
	
	//
	// For debugging purposes, this method dumps the configuration tree to
	//
	private static String DUMPED_TREES_DIRNAME = "cspace-app-dumpedTrees";
	void dumpTreeToFile(String treeString) throws Exception {
		File dumpedTreeFilesDir = new File(FileUtils.getTempDirectoryPath() + "/" + DUMPED_TREES_DIRNAME);
		if (dumpedTreeFilesDir.exists() == false) {
			dumpedTreeFilesDir.mkdir();
		}
		File dumpTreeFile = new File(dumpedTreeFilesDir.getAbsolutePath() + "/dumpTree-" + UUID.randomUUID().toString() + ".xml");
		dumpTreeFile.createNewFile();
		this.setContents(dumpTreeFile, treeString);
		log.debug("Config XML tree dumped to: " + dumpTreeFile.getAbsolutePath());
	}
	
	public void dump()
	{
		if (log.isDebugEnabled() == true) {
			StringBuffer strBuf = new StringBuffer();
			dumpNode(strBuf);
			try {
				dumpTreeToFile(strBuf.toString());
			} catch (Exception e) {
				log.debug("Could not dump configuration tree to debug log file.", e);
			}
		}
	}
	
	private void dumpNode(StringBuffer output) {
		if (is_text) {
			output.append("\""+text+"\"");
		} else {
			output.append("<"+text+">");
			for(TreeNode child : children)
				child.dumpNode(output);
			output.append("</"+text+">");
		}
	}
}
