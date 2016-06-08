/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.uploader.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.linuxtek.kona.uploader.entity.KUploadStatus;
import com.linuxtek.kona.uploader.service.KUploadException;
import com.linuxtek.kona.uploader.service.KUploadService;
import com.linuxtek.kona.encryption.KEncryptUtil;
import com.linuxtek.kona.http.KServletUtil;

public abstract class KAbstractUploadService implements KUploadService {

    private static Logger logger = Logger.getLogger(KAbstractUploadService.class);

    //indexed by accessToken
    protected static Map<String,Map<String,FileItem>> tokenFileMap = 
        new HashMap<String,Map<String,FileItem>>();

    //indexed by uploadKey
    protected static Map<String,KUploadStatus> uploadStatusMap = 
        new HashMap<String,KUploadStatus>();


    public static Boolean isMultipartContent(HttpServletRequest req) {
        return (ServletFileUpload.isMultipartContent(req));
    }

    public abstract void validateToken(String token);

    public static KUploadStatus getUploadStatus(String uploadKey) {
        return uploadStatusMap.get(uploadKey);
    }

    // return an uploadKey
    @Override
    public String initUpload(HttpServletRequest req, String token) {
        validateToken(token);

        String uploadKey = UUID.randomUUID().toString();

        try {
            uploadKey = KEncryptUtil.MD5(uploadKey.getBytes()).toUpperCase();
        } catch (Exception e) { logger.error(e); }

        KUploadStatus status = new KUploadStatus();
        uploadStatusMap.put(uploadKey, status);

        // save the uploadKey in a session object so we can retrieve 
        // during the actual download
        HttpSession session = req.getSession(true);
        session.setAttribute("uploadKey", uploadKey);

        //setSessionParam("uploadKey", uploadKey);

        logger.debug("initUpload: uploadKey: " + uploadKey);

        return uploadKey;
    }

    @Override
    public KUploadStatus getStatus(String token, String uploadKey) {
        validateToken(token);

        KUploadStatus status = uploadStatusMap.get(uploadKey);

        if (status == null) {
            logger.error("No status found for uploadKey: " + uploadKey);
        }

        return status;
    }

    @Override
    public void cancel(String token, String uploadKey) {
        validateToken(token);

        KUploadStatus status = uploadStatusMap.get(uploadKey);

        if (status == null) {
            logger.error("No status found for uploadKey: " + uploadKey);
            return;
        }

        //KUploadWatchDog.addCancelRequest(status);
        status.setState(KUploadStatus.State.CANCELED);
    }

    
    @Override
    public void remove(String token, String uploadKey) {
        validateToken(token);
    
        // remove entry from statusMap
        uploadStatusMap.remove(uploadKey);

        // remove entry from tokenFileMap
        Map<String,FileItem> map = tokenFileMap.get(token);
        if (map != null) {
            map.remove(uploadKey);
        }

        logger.debug("Remove call completed.");
    }


    public static Map<String,FileItem> getFileItemMap(String accessToken) {
        return tokenFileMap.get(accessToken);
    }

    private static KUploadStatus getUploadStatus(HttpServletRequest req) {
        HttpSession session = KServletUtil.getSession(req);
        String uploadKey = (String) session.getAttribute("uploadKey");

        if (uploadKey == null) {
            throw new IllegalStateException("UploadKey is null");
        }

        KUploadStatus status = uploadStatusMap.get(uploadKey);
        if (status == null) {
            throw new IllegalStateException("KUploadStatus is null");
        }

        status.setUploadKey(uploadKey);
        return (status);
    }

	/**
	 * Initialize the form object.
	 * The method first determines if the form is multipart then retrieves
	 * all of its parameters into the param hashtable.
	 */
        // assumptions:
        //  - only 1 file is processed per request
        //  - along with the file, accessToken is also passed in
        //      if accessToken is null, then no files are processed

