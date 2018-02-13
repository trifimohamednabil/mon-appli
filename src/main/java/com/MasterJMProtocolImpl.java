package com.jmcoin.network;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.List;

import com.google.gson.JsonSyntaxException;
import com.jmcoin.model.Block;
import com.jmcoin.model.Transaction;

public class MasterJMProtocolImpl extends JMProtocolImpl<MasterNode>{
	
	public MasterJMProtocolImpl(MasterNode masterNode) throws IOException {
		super(masterNode);
	}

	@Override
	protected String stopMining (){
		return craftMessage(NetConst.STOP_MINING, null);
	}

	@Override
	protected String giveMeUnverifiedTransactionsImpl(String id) {
		List<Transaction> transactions = this.peer.getUnverifiedTransactions().size() > NetConst.MAX_SENT_TRANSACTIONS ?
				this.peer.getUnverifiedTransactions().subList(0, NetConst.MAX_SENT_TRANSACTIONS):
					this.peer.getUnverifiedTransactions();
		return craftMessage(NetConst.RECEIVE_UNVERIFIED_TRANS, this.peer.getGson().toJson(transactions), id);
	}

	@Override
	protected String takeMyMinedBlockImpl(String payload) throws IOException {
		if (payload != null) {
			try {
				if(this.peer.processMinedBlock(this.peer.getGson().fromJson(payload, Block.class)))
					return stopMining();
			}
			catch(JsonSyntaxException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException jse) {
				jse.printStackTrace();
			}
		}
		return NetConst.RES_OKAY;
	}

	@Override
	protected boolean takeMyNewTransactionImpl(String payload) {
		try {
			System.out.println("MasterNode: transaction received");
			Transaction transaction = this.peer.getGson().fromJson(payload, Transaction.class);
			this.peer.getUnverifiedTransactions().add(transaction);
			return true;
		}
		catch(JsonSyntaxException jse) {
			System.out.println("MasterNode: transaction rejected");
			jse.printStackTrace();
		}
		return false;
	}

	@Override
	protected String giveMeDifficulty(String id) {
		return craftMessage(NetConst.RECEIVE_DIFFICULTY, Integer.toString(this.peer.getDifficulty()), id);
	}
	
	@Override
	protected String giveMeRewardAmountImpl(String id) {
		return craftMessage(NetConst.RECEIVE_REWARD_AMOUNT, Integer.toString(this.peer.getRewardAmount()), id);
	}
	
	@Override
	protected String giveMeBlockChainCopyImpl(String id) {
		return craftMessage(NetConst.RECEIVE_BLOCKCHAIN_COPY, this.peer.getGson().toJson(this.peer.getChain()), id);
	}

	@Override
	protected String giveMeUnspentOutputs(String id) {
		return craftMessage(NetConst.RECEIVE_UNSPENT_OUTPUTS, this.peer.getGson().toJson(this.peer.getUnspentOutputs()), id);
	}

	@Override
	protected void receiveDifficulty(String string, String id) {}

	@Override
	protected void receiveUnverifiedTransactions(String string, String id) {}

	@Override
	protected void receiveRewardAmount(String string, String id) {}

	@Override
	protected void receiveBlockchainCopy(String nextToken, String id) {}

	@Override
	protected void receiveUnspentOutputs(String string, String id) {}

	@Override
	protected String giveMeLastBlock(String id) {
		return craftMessage(NetConst.RECEIVE_LAST_BLOCK, this.peer.getGson().toJson(this.peer.getLastBlock()), id);
	}

	@Override
	protected void receiveLastBlock(String block, String id) {}

	@Override
	protected void receiveTransactionToThisAddress(String trans, String id) {}

	@Override
	protected String giveMeTransactionsToThisAddress(String address, String id) {
		return craftMessage(NetConst.RECEIVE_TRANS_TO_THIS_ADDRESS, this.peer.getGson().toJson(this.peer.getTransactionsToThisAddress(address)), id);
	}
}
