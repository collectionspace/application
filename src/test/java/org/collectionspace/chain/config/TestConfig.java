package org.collectionspace.chain.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TestConfig {
	@Test public void testBasic() throws ConfigLoadFailedException {
		System.setProperty("test_property","org/collectionspace/chain/config");
		ConfigLoadController config_controller=new ConfigLoadController(null);
		config_controller.addSearchSuffix("test-config-loader.xml");
		config_controller.go();
		assertEquals("success",config_controller.getOption("test"));
		assertEquals("success",config_controller.getOption("test-properties"));
		assertEquals("alt-success",config_controller.getOption("test-alt-properties"));
	}
	
	@Test public void testFileSystem() throws Exception {
		File outfile=File.createTempFile("fs-test",".properties");
		outfile.deleteOnExit();
		FileOutputStream out=new FileOutputStream(outfile);
		IOUtils.write("test.fs=successful\n",out);
		out.close();
		System.setProperty("test_fs_path",outfile.getCanonicalPath());
		ConfigLoadController config_controller=new ConfigLoadController(null);
		config_controller.addSearchSuffix("test-config-loader.xml");
		config_controller.go();
		assertEquals("successful",config_controller.getOption("test-fs-properties"));
	}
}
