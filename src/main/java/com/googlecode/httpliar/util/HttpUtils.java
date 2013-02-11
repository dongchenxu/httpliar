package com.googlecode.httpliar.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpFields;

/**
 * Http相关工具类
 * @author luanjia@taobao.com
 *
 */
public class HttpUtils {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	
	/**
	 * 从ContentType中获取字符编码
	 * @param fields
	 * @return
	 */
	public static Charset getCharset(HttpFields fields, Charset defaultCharset) {
		final String ct = fields.getStringField("Content-Type").toLowerCase();
		return getCharset(ct, defaultCharset);
	}
	
	/**
	 * 从ContentType的value中获取字符编码
	 * @param ct
	 * @param defaultCharset
	 * @return
	 */
	public static Charset getCharset(String contentTypeString, Charset defaultCharset) {
		Charset charset = defaultCharset;
		if( StringUtils.isBlank(contentTypeString) ) {
			return charset;
		}
		final String ct = contentTypeString.toLowerCase();
		final String look = "charset=";
        int beginIndex = ct.indexOf(look);
        if (beginIndex > 0) {
            beginIndex += look.length();
            String charSet = ct.substring(beginIndex).trim();
//            charSet = charSet.replace("_", "").replace("-", "");
            charset = Charset.forName(charSet);
        }
        return charset;
	}
	
	/**
	 * 从ContentType获取MIME类型
	 * @param fields
	 * @return
	 */
	public static String getMIME(HttpFields fields) {
		final String ct = fields.getStringField("Content-Type");
		if( StringUtils.isBlank(ct) ) {
			return null;
		}
		int endIndex = ct.indexOf(";");
		final String mime = endIndex > 0 ? ct.substring(0, endIndex) : ct;
		return mime;
	}
	
	/**
	 * 解析uri中的query字符串
	 * @param uri
	 * @return
	 */
	public static Map<String,String[]> parseRequestParamters(String uri) {
		final Map<String,String[]> params = new HashMap<String,String[]>();
		if( StringUtils.isBlank(uri) ) {
			return params;
		}
		try {
			final String queryString = new URI(uri).getQuery();
			final String[] kvStrs = StringUtils.split(queryString, "&");
			if( null != kvStrs ) {
				for( String kvStr : kvStrs ) {
					final String[] kv = StringUtils.split(kvStr, "=");
					if( null != kv
							&& kv.length == 2) {
						final String key = kv[0];
						final String val = kv[1];
						final String[] valSplit = StringUtils.split(val, ",");
						params.put(key, valSplit);
					}
				}
			}
		} catch (URISyntaxException e) {
			return params;
		}
		return params;
	}
	
}
