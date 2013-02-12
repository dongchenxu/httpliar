package com.googlecode.httpliar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

import com.googlecode.httpliar.util.JvmUtils;
import com.googlecode.httpliar.util.JvmUtils.ShutdownHook;

/**
 * Hello world!
 * 
 */
public class Launcher {
	
	/**
	 * 从参数中解析配置文件
	 * @param args
	 * @return
	 * @throws IOException
	 */
	private static Configer getConfiger(String[] args) throws IOException {
		if( ArrayUtils.isNotEmpty(args) ) {
			return Configer.loadConfiger(new FileInputStream(new File(args[0])));
		}
		return Configer.loadDefaultConfiger();
	}
	
	public static void main(String[] args) throws Exception {
		final Configer configer = getConfiger(args);
		final HttpLiarServer server = new HttpLiarServer(configer);
		
		JvmUtils.registShutdownHook("httpliar-shutdown", new ShutdownHook(){

			@Override
			public void shutdown() throws Throwable {
				server.stopProxy();
			}
			
		});
		
		server.startProxy();
		
	}
	
}
