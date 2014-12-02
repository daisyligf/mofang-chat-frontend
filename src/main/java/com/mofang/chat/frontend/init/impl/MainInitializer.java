package com.mofang.chat.frontend.init.impl;

import org.apache.log4j.PropertyConfigurator;

import com.mofang.chat.frontend.init.AbstractInitializer;
import com.mofang.chat.frontend.init.Initializer;
import com.mofang.chat.frontend.global.GlobalConfig;

/**
 * 
 * @author zhaodx
 *
 */
public class MainInitializer extends AbstractInitializer
{
	private String configPath;
	
	public MainInitializer(String configPath)
	{
		this.configPath = configPath;
	}
	
	@Override
	public void load() throws Exception
	{
		Initializer globalConf = new GlobalConfigInitializer(configPath);
		globalConf.init();
		
		PropertyConfigurator.configure(GlobalConfig.LOG4J_CONFIG_PATH);
		
		Initializer globalObject = new GlobalObjectInitializer();
		globalObject.init();
	}
}