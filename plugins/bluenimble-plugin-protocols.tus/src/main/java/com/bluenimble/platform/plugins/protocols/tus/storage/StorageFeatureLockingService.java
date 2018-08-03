package com.bluenimble.platform.plugins.protocols.tus.storage;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang3.Validate;

import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadAlreadyLockedException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadIdFactory;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadLock;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadLockingService;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.Folder.Visitor;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;

/**
 * {@link UploadLockingService} implementation that uses the file system for implementing locking
 * <p/>
 * File locking can also apply to shared network drives. This way the framework supports clustering as long as
 * the upload storage directory is mounted as a shared (network) drive.
 * <p/>
 * File locks are also automatically released on application (JVM) shutdown. This means the file locking is not
 * persistent and prevents cleanup and stale lock issues.
 */
public class StorageFeatureLockingService implements UploadLockingService {

    private Storage 		storage;
    private	String			locksFolder; 
    private UploadIdFactory idFactory;

    public StorageFeatureLockingService (UploadIdFactory idFactory, Storage storage, String locksFolder) {
        
    	this.storage = storage;
    	this.locksFolder = locksFolder;
        
        Validate.notNull(idFactory, "The IdFactory cannot be null");
        this.idFactory = idFactory;
    }

    @Override
    public UploadLock lockUploadByUri (String requestURI) throws TusException, IOException {

        UUID id = idFactory.readUploadId (requestURI);
        if (id == null) {
        	return null;
        }

        UploadLock lock = null;

        StorageObject lockFile = lockFile (id);
        //If lockPath is not null, we know this is a valid Upload URI
        if (lockFile != null) {
            lock = new StorageFeatureLock (requestURI, lockFile);
        }
        return lock;
    }

    @Override
    public void cleanupStaleLocks () throws IOException {
    	Folder locks = locksFolder ();
    	if (locks == null) {
    		return;
    	}
    	try {
    		locks.list (new Visitor () {
    			@Override
    			public void visit (StorageObject so) {
    				try {
    					if (so.timestamp ().getTime () < System.currentTimeMillis () - 10000L) {
    	                    UUID id = UUID.fromString (so.name ());
    	                    if (!isLocked (id)) {
    	                        so.delete ();
    	                    }
    	                }
					} catch (StorageException e) {
						throw new RuntimeException (e.getMessage (), e);
					}
    			}
    		}, null);
    	} catch (StorageException sex) {
    		throw new IOException (sex.getMessage (), sex);
    	}
        
        
    }

    @Override
    public boolean isLocked (UUID id) {
        boolean locked = false;
        
        StorageObject lockFile = lockFile (id);

        if (lockFile != null) {
            //Try to obtain a lock to see if the upload is currently locked
            try (UploadLock lock = new StorageFeatureLock (id.toString (), lockFile)) {

                //We got the lock, so it means no one else is locking it.
                locked = false;

            } catch (UploadAlreadyLockedException | IOException e) {
                //There was already a lock
                locked = true;
            }
        }

        return locked;
    }

    @Override
    public void setIdFactory(UploadIdFactory idFactory) {
        this.idFactory = idFactory;
    }
    
    private StorageObject lockFile (UUID id) {
    	Folder locks = locksFolder ();
    	if (locks == null) {
    		return null;
    	}
    	try {
        	return locks.get (id.toString ());
    	} catch (StorageException sex) {
    		return null;
    	}
    }
    
    private Folder locksFolder () {
    	try {
			return (Folder)storage.root ().get (locksFolder);
		} catch (StorageException e) {
			return null;
		}
    }

}
