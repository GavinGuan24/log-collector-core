package org.gavin.log.collector.service.log;

import org.gavin.log.collector.service.log.protocol.LogProtocol;
import org.gavin.log.collector.service.log.protocol.LogEater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ---------------------------------------------------
 * File:    LogClient
 * Package: org.gavin.logCollector.service.log
 * Project: GGLogCollector
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/9 12:54.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class LogClient implements Runnable {

    private static final long periodMilliSecond = 200L;
    private static final long timeoutSecond = 10L;

    private static Logger logger = LoggerFactory.getLogger(LogClient.class);

    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ScheduledThreadPoolExecutor executor;
    private LogEater logEater;
    private long timeout;
    private boolean first;
    private int period2Client;

    public LogClient(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.timeout = timeoutSecond * (1000L / periodMilliSecond);
        this.first = true;
        period2Client = 0;
    }

    @Override
    public void run() {
        if (first) {
            first = false;
            try {
                inputStream = clientSocket.getInputStream();
                outputStream = clientSocket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("异步初始化 LogClient 失败");
                return;
            }
            executor = new ScheduledThreadPoolExecutor(1);
            executor.scheduleAtFixedRate(this, 1L, periodMilliSecond, TimeUnit.MILLISECONDS);
            this.logEater = new LogEater();
            new Thread(this.logEater).start();
            logger.info("异步初始化 LogClient 完成");
        } else {
            logger.debug("heartbeat");
            period2Client++;
            if (period2Client == 1000L / periodMilliSecond) period2Client = 0;

            if (timeout == 0) {
                logger.error("LogClient 心跳超时, 主动停用自身");
                shutdown();
                return;
            }

            try {
                if (inputStream.available() > 0) {
                    timeout = timeoutSecond * (1000L / periodMilliSecond);
                    readInputStream();
                } else {
                    timeout = timeout - 1;
                }
                if (period2Client == 0) {
                    outputStream.write(LogProtocol.heartbeatMsg());
                    logger.debug("对 client 心跳");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SocketException && e.getMessage().equals("Broken pipe")) {
                    logger.error("LogClient 断链, 被动停用自身");
                    shutdown();
                    return;
                }
            }


        }
    }

    private void readInputStream() throws Exception {
        //尝试读取 len 个字节
        int len = inputStream.available();
        if (len <= 0) return;
        byte[] bs = new byte[len];
        int readCount = inputStream.read(bs);
        //修正
        if (readCount < len) {
            byte[] canEat = new byte[readCount];
            System.arraycopy(bs, 0, canEat, 0, readCount);
            bs = canEat;
        }
        //丢给 logEater 自行处理
        logEater.eat(bs);
    }

    private void shutdown() {

        try {
            if (inputStream.available() > 0) {
                readInputStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logEater.shouldStop();
        executor.shutdown();
        period2Client = 0;
        try {
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
