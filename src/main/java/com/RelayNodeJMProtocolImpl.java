package com.jmcoin.network;

import java.io.IOException;

/**
 * Class RelayNodeJMProtocolImpl
 * Implementation on the J-M protocol from the the {@link RelayNode}'s POV
 * @author enzo
 *
 */
public class RelayNodeJMProtocolImpl extends JMProtocolImpl<RelayNode> {

    public void setClient(ClientSC client) {
        this.client = client;
    }
    
    private ClientSC client;
	
	public RelayNodeJMProtocolImpl(RelayNode relay) throws IOException {
		super(relay);
	}
    
    private void receiveData(String payload, String pId) {
    	int id = 0;
    	try {
    		id = Integer.parseInt(pId);
    	}
    	catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
    	WorkerRunnableSC wrsc = this.client.getServer().findWorkerRunnable(id);
    	wrsc.setToSend(payload);
        /*this.client.getServer().getAwaitingAnswers().firstElement().setToSend(payload);
        this.client.getServer().getAwaitingAnswers().remove(this.client.getServer().getAwaitingAnswers().firstElement());*/
    }
    
    @Override
	protected void receiveUnspentOutputs(String string, String id) {
    	receiveData(craftMessage(NetConst.RECEIVE_UNSPENT_OUTPUTS, string), id);
	}
    
	@Override
	protected void receiveUnverifiedTransactions(String string, String id) {
		receiveData(craftMessage(NetConst.RECEIVE_UNVERIFIED_TRANS, string), id);
	}

	@Override
	protected void receiveRewardAmount(String string, String id) {
		receiveData(craftMessage(NetConst.RECEIVE_REWARD_AMOUNT, string), id);
	}

	@Override
	protected void receiveDifficulty(String payload, String id) {
		receiveData(craftMessage(NetConst.RECEIVE_DIFFICULTY, payload), id);
	}
	
	@Override
	protected void receiveBlockchainCopy(String string, String id) {
		this.peer.updateBlockChain(string);
		receiveData(craftMessage(NetConst.RECEIVE_BLOCKCHAIN_COPY, string), id);
	}
	
	@Override
	protected void receiveLastBlock(String block, String id) {
		receiveData(craftMessage(NetConst.RECEIVE_LAST_BLOCK, block), id);
	}

	@Override
	protected void receiveTransactionToThisAddress(String trans, String id) {
		receiveData(craftMessage(NetConst.RECEIVE_TRANS_TO_THIS_ADDRESS, trans), id);
	}

	@Override
	protected String stopMining(){
		this.peer.setChainUpToDate(false);
		return craftMessage(NetConst.STOP_MINING, null);
	}

	@Override
	protected String giveMeBlockChainCopyImpl(String id) {
		if(this.peer.isChainUpToDate()) {
			System.err.println(getClass().getSimpleName() + ": blockchain returned by relay");
			//this.client.getServer().getAwaitingAnswers().remove(this.client.getServer().getAwaitingAnswers().firstElement());
			return craftMessage(NetConst.RECEIVE_BLOCKCHAIN_COPY, this.peer.getGson().toJson(this.peer.getLocalChainCopy()), id);
		}
		System.err.println(getClass().getSimpleName() + ": request forwarded");
		this.client.setToSend(craftMessage(NetConst.GIVE_ME_BLOCKCHAIN_COPY, null, id));
		return null;
	}

	@Override
	protected String giveMeRewardAmountImpl(String id) {
		this.client.setToSend(craftMessage(NetConst.GIVE_ME_REWARD_AMOUNT, null, id));
		return null;
	}

	@Override
	protected String giveMeUnverifiedTransactionsImpl(String id) {
		this.client.setToSend(craftMessage(NetConst.GIVE_ME_UNVERIFIED_TRANSACTIONS, null, id));
		return null;
	}

	@Override
	protected String takeMyMinedBlockImpl(String payload) {
		if(payload != null) {
			this.client.setToSend(craftMessage(NetConst.TAKE_MY_MINED_BLOCK, payload));
		}
		return null;
	}

	@Override
	protected boolean takeMyNewTransactionImpl(String payload) {
		if (payload != null) {
			System.out.println("BEFORE - this.client.setToSend ");
			this.client.setToSend(craftMessage(NetConst.TAKE_MY_NEW_TRANSACTION, payload));
			System.out.println("AFTER - this.client.setToSend \n");
			return true;
		}
		return false;
	}

	@Override
	protected String giveMeDifficulty(String id) {
		this.client.setToSend(craftMessage(NetConst.GIVE_ME_DIFFICULTY, null, id));
		return null;
	}

	@Override
	protected String giveMeUnspentOutputs(String id) {
		this.client.setToSend(craftMessage(NetConst.GIVE_ME_UNSPENT_OUTPUTS, null, id));
		return null;
	}

	@Override
	protected String giveMeLastBlock(String id) {
		this.client.setToSend(craftMessage(NetConst.GIVE_ME_LAST_BLOCK, null, id));
		return null;
	}

	@Override
	protected String giveMeTransactionsToThisAddress(String address, String id) {
		this.client.setToSend(craftMessage(NetConst.GIVE_ME_TRANS_TO_THIS_ADDRESS, address, id));
		return null;
	}
}