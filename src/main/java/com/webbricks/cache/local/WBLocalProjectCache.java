package com.webbricks.cache.local;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.webbricks.cache.WBProjectCache;
import com.webbricks.cms.Pair;
import com.webbricks.cmsdata.WBArticle;
import com.webbricks.cmsdata.WBProject;
import com.webbricks.datautility.AdminDataStorage;
import com.webbricks.datautility.AdminDataStorageFactory;
import com.webbricks.exception.WBIOException;

public class WBLocalProjectCache implements WBProjectCache {
	private WBProject project;
	Pair<String, String> defaultLocale;
	Set<String> supportedLanguages;
	private AdminDataStorage dataStorage;
	private static final Object lock = new Object();

	public WBLocalProjectCache()
	{
		dataStorage = AdminDataStorageFactory.getInstance();
		try
		{
			if (dataStorage != null)
			{
				Refresh();
			}
		} catch (WBIOException e)
		{
			
		}
	}
	public String getDefaultLanguage() throws WBIOException
	{
		return project.getDefaultLanguage();
	}
	
	public Pair<String, String> getDefaultLocale() throws WBIOException
	{
		return defaultLocale;
	}
	public Set<String> getSupportedLanguages() throws WBIOException
	{
		return supportedLanguages;
	}
	public WBProject getProject() throws WBIOException
	{
		return project;
	}
	
	private WBProject createDefaultProject() throws WBIOException
	{
		WBProject project = new WBProject();
		project.setKey(WBProject.PROJECT_KEY);
		project.setDefaultLanguage("en");
		project.setSupportedLanguages("en");
		project.setLastModified(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());	
		dataStorage.addWithKey(project);		
		return project;
	}
	@Override
	public void Refresh() throws WBIOException {
		synchronized (lock) {
			project = dataStorage.get(WBProject.PROJECT_KEY, WBProject.class);
			if (null == project)
			{
				project = createDefaultProject();
			}
			String defaultLanguage = project.getDefaultLanguage();
			String[] langs_ = defaultLanguage.split("_");
			if (langs_.length == 1)
			{
				defaultLocale = new Pair<String, String>(langs_[0], "");
			} else
			if (langs_.length == 2)
			{
				defaultLocale = new Pair<String, String>(langs_[0], langs_[1]);
			} else
				throw new WBIOException("Invalid default language");
			
			supportedLanguages = project.getSupportedLanguagesSet();
		}
	}
}
