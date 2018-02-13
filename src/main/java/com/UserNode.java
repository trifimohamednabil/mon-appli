package com.jmcoin.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bouncycastle.util.encoders.Hex;
import com.jmcoin.crypto.SignaturesVerification;
import com.jmcoin.crypto.AES.InvalidAESStreamException;
import com.jmcoin.crypto.AES.InvalidPasswordException;
import com.jmcoin.crypto.AES.StrongEncryptionNotAvailableException;
import com.jmcoin.model.Input;
import com.jmcoin.model.Output;
import com.jmcoin.model.Transaction;
import com.jmcoin.model.Wallet;

public class UserNode extends Peer{

	private Wallet wallet;
	
	public UserNode(String password) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException, InvalidPasswordException, InvalidAESStreamException, StrongEncryptionNotAvailableException {
		this.wallet = new Wallet(password);
	}
	
	public Transaction[] getAvailableTransactionsForAddress(UserJMProtocolImpl protocol,String fromAddress, Map<String,Output> unspentOutputs){
		Transaction[] addressTransactions;
		try {
			addressTransactions = protocol.downloadObject(NetConst.GIVE_ME_TRANS_TO_THIS_ADDRESS, "[\""+fromAddress+"\"]", protocol.getClient());
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		if(addressTransactions == null)return null;
		ArrayList<Transaction> availableTransactions = new ArrayList<Transaction>();
		for(Transaction tr : addressTransactions) {
			if(Objects.equals(tr.getOutputBack().getAddress(), fromAddress)) {
				String keyBack = Hex.toHexString(tr.getHash())+DELIMITER+tr.getOutputBack().getAddress();
				//Si l'output est contenue dans le pool des output disponibles et que l'output n'est pas en attente
				if(unspentOutputs.containsKey(keyBack) && !this.wallet.getPendingOutputs().containsKey(keyBack)){
					availableTransactions.add(tr);
				}
			}
			else if(Objects.equals(tr.getOutputOut().getAddress(), fromAddress)){
				String keyOut = Hex.toHexString(tr.getHash())+DELIMITER+tr.getOutputOut().getAddress();
				if((unspentOutputs.containsKey(keyOut)) && !this.wallet.getPendingOutputs().containsKey(keyOut)){
					availableTransactions.add(tr);
				}
			}
		}
		return availableTransactions.toArray(new Transaction[0]);
	}
	
	public Transaction createTransaction(UserJMProtocolImpl protocol, String fromAddress, String toAddress,
			double amountToSend, PrivateKey privKey, PublicKey pubKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, IOException, FileNotFoundException, SignatureException{
		Map<String, Output> unspentOutputs = protocol.downloadObject(NetConst.GIVE_ME_UNSPENT_OUTPUTS, null, protocol.getClient());
		this.wallet.updatePendingOutputs(unspentOutputs);
		
		Transaction [] addressTransactions = getAvailableTransactionsForAddress(protocol,fromAddress, unspentOutputs);
		if(addressTransactions == null) return null;
		Transaction tr = new Transaction();
        double totalOutputAmount = 0;
        int i = 0;
        Map<String, Output> usedOutputs = new HashMap<>();
        while( i < addressTransactions.length && totalOutputAmount <= amountToSend){
        	String key = Hex.toHexString(addressTransactions[i].getHash());
            if(Objects.equals(addressTransactions[i].getOutputBack().getAddress(), fromAddress)){
            	//verifier si Out pas encore utilisée localement
            	key += DELIMITER+addressTransactions[i].getOutputBack().getAddress();
            	if(!this.wallet.getPendingOutputs().containsKey(key)){
            		totalOutputAmount+= addressTransactions[i].getOutputBack().getAmount();
                    tr.addInput(new Input(addressTransactions[i].getOutputBack().getAmount(),addressTransactions[i].getHash()));
                    usedOutputs.put(key, addressTransactions[i].getOutputBack());
                }
            }
            else if(Objects.equals(addressTransactions[i].getOutputOut().getAddress(), fromAddress)){
            	key += DELIMITER+addressTransactions[i].getOutputOut().getAddress();
            	//verifier si Out pas encore utilisée localement
            	if(!this.wallet.getPendingOutputs().containsKey(key)){
            		totalOutputAmount+= addressTransactions[i].getOutputOut().getAmount();
            		tr.addInput(new Input(addressTransactions[i].getOutputOut().getAmount(),addressTransactions[i].getHash()));
            		usedOutputs.put(key, addressTransactions[i].getOutputOut());
            	}
            }
            else {
        		System.out.println("Usernode : No output belonging to this address");
            }
            i++;
        }
        System.out.println("Amount to send: " + amountToSend);
        System.out.println("Available amount: " + totalOutputAmount);
        if(amountToSend <= totalOutputAmount){
            Output oOut = new Output(amountToSend, toAddress);
            Output oBack = new Output(totalOutputAmount-amountToSend, fromAddress);
            tr.setPubKey(pubKey.getEncoded());
            tr.setOutputBack(oBack);
            tr.setOutputOut(oOut);
            tr.setSignature(SignaturesVerification.signTransaction(tr.getBytes(false), privKey));
            tr.computeHash();
            System.out.println("Back: "+tr.getOutputBack().getAmount());
            System.out.println("Out: "+tr.getOutputOut().getAmount());
          	this.wallet.getPendingOutputs().putAll(usedOutputs);
          	return tr;
        }
        System.out.println("Wallet: Insuficient amount for that address");
		return null;
    }

	public Wallet getWallet() {
		return wallet;
	}
}
