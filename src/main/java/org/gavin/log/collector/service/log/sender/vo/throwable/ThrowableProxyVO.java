package org.gavin.log.collector.service.log.sender.vo.throwable;

import java.util.List;

/**
 * ---------------------------------------------------
 * File:    ThrowableProxyVO
 * Package: org.gavin.log.collector.service.log.sender.vo.throwable
 * Project: log-collector-core
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/12 14:50.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class ThrowableProxyVO {

    private String className;
    private String message;
    private List<StackTraceElementVO> stackTraceElementList;

    public ThrowableProxyVO() {}

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<StackTraceElementVO> getStackTraceElementList() {
        return stackTraceElementList;
    }

    public void setStackTraceElementList(List<StackTraceElementVO> stackTraceElementList) {
        this.stackTraceElementList = stackTraceElementList;
    }
}
