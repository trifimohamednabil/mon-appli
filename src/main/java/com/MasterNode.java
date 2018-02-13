package com.jmcoin.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.jmcoin.crypto.AES;
import org.bouncycastle.util.encoders.Hex;

import com.jmcoin.crypto.AES.InvalidKeyLengthException;
import com.jmcoin.crypto.AES.StrongEncryptionNotAvailableException;
import com.jmcoin.crypto.SignaturesVerification;
import com.jmcoin.database.DatabaseFacade;
import com.jmcoin.model.Block;
import com.jmcoin.model.Chain;
import com.jmcoin.model.Input;
import com.jmcoin.model.KeyGenerator;
import com.jmcoin.model.Output;
import com.jmcoin.model.Transaction;

public class MasterNode extends Peer {

    private static MasterNode instance = new MasterNode();
	public static final int REWARD_START_VALUE = 10;
	public static final int REWARD_RATE = 100;
    private LinkedList<Transaction> unverifiedTransactions;
    
    private Map<String, Output> unspentOutputs;
    private Chain chain;
    private Block lastBlock;
       
    private int difficulty = NetConst.DEFAULT_DIFFICULTY;

    private MasterNode(){
    	super();
    	this.unverifiedTransactions = new LinkedList<>();
    	this.unspentOutputs = new HashMap<>();
    	this.chain = DatabaseFacade.getStoredChain();
    	if(chain == null){
    		chain = new Chain();
    		DatabaseFacade.storeBlockChain(chain);
		}
    	this.lastBlock = DatabaseFacade.getLastBlock();
		if(chain.getSize() == 0) {
			try {
				addGenesisToUnverified();
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException
					| IOException | InvalidKeyLengthException | StrongEncryptionNotAvailableException e) {
				e.printStackTrace();
			}
		}
		else{
			this.unspentOutputs = initiateUnspentOutputs();
		}
    }
    
    private Map<String,Output> initiateUnspentOutputs (){
		Map<String,Output> unspentOutputs = new HashMap<String,Output>();
		Map<String,Input> inputs = new HashMap<String,Input>();
		Block currentBlock = DatabaseFacade.getBlockWithHash(lastBlock.getFinalHash());
		String previousBlockHash = currentBlock.getPrevHash();
		while(previousBlockHash != null){
			for(Transaction tr : currentBlock.getTransactions()) {			
				for(Input input : tr.getInputs()){
					String adr = SignaturesVerification.DeriveJMAddressFromPubKey(tr.getPubKey());
					if(input.getPrevTransactionHash() != null)
						inputs.put(Hex.toHexString(input.getPrevTransactionHash())+DELIMITER+adr,input);
				}
				boolean foundBack = false;
				boolean foundOut = false;
				Iterator<Entry<String, Input>> iter = inputs.entrySet().iterator();
				while (iter.hasNext()){
					Map.Entry<String, Input> pair = iter.next();
					String[] splitArray = pair.getKey().split("[%]");
					if(splitArray.length >= 2 && Arrays.equals(pair.getValue().getPrevTransactionHash(), tr.getHash()) && Objects.equals(tr.getOutputBack().getAddress(), splitArray[1])){
						foundBack = true;
						iter.remove();
					}
				}
				iter = inputs.entrySet().iterator();
				while(iter.hasNext()){
					Map.Entry<String, Input> pair = iter.next();
					String[] splitArray = pair.getKey().split("[%]");
					if(splitArray.length >= 2 && Arrays.equals(pair.getValue().getPrevTransactionHash(), tr.getHash()) && Objects.equals(tr.getOutputOut().getAddress(), splitArray[1])){
						foundOut = true;
						iter.remove();
					}
				}
				if(!foundBack){
					unspentOutputs.put(Hex.toHexString(tr.getHash())+DELIMITER+tr.getOutputBack().getAddress(), tr.getOutputBack());
				}
				if(!foundOut){
					unspentOutputs.put(Hex.toHexString(tr.getHash())+DELIMITER+tr.getOutputOut().getAddress(), tr.getOutputOut());
				}					
			}
			currentBlock = DatabaseFacade.getBlockWithHash(currentBlock.getPrevHash());
			previousBlockHash = currentBlock.getPrevHash();
		}
		return unspentOutputs;
	}
    
