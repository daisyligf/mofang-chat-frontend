package com.mofang.chat.frontend.controller;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.business.sysconf.ReturnCodeHelper;
import com.mofang.chat.frontend.global.GlobalObject;
import com.mofang.framework.web.server.reactor.context.RequestContext;
import com.mofang.framework.web.server.reactor.proxy.ActionExecutor;

/**
 * 
 * @author zhaodx
 *
 */
public abstract class AbstractActionExecutor implements ActionExecutor
{
	@Override
	public String execute(RequestContext context)
	{
		long start = System.currentTimeMillis();
		long end;
		String clsName = getClass().getSimpleName();
		try
		{
			ResultValue result = exec(context);
			end = System.currentTimeMillis();
			String retrurn = result.toJsonString();
			String log = (end - start) + " | " + clsName + " | " + result.getCode() + " | " + "request=" + context.getPostData() + "&response=" + retrurn;
			GlobalObject.INFO_LOG.info(log);
			return retrurn;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at " + clsName + ".execute throw an error.", e);
			return ReturnCodeHelper.serverError().toJsonString();
		}
	}
	
	protected abstract ResultValue exec(RequestContext context) throws Exception;
}