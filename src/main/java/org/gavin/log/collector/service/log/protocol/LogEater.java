package org.gavin.log.collector.service.log.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * ---------------------------------------------------
 * File:    LogEater
 * Package: org.gavin.logCollector.service.log.protocol
 * Project: GGLogCollector
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/9 12:55.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class LogEater implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(LogEater.class);

    private LinkedList<byte[]> queue;
    private byte[] buffer;
    private boolean noLogClient;

    public LogEater() {
        this.queue = new LinkedList<>();
        this.noLogClient = false;
    }

    @Override
    public void run() {
        while (!(queue.size() == 0 && noLogClient)) {

            byte[] nextBytes = queue.poll();
            if (nextBytes == null) continue;
            if (buffer != null) {
                nextBytes = bufferAppendNextBytes(buffer, nextBytes);
                buffer = null;
            }

            List<Byte> newBuffer = new ArrayList<>();

            List<String> sentenceList = new ArrayList<>();

            for (byte byte0 : nextBytes) {
                if (byte0 == 3) {
                    //分句操作
                    byte[] bytes = new byte[newBuffer.size()];
                    for (int i = 0; i < newBuffer.size(); i++) {
                        bytes[i] = newBuffer.get(i);
                    }
                    String sentence = null;
                    try {
                        sentence = new String(bytes, "UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    newBuffer.clear();
                    if (sentence == null) continue;
                    if (sentence.equals(LogProtocol.HeartbeatIdentify)) continue;
                    sentenceList.add(sentence);
                } else {
                    newBuffer.add(byte0);
                }
            }

            if (newBuffer.size() > 0) {
                buffer = new byte[newBuffer.size()];
                for (int i = 0; i < newBuffer.size(); i++) {
                    buffer[i] = newBuffer.get(i);
                }
            }


            for (String sentence : sentenceList) {
                logger.info("收到的log" + sentence);
            }
        }
    }

    public void eat(byte[] bs) {
        queue.add(bs);
    }

    public void shouldStop() {
        this.noLogClient = true;
    }

    //--------------------------------------------------------------

    private byte[] bufferAppendNextBytes(byte[] buffer, byte[] nextBytes) {
        int bufferLen = buffer.length;
        int nextBytesLen = nextBytes.length;

        byte[] newNextBytes = new byte[bufferLen + nextBytesLen];

        System.arraycopy(buffer, 0, newNextBytes, 0, bufferLen);
        System.arraycopy(nextBytes, 0, newNextBytes, bufferLen, nextBytesLen);

        return newNextBytes;
    }


}
