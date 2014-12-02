package com.mofang.chat.frontend.init.impl;

import com.mofang.chat.business.sysconf.SysObject;
import com.mofang.chat.frontend.init.AbstractInitializer;
import com.mofang.chat.frontend.global.GlobalConfig;

/**
 * 
 * @author zhaodx
 *
 */
public class GlobalObjectInitializer extends AbstractInitializer
{
	@Override
	public void load() throws Exception
	{
		SysObject.initRedisMaster(GlobalConfig.REDIS_MASTER_CONFIG_PATH);
		SysObject.initRedisSlave(GlobalConfig.REDIS_SLAVE_CONFIG_PATH);
		SysObject.initWriteQueue(GlobalConfig.WRITE_QUEUE_CONFIG_PATH);
		SysObject.initPushQueue(GlobalConfig.PUSH_QUEUE_CONFIG_PATH);
		SysObject.initMysql(GlobalConfig.MYSQL_CONFIG_PATH);
		SysObject.initHttpClient(GlobalConfig.HTTP_CLIENT_CONFIG_PATH);
		
		SysObject.USER_AUTH_URL = GlobalConfig.USER_AUTH_URL;
		SysObject.USER_INFO_URL = GlobalConfig.USER_INFO_URL;
		SysObject.ALLOW_SEND_URL = GlobalConfig.ALLOW_SEND_URL;
	}
}