    public Block getLastBlock() {
		return lastBlock;
	}
    
    public Map<String, Output> getUnspentOutputs() {
		return unspentOutputs;
	}
	
	protected LinkedList<Transaction> getUnverifiedTransactions() {
		return unverifiedTransactions;
	}

    public static MasterNode getInstance(){
        return instance;
    }
    
    public int getDifficulty() {
		return difficulty;
	}
    
    public Chain getChain() {
		return chain;
	}
    
    public int getRewardAmount() {
    	return REWARD_START_VALUE / ((chain.getSize() / REWARD_RATE) + 1);
    }
    
    /**
     * @param pBlock
     * @throws IOException 
     * @throws SignatureException 
     * @throws NoSuchProviderException 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     */
    public boolean processMinedBlock(Block pBlock) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, IOException {
    	if(!canBeAdded(pBlock))return false;
		//variable temporaires utilisées pour mettre à jour les pools de manière atomique
		Map<String,Output> tempToRemoveOutputs = new HashMap<String,Output>();
		Map<String,Output> tempToAddOutputs = new HashMap<String,Output>();
		for(final Transaction trans : pBlock.getTransactions()){
			if(verifyBlockTransaction(trans, chain, this.unspentOutputs)){
				String address = SignaturesVerification.DeriveJMAddressFromPubKey(trans.getPubKey());
				//reparcourir les inputs déja validés pour traitement
				for(Input input : trans.getInputs()){
					Transaction prevTrans = chain.findInBlockChain(input.getPrevTransactionHash());
					if(prevTrans != null) {
						if(Objects.equals(prevTrans.getOutputOut().getAddress(), address)){
							tempToRemoveOutputs.put(Hex.toHexString(prevTrans.getHash())+DELIMITER+prevTrans.getOutputOut().getAddress(),prevTrans.getOutputOut());
						}
						else if (Objects.equals(prevTrans.getOutputBack().getAddress(), address)){
							tempToRemoveOutputs.put(Hex.toHexString(prevTrans.getHash())+DELIMITER+prevTrans.getOutputBack().getAddress(),prevTrans.getOutputBack());
						}
						else {
							return false; //sinon probleme mais normalement impossible
						}
					}
				}
				//adding new outputs to the pool
				System.out.println("Trans: "+this.gson.toJson(trans));
				System.out.println("Hash "+Hex.toHexString(trans.getHash()));
				tempToAddOutputs.put(Hex.toHexString(trans.getHash())+DELIMITER+trans.getOutputOut().getAddress(),trans.getOutputOut());//delimiter pour avoir une clé unique : concat du hash / adresse
				if(trans.getOutputBack().getAddress() != null) {
					tempToAddOutputs.put(Hex.toHexString(trans.getHash())+DELIMITER+trans.getOutputBack().getAddress(),trans.getOutputBack());
				}
			}
			else {
				return false;
			}
		}
		for(final Transaction trans : pBlock.getTransactions()){
			if(!this.unverifiedTransactions.removeIf(trans::equals) && trans.getOutputBack().getAddress() != null) {
				return false;
			}
			//transaction has to be in unverified transaction pool before being added to the chain,
			//except when it's a reward!!
		}
		System.out.println("--------------------------------------------");
		System.out.println(this.gson.toJson(this.unspentOutputs));
		for (String key : tempToRemoveOutputs.keySet()){
		    unspentOutputs.remove(key);
		}		
		for (Map.Entry<String,Output> entry : tempToAddOutputs.entrySet()){
		    unspentOutputs.put(entry.getKey(),entry.getValue());
		}
		System.out.println(this.gson.toJson(this.unspentOutputs));
		System.out.println("--------------------------------------------");
		this.chain.getBlocks().put(pBlock.getFinalHash() + pBlock.getTimeCreation(), pBlock);
		this.lastBlock = pBlock;
		DatabaseFacade.updateChain(this.chain);
		return true;
    }
    
