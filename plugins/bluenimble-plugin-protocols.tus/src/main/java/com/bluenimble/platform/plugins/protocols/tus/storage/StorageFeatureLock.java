package com.bluenimble.platform.plugins.protocols.tus.storage;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadAlreadyLockedException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadLock;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.disk.FileBasedLock;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;
import com.bluenimble.platform.storage.StorageObject;

/**
 * Upload locking implementation using the file system file locking mechanism.
 * File locking can also apply to shared network drives. This way the framework supports clustering as long as
 * the upload storage directory is mounted as a shared (network) drive.
 * <p/>
 * File locks are also automatically released on application (JVM) shutdown. This means the file locking is not
 * persistent and prevents cleanup and stale lock issues.
 */
public class StorageFeatureLock implements UploadLock {

    private static final Logger log = LoggerFactory.getLogger(FileBasedLock.class);

    protected FileChannel 	fileChannel;
    protected String 		uploadUri;
    protected StorageObject lockFile;

    public StorageFeatureLock (String uploadUri, StorageObject lockFile) throws UploadAlreadyLockedException, IOException {
        Validate.notBlank(uploadUri, "The upload URI cannot be blank");
        Validate.notNull (lockFile, "The path to the lock cannot be null");
        this.uploadUri = uploadUri;
        this.lockFile = lockFile;

        tryToObtainFileLock();
    }

    private void tryToObtainFileLock() throws UploadAlreadyLockedException, IOException {
        String message = "The upload " + getUploadUri() + " is already locked";

        try {
            //Try to acquire a lock
            fileChannel = createFileChannel();
            FileLock fileLock = Utils.lockFileExclusively(fileChannel);

            //If the upload is already locked, our lock will be null
            if (fileLock == null) {
                fileChannel.close();
                throw new UploadAlreadyLockedException(message);
            }

        } catch (OverlappingFileLockException e) {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e1) {
                    //Should not happen
                }
            }
            throw new UploadAlreadyLockedException(message);
        } catch (Exception e) {
            throw new IOException ("Unable to create or open file required to implement file-based locking", e);
        }
    }

    @Override
    public String getUploadUri() {
        return uploadUri;
    }

    @Override
    public void release () {
        try {
            //Closing the channel will also release the lock
            fileChannel.close ();
            if (lockFile != null && lockFile.exists ()) {
            	lockFile.delete ();
            }
        } catch (Exception e) {
            log.warn ("Unable to release file lock for URI " + getUploadUri(), e);
        }
    }

    @Override
    public void close() throws IOException {
        release ();
    }

    protected FileChannel createFileChannel () throws Exception {
        return (FileChannel)lockFile.channel (CREATE, WRITE);
    }

}
