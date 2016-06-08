/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.uploader.service;

import java.util.Date;

import com.linuxtek.kona.uploader.entity.KUploadStatus;
import com.linuxtek.kona.uploader.service.KUploadException;

import org.apache.commons.fileupload.ProgressListener;

import org.apache.log4j.Logger;

public class KUploadListener implements ProgressListener {
    private static Logger logger = Logger.getLogger(KUploadListener.class);

    private static final Long MAX_STALL_TIME = 5L * 1000L; // 5 sec

    KUploadStatus status = null;
    Long startTime = null;
    Long lastBytesRead = null;

    public KUploadListener(KUploadStatus status) {
        this.status = status;
        startTime = (new Date()).getTime();
    }

    public void update(long bytesRead, long contentLength, int item) {
        /*
        logger.debug("KUploadListener.update called:"
                + "\n\tuploadKey: " + status.getUploadKey()
                + "\n\tbytesRead: " + bytesRead
                + "\n\tcontentLength: " + contentLength
                + "\n\titem: " + item);
        */

        if (status.getState() == KUploadStatus.State.CANCELED) {
            KUploadException.Type type = KUploadException.Type.CANCELED;
            throw new KUploadException(type, status);
        }

        if (item == 0) return;
        status.setBytesRead(bytesRead);
        status.setFileSize(contentLength);
        status.setLastUpdated(new Date());

        // see if bytes are actually being read; throw exception if stalled
        if (lastBytesRead == null) {
            lastBytesRead = bytesRead;
            return;
        }

        if (lastBytesRead == bytesRead) {
            Date now = new Date();
            Long stallTime = now.getTime() - startTime;

            logger.debug("bytesRead == lastBytesRead: " + bytesRead
                + "\nstallTime: " + stallTime);

            if (stallTime >= MAX_STALL_TIME) {
                status.setState(KUploadStatus.State.STALLED);
                KUploadException.Type type = KUploadException.Type.STALLED;
                throw new KUploadException(type, status);
            }
        }

        lastBytesRead = bytesRead;
    }
}