	public static void processFiles(HttpServletRequest req, 
            HttpServletResponse resp) {
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        KUploadStatus status = null;

        try {
            status = getUploadStatus(req);
            status.setState(KUploadStatus.State.EXECUTING);
            status.setStartDate(new Date());
            KUploadListener listener = new KUploadListener(status);
            upload.setProgressListener(listener);

            //KUploadWatchDog watchDog = new KUploadWatchDog(status);
            @SuppressWarnings("unchecked")
			List<FileItem> fileItemList = upload.parseRequest(req);
            logger.debug("upload parse completed ...");
            //watchDog.cancel();

            // should only have 2 items
            if (fileItemList.size() != 2) {
                String error = "Invalid FileItemList size: " 
                        + fileItemList.size();

                logger.error(error);

                status.setError(error);
                status.setState(KUploadStatus.State.ERROR);
                KUploadException.Type type = KUploadException.Type.ERROR;
                throw new KUploadException(type, error, status);
            }

            String accessToken = null;

			logger.debug("form is multipart/form-data");

            for (FileItem item : fileItemList) {
                String fieldName = item.getFieldName().trim();

                if (item.isFormField()) {
			        logger.debug("** Processing Form Param: " + fieldName);

                    if (fieldName.equalsIgnoreCase("accessToken")) {
                        accessToken = Streams.asString(item.getInputStream());
                    }
                } else {
			        logger.debug("** Processing Form File: " + fieldName);

                    if (accessToken == null) {
                        String error = "AccessToken is null";
                        logger.error(error);
                        status.setError(error);
                        status.setState(KUploadStatus.State.ERROR);
                        KUploadException.Type type = 
                            KUploadException.Type.ERROR;
                        throw new KUploadException(type, error, status);
                    }

                    Map<String,FileItem> fileItemMap = 
                        tokenFileMap.get(accessToken);

                    if (fileItemMap == null) {
                        fileItemMap = new HashMap<String,FileItem>();
                        tokenFileMap.put(accessToken, fileItemMap);
                    }

                    // the fileItemMap is indexed by the uploadKey
                    fileItemMap.put(status.getUploadKey(), item);
				    logger.debug("FILE field name: " + fieldName);
                    status.setFileName(item.getName());
                    status.setContentType(item.getContentType());

                    // set the view url
                    String viewPath = KUploadService.VIEW_PATH + "/"
                        + accessToken + "|" + status.getUploadKey();

                    status.setViewPath(viewPath);
                    
                    if (item.getSize() != status.getFileSize()) {
                        logger.info("FileItem size differs from estimated size:"
                            + "\n\tfileItem field: " + fieldName
                            + "\n\tfileItem size: " + item.getSize()
                            + "\n\tstatus size: " + status.getFileSize());
                    }

                    status.setState(KUploadStatus.State.COMPLETED);
                    sendResponse(resp, status.getUploadKey());
                }
            }
        } catch (KUploadException e) {
            KUploadException.Type type = e.getType();

            if (type == KUploadException.Type.CANCELED) {
                logger.debug("KUploadException: status canceled:\n");
            }

            try {
                req.getInputStream().close();

                // Status code (204) indicating that the request succeeded 
                // but that there was no new information to return.
                resp.sendError(HttpServletResponse.SC_NO_CONTENT);
                resp.flushBuffer();
                uploadStatusMap.remove(status.getUploadKey());
            } catch (IOException e1) {
                logger.error(e1);
            }
        } catch (Exception e) {
            logger.error("Error parsing multipart form", e);
        }
	}

    private static void sendResponse(HttpServletResponse resp, 
            String uploadKey) throws IOException {
        resp.setContentType("text/html");
        resp.setHeader("Pragma", "No-cache");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Cache-Control", "no-cache");

        PrintWriter out = resp.getWriter();

        out.println("<html>");
        out.println("<body>");
        out.println("<script type=\"text/javascript\">");
        out.println("if (parent.uploadPosted) parent.uploadPosted('"
                + uploadKey + "');");
        out.println("</script>");
        out.println("</body>");
        out.println("</html>");
        out.flush();
    }


    // FileKey is a pipe separated String where the first part
    // is the accessToken and the second part is the uploadKey
    public static void view(HttpServletResponse resp, String fileKey) 
            throws IOException {

        String[] keys = fileKey.split("\\|");
        String accessToken = keys[0];
        String uploadKey = keys[1];

        logger.debug("fileKey: " + fileKey);
        logger.debug("accessToken: " + accessToken);
        logger.debug("uploadKey: " + uploadKey);

        Map<String,FileItem> fileItemMap =  tokenFileMap.get(accessToken);
        if (fileItemMap == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        FileItem item = fileItemMap.get(uploadKey);
        if (item == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }

        String contentType = item.getContentType();
        byte[] data = item.get();
        String filename = item.getName();

        KServletUtil.writeObject(resp, contentType, data, filename, false);
    }


    /*
    protected static void writeObject(HttpServletResponse resp, 
            String contentType, byte[] data, String filename, 
            Boolean cache) throws IOException {
        Date lastModified = new Date();
        int maxAge = 3600;
        boolean cacheControlEnabled = cache;

        if (contentType == null) {
            throw new IllegalArgumentException("ContentType is null");
        }

        if (data == null) {
            throw new IllegalArgumentException("Data is null");
        }

        String md5 = null;
        try {
            md5 = KEncryptUtil.MD5(data);
        } catch (NoSuchAlgorithmException e) { logger.error(e); }


        resp.addHeader("Content-Length", Integer.toString(data.length));
        resp.addHeader("Last-Modified", KDateUtil.formatHttp(lastModified));

        if (md5 != null) {
            resp.addHeader("ETag", "\"" + md5 + "\"");
        }

        if (cacheControlEnabled) {
            resp.addHeader("Cache-Control", 
                "public, must-revalidate, max-age=" + maxAge);
        } else {
            resp.addHeader("Cache-Control", 
                "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "No-cache");
            resp.setDateHeader("Expires", 0);
        }

        if (filename != null) {
            resp.addHeader("Content-Disposition", 
                "attachment;filename=" + filename);
        }

        ServletOutputStream out = resp.getOutputStream();
        resp.setContentType(contentType);
        out.write(data);
        out.close();
    }
    */


    @Override
    public Long getUploadTime(String uploadKey) {
        Long uploadTime = null;
        KUploadStatus status = getUploadStatus(uploadKey);
        if (status != null) {
            Long start = status.getStartDate().getTime();
            Long end = status.getLastUpdated().getTime();
            uploadTime = end - start;
        }   

        return uploadTime;
    }   

}
