package org.gavin.log.collector.service.log.sender;

import org.gavin.log.collector.service.log.sender.vo.Caller;
import org.gavin.log.collector.service.log.sender.vo.LoggingEventVO;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.alibaba.fastjson.JSON;
import org.gavin.log.collector.service.log.protocol.LogProtocol;
import org.gavin.log.collector.service.log.sender.vo.throwable.StackTraceElementVO;
import org.gavin.log.collector.service.log.sender.vo.throwable.ThrowableProxyVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ---------------------------------------------------
 * File:    GGTcpSocketAppender
 * Package: org.gavin.logCollector.service.log
 * Project: GGLogCollector
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/9 13:47.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class GGTcpSocketAppender extends AppenderBase implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(GGTcpSocketAppender.class);

    private String host;
    private int port;
    private int timeoutSecond;
    //--------------------------------------------------------------
    private ScheduledThreadPoolExecutor executor;
    private Socket serverSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean connected;

    private boolean notCreatedServerSocket;
    private int originalTimeoutSecond;
    private int failCount;

    private Long failConnectMoment;

    public GGTcpSocketAppender() {
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.executor.scheduleAtFixedRate(this, 1L, 1000L, TimeUnit.MILLISECONDS);
        this.notCreatedServerSocket = true;
        this.failCount = 0;
        this.connected = false;
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

    public int getTimeoutSecond() {
        return timeoutSecond;
    }

    public void setTimeoutSecond(int timeoutSecond) {
        this.timeoutSecond = timeoutSecond;
    }

    //--------------------------------------------------------------

    @Override
    public void stop() {
        executor.shutdown();
        reset();
        super.stop();
    }

    @Override
    protected void append(Object o) {

        byte status0 = lazyCreateServerSocket();
        //1.(首次调用失败) 2.(曾经调用过, 但未连接) --> [直接return, 放弃处理Log] -??-> 考虑做部分数据缓存吗?
        if (status0 == -1 || (status0 == 0 && !connected)) return;
        //暂无迹象表明 断链, 尝试处理 LoggingEvent
        eatLog((LoggingEvent) o);
    }

    @Override
    public void run() {
        if (notCreatedServerSocket) return;
        //被重置过, 直接尝试重连
        if (serverSocket == null) {
            logger.debug("serverSocket[{}]:{} == null", host, port);
            reconnecting();
            return;
        }
        //检查 ServerSocket 断链
        if (serverSocket.isClosed()) {
            logger.error("LogServer[{}]:{} 断链, serverSocket.isClosed == true", host, port);
            reset();
            return;
        }

        try {
            outputStream.write(LogProtocol.heartbeatMsg());
            logger.debug("对 LogServer 心跳");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("LogServer[{}]:{} 断链, 心跳失败", host, port);
            reset();
            return;
        }

        //日常消耗 LogServer 心跳数据
        if (timeoutSecond <= 0) {
            //主动断链, 稍后尝试重连
            logger.error("LogServer[{}]:{} 心跳超时, 主动断链", host, port);
            reset();
            failConnectMoment = System.currentTimeMillis();
            failCountAutoIncrement();
            return;
        }

        //尝试读取 len 个字节
        try {
            int len = inputStream.available();
            if (len <= 0) {
                timeoutSecond--;
            } else {
                timeoutSecond = originalTimeoutSecond;
            }
            byte[] bs = new byte[len];
            int readCount = inputStream.read(bs);
            //修正
            if (readCount < len) {
                byte[] canEat = new byte[readCount];
                System.arraycopy(bs, 0, canEat, 0, readCount);
                bs = canEat;
            }

            logger.debug("LogServer心跳: " + new String(bs, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //--------------------------------------------------------------

    private void eatLog(LoggingEvent event) {

        LoggingEventVO loggingEventVO = new LoggingEventVO();
        ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();

        if (throwableProxy != null) {
            //含异常代理, 主信息处理
            ThrowableProxyVO throwableProxyVO = new ThrowableProxyVO();
            throwableProxyVO.setClassName(throwableProxy.getClassName());
            throwableProxyVO.setMessage(throwableProxy.getMessage());

            StackTraceElementProxy[] stackTraceElementProxies = throwableProxy.getStackTraceElementProxyArray();
            List<StackTraceElementVO> stackTraceElementVOList = new ArrayList<>(6);
            //最多向上追溯6个堆信息, 直接拼接最多3个堆信息至LoggingEventVO.message
            int StackTraceElementProxyArraySize = stackTraceElementProxies.length;
            if (StackTraceElementProxyArraySize > 6) StackTraceElementProxyArraySize = 6;

            StringBuilder stackTraceMsgSB = new StringBuilder("\n\tstackTraceMsg ===>\n");

            for (int i = 0; i < StackTraceElementProxyArraySize; i++) {
                StackTraceElementProxy eProxy = stackTraceElementProxies[i];
                StackTraceElement ste = eProxy.getStackTraceElement();
                if (i < 3) {
                    stackTraceMsgSB.append("{class:");
                    stackTraceMsgSB.append(ste.getClassName());
                    stackTraceMsgSB.append(".");
                    stackTraceMsgSB.append(ste.getMethodName());
                    stackTraceMsgSB.append(" file:");
                    stackTraceMsgSB.append(ste.getFileName());
                    stackTraceMsgSB.append("@");
                    stackTraceMsgSB.append(ste.getLineNumber());
                    stackTraceMsgSB.append("}\n");
                }
                stackTraceElementVOList.add(new StackTraceElementVO(ste));
            }
            throwableProxyVO.setStackTraceElementList(stackTraceElementVOList);

            loggingEventVO.setThrowableProxyVO(throwableProxyVO);
            loggingEventVO.setMessage("["+ throwableProxy.getClassName() + ": " + throwableProxy.getMessage() +"]" + stackTraceMsgSB.toString());
        } else {
            //不含异常代理, 主信息处理
            loggingEventVO.setMessage(event.getMessage());
        }

        loggingEventVO.setTimeStamp(event.getTimeStamp());
        loggingEventVO.setLoggerName(event.getLoggerName());
        loggingEventVO.setThreadName(event.getThreadName());

        StackTraceElement[] stackTraceElementArray = event.getCallerData();
        List<Caller> callerList = new ArrayList<>();
        for (StackTraceElement ste : stackTraceElementArray) {
            Caller caller = new Caller(ste);
            callerList.add(caller);
        }
        loggingEventVO.setCallerList(callerList);

        try {
            outputStream.write(LogProtocol.appendEndByte(JSON.toJSONString(loggingEventVO)));
        } catch (Exception e) {
            e.printStackTrace();
            //无法发送 Log, 重置链接
            reset();
        }
    }

    private synchronized void reconnecting() {
        //没有尝试过重连, 直接开始
        if (failConnectMoment == null) {
            logger.info("尝试重连 LogServer");
            tryToRebuildConnection();
        } else {
            //是否到达重连时间
            if ((System.currentTimeMillis() - failConnectMoment) / 1000L > originalTimeoutSecond * Math.pow(2, failCount)) {
                logger.info("尝试重连 LogServer");
                tryToRebuildConnection();
            }
        }
    }

    private void tryToRebuildConnection() {
        try {
            serverSocket = new Socket(host, port);
            inputStream = serverSocket.getInputStream();
            outputStream = serverSocket.getOutputStream();

            connected = true;
            failCount = 0;
            failConnectMoment = null;
            timeoutSecond = originalTimeoutSecond;
            logger.info("LogServer[{}]:{} 重连成功", host, port);

        } catch (Exception e) {
            e.printStackTrace();
            reset();
            failConnectMoment = System.currentTimeMillis();
            failCountAutoIncrement();
            logger.error("LogServer[{}]:{} 重连失败, {}秒后重试", host, port, (originalTimeoutSecond * Math.pow(2, failCount)));
        }
    }

    //0: 曾经调用过 1:首次调用成功 -1:首次调用失败
    private byte lazyCreateServerSocket() {
        if (!notCreatedServerSocket) return 0;
        notCreatedServerSocket = false;
        if (timeoutSecond < 10) timeoutSecond = 10;
        originalTimeoutSecond = timeoutSecond;
        try {
            serverSocket = new Socket(host, port);
            inputStream = serverSocket.getInputStream();
            outputStream = serverSocket.getOutputStream();
            connected = true;
        } catch (Exception e) {
            e.printStackTrace();
            reset();
            failConnectMoment = System.currentTimeMillis();
            logger.error("LogServer[{}]:{} 连接失败, {}秒后重试", host, port, (originalTimeoutSecond * Math.pow(2, failCount)));
            return -1;
        }
        return 1;
    }

    private void failCountAutoIncrement() {
        if (failCount < 6) failCount++;
    }

    private void reset() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception e0) {
                e0.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        serverSocket = null;
        inputStream = null;
        outputStream = null;
        connected = false;
    }

}
