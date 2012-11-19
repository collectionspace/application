/**
 * 
 */
package org.collectionspace.csp.helper.core;

import java.io.InputStream;

import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.CacheException;
/**
 * @author pschmitz
 *
 */
public class ResponseCache {
	private static final Logger log=LoggerFactory.getLogger(ConfigFinder.class);

	private static CacheManager cacheManager;
	private static ResponseCache instance = new ResponseCache();
	
	public static final String USER_PERMS_CACHE = "userperms";

	
	private ResponseCache() {
		if(instance != null)
			throw new RuntimeException("ResponseCache Ctor called when singleton exists!");
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/ehcache.xml";
		InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if(in==null) {
			log.warn("ResponseCache could not find the ehcache.xml config file as a resource (will use defaults)!");
			log.warn("ResponseCache using path: "+path);
			this.cacheManager = CacheManager.create();
		} else {
			 cacheManager = CacheManager.create(in);
		}
	}
	
	public static Ehcache getCache(String name) {
		Ehcache cache = cacheManager.getCache(name);
		return cache;
	}

	public static void clearCache(String name) {
		// Update cache with the permissions for this user and tenant.
		try {
			Ehcache cache = cacheManager.getCache(name);
			cache.removeAll();
		} catch (IllegalStateException ise) {
			log.warn("WebLoginStatus - '"+name+"' cache not active: "+ise.getLocalizedMessage());
		} catch (CacheException ce) {
			log.warn("WebLoginStatus - '"+name+"' cache exception:"+ce.getLocalizedMessage());
		}
	}

}
