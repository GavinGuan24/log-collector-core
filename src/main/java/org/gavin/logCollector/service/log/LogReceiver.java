package org.gavin.logCollector.service.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
        if (connectionMax <= 0 || connectionMax > 65535) connectionMax = Runtime.getRuntime().availableProcessors() * 10;
        this.port = port;
        this.pool = Executors.newFixedThreadPool(connectionMax);
    }

    public void startupListener() throws IOException {
        if (available()) return;
        this.serverSocket = new ServerSocket(this.port);
        new Thread( () -> runLoop() ).start();
        logger.info("LogReceiver 监听开启");
    }

    public void closeListener() throws IOException {
        if (!available()) return;
        this.serverSocket.close();
        logger.info("LogReceiver 监听停用");
    }

    public void shutdown() throws IOException {
        if (this.serverSocket != null && !this.serverSocket.isClosed()) {
            this.serverSocket.close();
        }
        if (!pool.isShutdown()) {
            pool.shutdown();
        }
        logger.info("LogReceiver Shutdown");
    }

    public boolean available() {
        return this.serverSocket != null && !this.serverSocket.isClosed() && !pool.isShutdown();
    }

    public int getPort() {
        return port;
    }

    private void runLoop() {
        while (true) {
            try {
                if (this.serverSocket.isClosed()) break;
                logger.debug("等待下一个client");
                Socket clientSocket = this.serverSocket.accept();
                logger.debug("收到一个client");
                pool.execute(new LogClient(clientSocket));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
