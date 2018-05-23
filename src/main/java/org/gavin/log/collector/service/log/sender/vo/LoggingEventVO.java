package org.gavin.log.collector.service.log.sender.vo;

import org.gavin.log.collector.service.log.sender.vo.throwable.ThrowableProxyVO;

import java.util.List;

/**
 * ---------------------------------------------------
 * File:    LoggingEventVO
 * Package: org.gavin.log.collector.service.log.sender.vo
 * Project: log-collector-core
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/12 14:43.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class LoggingEventVO {

    private String message;
    private long timeStamp;

    private String loggerName;
    private String threadName;

    private List<Caller> callerList;

    private ThrowableProxyVO throwableProxyVO;

    public LoggingEventVO() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public List<Caller> getCallerList() {
        return callerList;
    }

    public void setCallerList(List<Caller> callerList) {
        this.callerList = callerList;
    }

    public ThrowableProxyVO getThrowableProxyVO() {
        return throwableProxyVO;
    }

    public void setThrowableProxyVO(ThrowableProxyVO throwableProxyVO) {
        this.throwableProxyVO = throwableProxyVO;
    }
}
