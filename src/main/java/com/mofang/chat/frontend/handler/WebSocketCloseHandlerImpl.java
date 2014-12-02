package com.mofang.chat.frontend.handler;

import io.netty.channel.ChannelHandlerContext;

import com.mofang.chat.frontend.logic.UserLogic;
import com.mofang.chat.frontend.logic.impl.UserLogicImpl;
import com.mofang.framework.web.server.handler.WebSocketCloseHandler;

/**
 * 
 * @author zhaodx
 *
 */
public class WebSocketCloseHandlerImpl implements WebSocketCloseHandler
{
	private UserLogic userLogic = UserLogicImpl.getInstance();
	
	@Override
	public void handle(ChannelHandlerContext context)
	{
		userLogic.leave(context);
	}
}