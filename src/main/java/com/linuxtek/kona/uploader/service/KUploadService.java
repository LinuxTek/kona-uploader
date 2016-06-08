/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.uploader.service;

import javax.servlet.http.HttpServletRequest;

import com.linuxtek.kona.remote.service.KService;
import com.linuxtek.kona.remote.service.KServiceRelativePath;
import com.linuxtek.kona.uploader.entity.KUploadStatus;

/**
 * File Upload Service.  
 */
@KServiceRelativePath(KUploadService.SERVICE_PATH)
public interface KUploadService extends KService {

    // NOTE: SERVICE_PATH must begin with rpc/ prefix
    public static final String SERVICE_PATH = "rpc/FileUploadService";
    public static final String UPLOAD_PATH = SERVICE_PATH + "/upload";
    public static final String VIEW_PATH = SERVICE_PATH + "/view";

    // return a uploadKey
    public String initUpload(HttpServletRequest req, String token); 

    public KUploadStatus getStatus(String token, String uploadKey);

    // cancel upload in progress.
    public void cancel(String token, String uploadKey);

    // remove completed uploaded file.
    public void remove(String token, String uploadKey);


    public Long getUploadTime(String uploadKey);
}
