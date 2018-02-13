package com.jmcoin.network;

import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Map;
import java.util.Objects;

import org.bouncycastle.util.encoders.Hex;

import com.jmcoin.crypto.SignaturesVerification;
import com.jmcoin.model.Bundle;
import com.jmcoin.model.Chain;
import com.jmcoin.model.Input;
import com.jmcoin.model.KeyGenerator;
import com.jmcoin.model.Output;
import com.jmcoin.model.Transaction;

public abstract class Peer {
	
	protected static final String DELIMITER = "%";
		
	protected Bundle<? extends Object> bundle;
	protected Gson gson;
	
	public Peer() {
		this.bundle = new Bundle<>();
		this.gson = new Gson();
	}
	
	public Gson getGson() {
		return gson;
	}
	
	public Bundle<? extends Object> getBundle() {
		return bundle;
	}
	
	protected <T> Bundle<T> createBundle(Type type) {
		Bundle<T> bundle = new Bundle<>();
		setBundle(bundle);
		return bundle;
	}
	
	protected void setBundle(Bundle<? extends Object> bundle) {
		this.bundle = bundle;
	}

	protected boolean verifyBlockTransaction(Transaction trans, Chain chain, Map<String, Output> unspentOutputs) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, IOException {
		if(SignaturesVerification.verifyTransaction(trans.getSignature(), trans.getBytes(false), KeyGenerator.getPublicKey(trans.getPubKey()))) {
			String address = SignaturesVerification.DeriveJMAddressFromPubKey(trans.getPubKey());
			for(Input input : trans.getInputs()) {
				Transaction prevTrans = chain.findInBlockChain(input.getPrevTransactionHash());
				if(prevTrans != null) { 
					// ce n'est pas un reward
					Output outToMe = null; //in previous transaction, find the output took as new input (outToMe)
					if(Objects.equals(prevTrans.getOutputBack().getAddress(), address))
						outToMe = prevTrans.getOutputBack();
					else if (Objects.equals(prevTrans.getOutputOut().getAddress(), address))
						outToMe = prevTrans.getOutputOut();
					else
						return false; //not normal
					boolean unspent = false;
					String outputKey =  Hex.toHexString(prevTrans.getHash())+DELIMITER+outToMe.getAddress();
					for (Map.Entry<String,Output> entry : unspentOutputs.entrySet()){
						if(outputKey.equals(entry.getKey())) {//Si ce n'est pas trouvé dans la liste
							unspent = true;
							break;
						}
					}
					if(!unspent) {
						return false; // Output déja dépensée
					}
					System.out.println(outToMe.getAmount() + " " + input.getAmount());
					if(outToMe.getAmount() == input.getAmount()){	
						unspentOutputs.remove(outputKey);
					}
					else {
						return false; // Not normal, les montants doivent correspondre car on consomme tout l'ouput
					}	
				}
				else {
					if(input.getPrevTransactionHash() != null) {
						return false;//means that the prevHash exists but the transaction doesn't
					}
				}
			}
			return true;
		}
		else{
			return false; // signatures non confirmée : vérifier si on ne retire pas les output à la toute fin quand tout est bon
		}
	}
}
