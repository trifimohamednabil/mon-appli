package com.jmcoin.network;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.jmcoin.model.Block;
import com.jmcoin.model.Chain;
import com.jmcoin.model.Output;
import com.jmcoin.model.Transaction;

public class MinerJMProtocolImpl extends JMProtocolImpl<MinerNode>{
	
	private Client client;
	
	public MinerJMProtocolImpl(MinerNode peer) throws IOException, NoSuchAlgorithmException {
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

	public MinerJMProtocolImpl(MinerNode peer, String hostname) throws IOException, NoSuchAlgorithmException {
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
	protected String stopMining() {
		System.err.println("**************** !! STOP !! ****************");
		this.peer.stopMiningThread();
		return NetConst.RES_OKAY;
	}

	@Override
	//unused
	protected String giveMeBlockChainCopyImpl(String id) {return null;}

	@Override
	//unused
	protected String giveMeRewardAmountImpl(String id) {return null;}

	@Override
	//unused
	protected String giveMeUnverifiedTransactionsImpl(String id) {return null;}

	@Override
	//unused
	protected String takeMyMinedBlockImpl(String payload) {return null;}

	@Override
	//unused
	protected boolean takeMyNewTransactionImpl(String payload) {return false;}
	
	@Override
	protected String giveMeDifficulty(String id) {return null;}

	@Override
	protected String giveMeUnspentOutputs(String id) {return null;}

	@Override
	protected void receiveDifficulty(String string, String id) {
		setBundle(string , Integer.class);
	}

	@Override
	protected void receiveUnverifiedTransactions(String string, String id) {
		setBundle(string , Transaction[].class);
	}

	@Override
	protected void receiveRewardAmount(String string, String id) {
		setBundle(string , Integer.class);
	}

	@Override
	protected void receiveBlockchainCopy(String string, String id) {
		setBundle(string , Chain.class);
	}

	@Override
	protected void receiveUnspentOutputs(String string, String id) {
		setBundle(string , new TypeToken<Map<String, Output>>(){}.getType());
	}
	
	public void sendMinedBlock(Block block) throws IOException {
		this.client.sendMessage(craftMessage(NetConst.TAKE_MY_MINED_BLOCK, this.peer.getGson().toJson(block)));
	}

	@Override
	protected String giveMeLastBlock(String id) {return null;}

	@Override
	protected void receiveLastBlock(String block, String id) {
		setBundle(block, Block.class);
	}

	@Override
	protected void receiveTransactionToThisAddress(String trans, String id) {}

	@Override
	protected String giveMeTransactionsToThisAddress(String address, String id) {return null;}
}
