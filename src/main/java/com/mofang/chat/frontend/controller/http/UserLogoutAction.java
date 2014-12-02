package com.mofang.chat.frontend.controller.http;

import com.mofang.chat.business.sysconf.ResultValue;
import com.mofang.chat.frontend.controller.AbstractActionExecutor;
import com.mofang.chat.frontend.logic.UserLogic;
import com.mofang.chat.frontend.logic.impl.UserLogicImpl;
import com.mofang.framework.web.server.annotation.Action;
import com.mofang.framework.web.server.reactor.context.HttpRequestContext;
import com.mofang.framework.web.server.reactor.context.RequestContext;

/**
 * 
 * @author zhaodx
 *
 */
@Action(url="user_logout")
public class UserLogoutAction extends AbstractActionExecutor 
{
	private UserLogic logic = UserLogicImpl.getInstance();
	
	@Override
	protected ResultValue exec(RequestContext context) throws Exception
	{
		HttpRequestContext ctx = (HttpRequestContext)context;
		logic.logout(ctx);
		ResultValue result = new ResultValue();
		result.setAction("user_logout_response");
		result.setCode(0);
		return result;
	}
}