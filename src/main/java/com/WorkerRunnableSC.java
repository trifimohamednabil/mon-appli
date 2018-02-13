package com.jmcoin.network;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;

public class WorkerRunnableSC extends WorkerRunnable {

    private ClientSC client;
    private Thread thread;
    private int requestSenderId;

    public WorkerRunnableSC(Socket clientSocket, JMProtocolImpl<? extends Peer> protocol, ClientSC client) throws IOException {
        super(clientSocket, protocol);
        this.client = client;
        ((RelayNodeJMProtocolImpl) this.protocol).setClient(this.client);
        this.requestSenderId = new SecureRandom().nextInt();
    }
    
    public int getRequestSenderId() {
		return requestSenderId;
	}

    @Override
    public void run() {
        try {
            thread = new Thread(new ReceiverThread<WorkerRunnableSC>(this));
            thread.start();
            do {
                if (getToSend() != null){
//                    System.out.println("WorkRunnable Thread #"+Thread.currentThread().getId() +" WorkRunnableSC - to send : " + toSend.toString());
                    sendMessage(toSend);
                }
                Thread.sleep(100);
            } while (true);
        } catch (IOException|InterruptedException e) {
            try {
                close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    protected void handleMessage(Object msg) {
        switch (msg.toString()) {
            case NetConst.CONNECTED :
                break;
            case NetConst.CONNECTION_REQUEST:
                setToSend(NetConst.CONNECTED);
                break;
            default:
            	//this.client.getServer().getAwaitingAnswers().add(this);
                System.out.println("BEFORE - this.protocol.processInput INTO WorkerRunnableSC");
                setToSend(this.protocol.processInput( msg.toString().replace(NetConst.DEFAULT_TRAILER, NetConst.DELIMITER+Integer.toString(this.requestSenderId)+NetConst.DELIMITER)));
                System.out.println("AFTER - this.protocol.processInput INTO WorkerRunnableSC");
                break;
        }
    }
}
