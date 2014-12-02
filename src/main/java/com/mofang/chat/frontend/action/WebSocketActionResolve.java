package com.mofang.chat.frontend.action;

import org.json.JSONObject;

import com.mofang.framework.web.server.action.impl.AbstractActionResolve;

/**
 * 
 * @author zhaodx
 *
 */
public class WebSocketActionResolve extends AbstractActionResolve
{
	@Override
	protected String resolveAction(String content) throws Exception
	{
		JSONObject json = new JSONObject(content);
		return json.optString("act");
	}
}