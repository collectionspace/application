package org.collectionspace.xxu.test.main;

public class FieldImpl implements Field {
	private String id="";
	private Validation v;
	private Suggest s;
	private CSpace cspace;
	
	public void setID(String in) { id=in; }	
	public String getID() { return id; }
	
	public void addValidation(Validation in) { v=in; in.setField(this); }
	public boolean validate(String in) { return v.validate(in); }
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		out.append("  BEGIN Field\n");
		out.append("    id="+id+"\n");
		if(v!=null)
			out.append(v.dump());
		out.append("  END   Field\n");
		return out.toString();
	}
	
	public void setCSpace(CSpace in) { cspace=in; }
	public CSpace getCSpace() { return cspace; }
	
	public void addSuggest(Suggest in) { s=in; in.setField(this); }
	
	public String[] suggest(String in,int num) { 
		String[] out=new String[0];
		if(s!=null)
			out=s.suggest(in,num);
		if(out==null)
			out=new String[0];
		return out;
	}
}
