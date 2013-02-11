package com.googlecode.httpliar.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handler处理结果
 * @author luanjia@taobao.com
 *
 */
public class HandlerResult {

	private final Map<String, String> headerModifier;
	private final Set<String> headerRemover;
	
	public HandlerResult() {
		this.headerModifier = new HashMap<String, String>();
		this.headerRemover = new HashSet<String>();
	}
	
	/**
	 * 设置Header中的K-V
	 * @param name
	 * @param value
	 */
	public void setHeader(String name, String value) {
		headerModifier.put(name, value);
	}
	
	/**
	 * 删除Header中指定Key
	 * @param name
	 */
	public void removeHeader(String name) {
		headerRemover.add(name);
	}
	
	/**
	 * 获取Header需要修改的Key-Value
	 * @return
	 */
	public Map<String, String> getHeaderModifier() {
		return new HashMap<String, String>(headerModifier);
	}
	
	/**
	 * 获取Header需要删除的Key
	 * @return
	 */
	public Set<String> getHeaderRemover() {
		return new HashSet<String>(headerRemover);
	}
	
}
