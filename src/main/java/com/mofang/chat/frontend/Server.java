package com.mofang.chat.frontend;

import com.mofang.chat.frontend.action.HttpActionResolve;
import com.mofang.chat.frontend.action.WebSocketActionResolve;
import com.mofang.chat.frontend.init.Initializer;
import com.mofang.chat.frontend.init.impl.MainInitializer;
import com.mofang.chat.frontend.util.FrameworkLogger;
import com.mofang.chat.frontend.handler.WebSocketCloseHandlerImpl;
import com.mofang.chat.frontend.global.GlobalConfig;
import com.mofang.framework.web.server.action.ActionResolve;
import com.mofang.framework.web.server.conf.ChannelConfig;
import com.mofang.framework.web.server.conf.IdleConfig;
import com.mofang.framework.web.server.main.WebServer;
import com.mofang.framework.web.server.reactor.parse.PostDataParserType;

/**
 * 
 * @author zhaodx
 *
 */
public class Server
{
	public static void main(String[] args)
	{
		//String configpath = "/Users/milo/document/workspace/mofang.chat.frontend/src/main/resources/config.ini";
		
		if(args.length <= 0)
		{
			System.out.println("usage:java -server -Xms1024m -Xmx1024m -jar mofang-chat-frontend.jar configpath");
			System.exit(1);
		}
		String configpath = args[0];
		
		try
		{
			///服务器初始化
			System.out.println("prepare to initializing config......");
			Initializer initializer = new MainInitializer(configpath);
			initializer.init();
			System.out.println("initialize config completed!");
			
			///启动服务器
			ActionResolve websocketActionResolve = new WebSocketActionResolve();
			ActionResolve httpActionResolve = new HttpActionResolve();
			int port = GlobalConfig.SERVER_PORT;
			WebServer server = new WebServer(port, PostDataParserType.Json);
			
			///channel 配置
			ChannelConfig channelConfig = new ChannelConfig();
			channelConfig.setConnTimeout(GlobalConfig.CONN_TIMEOUT);
			channelConfig.setSoTimeout(GlobalConfig.READ_TIMEOUT);
			
			///Idle 配置
			IdleConfig idleConfig = new IdleConfig();
			idleConfig.setReadIdleTime(GlobalConfig.READ_IDLE_TIME);
			idleConfig.setWriteIdleTime(GlobalConfig.WRITE_IDLE_TIME);
			idleConfig.setAllIdleTime(GlobalConfig.ALL_IDLE_TIME);
			
			///webserver logger
			FrameworkLogger serverLogger = new FrameworkLogger();
			server.setWebServerLogger(serverLogger);
			
			server.setChannelConfig(channelConfig);
			server.setIdleConfig(idleConfig);
			server.setScanPackagePath(GlobalConfig.SCAN_PACKAGE_PATH);
			server.setWebSocketActionResolve(websocketActionResolve);
			server.setHttpActionResolve(httpActionResolve);
			server.setWebSocketCloseHandlerClass(WebSocketCloseHandlerImpl.class);
			try
			{
				System.out.println("Frontend Server Start on " + GlobalConfig.SERVER_PORT);
				server.start();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			System.out.println("frontend server start error. message:");
			e.printStackTrace();
		}
	}
}