package org.collectionspace.chain.config;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestConfig {
	@Test public void testBasic() throws ConfigLoadFailedException {
		ConfigLoadController config_controller=new ConfigLoadController(null);
		config_controller.addSearchSuffix("test-config-loader.xml");
		config_controller.go();
		assertEquals("success",config_controller.getOption("test"));
		assertEquals("success",config_controller.getOption("test-properties"));
	}

	
	@Test(expected=ConfigLoadFailedException.class) public void testMissingProperties() throws ConfigLoadFailedException {
		ConfigLoadController config_controller=new ConfigLoadController(null);
		config_controller.addSearchSuffix("bad-properties.xml");
		config_controller.go();
	}
}
