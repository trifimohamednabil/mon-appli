package com.jmcoin.network;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class WorkerRunnable extends TemplateThread{

    private MultiThreadedServer server;

    public MultiThreadedServer getServer() {
        return server;
    }

    public void setServer(MultiThreadedServer server) {
        this.server = server;
    }

    // To KEEP FOR THE USAGE OF WORKERRUNNABLESC
    public WorkerRunnable(Socket clientSocket, JMProtocolImpl<? extends Peer> protocol) throws  IOException{
        super(protocol);
        this.socket = clientSocket;
        socket.setSoTimeout(0);
        in  = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());

    }

    public WorkerRunnable(Socket clientSocket, JMProtocolImpl<? extends Peer> protocol, MultiThreadedServer srv) throws  IOException{
        super(protocol);
        this.socket = clientSocket;
        in  = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        setServer(srv);
    }

    public void run() {
        try {
            new Thread( new ReceiverThread<WorkerRunnable>(this)).start();
            boolean loop = true;
            do {
                if (getToSend() != null){
//                    System.out.println("Thread #"+Thread.currentThread().getId() +this.protocol.getClass().getSimpleName()+" WorkRunnable - to send : " + toSend.toString());
                    if (toSend.toString().equals(NetConst.STOP_MINING_REQ))
                        server.not();
                    else
                        sendMessage(toSend);
                }
                Thread.sleep(100);
            } while (loop);
        } catch (IOException e) {
            try {
                close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                System.out.println("BEFORE - this.protocol.processInput INTO WorkerRunnable");
                setToSend(this.protocol.processInput(msg));
                System.out.println("BEFORE - this.protocol.processInput INTO WorkerRunnable");
                break;
        }
    }

    synchronized protected void not(){
        try {
            sendMessage(this.protocol.craftMessage(NetConst.STOP_MINING, null));
        }
        catch (SocketException ignored){ }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

