package vom.bluenimble.platform.tools.binary;

import java.io.Serializable;

import com.bluenimble.platform.api.ApiRequest;

public interface BinaryClient extends Serializable {
	
	void send 		(ApiRequest request);
	
	void recycle	();
	
}
