package org.gavin.log.collector.service.log.protocol;

import com.alibaba.fastjson.JSON;
import org.gavin.log.collector.service.log.LogDocument;
import org.gavin.log.collector.service.log.LogReceiver;
import org.gavin.log.collector.service.log.sender.vo.LoggingEventVO;
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
    private LogReceiver logReceiver;

    private String host;
    private int port;

    private LinkedList<byte[]> queue;
    private byte[] buffer;
    private boolean noLogClient;

    public LogEater(String host, int port, LogReceiver logReceiver) {
        this.logReceiver = logReceiver;
        this.host = host;
        this.port = port;
        this.queue = new LinkedList<>();
        this.noLogClient = false;
    }

    @Override
    public void run() {
        while (!(queue.size() == 0 && noLogClient)) {//仅当被外界告知没有可处理的客户端了, 同时队列也没有未处理完的数据时, 才退出, 不考虑 this.buffer

            byte[] nextBytes = queue.poll();
            if (nextBytes == null) continue;
            if (buffer != null) {//如果上次run()有未处理完的数据, 将上次的于本次数据包拼接在在一起处理
                nextBytes = bufferAppendNextBytes(buffer, nextBytes);
                buffer = null;
            }

            List<Byte> tempBuffer = new ArrayList<>();

            List<String> sentenceList = new ArrayList<>();

            for (byte byte0 : nextBytes) {
                if (byte0 == 3) {
                    //ASCII码为3, 分句操作
                    byte[] bytes = new byte[tempBuffer.size()];
                    for (int i = 0; i < tempBuffer.size(); i++) {
                        bytes[i] = tempBuffer.get(i);
                    }
                    String sentence = null;
                    try {
                        sentence = new String(bytes, "UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    tempBuffer.clear();
                    if (sentence == null) continue;//如果是空句, 跳过本次
                    if (sentence.equals(LogProtocol.HeartbeatIdentify)) continue;//如果是心跳语句, 跳过本次
                    sentenceList.add(sentence);
                } else {
                    tempBuffer.add(byte0);
                }
            }

            if (tempBuffer.size() > 0) {//将本次未处理完的数据交给下一次run()处理
                buffer = new byte[tempBuffer.size()];
                for (int i = 0; i < tempBuffer.size(); i++) {
                    buffer[i] = tempBuffer.get(i);
                }
            }

            for (String sentence : sentenceList) {//将本次run()得出的可分句的数据, 交给老大处理
                LoggingEventVO loggingEventVO = JSON.parseObject(sentence, LoggingEventVO.class);
                logReceiver.pushLog(new LogDocument(host, port, loggingEventVO));
            }
        }
    }

    public void eat(byte[] bs) {
        queue.add(bs);
    }

    public void shouldStop() {
        noLogClient = true;
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
