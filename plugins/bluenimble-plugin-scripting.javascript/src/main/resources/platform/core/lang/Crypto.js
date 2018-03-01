/**
  Cryptograpphy Utility Class
  @namespace Crypto
*/
var Crypto = {
	/**	
	  Encrypt a string or a bytes array using md5 
	  @param {string|ByteArray} - the data to encrypt
	  @param {string} [charset] - the charset to be used if data is a string
	  
	  @returns {string} the encrypted string
	*/
	md5: function (data, charset) {
		if (charset) {
			return JC_Crypto.md5 (data, charset);
		} else {
			return JC_Crypto.md5 (data);
		}
	}, 
	/**	
	  Produces a hmac of a bytes array using an hmac algorith and a hashing algorithm
	  @param {ByteArray} - the data to hmac
	  @param {string} - the hmac key
	  @param {('SHA1'|'SHA256')} [alg=SHA256] - the hmac algorithm
	  @param {('HEXA'|'BASE64')} [hashing=HEXA] - the hashing algorithm
	  
	  @returns {string} the encrypted string
	*/
	hmac: function (data, key, alg, hashing) {
		if (!alg) {
			alg = 'SHA256';
		}
		alg = alg.toUpperCase ();
	
		if (!hashing) {
			hashing = 'HEXA';
		}
		hashing = hashing.toUpperCase ();
		return JC_Crypto.hmac (data, key, JC_Crypto_Hmac.valueOf (alg), JC_Crypto_Hashing.valueOf (hashing));
	}, 
	/**	
	  Encrypts a bytes array
	  @param {ByteArray} - the data to encrypt
	  @param {string} - the encryption paraphrase
	  @param {('AES'|'DES')} [alg=AES] - the encryption algorithm
	  
	  @returns {byte[]} the encrypted data
	*/
	encrypt: function (data, key, algorithm) {
		if (!algorithm) {
			algorithm = 'AES';
		}
		algorithm = algorithm.toUpperCase ();
		if (algorithm != 'DES' && algorithm != 'AES') {
			throw 'unsupported algorithm ' + algorithm;
		}
		return JC_Crypto.encrypt (data, key, JC_Crypto_Algorithm.valueOf (algorithm));
	}, 
	/**	
	  Decrypts a bytes array
	  @param {ByteArray} - the data to decrypt
	  @param {string} - the encryption paraphrase
	  @param {('AES'|'DES')} [alg=AES] - the encryption algorithm
	  
	  @returns {ByteArray} the decrypted data
	*/
	decrypt: function (data, key, algorithm) {
		if (!algorithm) {
			algorithm = 'AES';
		}
		algorithm = algorthm.toUpperCase ();
		if (algorithm != 'DES' && algorithm != 'AES') {
			throw 'unsupported algorithm ' + algorithm;
		}
		return JC_Crypto.decrypt (data, key, JC_Crypto_Algorithm.valueOf (algorithm));
	}
};