/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.uploader.service;

import java.io.Serializable;

import com.linuxtek.kona.uploader.entity.KUploadStatus;

@SuppressWarnings("serial")
public class KUploadException extends RuntimeException implements Serializable {

    public enum Type { ERROR, CANCELED, STALLED };

    private KUploadStatus status;
    private Type type;

    public KUploadException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public KUploadException(Type type, KUploadStatus status) {
        this.type = type;
        this.status = status;
    }

    public KUploadException(Type type, String message, KUploadStatus status) {
        super(message);
        this.type = type;
        this.status = status;
    }

    public KUploadStatus getStatus() {
        return status;
    }

    public Type getType() {
        return (type);
    }
}