     public boolean canBeAdded(Block pBlock){
    	if(pBlock == null)return false;
		if(chain.getSize() == 0 && pBlock.getPrevHash() == null) return true;
    	if(!isFinalHashRight(pBlock))return false;
    	if (DatabaseFacade.getBlockWithHash(pBlock.getPrevHash()) == null) return false;
//    	if (pBlock.getSize() > Block.MAX_BLOCK_SIZE) return false;
    	return true;
    }
     
    private boolean isFinalHashRight(Block pBlock) {
	    BigInteger value = new BigInteger(pBlock.getFinalHash(), 16);
	    return value.shiftRight(32*8 - pBlock.getDifficulty()).intValue() == 0;
    }

	public List<Transaction> getTransactionsToThisAddress(String addresses) {
		String[] tabAddresses = gson.fromJson(addresses, String[].class);
		//return debugGetTransactionsToThisAddress(tabAddresses);
		return DatabaseFacade.getAllTransactionsWithAddress(tabAddresses);
	}

	/*private ArrayList<Transaction> debugGetTransactionsToThisAddress(String[] addresses){
		ArrayList<Transaction> transactions = new ArrayList<>();
		for(String addr : addresses) {
			for(Transaction trans : this.unverifiedTransactions) {
				if(addr.equals(trans.getOutputBack().getAddress())||addr.equals(trans.getOutputOut().getAddress()))
					transactions.add(trans);
			}
		}
		return transactions;
	}*/

	private void addGenesisToUnverified() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, StrongEncryptionNotAvailableException, InvalidKeyLengthException, SignatureException, InvalidKeyException {
		Key[] keys = generateGenesisKeys(NetConst.GENESIS);
		PrivateKey privKey = (PrivateKey) keys[0];
		PublicKey pubKey = (PublicKey) keys[1];
		Input inGenesis = new Input();
		inGenesis.setPrevTransactionHash(null);
		Output outGenesis = new Output();
		outGenesis.setAmount(42);
		outGenesis.setAddress(SignaturesVerification.DeriveJMAddressFromPubKey(pubKey.getEncoded()));
		Transaction transGenesis = new Transaction();
		Output outputBack = new Output();
		outputBack.setAddress(null);
		outputBack.setAmount(0);
		transGenesis.setOutputBack(outputBack);
		transGenesis.setOutputOut(outGenesis);
		transGenesis.addInput(inGenesis);
		transGenesis.setPubKey(pubKey.getEncoded());
		transGenesis.setSignature(SignaturesVerification.signTransaction(transGenesis.getBytes(false), privKey));
		transGenesis.computeHash();
		unverifiedTransactions.add(transGenesis);
		this.lastBlock = new Block();
		this.lastBlock.setFinalHash(NetConst.GENESIS);
	}

	private Key[] generateGenesisKeys(String pass) throws NoSuchProviderException, NoSuchAlgorithmException, StrongEncryptionNotAvailableException, InvalidKeyLengthException, IOException {
		KeyGenerator keyGen = new KeyGenerator(1024);
		keyGen.createKeys();
		PrivateKey privateKey = keyGen.getPrivateKey();
		PublicKey publicKey = keyGen.getPublicKey();
		char[] AESpw = pass.toCharArray();
		ByteArrayInputStream inputPrivateKey = new ByteArrayInputStream(privateKey.getEncoded());
		ByteArrayOutputStream encryptedPrivateKey = new ByteArrayOutputStream();
		AES.encrypt(128, AESpw, inputPrivateKey , encryptedPrivateKey);
		return new Key[] {privateKey, publicKey};
	}
}
