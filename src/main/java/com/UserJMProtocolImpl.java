package com.jmcoin.network;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.bouncycastle.util.encoders.Hex;

import com.google.gson.reflect.TypeToken;
import com.jmcoin.model.Block;
import com.jmcoin.model.Chain;
import com.jmcoin.model.Output;
import com.jmcoin.model.Transaction;

public class UserJMProtocolImpl extends JMProtocolImpl<UserNode>{

	private Client client;
	
	public UserJMProtocolImpl(UserNode peer) throws IOException {
		super(peer);
		this.client = new Client(NetConst.RELAY_NODE_LISTEN_PORT, NetConst.RELAY_DEBUG_HOST_NAME, this);
        new Thread(new ReceiverThread<Client>(this.client)).start();
        new Thread(this.client).start();
        try {
            Thread.sleep(2000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
	
	public UserJMProtocolImpl(UserNode peer, String hostname) throws IOException {
		super(peer);
		this.client = new Client(NetConst.RELAY_NODE_LISTEN_PORT, hostname, this);
        new Thread(new ReceiverThread<Client>(this.client)).start();
        new Thread(this.client).start();
        try {
            Thread.sleep(2000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
	
	public Client getClient() {
		return client;
	}

	@Override
	protected void receiveUnspentOutputs(String string, String id) {
		setBundle(string, new TypeToken<Map<String, Output>>(){}.getType());		
	}

	@Override
	protected void receiveBlockchainCopy(String string, String id) {
		setBundle(string, Chain.class);
	}

	@Override
	protected void receiveUnverifiedTransactions(String string, String id) {}

	@Override
	protected void receiveRewardAmount(String string, String id) {}

	@Override
	protected void receiveDifficulty(String string, String id) {}

	@Override
	protected String stopMining() {return null;}

	@Override
	protected String giveMeUnspentOutputs(String id) {return null;}

	@Override
	protected String giveMeBlockChainCopyImpl(String id) {return null;}

	@Override
	protected String giveMeRewardAmountImpl(String id) {return null;}

	@Override
	protected String giveMeUnverifiedTransactionsImpl(String id) {return null;}

	@Override
	protected String takeMyMinedBlockImpl(String payload) throws IOException {return null;}

	@Override
	protected boolean takeMyNewTransactionImpl(String payload) {return false;}

	@Override
	protected String giveMeDifficulty(String id) {return null;}

	@Override
	protected String giveMeLastBlock(String id) {return null;}

	@Override
	protected void receiveLastBlock(String block, String id) {
		setBundle(block, Block.class);
	}

	@Override
	protected void receiveTransactionToThisAddress(String trans, String id) {
		setBundle(trans, Transaction[].class);
	}

	@Override
	protected String giveMeTransactionsToThisAddress(String address, String id) {return null;}
	
	public double getAddressBalance(String ... addresses) throws IOException{
		Transaction[] transactions = downloadObject(NetConst.GIVE_ME_TRANS_TO_THIS_ADDRESS, this.peer.getGson().toJson(addresses), getClient());
    	Map<String, Output> unspentOutputs = downloadObject(NetConst.GIVE_ME_UNSPENT_OUTPUTS, null, this.client);
    	this.peer.getWallet().updatePendingOutputs(unspentOutputs);
    	double totalOutputAmount = 0;
    	for(String address : addresses) {
    		int i = 0;
        	while(i < transactions.length) {
        		Output tmpOut = null;
				if(Objects.equals(transactions[i].getOutputBack().getAddress(), address)) {
					tmpOut = transactions[i].getOutputBack();
				}
				else if(Objects.equals(transactions[i].getOutputOut().getAddress(), address)){
					tmpOut = transactions[i].getOutputOut();
				}
        		if(tmpOut != null) {
        			String key = Hex.toHexString(transactions[i].getHash()) + Peer.DELIMITER+tmpOut.getAddress();
        			if(unspentOutputs.containsKey(key) && !this.peer.getWallet().getPendingOutputs().containsKey(key)){
        				totalOutputAmount+= tmpOut.getAmount();
    				}
        		}
        		i++;
        	}
    	}
        return totalOutputAmount;
    }
}
