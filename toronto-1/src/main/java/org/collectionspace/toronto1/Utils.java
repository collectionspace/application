package org.collectionspace.toronto1;

import javax.servlet.http.HttpServletRequest;

public class Utils {
	public static String[] getAccordion(HttpServletRequest req,String type) {
		String atype=(String) req.getSession().getAttribute("accordion-type");
		String[] ids=(String[]) req.getSession().getAttribute("accordion");
		if(atype==null || !atype.equals(type))
			return null;
		return ids;
	}
	
	public static void removeAccordion(HttpServletRequest req) {
		req.getSession().removeAttribute("accordion");
		req.getSession().removeAttribute("accordion-type");
	}
	
	public static void setAccordion(HttpServletRequest req,String type,String[] values) {
		req.getSession().setAttribute("accordion",values);
		req.getSession().setAttribute("accordion-type",type);
	}
}
