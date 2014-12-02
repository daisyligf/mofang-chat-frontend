package com.mofang.chat.frontend.controller.http;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.frontend.controller.AbstractActionExecutor;
import com.mofang.chat.frontend.logic.MessageLogic;
import com.mofang.chat.frontend.logic.impl.MessageLogicImpl;
import com.mofang.framework.web.server.annotation.Action;
import com.mofang.framework.web.server.reactor.context.HttpRequestContext;
import com.mofang.framework.web.server.reactor.context.RequestContext;

/**
 * 
 * @author zhaodx
 *
 */
@Action(url="push")
public class PushNotifyAction extends AbstractActionExecutor
{
	private MessageLogic logic = MessageLogicImpl.getInstance();
	
	@Override
	public ResultValue exec(RequestContext context) throws Exception
	{
		HttpRequestContext ctx = (HttpRequestContext)context;
		logic.pushNotify(ctx);
		ResultValue result = new ResultValue();
		result.setAction("push_response");
		result.setCode(0);
		return result;
	}
}