package com.jmcoin.network;

/**
 * The abtract class NetConst
 * Defines some useful constants regarding to the J-M protocol
 * @author enzo
 */
public abstract class NetConst {
	
	public static final int RELAY_NODE_LISTEN_PORT 				= 33333;
	public static final int MASTER_NODE_LISTEN_PORT				= 33334;
	public static final int MINER_NODE_LISTEN_PORT				= 33335;
	public static final int RELAY_BROADCAST_PORT 				= 4445;
	public static final int MINER_BROADCAST_PORT 				= 4444;
	public static final String MASTER_HOST_NAME					= "localhost";//"master.jmcoin.technology";
	public static final String RELAY_DEBUG_HOST_NAME			= "localhost";//"relay-01.jmcoin.technology";
	public static final String DEFAULT_ID						= "-1";
	public static final String GIVE_ME_BLOCKCHAIN_COPY			= "0"; //from wallets
	public static final String GIVE_ME_UNVERIFIED_TRANSACTIONS	= "1"; //from miners
	public static final String GIVE_ME_REWARD_AMOUNT			= "2"; //from miners
	public static final String TAKE_MY_MINED_BLOCK				= "3"; //from miners to master
	public static final String TAKE_MY_NEW_TRANSACTION			= "4"; //from wallets to master (goes to pool of unverif. transaction
	public static final String GIVE_ME_DIFFICULTY				= "5"; //from miners
	public static final String STOP_MINING						= "6"; //from master to miners (broadcast)
	public static final String GIVE_ME_UNSPENT_OUTPUTS 			= "7";
	public static final String GIVE_ME_LAST_BLOCK				= "8";
	public static final String GIVE_ME_TRANS_TO_THIS_ADDRESS	= "9";
	public static final String DELIMITER						= "$";
//	public static final String END								= "#";
	public static final String ERR_NOT_A_REQUEST				= "err_not_req";
	public static final String RES_OKAY							= "res_ok";
	public static final String RES_NOK							= "res_nok";
	public static final String ERR_BAD_REQUEST					= "bad_req";
	public static final String CONNECTION_REQUEST 				= "ConnectionRequest";
	public static final String CONNECTED 						= "Connected";
	public static final int MAX_SENT_TRANSACTIONS 				= 1000;
	public static final int DEFAULT_DIFFICULTY 					= 24;
	
	public static final String RECEIVE_DIFFICULTY 				= "A";
	public static final String RECEIVE_REWARD_AMOUNT 			= "B";
	public static final String RECEIVE_UNVERIFIED_TRANS 		= "C";
	public static final String RECEIVE_BLOCKCHAIN_COPY 			= "D";
	public static final String RECEIVE_UNSPENT_OUTPUTS			= "E";
	public static final String RECEIVE_LAST_BLOCK				= "F";
	public static final String RECEIVE_TRANS_TO_THIS_ADDRESS	= "G";
	
	public static final String DEFAULT_TRAILER					= NetConst.DELIMITER + NetConst.DEFAULT_ID + NetConst.DELIMITER;
	public static final String STOP_MINING_REQ 					= NetConst.STOP_MINING+NetConst.DELIMITER+"null"+NetConst.DEFAULT_TRAILER;
	public static final String GENESIS 							= "genesis";
}
