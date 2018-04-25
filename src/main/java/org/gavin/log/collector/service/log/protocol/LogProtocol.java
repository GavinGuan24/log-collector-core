package org.gavin.log.collector.service.log.protocol;

/**
 * ---------------------------------------------------
 * File:    LogProtocol
 * Package: org.gavin.logCollector.service.log.protocol
 * Project: GGLogCollector
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/9 12:56.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class LogProtocol {

    public static final String HeartbeatIdentify = "!HB@`";


    public static byte[] heartbeatMsg() {
        return appendEndByte(HeartbeatIdentify);
    }

    public static byte[] appendEndByte(String msg) {
        if (msg == null) msg = "";
        return appendEndByte(msg.getBytes());
    }

    private static byte[] appendEndByte(byte[] source) {
        if (source == null || source.length == 0) {
            source = new byte[]{32};//无效参数, 将信息改写为一个空格
        }
        byte[] target = new byte[source.length + 1];
        System.arraycopy(source, 0, target, 0, source.length);
        target[source.length] = 3;//最后一位ASCII码为3, ETX, 文本结束
        return target;
    }

}
