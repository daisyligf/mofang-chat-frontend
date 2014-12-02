package com.mofang.chat.frontend.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import com.mofang.chat.frontend.global.GlobalObject;
import com.mofang.framework.web.server.logger.WebServerLogger;
import com.mofang.framework.web.server.logger.WebServerLoggerEntity;

public class FrameworkLogger implements WebServerLogger
{

	@Override
	public void info(WebServerLoggerEntity entity) 
	{
		try
		{
			String message = entity.getMessage();
			ChannelHandlerContext context = entity.getContext();
			long userId = 0L;
			if(null != context)
			{
				Channel channel = context.channel();
				if(null != channel)
					userId = ContextUtil.getUserIdByChannel(channel);
			}
			GlobalObject.INFO_LOG.info("userId=" + userId + "&message=" + message);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at FrameworkLogger.info throw an error. ", e);
		}
	}

	@Override
	public void warning(WebServerLoggerEntity entity)
	{
		
	}

	@Override
	public void error(WebServerLoggerEntity entity) 
	{
		try
		{
			String message = entity.getMessage();
			ChannelHandlerContext context = entity.getContext();
			long userId = 0L;
			if(null != context)
			{
				Channel channel = context.channel();
				if(null != channel)
					userId = ContextUtil.getUserIdByChannel(channel);
			}
			GlobalObject.ERROR_LOG.error("userId=" + userId + "&message=" + message, entity.getCause());
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at FrameworkLogger.info throw an error. ", e);
		}
	}
}