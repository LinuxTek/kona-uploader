/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.uploader.entity;

import java.util.Date;

import com.linuxtek.kona.data.entity.KEntityObject;

import java.io.Serializable;

public class KUploadStatus implements KEntityObject {
	private static final long serialVersionUID = 1L;

	public enum State implements Serializable {
        EXECUTING,
        CANCELED,
        STALLED,
        COMPLETED,
        ERROR
    };

    private Long id;
    private String uploadKey;
    private State state;
    private String fileName;
    private String contentType;
    private String viewPath;
    private String error;
    private Long fileSize;
    private Long bytesRead;
    private Date startDate;
    private Date lastUpdated;

    // ---------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // ---------------------------------------------------------------

    public String getUploadKey() {
        return uploadKey;
    }

    public void setUploadKey(String uploadKey) {
        this.uploadKey = uploadKey;
    }

    // ---------------------------------------------------------------

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    // ---------------------------------------------------------------

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    // ---------------------------------------------------------------

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    // ---------------------------------------------------------------

    public String getViewPath() {
        return viewPath;
    }

    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    // ---------------------------------------------------------------

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // ---------------------------------------------------------------

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    // ---------------------------------------------------------------

    public Long getBytesRead() {
        return bytesRead;
    }

    public void setBytesRead(Long bytesRead) {
        this.bytesRead = bytesRead;
    }

    // ---------------------------------------------------------------

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    // ---------------------------------------------------------------

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
