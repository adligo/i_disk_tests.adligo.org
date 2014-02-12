package org.adligo.i.disk;

import java.io.File;

import org.adligo.i.util.shared.StringUtils;

public class BaseDir {
	public static String baseDir = "";

	static {
		File file = new File("source");
		if (file.exists()) {
			baseDir = "source" + File.separator;
		}
		file = new File("platforms");
		if (file.exists()) {
			baseDir = "platforms" + File.separator + "jse_main" + File.separator + "source" + File.separator;
		}
	}
	
	/**
	 * this setups a base directory for your module (ie i_disk_tests)
	 * so it can be found from the main build (ie jse_main)
	 * @param module
	 * @return
	 */
	public static String getBaseDir(String module) {
		if (StringUtils.isEmpty(baseDir)) {
			return "";
		}
		return baseDir + module + File.separator;
	}
}
