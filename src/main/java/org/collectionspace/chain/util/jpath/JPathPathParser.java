package org.collectionspace.chain.util.jpath;

import java.util.ArrayList;
import java.util.List;

public class JPathPathParser {
	private enum State { FIRST };
		
	private List<Object> out=new ArrayList<Object>();
	private char[] in;
	private int index=0;
	private State state=State.FIRST;
	
	public JPathPathParser(String path) throws InvalidJPathException {
		in=path.toCharArray();
		while(addComponent())
			;
		if(out.size()==0)
			throw new InvalidJPathException("Empty JPath is invalid");
	}

	private char nextChar(int inc) {
		if(index>=in.length)
			return '\0'; // Invalid unicode, anyway
		char out=in[index];
		index+=inc;
		return out;
	}
	
	private boolean isHexDigit(char c) {
		return (c>='0' && c<='9') || (c>='a' && c<='f') || (c>='A' && c<='F');
	}
	
	private char nextHexDigit(int inc) throws InvalidJPathException {
		char out=nextChar(inc);
		if(!isHexDigit(out))
			throw new InvalidJPathException("Invalid hex digit '"+out+"'");
		return out;
	}
	
	private char nextLetter(boolean string_context) throws InvalidJPathException {
		char first=nextChar(1);
		if(first!='\\') {
			if(first=='\0' && string_context)
				throw new InvalidJPathException("Bad character \0");
			if(string_context && first=='"')
				return '\0';
			return first; // Most characters except...
		}
		// ...a backslash!
		char second=nextChar(1);
		if(second=='u') {
			char a=nextHexDigit(1);
			char b=nextHexDigit(1);
			char c=nextHexDigit(1);
			char d=nextHexDigit(1);
			return (char)Integer.parseInt(new String(new char[]{a,b,c,d}),16);
		}
		if(!string_context)
			throw new InvalidJPathException("Only \\u allowed in dot components");
		switch(second) {
		case 'r': return '\r';
		case 'n': return '\n';
		case 't': return '\t';
		case 'b': return '\b';
		case 'f': return '\f';
		case '/': return '/';
		case '\\': return '\\';
		case '"': return '"';
		}
		throw new InvalidJPathException("Invalid escape sequence \\"+second);
	}
	
	private char getIdentifierChar(boolean first) throws InvalidJPathException {
		char letter=nextChar(0);
		if(letter!='\\') {
			// Normal char, must be letter, $ or _
			if(!Character.isLetter(letter) && letter!='$' && letter!='_' && letter!='\0' && letter!='.' && letter!='[') {
				if(first)
					throw new InvalidJPathException("Invalid character to start idefntifier '"+letter+"'");
				if(!Character.isDigit(letter) && Character.getType(letter)!=Character.CONNECTOR_PUNCTUATION)
					throw new InvalidJPathException("Invalid character to continue idefntifier '"+letter+"'");
			}
		}
		return nextLetter(false);
	}	

	private String getString() throws InvalidJPathException {
		StringBuffer out=new StringBuffer();
		nextChar(1); // We chould only be called if "
		while(true) {
			char next=nextLetter(true);
			if(next=='\0') {
				// IE, was unescaped ".
				return out.toString();
			}
			out.append(next);
		}
	}

	private void addBracketComponent() throws InvalidJPathException {
		boolean first=true;
		char type=nextChar(0);
		if(type=='"') {
			out.add(getString());
		} else {
			if(!Character.isDigit(type))
				throw new InvalidJPathException("Invalid character "+type);
			StringBuffer number=new StringBuffer();
			while(Character.isDigit(nextChar(0))) {
				number.append(nextChar(1));
			}
			out.add(new Integer(Integer.parseInt(number.toString())));
		}
		char last=nextChar(1);
		if(last!=']')
			throw new InvalidJPathException("Bracket component must end with ]");
	}
	
	private void addDotComponent() throws InvalidJPathException {
		StringBuffer name=new StringBuffer();
		boolean first=true;
		while(true) {
			char next=getIdentifierChar(first);
			if(next=='\0' || next=='.' || next=='[') {
				if(first)
					throw new InvalidJPathException("Component cannot be empty");
				out.add(name.toString());
				if(next!='\0')
					index--;
				return;
			}
			name.append(next);
			first=false;
		}
	}
	
	private boolean addComponent() throws InvalidJPathException {
		char type=nextChar(1);
		if(type=='.')
			addDotComponent();
		else if(type=='[')
			addBracketComponent();
		else if(type=='\0')
			return false;
		else
			throw new InvalidJPathException("Invalid character at start of component '"+type+"'");
		return true;
	}
	
	List<Object> getResult() { return out; }
}
