package FEC.common;

// FEC 인코딩 및 디코딩 인터페이스
//========================================= general channel code interface =====
public interface Code {
	//----- constraint : num_encoded > source_symbols.length
	byte[] encode(byte[] source_symbols, int num_encoded);  // FEC 인코딩을 수행하여 인코딩 심볼들을 리턴   
	//----- constraint : num_source < encoded_symbols.length
	//----- constraint : non_error.length == encoded_symbols.length
	//----- constraint : num_source == sucess.length
	byte[] decode(byte[] encoded_symbols, int num_source, boolean[] non_error, boolean[] sucess);   // FEC 디코딩을 수행하여 디코딩에 성공한 소스 심볼들을 리턴, non_error - 인코딩 심볼들의 손실 여부, success - 소스 심볼들의 디코딩 성공 여부
}
