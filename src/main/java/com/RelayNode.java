package com.jmcoin.network;

import java.io.IOException;

import com.jmcoin.model.Chain;

/**
 * Class RelayNode
 * Represents a peer allowing communication over the network
 * @author enzo
 */

public class RelayNode extends Peer{

	private Chain localChainCopy;
	private boolean chainUpToDate;

	public RelayNode() throws IOException {
		super();
	}
	
	
	public boolean isChainUpToDate() {
		return chainUpToDate;
	}
	
	public Chain getLocalChainCopy() {
		return this.localChainCopy;
	}
	
	public void setChainUpToDate(boolean chainUpToDate) {
		this.chainUpToDate = chainUpToDate;
	}
	
	public void updateBlockChain(String bc) {
		this.chainUpToDate = true;
		this.localChainCopy = this.gson.fromJson(bc, Chain.class);
	}
}
