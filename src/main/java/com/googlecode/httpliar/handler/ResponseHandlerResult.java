package com.googlecode.httpliar.handler;

import com.googlecode.httpliar.handler.block.DataBlock;


/**
 * 应答处理结果
 * @author luanjia@taobao.com
 *
 */
public class ResponseHandlerResult extends HandlerResult {

	private final DataBlock block;
	
	public ResponseHandlerResult(final DataBlock block) {
		this.block = block;
	}

	/**
	 * 获取数据块
	 * @return
	 */
	public DataBlock getBlock() {
		return block;
	}

}
