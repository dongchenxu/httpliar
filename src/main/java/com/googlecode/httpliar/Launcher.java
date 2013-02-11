package com.googlecode.httpliar;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.googlecode.httpliar.util.JvmUtils;
import com.googlecode.httpliar.util.JvmUtils.ShutdownHook;

/**
 * Hello world!
 * 
 */
public class Launcher {
	
	private static final int DEFAULT_PROXY_PORT = 9666;
	
	/**
	 * 从参数中解析代理端口
	 * @param args
	 * @return
	 */
	private static int getProxyPort(String[] args) {
		if( ArrayUtils.isNotEmpty(args) ) {
			NumberUtils.toInt(args[0], DEFAULT_PROXY_PORT);
		}
		return DEFAULT_PROXY_PORT;
	}
	
	public static void main(String[] args) throws Exception {
		final int port = getProxyPort(args);
		final HttpLiarServer server = new HttpLiarServer(port);
		
		JvmUtils.registShutdownHook("httpliar-shutdown", new ShutdownHook(){

			@Override
			public void shutdown() throws Throwable {
				server.stopProxy();
			}
			
		});
		
		server.startProxy();
		
	}
	
}
