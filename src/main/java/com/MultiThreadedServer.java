package com.jmcoin.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Vector;

public class MultiThreadedServer implements Runnable{
    protected int          serverPort   = -1;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected JMProtocolImpl<? extends Peer> protocol	= null;
    protected Vector<WorkerRunnable> lThreads;

    protected Client client;

    public MultiThreadedServer(int port, JMProtocolImpl<? extends Peer> protocol){
        this.serverPort = port;
        this.protocol = protocol;
        lThreads = new Vector<WorkerRunnable>();
    }

    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            try {
                lThreads.add(new WorkerRunnable(clientSocket, protocol, this));
                lThreads.lastElement().start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Server Stopped.") ;
    }


    protected synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    public synchronized void not(){
        for (WorkerRunnable wr: lThreads) {
            wr.not();
        }
    }

    protected void openServerSocket() {
        try {
            System.out.println("Launching server on port : " + this.serverPort);
            this.serverSocket = new ServerSocket(this.serverPort);
            System.out.println("Running server on port : " + this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port "+ this.serverPort, e);
        }
    }

}