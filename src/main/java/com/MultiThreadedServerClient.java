package com.jmcoin.network;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

public class MultiThreadedServerClient extends MultiThreadedServer{

    private LinkedList<WorkerRunnableSC> lThreadsSC;
    //private Vector<WorkerRunnableSC> awaitingAnswers;
    //private LinkedList<WorkerRunnableSC> awaitingAnswers;
    private ClientSC client;

    /*public Vector<WorkerRunnableSC> getAwaitingAnswers() {
        return awaitingAnswers;
    }*/
    /*public LinkedList<WorkerRunnableSC> getAwaitingAnswers(){
    	return this.awaitingAnswers;
    }*/

    public MultiThreadedServerClient(int port, RelayNodeJMProtocolImpl protocol){
        super(port, protocol);
        lThreadsSC = new LinkedList<WorkerRunnableSC>();
        //this.awaitingAnswers = new LinkedList<>();//awaitingAnswers = new Vector<WorkerRunnableSC>();
    }

    public void setClient(ClientSC client) {
        this.client = client;
    }

    public LinkedList<WorkerRunnableSC> getlThreadsSC() {
        return lThreadsSC;
    }
    
    public WorkerRunnableSC findWorkerRunnable(int reqId) {
    	WorkerRunnableSC wrsc = null;
    	for(WorkerRunnableSC w : this.lThreadsSC) {
    		if(w.getRequestSenderId() == reqId) {
    			wrsc = w;
    			break;
    		}
    	}
    	return wrsc;
    }

    @Override
    public void run() {
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(!isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            try {
                lThreadsSC.add(new WorkerRunnableSC(clientSocket, protocol, this.client));
                lThreadsSC.getLast().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Server Stopped.");
    }

    @Override
    public synchronized void not(){
        for (WorkerRunnableSC wr: lThreadsSC) {
            wr.not();
        }
    }
}


