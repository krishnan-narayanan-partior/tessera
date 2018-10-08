package com.quorum.tessera.key;

import com.quorum.tessera.config.keypairs.ConfigKeyPair;
import com.quorum.tessera.key.exception.KeyNotFoundException;
import com.quorum.tessera.nacl.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class KeyManagerImpl implements KeyManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyManagerImpl.class);

    /**
     * A list of all pub/priv keys that are attached to this node
     */
    private final Set<KeyPair> localKeys;
    
    private final KeyPair defaultKeys;
    
    private final Set<PublicKey> forwardingPublicKeys;
    
    public KeyManagerImpl(final Collection<ConfigKeyPair> keys, Collection<Key> forwardKeys) {
        this.localKeys = keys
                .stream()
                .map(kd
                        -> new KeyPair(
                        PublicKey.from(Base64.getDecoder().decode(kd.getPublicKey())),
                        PrivateKey.from(Base64.getDecoder().decode(kd.getPrivateKey()))
                )
                ).collect(Collectors.toSet());
        
        this.defaultKeys = localKeys.iterator().next();
        
        this.forwardingPublicKeys = forwardKeys.stream()
                .map(Key::getKeyBytes)
                .map(PublicKey::from)
                .collect(Collectors.toSet());
    }
    
    @Override
    public PublicKey getPublicKeyForPrivateKey(final PrivateKey privateKey) {
        LOGGER.debug("Attempting to find public key for the private key {}", privateKey);
        
        final PublicKey publicKey = localKeys
                .stream()
                .filter(keypair -> Objects.equals(keypair.getPrivateKey(), privateKey))
                .findFirst()
                .map(KeyPair::getPublicKey)
                .orElseThrow(
                        () -> new KeyNotFoundException("Private key " + 
                                KeyUtil.encodeToBase64(privateKey) 
                                + " not found when searching for public key")
                );
        
        LOGGER.debug("Found public key {} for private key {}", publicKey, privateKey);
        
        return publicKey;
    }
    
    
    @Override
    public PrivateKey getPrivateKeyForPublicKey(final PublicKey publicKey) {
        LOGGER.debug("Attempting to find private key for the public key {}", publicKey);

        final PrivateKey privateKey = localKeys
                .stream()
                .filter(keypair -> Objects.equals(keypair.getPublicKey(), publicKey))
                .findFirst()
                .map(KeyPair::getPrivateKey)
                .orElseThrow(
                        () -> new KeyNotFoundException("Public key " + KeyUtil.encodeToBase64(publicKey) 
                                + " not found when searching for private key")
                );
        
        LOGGER.debug("Found private key {} for public key {}", privateKey, publicKey);
        
        return privateKey;
    }
    
    @Override
    public Set<PublicKey> getPublicKeys() {
        return localKeys
                .stream()
                .map(KeyPair::getPublicKey)
                .collect(Collectors.toSet());
    }
    
    @Override
    public PublicKey defaultPublicKey() {
        return PublicKey.from(defaultKeys.getPublicKey().getKeyBytes());
    }
    
    @Override
    public Set<PublicKey> getForwardingKeys() {
        return this.forwardingPublicKeys;
    }
    
}
