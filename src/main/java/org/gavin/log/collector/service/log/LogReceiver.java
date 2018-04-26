package org.gavin.log.collector.service.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ---------------------------------------------------
 * File:    LogReceiver
 * Package: org.gavin.logCollector.service.log
 * Project: GGLogCollector
 * ---------------------------------------------------
 * Created by gavinguan on 2018/2/9 12:53.
 * Copyright © 2018 gavinguan. All rights reserved.
 */
public class LogReceiver {

    private static Logger logger = LoggerFactory.getLogger(LogReceiver.class);

    //这个队列类使用CAS算法, 多线程问题可以应对, 无需考虑极端的ABA
    //如果以后有BUG, 可以考虑悲观锁list1 = collections.synchronized(list0) -> synchronized(list1) {}
    //ConcurrentLinkedQueue 的 size() 方法比较耗时, 但我也想控制ta的内容量, 暂时没有什么好的想法
    //你想, 如果外界没有持续消耗logBuffer, 而客户端一直都有log传过来, 最终一定会内存溢出的
    private ConcurrentLinkedQueue<LogDocument> logBuffer;

    private ServerSocket serverSocket;
    private ExecutorService pool;
    private int port;


    public LogReceiver() {
        this(5544, Runtime.getRuntime().availableProcessors() * 16);
    }

    public LogReceiver(int connectionMax) {
        this(5544, connectionMax);
    }

    public LogReceiver(int port, int connectionMax) {
        if (port <= 0) port = 5544;
        if (connectionMax <= 0 || connectionMax > 5000) connectionMax = Runtime.getRuntime().availableProcessors() * 10;
        this.port = port;
        this.pool = Executors.newFixedThreadPool(connectionMax);
        this.logBuffer = new ConcurrentLinkedQueue<>();
    }

    public void startupListener() throws IOException {
        if (available()) return;
        serverSocket = new ServerSocket(port);
        new Thread(this::runLoop).start();
        logger.info("LogReceiver 监听开启");
    }

    public void closeListener() throws IOException {
        if (!available()) return;
        serverSocket.close();
        logger.info("LogReceiver 监听停用");
    }

    public void shutdown() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (!pool.isShutdown()) {
            pool.shutdown();
        }
        logger.info("LogReceiver Shutdown");
    }

    public boolean available() {
        return serverSocket != null && !serverSocket.isClosed() && !pool.isShutdown();
    }

    public int getPort() {
        return port;
    }

    public boolean logBufferIsEmpty() {
        return logBuffer.size() == 0;
    }

    public void pushLog(LogDocument logDocument) {
        logBuffer.add(logDocument);
    }

    public LogDocument pollLog() {
        return logBuffer.poll();
    }

    private void runLoop() {
        while (true) {
            try {
                if (serverSocket.isClosed()) break;
                logger.debug("等待下一个client");
                Socket clientSocket = serverSocket.accept();
                logger.debug("收到一个client");
                pool.execute(new LogClient(clientSocket, this));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
