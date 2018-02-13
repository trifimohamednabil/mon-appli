package com.jmcoin.network;

import java.io.IOException;
import java.io.ObjectInputStream;

public class ReceiverThread<X extends TemplateThread> implements Runnable{

    protected ObjectInputStream input;
    protected X runnable; 

    public ReceiverThread(X workerRunnable) {
        runnable = workerRunnable;
        input = workerRunnable.getIn();
    } 

    @Override
    public void run() {
        boolean loop = true;
        try {
            do {
                Object read = input.readObject();
                if (read != null) {
                    System.out.println("BEFORE - this.runnable.handleMessage INTO ReceiverThread" + read);
                    this.runnable.handleMessage(read);
                    System.out.println("AFTER - this.runnable.handleMessage INTO ReceiverThread\n");
                }
                Thread.sleep(10);
            }while (loop);
        } catch (IOException e) {
            try {
                this.runnable.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (ClassNotFoundException|InterruptedException e) {
            e.printStackTrace();
        }

    }
}
