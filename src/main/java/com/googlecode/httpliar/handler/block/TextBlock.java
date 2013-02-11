package com.googlecode.httpliar.handler.block;

import java.nio.charset.Charset;

/**
 * 文本块
 * @author luanjia@taobao.com
 *
 */
public class TextBlock extends DataBlock {

	// 字符集
	private final Charset charset;
	private final String text;
	
	public TextBlock(byte[] datas, Charset charset) {
		super(datas);
		this.charset = charset;
		this.text = new String(datas, charset);
	}
	
	public TextBlock(String text, Charset charset) {
		this(text.getBytes(charset), charset);
	}

	/**
	 * 获取文本块中的文本内容
	 * @return
	 */
	public String getText() {
		return this.text;
	}
	
	/**
	 * 获取文本块中的字符集
	 * @return
	 */
	public Charset getCharset() {
		return charset;
	}
	
}
