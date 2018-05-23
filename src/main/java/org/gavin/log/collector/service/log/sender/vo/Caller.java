package org.gavin.log.collector.service.log.sender.vo;

/**
 * ---------------------------------------------------
 * File:    Caller
 * Package: org.gavin.log.collector.service.log.sender.vo.loggerContext
 * Project: log-collector-core
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/12 16:22.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class Caller {

    private String className;
    private String methodName;

    private String fileName;
    private int lineNumber;

    public Caller() {}

    public Caller(StackTraceElement ste) {
        this.className = ste.getClassName();
        this.methodName = ste.getMethodName();

        this.fileName = ste.getFileName();
        this.lineNumber = ste.getLineNumber();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
