package com.googlecode.httpliar.mime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.ArrayUtils;

/**
 * HTTP MIME 类型定义
 * @author luanjia@taobao.com
 *
 */
public class MIME {

	private static final String[] _HTML = new String[]{
		"text/html",
	};
	
	private static final String[] _XML = new String[]{
		"text/xml",
		"application/xhtml+xml",
		"application/xml",
	};
	
	private static final String[] _CSS = new String[]{
		"text/css",
	};
	
	private static final String[] _JSON = new String[]{
		"application/json",
	};
	
	private static final String[] _TEXT = new String[]{
		"text/plain",
		"text/javascript",
		"application/x-javascript",
	};
	
	/**
	 * 列出所支持的 mime
	 * @return
	 */
	public static Collection<String> listSupports() {
		final Collection<String> mimes = new ArrayList<String>();
		mimes.addAll(Arrays.asList(_HTML));
		mimes.addAll(Arrays.asList(_XML));
		mimes.addAll(Arrays.asList(_CSS));
		mimes.addAll(Arrays.asList(_JSON));
		mimes.addAll(Arrays.asList(_TEXT));
		return mimes;
	}
	
	/**
	 * 判断当前mime是否在支持范围
	 * @param mime
	 * @return
	 */
	public static boolean isSupport(final String mime) {
		return listSupports().contains(mime);
	}
	
	public static boolean isHtml(final String mime) {
		return ArrayUtils.contains(_HTML, mime);
	}
	
	public static boolean isCss(final String mime) {
		return ArrayUtils.contains(_CSS, mime);
	}

	public static boolean isJson(final String mime) {
		return ArrayUtils.contains(_JSON, mime);
	}

	public static boolean isXml(final String mime) {
		return ArrayUtils.contains(_XML, mime);
	}

	public static boolean isText(final String mime) {
		// text 比较特殊，因为html/css/json/xml都算是text
		return ArrayUtils.contains(_TEXT, mime)
				|| isHtml(mime)
				|| isCss(mime)
				|| isJson(mime)
				|| isXml(mime)
		;
	}
	
}
