package com.jmcoin.network;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.StringTokenizer;

import com.jmcoin.model.Block;
import com.jmcoin.model.Bundle;
import com.jmcoin.model.Output;
import com.jmcoin.model.Transaction;

/**
 * Global protocol. Is redefined is sub-classes in order to fit requirements for each node
 * @author enzo
 */
public abstract class JMProtocolImpl<X extends Peer> {
	
	protected X peer;
	
	public JMProtocolImpl(X peer) {
		this.peer = peer;
	}
	
	protected <T> void setBundle(String payload, Type type) {
		Bundle<T> bundle = this.peer.createBundle(type);
		bundle.setObject(this.peer.getGson().fromJson(payload, type));
		this.peer.setBundle(bundle);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T downloadObject(String req, String body, Client client) throws IOException {
		client.sendMessage(craftMessage(req, body));
		T t;
        while((t = (T) this.peer.getBundle().getObject()) == null) {
        	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
		return t;
	}

	/**
	 * Will assume that the payload is built as follows:
	 * x$yyyyyyyyyyyyy$zz
	 * where x is the type
	 * and y the payload itself (can be empty, like 0$$#)
	 * @throws IOException 
	 */
	public String processInput(Object message) {
		String content = (String)message;
		StringTokenizer tokenizer = new StringTokenizer(content, NetConst.DELIMITER);
		if(!tokenizer.hasMoreTokens()) return null;
		String type = tokenizer.nextToken();
		if(type != null && type.length() != 0) {
			if(!tokenizer.hasMoreTokens()) return null;
			switch (type) {
			case NetConst.GIVE_ME_BLOCKCHAIN_COPY:
				tokenizer.nextToken();
				return giveMeBlockChainCopyImpl(tokenizer.nextToken());
			case NetConst.GIVE_ME_REWARD_AMOUNT:
				tokenizer.nextToken();
				return giveMeRewardAmountImpl(tokenizer.nextToken());
			case NetConst.GIVE_ME_UNVERIFIED_TRANSACTIONS:
				tokenizer.nextToken();
				return giveMeUnverifiedTransactionsImpl(tokenizer.nextToken());
			case NetConst.TAKE_MY_MINED_BLOCK:
				try {
					return takeMyMinedBlockImpl(tokenizer.nextToken()); 
				} catch (IOException e) {
					e.printStackTrace();
				}
				return NetConst.RES_NOK;
			case NetConst.TAKE_MY_NEW_TRANSACTION:
				return takeMyNewTransactionImpl(tokenizer.nextToken()) ? NetConst.RES_OKAY : NetConst.RES_NOK;
			case NetConst.GIVE_ME_UNSPENT_OUTPUTS:
				tokenizer.nextToken();
				return giveMeUnspentOutputs(tokenizer.nextToken());
			case NetConst.GIVE_ME_TRANS_TO_THIS_ADDRESS:
            	return giveMeTransactionsToThisAddress(tokenizer.nextToken(), tokenizer.nextToken());
			case NetConst.GIVE_ME_DIFFICULTY:
				tokenizer.nextToken();
				return giveMeDifficulty(tokenizer.nextToken());
            case NetConst.RECEIVE_DIFFICULTY:
            	receiveDifficulty(tokenizer.nextToken(), tokenizer.nextToken());
            	return NetConst.RES_OKAY;
            case NetConst.RECEIVE_REWARD_AMOUNT:
            	receiveRewardAmount(tokenizer.nextToken(), tokenizer.nextToken());
            	return NetConst.RES_OKAY;
            case NetConst.RECEIVE_UNVERIFIED_TRANS:
            	receiveUnverifiedTransactions(tokenizer.nextToken(), tokenizer.nextToken());
            	return NetConst.RES_OKAY;
            case NetConst.RECEIVE_BLOCKCHAIN_COPY:
            	receiveBlockchainCopy(tokenizer.nextToken(), tokenizer.nextToken());
            	return NetConst.RES_OKAY;
            case NetConst.RECEIVE_UNSPENT_OUTPUTS:
            	receiveUnspentOutputs(tokenizer.nextToken(), tokenizer.nextToken());
            	return NetConst.RES_OKAY;
            case NetConst.GIVE_ME_LAST_BLOCK:
            	tokenizer.nextToken();
            	return giveMeLastBlock(tokenizer.nextToken());
            case NetConst.RECEIVE_LAST_BLOCK:
            	receiveLastBlock(tokenizer.nextToken(), tokenizer.nextToken());
            	return NetConst.RES_OKAY;
            case NetConst.RECEIVE_TRANS_TO_THIS_ADDRESS:
            	receiveTransactionToThisAddress(tokenizer.nextToken(), tokenizer.nextToken());
            	return NetConst.RES_OKAY;
            case NetConst.STOP_MINING:
                return stopMining();
			default:
				return NetConst.ERR_NOT_A_REQUEST;
			}
		}
		return NetConst.ERR_BAD_REQUEST;
	}

    protected abstract void receiveUnspentOutputs(String string, String string2);
	protected abstract void receiveBlockchainCopy(String nextToken, String string);
	protected abstract void receiveUnverifiedTransactions(String string, String string2);
	protected abstract void receiveRewardAmount(String string, String string2);
	protected abstract void receiveDifficulty(String string, String string2);
	protected abstract void receiveLastBlock(String block, String string);
	protected abstract void receiveTransactionToThisAddress(String trans, String string);
	
    protected abstract String stopMining();
	/**
	 * Returns unspent {@link Output} as a list
	 * @param string 
	 * @return
	 */
	protected abstract String giveMeUnspentOutputs(String string);

	/**
	 * Returns the last version of the blockchain
	 * @param string 
	 * @return blockchain as a string
	 */
	protected abstract String giveMeBlockChainCopyImpl(String string);
	
	/**
	 * Returns the last amount of the reward, computed according to the time
	 * (or arbitrarily set)
	 * @param string 
	 * @return amount of the reward
	 */
	protected abstract String giveMeRewardAmountImpl(String string);
	
	/**
	 * Returns all non-verified pending transactions
	 * @param string 
	 * @return set of transactions
	 */
	protected abstract String giveMeUnverifiedTransactionsImpl(String string);
	
	/**
	 * Gets a new mined {@link Block}, and returns false only if the body cannot be parsed
	 * Getting a "true" doesn't mean that the {@link Block} is valid regarding to the protocol
	 * @param payload {@link Block} to parse
	 * @return true if the {@link Block} has been received properly
	 * @throws IOException 
	 */
	protected abstract String takeMyMinedBlockImpl(String payload) throws IOException;
	
	/**
	 * Gets a new {@link Transaction}, and returns false only if the body cannot be parsed
	 * Getting a "true" doesn't mean that the {@link Transaction} is valid regarding to the protocol
	 * @param payload {@link Transaction} to parse
	 * @return true if the {@link Transaction} has been received properly
	 */
	protected abstract boolean takeMyNewTransactionImpl(String payload);	
	
	/**
	 * Returns difficulty on demand
	 * @param string 
	 * @return
	 */
	protected abstract String giveMeDifficulty(String string);
	protected abstract String giveMeLastBlock(String string);
	protected abstract String giveMeTransactionsToThisAddress(String address, String string);
	
	/**
	 * Builds a message to send over the network, compliant with the protocol
	 * @param request {@link NetConst}.xxxxx
	 * @param body JSON object if needed
	 * @return message
	 */
	public String craftMessage(String request, String body) {
		return request + NetConst.DELIMITER + body + NetConst.DEFAULT_TRAILER;
	}
	
	public String craftMessage(String request, String body, String rand) {
		return request + NetConst.DELIMITER + body + NetConst.DELIMITER +rand;
	}
}
