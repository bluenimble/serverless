package com.bluenimble.platform.plugins.protocols.tus.storage;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.InvalidUploadOffsetException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.TusException;
import com.bluenimble.platform.plugins.protocols.tus.impl.exception.UploadNotFoundException;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadIdFactory;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadInfo;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadLockingService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadType;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.concatenation.UploadConcatenationService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.concatenation.VirtualConcatenationService;
import com.bluenimble.platform.plugins.protocols.tus.impl.util.Utils;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.Folder.Visitor;
import com.bluenimble.platform.storage.Storage;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;

/**
 * Implementation of {@link UploadStorageService} that implements storage on disk
 */
public class StorageFeatureService implements UploadStorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageFeatureService.class);

    private Storage storage;
    private String 	folder;
    
    private boolean multiTenant = true;
    
    private String dataFile = "data";
    private String metaFile = "meta";
    
    private Long maxUploadSize = null;
    private Long uploadExpirationPeriod = null;
    private UploadIdFactory idFactory;
    private UploadConcatenationService uploadConcatenationService;
    
    private MetaSerializer serializer = MetaSerializer.JSON;

    public StorageFeatureService (UploadIdFactory idFactory, Storage storage, String folder) {
        Validate.notNull(idFactory, "The IdFactory cannot be null");
        this.idFactory = idFactory;
        this.storage = storage;
        this.folder = folder;
        setUploadConcatenationService (new VirtualConcatenationService (this));
    }

    @Override
    public void setIdFactory(UploadIdFactory idFactory) {
        this.idFactory = idFactory;
    }

    @Override
    public void setMaxUploadSize(Long maxUploadSize) {
        this.maxUploadSize = (maxUploadSize != null && maxUploadSize > 0 ? maxUploadSize : 0);
    }

    @Override
    public long getMaxUploadSize() {
        return maxUploadSize == null ? 0 : maxUploadSize;
    }

    @Override
    public UploadInfo getUploadInfo (String uploadUrl, String ownerKey) throws IOException {
        UploadInfo uploadInfo = getUploadInfo (idFactory.readUploadId (uploadUrl), ownerKey);
        if (uploadInfo == null || !Objects.equals (uploadInfo.getOwnerKey (), ownerKey)) {
            return null;
        } else {
            return uploadInfo;
        }
    }

    @Override
    public UploadInfo getUploadInfo (UUID id, String ownerKey) throws IOException {
    	if (MetaSerializer.NATIVE.equals (serializer)) {
            return readSerializable (id, ownerKey, UploadInfo.class);
    	}
    	return read (id, ownerKey);
    }

    @Override
    public String getUploadURI() {
        return idFactory.getUploadURI ();
    }

    @Override
    public UploadInfo create (UploadInfo info, String ownerKey) throws IOException {
        UUID id = createNewId (ownerKey);

        try {
            
        	Folder root = folder ();
        	
        	if (multiTenant && !Lang.isNullOrEmpty (ownerKey)) {
            	Folder tenantFolder = null;
        		try {
        			tenantFolder = (Folder)root.get (ownerKey);
        		} catch (StorageException sex) {
        			// ignore
        		}
        		if (tenantFolder == null) {
        			tenantFolder = root.add (ownerKey, false);
        		}
        		root = tenantFolder;
        	}
        	
        	Folder uploadFolder = root.add (id.toString (), false);
            
            // Create an empty file to store the bytes of this upload
            uploadFolder.add (null, dataFile, false);

            //Set starting values
            info.setId(id);
            info.setOffset(0L);
            info.setOwnerKey(ownerKey);

            update (info, ownerKey);

            return info;
        } catch (UploadNotFoundException e) {
            //Normally this cannot happen
            log.error("Unable to create UploadInfo because of an upload not found exception", e);
            return null;
        } catch (StorageException e) {
        	throw new IOException (e.getMessage (), e);
        }
    }

    @Override
    public void update (UploadInfo uploadInfo, String ownerKey) throws IOException, UploadNotFoundException {
    	if (MetaSerializer.NATIVE.equals (serializer)) {
            writeSerializable (uploadInfo, ownerKey, uploadInfo.getId ());
            return;
    	}
    	write (uploadInfo, ownerKey, uploadInfo.getId ());
    }

    @Override
    public UploadInfo append (UploadInfo info, String ownerKey, InputStream inputStream) throws IOException, TusException {
        if (info != null) {

            long max = getMaxUploadSize() > 0 ? getMaxUploadSize() : Long.MAX_VALUE;
            long transferred = 0;
            Long offset = info.getOffset();
            long newOffset = offset;
            
            try (ReadableByteChannel uploadedBytes = Channels.newChannel (inputStream);
                FileChannel file = dataChannel (info.getId (), ownerKey, WRITE)) {

                try {
                    //Lock will be released when the channel closes
                    file.lock ();

                    //Validate that the given offset is at the end of the file
                    if (!offset.equals(file.size())) {
                        throw new InvalidUploadOffsetException("The upload offset does not correspond to the written"
                                + " bytes. You can only append to the end of an upload");
                    }

                    //write all bytes in the channel up to the configured maximum
                    transferred = file.transferFrom(uploadedBytes, offset, max - offset);
                    file.force (true);
                    newOffset = offset + transferred;

                } catch (Exception ex) {
                    //An error occurred, try to write as much data as possible
                    newOffset = writeAsMuchAsPossible(file);
                    throw ex;
                }

            } finally {
                info.setOffset(newOffset);
                update (info, ownerKey);
            }
        }

        return info;
    }

    @Override
    public void removeLastNumberOfBytes(UploadInfo info, String ownerKey, long byteCount)
            throws UploadNotFoundException, IOException {

        if (info != null && byteCount > 0) {
            try (FileChannel file = dataChannel (info.getId (), ownerKey, WRITE)) {

                //Lock will be released when the channel closes
                file.lock ();

                file.truncate (file.size() - byteCount);
                file.force (true);

                info.setOffset (file.size());
                update (info, ownerKey);
            }
        }
    }

    @Override
    public void terminateUpload (UploadInfo info, String ownerKey) throws UploadNotFoundException, IOException {
        if (info == null) {
            return;
        }
        
        Folder uploadFolder = null;
        try {
            uploadFolder = (Folder)folder ().get (tenantFolder (ownerKey) + info.getId ().toString ());
        } catch (StorageException sex) {
        	throw new UploadNotFoundException (sex.getMessage (), sex);
        }
        if (uploadFolder == null) {
        	throw new UploadNotFoundException ("folder " + info.getId ().toString () + " not found");
        }
        try {
            uploadFolder.delete ();
        } catch (StorageException sex) {
        	throw new IOException (sex.getMessage (), sex);
        }
    }

    @Override
    public Long getUploadExpirationPeriod() {
        return uploadExpirationPeriod;
    }

    @Override
    public void setUploadExpirationPeriod(Long uploadExpirationPeriod) {
        this.uploadExpirationPeriod = uploadExpirationPeriod;
    }

    @Override
    public void setUploadConcatenationService(UploadConcatenationService concatenationService) {
        Validate.notNull(concatenationService);
        this.uploadConcatenationService = concatenationService;
    }

    @Override
    public UploadConcatenationService getUploadConcatenationService() {
        return uploadConcatenationService;
    }

    @Override
    public InputStream getUploadedBytes (String uploadURI, String ownerKey)
            throws IOException, UploadNotFoundException {

        UUID id = idFactory.readUploadId(uploadURI);

        UploadInfo uploadInfo = getUploadInfo (id, ownerKey);
        if (uploadInfo == null || !Objects.equals(uploadInfo.getOwnerKey(), ownerKey)) {
            throw new UploadNotFoundException("The upload with id " + id + " could not be found for owner " + ownerKey);
        } 

        return getUploadedBytes (uploadInfo, ownerKey);
    }

    @Override
    public InputStream getUploadedBytes (UploadInfo uploadInfo, String ownerKey) throws IOException, UploadNotFoundException {
        InputStream inputStream = null;
        //UploadInfo uploadInfo = getUploadInfo (id, ownerKey);
        if (UploadType.CONCATENATED.equals (uploadInfo.getUploadType ()) && uploadConcatenationService != null) {
            inputStream = uploadConcatenationService.getConcatenatedBytes (uploadInfo, ownerKey);
        } else {
        	StorageObject dataFile = dataFile (uploadInfo.getId (), ownerKey);
            //If bytesPath is not null, we know this is a valid Upload URI
            if (dataFile != null) {
                try {
					inputStream = Channels.newInputStream ( (FileChannel)dataFile.channel (READ));
				} catch (StorageException ex) {
					throw new IOException (ex.getMessage (), ex);
				}
            }
        }

        return inputStream;
    }

    @Override
    public void copyUploadTo(UploadInfo info, String ownerKey, OutputStream outputStream)
            throws UploadNotFoundException, IOException {

        List<UploadInfo> uploads = getUploads(info);

        WritableByteChannel outputChannel = Channels.newChannel(outputStream);

        for (UploadInfo upload : uploads) {
            if (upload == null) {
                log.warn("We cannot copy the bytes of an upload that does not exist");

            } else if (upload.isUploadInProgress()) {
                log.warn("We cannot copy the bytes of upload {} because it is still in progress", upload.getId());

            } else {
                try (FileChannel file = dataChannel (upload.getId (), ownerKey, READ)) {
                    //Efficiently copy the bytes to the output stream
                    file.transferTo(0, upload.getLength(), outputChannel);
                }
            }
        }
    }

    @Override
    public void cleanupExpiredUploads (UploadLockingService uploadLockingService) throws IOException {
    	
    	/***
    	 * 
    	 * 
    	 * 
    	 * REVIEWWWWWWWW, OWNER KEY NOT PRESENT
    	 * 
    	 * WE SHOULD LOOP THROUGH TENANT FOLDERS
    	 * 
    	 * 
    	 * SHOULD ALSO HAVE A FILTER
    	 * 
    	 * 
    	 * 
    	 * 
    	 */
    	
    	try {
        	folder ().list (new Visitor () {
    			@Override
    			public void visit (StorageObject so) {
    				if (!so.isFolder ()) {
    					return;
    				}
    				
    				UUID id = UUID.fromString (so.name ());
    				try {
	    				UploadInfo info = getUploadInfo (id, null);
	    		        if (info == null || !info.isExpired () || uploadLockingService.isLocked (id)) {
	    		            return;
	    		        }
						so.delete ();
					} catch (Exception e) {
						throw new RuntimeException (e.getMessage (), e);
					}
    			}
    		}, null);
    	} catch (StorageException sex) {
    		throw new IOException (sex.getMessage (), sex);
    	}

    }

    public void setMultiTenant (boolean multiTenant) {
    	this.multiTenant = multiTenant;
    }

    public void setDataFile (String dataFile) {
    	this.dataFile = dataFile;
    }

    public void setMetaFile (String metaFile) {
    	this.metaFile = metaFile;
    }

    private List<UploadInfo> getUploads(UploadInfo info) throws IOException, UploadNotFoundException {
        List<UploadInfo> uploads;

        if (info != null && UploadType.CONCATENATED.equals(info.getUploadType())
                && uploadConcatenationService != null) {
            uploadConcatenationService.merge(info);
            uploads = uploadConcatenationService.getPartialUploads(info);
        } else {
            uploads = Collections.singletonList(info);
        }
        return uploads;
    }

    private synchronized UUID createNewId (String ownerKey) throws IOException {
        UUID id;
        do {
            id = idFactory.createId ();
            //For extra safety, double check that this ID is not in use yet
        } while (getUploadInfo (id, ownerKey) != null);
        return id;
    }

    private long writeAsMuchAsPossible(FileChannel file) throws IOException {
        long offset = 0;
        if (file != null) {
            file.force(true);
            offset = file.size();
        }
        return offset;
    }
    
    private Folder folder () {
    	try {
			return (Folder)storage.root ().get (folder);
		} catch (StorageException e) {
			return null;
		}
    }
    
    private FileChannel dataChannel (UUID id, String ownerKey, OpenOption... options) throws IOException {
    	StorageObject file = dataFile (id, ownerKey);
    	if (file == null) {
    		throw new IOException ("file " + id + "/" + dataFile + " not found");
    	}
    	try {
        	return (FileChannel)file.channel (options);
    	} catch (StorageException sex) {
    		throw new IOException (sex.getMessage (), sex);
    	}
    }
    
    private StorageObject file (UUID id, String ownerKey, String name) {
    	try {
        	return folder ().get (tenantFolder (ownerKey) + id.toString () + Lang.SLASH + name);
    	} catch (StorageException sex) {
    		return null;
    	}
    }
    
    private StorageObject dataFile (UUID id, String ownerKey) throws IOException {
    	return file (id, ownerKey, dataFile);
    }
    
    private StorageObject infoFile (UUID id, String ownerKey, boolean createIfNotFound) throws IOException {
    	StorageObject infoFile = file (id, ownerKey, metaFile);
    	if (infoFile == null && createIfNotFound) {
    		try {
				infoFile = ((Folder)folder ().get (tenantFolder (ownerKey) + id.toString ())).add (null, metaFile, false);
			} catch (StorageException ex) {
				throw new IOException (ex.getMessage (), ex);
			}
    	}
    	return infoFile;
    }
    
    private UploadInfo read (UUID id, String ownerKey) throws IOException {
    	UploadInfo info = null;
        
    	StorageObject infoFile = infoFile (id, ownerKey, false);
    	if (infoFile == null) {
    		return null;
    	}
    	
        try (FileChannel channel = (FileChannel)infoFile.channel (READ)) {
            //Lock will be released when the channel is closed
            if (Utils.lockFileShared(channel) != null) {

                try (InputStream ois = Channels.newInputStream (channel)) {
                    info = new UploadInfo (Json.load (ois));
                } catch (Exception e) {
                    //This should not happen
                    info = null;
                }
            } else {
                throw new IOException("Unable to lock file " + id + "/" + metaFile);
            }
        } catch (StorageException ex) {
			throw new IOException (ex.getMessage (), ex);
		}
        return info;
    }

    private void write (UploadInfo info, String ownerKey, UUID id) throws IOException {
    	
    	StorageObject infoFile = infoFile (id, ownerKey, true);
    	if (infoFile == null) {
    		return;
    	}
    	
        try (FileChannel channel = (FileChannel)infoFile.channel (WRITE, CREATE, TRUNCATE_EXISTING)) {
            //Lock will be released when the channel is closed
            if (Utils.lockFileExclusively (channel) != null) {

                try (Writer buffer = Channels.newWriter (channel, Encodings.UTF8)) {
                	info.toJson ().write (new AbstractEmitter () {
            			@Override
            			public JsonEmitter write (String chunk) {
            				try {
            					buffer.write (chunk);
            				} catch (IOException e) {
            					throw new RuntimeException (e.getMessage (), e);
            				}
            				return this;
            			}
            		});
                }
            } else {
                throw new IOException ("Unable to lock file " + id + "/" + metaFile);
            }
        } catch (StorageException ex) {
			throw new IOException (ex.getMessage (), ex);
		}
    }

    private <T> T readSerializable (UUID id, String ownerKey, Class<T> clazz) throws IOException {
        T info = null;
        
    	StorageObject infoFile = infoFile (id, ownerKey, false);
    	if (infoFile == null) {
    		return null;
    	}
    	
        try (FileChannel channel = (FileChannel)infoFile.channel (READ)) {
            //Lock will be released when the channel is closed
            if (Utils.lockFileShared(channel) != null) {

                try (ObjectInputStream ois = new ObjectInputStream(Channels.newInputStream(channel))) {
                    info = clazz.cast(ois.readObject());
                } catch (ClassNotFoundException e) {
                    //This should not happen
                    info = null;
                }
            } else {
                throw new IOException("Unable to lock file " + id + "/" + metaFile);
            }
        } catch (StorageException ex) {
			throw new IOException (ex.getMessage (), ex);
		}
        return info;
    }


    private void writeSerializable (Serializable object, String ownerKey, UUID id) throws IOException {
    	
    	StorageObject infoFile = infoFile (id, ownerKey, true);
    	if (infoFile == null) {
    		return;
    	}
    	
        try (FileChannel channel = (FileChannel)infoFile.channel (WRITE, CREATE, TRUNCATE_EXISTING)) {
            //Lock will be released when the channel is closed
            if (Utils.lockFileExclusively (channel) != null) {

                try (OutputStream buffer = new BufferedOutputStream (Channels.newOutputStream (channel));
                     ObjectOutput output = new ObjectOutputStream (buffer)) {

                    output.writeObject (object);
                }
            } else {
                throw new IOException ("Unable to lock file " + id + "/" + metaFile);
            }
        } catch (StorageException ex) {
			throw new IOException (ex.getMessage (), ex);
		}
    }

	@Override
	public StorageObject getData (UploadInfo info, String ownerKey) throws IOException {
		return dataFile (info.getId (), ownerKey);
	}
	
	private String tenantFolder (String ownerKey) {
		if (!multiTenant || Lang.isNullOrEmpty (ownerKey)) {
			return Lang.BLANK;
		}
		return ownerKey + Lang.SLASH;
	}

	@Override
	public boolean isMultiTenant () {
		return multiTenant;
	}

	@Override
	public void setMetaSerializer (MetaSerializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public MetaSerializer getMetaSerializer () {
		return serializer;
	}
    
}
