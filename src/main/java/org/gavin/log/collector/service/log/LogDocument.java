package org.gavin.log.collector.service.log;

import org.gavin.log.collector.service.log.sender.vo.LoggingEventVO;

/**
 * ---------------------------------------------------
 * File:    LogDocument
 * Package: org.gavin.log.collector.service.log
 * Project: log-collector-core
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/12 18:43.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class LogDocument {

    private String host;
    private int port;
    private LoggingEventVO loggingEventVO;

    public LogDocument() {}

    public LogDocument(String host, int port, LoggingEventVO loggingEventVO) {
        this.host = host;
        this.port = port;
        this.loggingEventVO = loggingEventVO;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public LoggingEventVO getLoggingEventVO() {
        return loggingEventVO;
    }

    public void setLoggingEventVO(LoggingEventVO loggingEventVO) {
        this.loggingEventVO = loggingEventVO;
    }
}
