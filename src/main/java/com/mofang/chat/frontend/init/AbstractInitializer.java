package com.mofang.chat.frontend.init;

/**
 * 
 * @author zhaodx
 *
 */
public abstract class AbstractInitializer implements Initializer 
{
	@Override
	public void init() throws Exception
	{
		try
		{
			load();
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	public abstract void load() throws Exception;
}