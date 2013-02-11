package com.googlecode.httpliar.handler.block;

/**
 * 数据块
 * @author luanjia@taobao.com
 *
 */
public class DataBlock {

	// 数据存储块
	private final byte[] datas;
	
	public DataBlock(byte[] datas) {
		this.datas = datas;
	}

	/**
	 * 获取数据块中的数据
	 * @return
	 */
	public byte[] getDatas() {
		return datas.clone();
	}
	
}
