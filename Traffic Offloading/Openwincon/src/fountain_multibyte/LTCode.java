package fountain_multibyte;

public class LTCode {
    public native byte[] encode(byte[] source_symbols, int num_source, int num_encoded, int symbol_size);

    public native byte[] decode(byte[] encoded_symbols, int num_encoded, int num_source, int symbol_size, boolean[] non_error, boolean[] success);   

    static {
        System.loadLibrary("LTCodeDLL");
    }

    public static void main(String args[]) {
        byte[] fountainEncodedVideo = new byte[11600];
        LTCode code = new LTCode();
        code.encode(fountainEncodedVideo, 100, 120, 64);
    }
}
