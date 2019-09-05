package FEC.rs;

public class RsCoder implements FEC.common.Code {

    int t = 255;    
    int k = 223;    
    int r = t - k;  
    RsEncode enc;   
    RsDecode dec;   

    public RsCoder() {
        enc = new RsEncode(r);
        dec = new RsDecode(r);
    }

    public byte[] encode(byte[] source_symbols, int num_encoded) {
        int source_length = source_symbols.length;
        int stack_length = (int) Math.ceil(source_length / (double) k);
        int[] source_int = Util.Tools.byte_to_int(source_symbols);
        int[] encoded_int = new int[stack_length * t];
        int[] tmp_int = new int[k];
        for (int i = 0; i < stack_length; i++) {
            int s_base = k * i;
            int e_base = t * i;
            for (int j = 0; j < k; j++) {
                encoded_int[e_base + j] = source_int[s_base + j];
                tmp_int[j] = source_int[s_base + j];
            }
            enc.encode(tmp_int, k, encoded_int, e_base + k);
        }
        return Util.Tools.interleave(Util.Tools.int_to_byte(encoded_int), t);
    }

    public byte[] decode(byte[] encoded_symbols, int num_source, boolean[] non_error, boolean[] sucess) {
        int encoded_length = encoded_symbols.length;
        int stack_length = (int) Math.ceil(encoded_length / (double) t);
        int[] encoded_int = Util.Tools.byte_to_int(Util.Tools.deterleave(encoded_symbols, t));
        int[] source_int = new int[stack_length * k];
        int[] tmp_int = new int[t];
        for (int i = 0; i < stack_length; i++) {
            int s_base = k * i;
            int e_base = t * i;
            for (int j = 0; j < t; j++) {
                tmp_int[j] = encoded_int[e_base + j];
            }
            dec.decode(tmp_int);
            for (int j = 0; j < k; j++) {
                source_int[s_base + j] = tmp_int[j];
            }
        }
        return Util.Tools.int_to_byte(source_int);
    }
    public static void main(String[] args) {
        FEC.common.Code code = new RsCoder();
        byte[] source = new byte[223 * 356];
        java.util.Random rand = new java.util.Random();
        rand.nextBytes(source);
        Util.ExecutionTimer t = new Util.ExecutionTimer();
        t.start();
        byte[] encoded = code.encode(source, 0);
        for (int i = 0; i < encoded.length * 0.02; i++) {
            encoded[rand.nextInt(encoded.length)] = 0;
        }
        byte[] reconstructed = code.decode(encoded, 0, new boolean[1], new boolean[1]);

        t.end();
        Util.Tools.compare(source, reconstructed, true);
        System.out.println("duration : " + t.duration());
        System.out.println(java.util.Arrays.toString(source));
        System.out.println(java.util.Arrays.toString(encoded));
        System.out.println(java.util.Arrays.toString(reconstructed));

    }
}
