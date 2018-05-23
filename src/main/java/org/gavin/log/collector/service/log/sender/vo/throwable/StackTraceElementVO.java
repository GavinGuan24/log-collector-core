package org.gavin.log.collector.service.log.sender.vo.throwable;

import org.gavin.log.collector.service.log.sender.vo.Caller;

/**
 * ---------------------------------------------------
 * File:    StackTraceElementVO
 * Package: org.gavin.log.collector.service.log.sender.vo.throwable
 * Project: log-collector-core
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/12 14:52.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class StackTraceElementVO extends Caller {

    public StackTraceElementVO() {}

    public StackTraceElementVO(StackTraceElement ste) {
        super(ste);
    }
}
