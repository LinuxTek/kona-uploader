/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.uploader.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.linuxtek.kona.uploader.entity.KUploadStatus;
import com.linuxtek.kona.uploader.service.KUploadException;

public class KUploadWatchDog extends Thread {
    private static Logger logger = Logger.getLogger(KUploadWatchDog.class);

    //values are KUploadStatus
    protected static List<String> runList = new ArrayList<String>();
    protected static List<String> cancelList = new ArrayList<String>();

    private KUploadStatus status;

    public KUploadWatchDog(KUploadStatus status) {
        this.status = status;
        start();
    }

    // FIXME: use this watchdog to catch frozen uploads.
    // how to test??
    @Override
    public void run() {
        logger.debug("Starting WatchDog ...");
        String uploadKey = status.getUploadKey();
        runList.add(uploadKey);

        while (runList.contains(uploadKey)) {
            logger.debug("checking cancel list ...");
            if (cancelList.contains(uploadKey)) {
                logger.debug("request canceled");

                // clean up lists
                runList.remove(uploadKey);
                cancelList.remove(uploadKey);

                throw new KUploadException(
                    KUploadException.Type.CANCELED, status);
            }

            // check every .5 sec
            try { Thread.sleep(100); } catch (Exception e) { logger.error(e); }
        }
    }

    public void cancel() {
        // first remove the status from the runList so the
        // watchdog stop executing.
        logger.debug("Cancelling WatchDog ...");

        String uploadKey = status.getUploadKey();
        runList.remove(uploadKey);

        // do a final check in case there's a timer issue
        if (cancelList.contains(uploadKey)) {
            throw new KUploadException(KUploadException.Type.CANCELED, status);
        }
    }

    public synchronized static void addCancelRequest(KUploadStatus status) {
        cancelList.add(status.getUploadKey());
    }
}
