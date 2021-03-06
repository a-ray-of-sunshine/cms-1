/*
 *   Copyright 2014 Webpagebytes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.webpagebytes.cms.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;

import com.webpagebytes.cms.WPBFilePath;
import com.webpagebytes.cms.WPBAdminDataStorage;
import com.webpagebytes.cms.WPBCacheFactory;
import com.webpagebytes.cms.WPBFileInfo;
import com.webpagebytes.cms.WPBFileStorage;
import com.webpagebytes.cms.WPBFilesCache;
import com.webpagebytes.cms.WPBAdminDataStorage.AdminQueryOperator;
import com.webpagebytes.cms.WPBAdminDataStorage.AdminSortOperator;
import com.webpagebytes.cms.cmsdata.WPBFile;
import com.webpagebytes.cms.cmsdata.WPBResource;
import com.webpagebytes.cms.cmsdata.WPBUri;
import com.webpagebytes.cms.engine.DefaultWPBCacheFactory;
import com.webpagebytes.cms.engine.WPBAdminDataStorageListener;
import com.webpagebytes.cms.exception.WPBException;
import com.webpagebytes.cms.exception.WPBIOException;
import com.webpagebytes.cms.utility.ContentTypeDetector;


public class FileController extends Controller implements WPBAdminDataStorageListener{
	public static final String PUBLIC_BUCKET = "public";
	
	private FileValidator validator;
	private WPBFilesCache filesCache;
	
	private static final int MAX_DIR_DEPTH = 25;
	
	public FileController()
	{
		validator = new FileValidator();
		WPBCacheFactory wbCacheFactory = DefaultWPBCacheFactory.getInstance();
		filesCache = wbCacheFactory.getFilesCacheInstance();	
		adminStorage.addStorageListener(this);
	}
	
	public<T> void notify (T t, AdminDataStorageOperation o, Class<? extends Object> type)
	{
		try
		{
			if (type.equals(WPBFile.class))
			{
				filesCache.Refresh();
			}
		} catch (WPBIOException e)
		{
			// TBD
		}
	}

	public static String getDirectoryFullPath(String externalKey, WPBAdminDataStorage adminStorage) throws WPBException 
	{
	    if (externalKey == null || externalKey.length() == 0)
	    {
	        return "";
	    }
	    String result = "";
	    String parentExtKey = externalKey;
	    boolean completed = false;
	    for(int i = 0; i< MAX_DIR_DEPTH; i++)
	    {
	        if (parentExtKey != null && parentExtKey.length()>0)
	        {
	            WPBFile parent = getDirectory(parentExtKey, adminStorage);
	            if (parent != null)
	            {
	                result = parent.getFileName() + "/" + result;
	                parentExtKey = parent.getOwnerExtKey();
	            } else
	            {
	                throw new WPBException("WPBFile should not be null");
	            }
	        } else
	        {
	            completed = true;
	            break;
	        }
	    }
	    if (completed)
	    {
	        if (result.startsWith("/")) result = result.substring(1);
	        if (result.endsWith("/")) result = result.substring(0, result.length()-1);
	        
	        return result;
	    } else
	    {
	        throw new WPBException("Cannot calculate full dir path");
	    }
	}
	
	public static WPBFile getDirectory(String externalKey, WPBAdminDataStorage adminStorage) throws WPBException
	{
	    List<WPBFile> result = adminStorage.query(WPBFile.class, "externalKey", AdminQueryOperator.EQUAL, externalKey);
	    if (result.size() == 1)
	    {
	        WPBFile file = result.get(0); 
	        if (file.getDirectoryFlag() != null && file.getDirectoryFlag() == 1)
	        {
	            return file;
	        }
	    }
	    return null;
	}
	
	private String getDirectoryFromLongName(String longName)
	{
	    int index = longName.lastIndexOf('/');
	    if (index>0)
	    {
	        return longName.substring(0, index);
	    }
	    return null;
	}
	private String getFileNameFromLongName(String longName)
	{
        int index = longName.lastIndexOf('/');
        if (index>0)
        {
            return longName.substring(index+1);
        }
        return longName;
	    
	}
	private List<WPBFile> getFilesFromDirectory(WPBFile directory) throws WPBException
	{
	    if (null == directory || directory.getDirectoryFlag() != 1)
	    {
	        return new ArrayList<WPBFile>();
	    }

        String ownerExtKey = directory.getExternalKey();
        return adminStorage.query(WPBFile.class, "ownerExtKey", AdminQueryOperator.EQUAL, ownerExtKey);       
   }
	
	private WPBFile getFileFromDirectory(WPBFile directory, String fileName) throws WPBException
	{
	    String ownerExtKey = "";
	    if (null != directory)
	    {
	        ownerExtKey = directory.getExternalKey();
	    }
	    Set<String> propertyNames = new HashSet<String>();
	    propertyNames.add("ownerExtKey");
	    propertyNames.add("fileName");
	    HashMap<String, AdminQueryOperator> operators = new HashMap<String, AdminQueryOperator>();
	    operators.put("ownerExtKey", AdminQueryOperator.EQUAL);
	    operators.put("fileName", AdminQueryOperator.EQUAL);
	    
	    HashMap<String, Object> values = new HashMap<String, Object>();
        values.put("ownerExtKey", ownerExtKey);
        values.put("fileName", fileName);
        
	    List<WPBFile> result = adminStorage.queryEx(WPBFile.class, propertyNames, operators, values);
	    if (result.size() == 1)
	    {
	        return result.get(0);
	    }
	    return null;
	}
	
   private void deleteFile(WPBFile file, int level) throws WPBException, IOException
    {
        //we need to protect from infinite loops
       if (level > MAX_DIR_DEPTH) return;
       
     
        if (file.getDirectoryFlag()!= null && file.getDirectoryFlag() == 1)
        {
            List<WPBFile> files = getFilesFromDirectory(file);
            for(WPBFile afile: files)
            {
                deleteFile(afile, level+1);
            }
        } 
        if (file.getBlobKey() != null && file.getBlobKey().length()>0)
        {
            WPBFilePath contentFile = new WPBFilePath(PUBLIC_BUCKET, file.getBlobKey());
            
            cloudFileStorage.deleteFile(contentFile);             
        }
        adminStorage.delete(file.getExternalKey(), WPBFile.class);
    }

	private WPBFile createDirectory(WPBFile parentdirectory, String dirName) throws WPBException
	{
	    WPBFile file = new WPBFile();
	    file.setDirectoryFlag(1);
	    file.setFileName(dirName);
	    file.setSize(0L);
	    file.setExternalKey(adminStorage.getUniqueId());
	    file.setLastModified(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
	    if (parentdirectory == null)
	    {
	        file.setOwnerExtKey("");
	    } else
	    {
	        file.setOwnerExtKey(parentdirectory.getExternalKey());
	    }
	    file.setVersion(UUID.randomUUID().toString());
	    
	    file = adminStorage.addWithKey(file);
	    
        WPBResource resource = new WPBResource(file.getExternalKey(), file.getFileName(), WPBResource.FILE_TYPE);
        try
        {
            adminStorage.addWithKey(resource);
        } catch (WPBException e)
        {
            // do not propagate further
        }

	    return file;
	}
	
	private void addFileToDirectory(WPBFile parentDirectory, WPBFile file, InputStream is) throws WPBException, IOException
	{
	    String dirPath = "";
	    String filePath  = null;
	    if (parentDirectory != null)
	    {
	        dirPath = getDirectoryFullPath(parentDirectory.getExternalKey(), adminStorage);
	    }
	    if (dirPath.length()>0)
	    {
	        filePath = dirPath + "/" + file.getFileName();
	    } else
	    {
	        filePath = file.getFileName();
	    }
	     
         WPBFilePath cloudFile = new WPBFilePath(PUBLIC_BUCKET, filePath);
         cloudFileStorage.storeFile(is, cloudFile);
         cloudFileStorage.updateContentType(cloudFile, ContentTypeDetector.fileNameToContentType(file.getFileName()));
    
         WPBFileInfo fileInfo = cloudFileStorage.getFileInfo(cloudFile);
         file.setBlobKey(cloudFile.getPath());
         file.setHash(fileInfo.getCrc32());
         file.setSize(fileInfo.getSize());
         file.setContentType(fileInfo.getContentType());
         file.setAdjustedContentType(file.getContentType());
         file.setDirectoryFlag(0);
         if (parentDirectory != null)
         {
             file.setOwnerExtKey(parentDirectory.getExternalKey());
         } else
         {
             file.setOwnerExtKey("");
         }
         file.setVersion(UUID.randomUUID().toString());
         
         WPBResource resource = new WPBResource(file.getExternalKey(), file.getFileName(), WPBResource.FILE_TYPE);

       	 adminStorage.delete(file.getExternalKey(), WPBFile.class);
    	 adminStorage.addWithKey(file);
    	 
         try
         {
        	 adminStorage.delete(resource.getRkey(), WPBResource.class);
        	 adminStorage.addWithKey(resource);
        	 
         } catch (WPBException e)
         {
             // do not propagate further
         }
         
	}
	
	/**
	 * Checks and creates subdirectories from a path relative to the owner
	 * @param subDirectory path that is relative to the owner directory
	 * @param owner The owner directory, or null if the owner is the root
	 * @return
	 */
	
	private Map<String, WPBFile> checkAndCreateSubDirectory(String fullDirectory, WPBFile owner) throws WPBException
	{
	    Map<String, WPBFile> subfolderFiles = new HashMap<String, WPBFile>();
	    
	    WPBFile currentDirectory = owner;
	    String advancePath = "";
	    while (fullDirectory.length()>0)
	    {
	        int index = fullDirectory.indexOf('/');
	        if (index == 0)
	        {
	            fullDirectory = fullDirectory.substring(1);
	            index = fullDirectory.indexOf('/');
	        }
	        String subDir = fullDirectory;
	        if (index > 0)
	        {
	            subDir = fullDirectory.substring(0, index);
	            fullDirectory = fullDirectory.substring(index+1);
	        } else
	        {
	            fullDirectory = "";
	        }
	        if (subDir.length()>0)
	        {
	            WPBFile subDirFile = getFileFromDirectory(currentDirectory, subDir);
	            if (null == subDirFile)
	            {
	                subDirFile = createDirectory(currentDirectory, subDir);
	            }
	            if (advancePath.length() == 0)
	            {
	                advancePath = subDir;
	            } else
	            {
	                advancePath = advancePath + "/" + subDir;
	            }
	            subfolderFiles.put(advancePath, subDirFile);
	            currentDirectory = subDirFile;
	        }
	    }
	    
	    return subfolderFiles;
	}
	   public void uploadFolder(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	    {
	        try
	        {
	              ServletFileUpload upload = new ServletFileUpload();
	              upload.setHeaderEncoding("UTF-8");
	              FileItemIterator iterator = upload.getItemIterator(request);
	              WPBFile ownerFile = null;
	              Map<String, WPBFile> subfolderFiles = new HashMap<String, WPBFile>();
	              
	              while (iterator.hasNext()) {
	                FileItemStream item = iterator.next();
	               
	                if (item.isFormField() && item.getFieldName().equals("ownerExtKey"))
	                {
	                    String ownerExtKey = Streams.asString(item.openStream());
	                    ownerFile = getDirectory(ownerExtKey, adminStorage);	            
	                } else
	                if (!item.isFormField() && item.getFieldName().equals("file")) {
	                  
	                  String fullName = item.getName();
	                  String directoryPath = getDirectoryFromLongName(fullName);
	                  String fileName = getFileNameFromLongName(fullName);
	                  
	                  Map<String, WPBFile> tempSubFolders = checkAndCreateSubDirectory(directoryPath, ownerFile);
	                  subfolderFiles.putAll(tempSubFolders);
	                  
	                  // delete the existing file
	                  WPBFile existingFile = getFileFromDirectory(subfolderFiles.get(directoryPath) , fileName);
	                  if (existingFile != null)
	                  {
	                      deleteFile(existingFile, 0);
	                  }
	                  
	                  // create the file
	                  WPBFile file = new WPBFile();
	                  file.setExternalKey(adminStorage.getUniqueId());
	                  file.setFileName(fileName);
	                  file.setLastModified(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
	                  file.setDirectoryFlag(0);
	                  
	                  addFileToDirectory(subfolderFiles.get(directoryPath), file, item.openStream());
	                  
	                }
	              } 
	              
	              org.json.JSONObject returnJson = new org.json.JSONObject();
                  returnJson.put(DATA, jsonObjectConverter.JSONFromObject(null));           
                  httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);
	        } catch (Exception e)
	        {
	            Map<String, String> errors = new HashMap<String, String>();     
	            errors.put("", WPBErrors.WB_CANT_UPDATE_RECORD);
	            httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);          
	        }
	    }

   public void upload(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
   {
	try
	{
	    ServletFileUpload upload = new ServletFileUpload();
	    upload.setHeaderEncoding("UTF-8");
	    FileItemIterator iterator = upload.getItemIterator(request);
	    WPBFile ownerFile = null;
  
	    while (iterator.hasNext()) {
	        FileItemStream item = iterator.next();
	        if (item.isFormField() && item.getFieldName().equals("ownerExtKey"))
	        {
	            String ownerExtKey = Streams.asString(item.openStream());
	            ownerFile = getDirectory(ownerExtKey, adminStorage);              
	        } else
	        if (!item.isFormField() && item.getFieldName().equals("file")) {
	            InputStream stream = item.openStream();
	            WPBFile wbFile = null;
  
	            String fileName = getFileNameFromLongName(item.getName());
  
	            if (request.getAttribute("key") != null)
	            {
	                // this is an upload as update for an existing file
	                String key = (String)request.getAttribute("key");
	                wbFile = adminStorage.get(key, WPBFile.class);
	                
	                ownerFile = getDirectory(wbFile.getOwnerExtKey(), adminStorage);
  
	                //old file need to be deleted from cloud
	                String oldFilePath = wbFile.getBlobKey();
	                if (oldFilePath != null && oldFilePath.length()>0)
	                {
	                    // delete only if the blob key is set
	                    WPBFilePath oldCloudFile = new WPBFilePath(PUBLIC_BUCKET, oldFilePath);
	                    cloudFileStorage.deleteFile(oldCloudFile);
	                }
	            } else
	            {
	                // this is a new upload
	                wbFile = new WPBFile();
	                wbFile.setExternalKey(adminStorage.getUniqueId());	            
	            }
	            
                wbFile.setFileName(fileName);                       
	            wbFile.setLastModified(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
	            wbFile.setDirectoryFlag(0);
	            addFileToDirectory(ownerFile, wbFile, stream);
          		          
	        }
	    }	
	    org.json.JSONObject returnJson = new org.json.JSONObject();
	    returnJson.put(DATA, jsonObjectConverter.JSONFromObject(null));           
	    httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);
	} catch (Exception e)
	{
	Map<String, String> errors = new HashMap<String, String>();		
	errors.put("", WPBErrors.WB_CANT_UPDATE_RECORD);
	httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);			
	}
   }

	public void createDir(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
	       try
	        {
	            String jsonRequest = httpServletToolbox.getBodyText(request);
	            WPBFile wbFile = jsonObjectConverter.objectFromJSONString(jsonRequest, WPBFile.class);
	            Map<String, String> errors = validator.validateCreate(wbFile);
	            
	            if (errors.size()>0)
	            {
	                httpServletToolbox.writeBodyResponseAsJson(response, "{}", errors);
	                return;
	            }
	            wbFile.setAdjustedContentType(null);
	            wbFile.setBlobKey(null);
	            wbFile.setContentType(null);
	            wbFile.setDirectoryFlag(1);
	            wbFile.setHash(0L);
	            wbFile.setPublicUrl(null);
	            wbFile.setSize(0L);
	            wbFile.setLastModified(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
	            wbFile.setExternalKey(adminStorage.getUniqueId());
	            wbFile.setVersion(UUID.randomUUID().toString());
	            WPBFile newFile = adminStorage.addWithKey(wbFile);
	            
	            WPBResource resource = new WPBResource(newFile.getExternalKey(), newFile.getFileName(), WPBResource.FILE_TYPE);
	            try
	            {
	                adminStorage.addWithKey(resource);
	            } catch (Exception e)
	            {
	                // do not propagate further
	            }
	            org.json.JSONObject returnJson = new org.json.JSONObject();
	            returnJson.put(DATA, jsonObjectConverter.JSONFromObject(newFile));           
	            httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);

	        } catch (Exception e)
	        {
	            Map<String, String> errors = new HashMap<String, String>();     
	            errors.put("", WPBErrors.WB_CANT_CREATE_RECORD);
	            httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);          
	        }
	}
	
	public void update(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
		try
		{
			String key = (String)request.getAttribute("key");
			String jsonRequest = httpServletToolbox.getBodyText(request);
			WPBFile wbfile = (WPBFile)jsonObjectConverter.objectFromJSONString(jsonRequest, WPBFile.class);
			wbfile.setExternalKey(key);
			Map<String, String> errors = validator.validateUpdate(wbfile);
			
			if (errors.size()>0)
			{
				httpServletToolbox.writeBodyResponseAsJson(response, "", errors);
				return;
			}
			WPBFile existingFile = adminStorage.get(key, WPBFile.class);
			existingFile.setLastModified(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
			existingFile.setAdjustedContentType(wbfile.getAdjustedContentType());
			existingFile.setVersion(UUID.randomUUID().toString());
			WPBFile newFile = adminStorage.update(existingFile);
				
			org.json.JSONObject returnJson = new org.json.JSONObject();
			returnJson.put(DATA, jsonObjectConverter.JSONFromObject(newFile));			
			httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);
	
		} catch (Exception e)		
		{
			Map<String, String> errors = new HashMap<String, String>();		
			errors.put("", WPBErrors.WB_CANT_UPDATE_RECORD);
			httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);			
		}				
	}
	
	public void delete(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
		try
		{
			String key = (String)request.getAttribute("key");
			WPBFile tempFile = adminStorage.get(key, WPBFile.class);

			deleteFile(tempFile, 0);
			
			WPBFile param = new WPBFile();
			param.setExternalKey(key);
			
			org.json.JSONObject returnJson = new org.json.JSONObject();
			returnJson.put(DATA, jsonObjectConverter.JSONFromObject(param));			
			httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);
			
		} catch (Exception e)		
		{
			Map<String, String> errors = new HashMap<String, String>();		
			errors.put("", WPBErrors.WB_CANT_DELETE_RECORD);
			httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);	
		}		
	}

	public void getAll(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
		try
		{
			Map<String, Object> additionalInfo = new HashMap<String, Object> ();			
			String sortParamDir = request.getParameter(SORT_PARAMETER_DIRECTION);
			String sortParamProp = request.getParameter(SORT_PARAMETER_PROPERTY);

			String ownerParam = "ownerExtKey";
			String ownerValue = request.getParameter("parent");
			if (ownerValue == null) 
			    ownerValue="";
			else
			    ownerValue = ownerValue.trim();
			
			List<WPBFile> files = null;
			
			if (sortParamDir != null && sortParamProp != null)
			{
				if (sortParamDir.equals(SORT_PARAMETER_DIRECTION_ASC))
				{
					additionalInfo.put(SORT_PARAMETER_DIRECTION, SORT_PARAMETER_DIRECTION_ASC);
					additionalInfo.put(SORT_PARAMETER_PROPERTY, sortParamProp);
					
					files = adminStorage.queryWithSort(WPBFile.class, ownerParam , AdminQueryOperator.EQUAL, ownerValue, sortParamProp, AdminSortOperator.ASCENDING);

				} else if (sortParamDir.equals(SORT_PARAMETER_DIRECTION_DSC))
				{
					additionalInfo.put(SORT_PARAMETER_DIRECTION, SORT_PARAMETER_DIRECTION_DSC);
					additionalInfo.put(SORT_PARAMETER_PROPERTY, sortParamProp);
					files = adminStorage.queryWithSort(WPBFile.class, ownerParam , AdminQueryOperator.EQUAL, ownerValue, sortParamProp, AdminSortOperator.DESCENDING);
					
				} else
				{
					
					files = adminStorage.query(WPBFile.class, ownerParam, AdminQueryOperator.EQUAL, ownerValue);
				}
			} else
			{				
				files = adminStorage.query(WPBFile.class, ownerParam , AdminQueryOperator.EQUAL, ownerValue);
			}

			List<WPBFile> result = filterPagination(request, files, additionalInfo);
			for(WPBFile wbFile: result)
			{
				setPublicFilePath(wbFile, cloudFileStorage);
			}
			WPBFile ownerFile = null;
			if (ownerValue.length()>0)
			{
			    // get the owner parent info
			    List<WPBFile> queryRes = adminStorage.query(WPBFile.class, "externalKey", AdminQueryOperator.EQUAL, ownerValue);
			    if (queryRes.size() == 1)
			    {
			        ownerFile =  queryRes.get(0);
			    }
			} 
			org.json.JSONObject returnJson = new org.json.JSONObject();
			returnJson.put(DATA, jsonObjectConverter.JSONArrayFromListObjects(result));
			org.json.JSONObject additionalDataJson = jsonObjectConverter.JSONObjectFromMap(additionalInfo);
			additionalDataJson.put("owner", jsonObjectConverter.JSONFromObject(ownerFile));
			String ownerOfOwner = "";
			if (ownerFile != null)
			{
			    ownerOfOwner = ownerFile.getOwnerExtKey();
			}
            String dirPath = getDirectoryFullPath(ownerOfOwner, adminStorage);
            additionalDataJson.put("ownerFullDirectoryPath", dirPath);

			returnJson.put(ADDTIONAL_DATA, additionalDataJson);
		httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);
			
		} catch (Exception e)		
		{
			Map<String, String> errors = new HashMap<String, String>();		
			errors.put("", WPBErrors.WB_CANT_GET_RECORDS);
			httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);		
		}
	}


	private static void setPublicFilePath(WPBFile wbFile, WPBFileStorage cloudFileStorage)
	{
	    if (wbFile.getDirectoryFlag() == null || wbFile.getDirectoryFlag() == 0)
	    {
	        if (wbFile.getBlobKey() !=  null)
	        {
	            wbFile.setPublicUrl(cloudFileStorage.getPublicFileUrl(new WPBFilePath(PUBLIC_BUCKET, wbFile.getBlobKey())));
	        }
	    } 
	}
	private org.json.JSONObject get(HttpServletRequest request, WPBFile wbFile) throws WPBException
	{
		try
		{
			setPublicFilePath(wbFile, cloudFileStorage);
			org.json.JSONObject returnJson = new org.json.JSONObject();
			returnJson.put(DATA, jsonObjectConverter.JSONFromObject(wbFile));	

            org.json.JSONObject additionalData = new org.json.JSONObject();

            String dirPath = getDirectoryFullPath(wbFile.getOwnerExtKey(), adminStorage);
            additionalData.put("ownerFullDirectoryPath", dirPath);
            
			String includeLinks = request.getParameter("include_links");
			if (includeLinks != null && includeLinks.equals("1"))
			{
				List<WPBUri> uris = adminStorage.query(WPBUri.class, "resourceExternalKey", AdminQueryOperator.EQUAL, wbFile.getExternalKey());
				org.json.JSONArray arrayUris = jsonObjectConverter.JSONArrayFromListObjects(uris);
				additionalData.put("uri_links", arrayUris);
			}
            returnJson.put(ADDTIONAL_DATA, additionalData);         
			return returnJson;
		} catch (Exception e)
		{
			throw new WPBException("cannot get file details", e);
		}
	}
	public void get(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
		try
		{
			String key = (String)request.getAttribute("key");
			WPBFile wbFile = adminStorage.get(key, WPBFile.class);			
			org.json.JSONObject returnJson = get(request, wbFile);
			httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);
			
		} catch (Exception e)		
		{
			Map<String, String> errors = new HashMap<String, String>();		
			errors.put("", WPBErrors.WB_CANT_GET_RECORDS);
			httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);		
		}		
	}

	public void getExt(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
		try
		{
			String extKey = (String)request.getAttribute("key");
			List<WPBFile> wbFiles = adminStorage.query(WPBFile.class, "externalKey", AdminQueryOperator.EQUAL, extKey);			
			WPBFile wbFile = (wbFiles.size()>0) ? wbFiles.get(0) : null; 
			org.json.JSONObject returnJson = get(request, wbFile);
			httpServletToolbox.writeBodyResponseAsJson(response, returnJson, null);			
		} catch (Exception e)		
		{
			Map<String, String> errors = new HashMap<String, String>();		
			errors.put("", WPBErrors.WB_CANT_GET_RECORDS);
			httpServletToolbox.writeBodyResponseAsJson(response, jsonObjectConverter.JSONObjectFromMap(null), errors);		
		}		
	}

	public void downloadResource(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
		try
		{
			String key = (String)request.getAttribute("key");
			WPBFile wbfile = adminStorage.get(key, WPBFile.class);
			WPBFilePath cloudFile = new WPBFilePath(PUBLIC_BUCKET, wbfile.getBlobKey());
			InputStream is = cloudFileStorage.getFileContent(cloudFile);
			response.setContentType(wbfile.getContentType());			
			response.setHeader("Content-Disposition", "attachment; filename=\"" + wbfile.getFileName() + "\"");
			response.setContentLength(wbfile.getSize().intValue());
			OutputStream os = response.getOutputStream();
			IOUtils.copy(is, os);
			os.flush();
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		} catch (Exception e)		
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		
	}

	public void serveResource(HttpServletRequest request, HttpServletResponse response, String requestUri) throws WPBException
	{
	    InputStream is = null;
	    OutputStream os = null;
		try
		{
			String key = (String)request.getAttribute("key");
			WPBFile wbfile = adminStorage.get(key, WPBFile.class);
			WPBFilePath cloudFile = new WPBFilePath(PUBLIC_BUCKET, wbfile.getBlobKey());
			is = cloudFileStorage.getFileContent(cloudFile);
			response.setContentType(wbfile.getAdjustedContentType());
			response.setContentLength(wbfile.getSize().intValue());
			os = response.getOutputStream();
			IOUtils.copy(is, os);
			os.flush();
		} catch (Exception e)		
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		finally
		{
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);		    
		}
	}


}
