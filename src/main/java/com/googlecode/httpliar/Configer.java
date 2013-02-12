package com.googlecode.httpliar;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 配置对象
 * @author vlinux
 *
 */
public class Configer {

	private static final int DEFAULT_PROXY_PORT = 9666;
	private int proxyPort;
	
	private final String MIME_TYPE_HTML = "HTML";
	private final String MIME_TYPE_JSON = "JSON";
	private final String MIME_TYPE_TEXT = "TEXT";
	private final String MIME_TYPE_CSS  = "CSS";
	private final String MIME_TYPE_XML  = "XML";
	
	
	private final Map<String, Set<String>> mimeCfgs = new HashMap<String, Set<String>>();
	
	/**
	 * 传入的mime类型是否在支持的范围内
	 * @param mime
	 * @return
	 */
	public boolean isMimeSupport(final String mime) {
		for( Set<String> mimes : mimeCfgs.values() ) {
			if( mimes.contains(mime) ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断mimes集合中是否包含了mime
	 * @param mimes
	 * @param mime
	 * @return
	 */
	private boolean isMimeContains(final Set<String> mimes, final String mime) {
		if( CollectionUtils.isEmpty(mimes) ) {
			return false;
		}
		return mimes.contains(mime);
	}
	
	public boolean isMimeHtml(final String mime) {
		return isMimeContains(mimeCfgs.get(MIME_TYPE_HTML), mime);
	}
	
	public boolean isMimeCss(final String mime) {
		return isMimeContains(mimeCfgs.get(MIME_TYPE_CSS), mime);
	}

	public boolean isMimeJson(final String mime) {
		return isMimeContains(mimeCfgs.get(MIME_TYPE_JSON), mime);
	}

	public boolean isMimeXml(final String mime) {
		return isMimeContains(mimeCfgs.get(MIME_TYPE_XML), mime);
	}

	public boolean isMimeText(final String mime) {
		return isMimeContains(mimeCfgs.get(MIME_TYPE_TEXT), mime);
	}
	
	/**
	 * 获取代理端口
	 * @return
	 */
	public int getProxyPort() {
		return proxyPort;
	}
	
	
	/**
	 * 解析配置文件中的代理服务器端口
	 * @param doc
	 */
	private static void parseProxyPort(final Configer cfg, final Document doc) {
		final Element proxyPortElement = doc.select("httpliar > proxy-config > proxy-port").first();
		if( null != proxyPortElement ) {
			cfg.proxyPort = NumberUtils.toInt(proxyPortElement.text(), DEFAULT_PROXY_PORT);
		} else {
			cfg.proxyPort = DEFAULT_PROXY_PORT;
		}
	}
	
	/**
	 * 解析配置文件中mime的配置
	 * @param cfg
	 * @param doc
	 */
	private static void parseMimeConfig(final Configer cfg, final Document doc) {
		final Iterator<Element> itMimeCfgElement = doc.select("httpliar > mimes-config > mimes").listIterator();
		while( itMimeCfgElement.hasNext() ) {
			final Element mimesElement = itMimeCfgElement.next();
			final String typeAttr = mimesElement.attr("type");
			if( StringUtils.isBlank(typeAttr) ) {
				continue;
			}
			final Set<String> mimes = new HashSet<String>();
			cfg.mimeCfgs.put(typeAttr.toUpperCase(), mimes);
			
			final Iterator<Element> itMimeElement = mimesElement.select("mime").listIterator();
			while( itMimeElement.hasNext() ) {
				final String mimeStr = itMimeElement.next().text();
				if( StringUtils.isBlank(mimeStr) ) {
					continue;
				}
				mimes.add(mimeStr);
			}
		}
	}
	
	/**
	 * 解析并生成配置文件
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static Configer loadConfiger(final InputStream is) throws IOException {
		final Document doc = Jsoup.parse(is, "UTF-8", StringUtils.EMPTY);
		final Configer cfg = new Configer();
		
		// proxy-port
		parseProxyPort(cfg, doc);
		
		// mimes
		parseMimeConfig(cfg, doc);
		
		return cfg;
	}
	
	/**
	 * 获取默认配置文件
	 * @return
	 * @throws IOException
	 */
	public static Configer loadDefaultConfiger() throws IOException {
		return loadConfiger(Configer.class.getResourceAsStream("/httpliar-mime.xml"));
	}
	
}
