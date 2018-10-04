package com.bluenimble.platform.plugins.protocols.tus.impl.upload;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class UploadInfo implements Serializable {

	private static final long serialVersionUID = -3374523108833107614L;

	private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static List<String> fileNameKeys = Arrays.asList("filename", "name");
    private static List<String> mimeTypeKeys = Arrays.asList("mimetype", "filetype", "type");

    private UploadType uploadType;
    private Long offset;
    private String encodedMetadata;
    private Long length;
    private UUID id;
    private String ownerKey;
    private Long expirationTimestamp;
    private List<String> concatenationParts;
    private String uploadConcatHeaderValue;

    public UploadInfo() {
        offset = 0L;
        encodedMetadata = null;
        length = null;
    }

    public UploadInfo (JsonObject data) {
    	System.out.println ("Create UploadInfo with data: " + data);
        setId (UUID.fromString (Json.getString (data, "id")));
        if (data.containsKey ("length")) {
            setLength (Json.getLong (data, "length", 0));
        }
        setEncodedMetadata (Json.getString (data, "metadata"));
        setOffset (Json.getLong (data, "offset", 0));
        setOwnerKey (Json.getString (data, "owner"));
        setUploadConcatHeaderValue (Json.getString (data, "concatHeader"));
        if (data.containsKey ("type")) {
            setUploadType (UploadType.valueOf (Json.getString (data, "type")));
        }
        
        if (data.containsKey ("expiration")) {
        	expirationTimestamp = Json.getLong (data, "expiration", 0);
        }
        
        JsonArray parts = Json.getArray (data, "parts");
        if (!Json.isNullOrEmpty (parts)) {
        	concatenationParts = new ArrayList<String>();
            for (Object p : parts) {
            	concatenationParts.add (String.valueOf (p));
            }
        }
    }
    
    public JsonObject toJson (boolean parseMetadata) {
    	JsonObject json = new JsonObject ();
    	
    	json.set ("id", id.toString ());
    	json.set ("length", length);
    	json.set ("offset", offset);
    	json.set ("owner", ownerKey);
    	if (!Lang.isNullOrEmpty (encodedMetadata) && parseMetadata) {
        	json.set ("metadata", getMetadata ());
    	} else {
        	json.set ("metadata", encodedMetadata);
    	}
    	json.set ("expiration", expirationTimestamp);
    	json.set ("concatHeader", uploadConcatHeaderValue);
    	if (uploadType != null) {
        	json.set ("type", uploadType.name ());
    	}
    	if (concatenationParts != null && !concatenationParts.isEmpty ()) {
            JsonArray parts = new JsonArray ();
    		for (String p : concatenationParts) {
    			parts.add (p);
            }
    		json.set ("parts", parts);
        }
    	
    	return json;
    }

   public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public String getEncodedMetadata() {
        return encodedMetadata;
    }

    public void setEncodedMetadata(String encodedMetadata) {
        this.encodedMetadata = encodedMetadata;
    }

    public JsonObject getMetadata () {
        JsonObject metadata = new JsonObject ();
        for (String valuePair : splitToArray(encodedMetadata, ",")) {
            String[] keyValue = splitToArray(valuePair, "\\s");
            String key = null;
            String value = null;
            if (keyValue.length > 0) {
                key = StringUtils.trimToEmpty(keyValue[0]);

                //Skip any blank values
                int i = 1;
                while (keyValue.length > i && StringUtils.isBlank(keyValue[i])) {
                    i++;
                }

                if (keyValue.length > i) {
                    value = decode(keyValue[i]);
                }

                metadata.set (key, value);
            }
        }
        return metadata;
    }

    public boolean hasMetadata() {
        return StringUtils.isNotBlank(encodedMetadata);
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = (length != null && length > 0 ? length : null);
    }

    public boolean hasLength() {
        return length != null;
    }

    /**
     * An upload is still in progress:
     * - as long as we did not receive information on the total length
     * - the total length does not match the current offset
     * @return true if the upload is still in progress, false otherwise
     */
    public boolean isUploadInProgress () {
        return length == null || !offset.equals(length);
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public Long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public void updateExpiration(long expirationPeriod) {
        expirationTimestamp = getCurrentTime() + expirationPeriod;
    }

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public void setConcatenationParts(List<String> concatenationParts) {
        this.concatenationParts = concatenationParts;
    }

    public List<String> getConcatenationParts() {
        return concatenationParts;
    }

    public void setUploadConcatHeaderValue(String uploadConcatHeaderValue) {
        this.uploadConcatHeaderValue = uploadConcatHeaderValue;
    }

    public String getUploadConcatHeaderValue() {
        return uploadConcatHeaderValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UploadInfo info = (UploadInfo) o;

        return new EqualsBuilder()
                .append(getOffset(), info.getOffset())
                .append(getEncodedMetadata(), info.getEncodedMetadata())
                .append(getLength(), info.getLength())
                .append(getId(), info.getId())
                .append(getOwnerKey(), info.getOwnerKey())
                .append(getExpirationTimestamp(), info.getExpirationTimestamp())
                .append(getUploadType(), info.getUploadType())
                .append(getUploadConcatHeaderValue(), info.getUploadConcatHeaderValue())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getOffset())
                .append(getEncodedMetadata())
                .append(getLength())
                .append(getId())
                .append(getOwnerKey())
                .append(getExpirationTimestamp())
                .append(getUploadType())
                .append(getUploadConcatHeaderValue())
                .toHashCode();
    }

    /**
     * Try to guess the filename of the uploaded data. If we cannot guess the name
     * we fall back to the ID.
     * <p/>
     * NOTE: This is only a guess, there are no guarantees that the return value is correct
     *
     * @return A potential file name
     */
    public String getFileName() {
        JsonObject metadata = getMetadata();
        for (String fileNameKey : fileNameKeys) {
            if (metadata.containsKey (fileNameKey)) {
                return metadata.getString (fileNameKey);
            }
        }

        return getId().toString();
    }

    /**
     * Try to guess the mime-type of the uploaded data.
     * <p/>
     * NOTE: This is only a guess, there are no guarantees that the return value is correct
     *
     * @return A potential file name
     */
    public String getFileMimeType() {
    	JsonObject metadata = getMetadata();
        for (String fileNameKey : mimeTypeKeys) {
            if (metadata.containsKey (fileNameKey)) {
                return metadata.getString (fileNameKey);
            }
        }

        return APPLICATION_OCTET_STREAM;
    }

    /**
     * Check if this upload is expired
     * @return True if the upload is expired, false otherwise
     */
    public boolean isExpired() {
        return expirationTimestamp != null && expirationTimestamp < getCurrentTime();
    }

    /**
     * Get the current time in the number of milliseconds since January 1, 1970, 00:00:00 GMT
     */
    protected long getCurrentTime() {
        return new Date().getTime();
    }

    private String[] splitToArray(String value, String separatorRegex) {
        if (StringUtils.isBlank(value)) {
            return new String[0];
        } else {
            return StringUtils.trimToEmpty(value).split(separatorRegex);
        }
    }

    private String decode(String encodedValue) {
        if (encodedValue == null) {
            return null;
        } else {
            return new String(DatatypeConverter.parseBase64Binary(encodedValue), Charset.forName("UTF-8"));
        }
    }
}
