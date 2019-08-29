package process264Real.block_process;

import Player.YuvViewer;
import java.util.logging.Level;
import java.util.logging.Logger;
import Util.ExecutionTimer;
import Util.Tools;

public class ProcessBasic {

    fountain_multibyte.LTCode code;
    FEC.common.Code rsCode;
    video.H264encoder encoder;
    video.H264decoder decoder;
    FEC.common.TypeFEC fType;
    int video_type;
    
    public ProcessBasic(FEC.common.TypeFEC fType, int video_type) {

        this.fType = fType;
        this.video_type = video_type;

        if (this.fType == FEC.common.TypeFEC.RS223) {
            rsCode = new FEC.rs.RsCoder();
        }
        code = new fountain_multibyte.LTCode();
        encoder = new video.H264encoder();
        decoder = new video.H264decoder();
    }

    public FEC.common.TypeFEC getFecType() {
        return fType;
    }

    public byte[] video_encoder(byte[] yuv_stream, int qp_size) {
        return encoder.encode(yuv_stream, qp_size, video_type);
    }

    public byte[] video_decoder(byte[] h263_stream, int size) {
        byte[] tmp = decoder.decode(h263_stream, size, video_type);     
        return tmp;
    }

    public byte[] fec_encoder(byte[] source_symbols, int num_encoded, int num_source, int symbol_size) {
        if (fType == FEC.common.TypeFEC.RS223) {
            int num_padded = (int) (Math.ceil(source_symbols.length / 223.0) * 223);
            System.out.println("length of source_symbols: " + Util.Tools.pad(source_symbols, num_padded).length);
            return rsCode.encode(Util.Tools.pad(source_symbols, num_padded), num_encoded);
        } else {
            System.out.println("sdlfjalkfjal;sjfl;asjfl;ajsl;fjasl;jdfl;jasl;f                 " + num_source + " " + num_encoded + " " + symbol_size);
            return code.encode(source_symbols, num_source, num_encoded, symbol_size);
        }
    }

    public byte[] fec_decoder(byte[] encoded_symbols, int num_encoded, int num_source, int symbol_size, boolean[] non_error, boolean[] success) {
        if (fType == FEC.common.TypeFEC.RS223) {
            int num_padded = (int) (Math.ceil(num_source / 223.0) * 255);
            return Util.Tools.pad(rsCode.decode(Util.Tools.pad(encoded_symbols, num_padded), num_source, non_error, success), num_source);
        } else {
            return code.decode(encoded_symbols, num_encoded, num_source, symbol_size, non_error, success);
        }
    }

    public static void main(String[] args) {
        ProcessBasic op = new ProcessBasic(FEC.common.TypeFEC.RS223, 1);
        byte[] input = Tools.file_to_byte("c:/lab/data/f.yuv");
        byte[] input_small = java.util.Arrays.copyOf(input, input.length / 10);
        System.out.println("size of input: " + input.length);
        System.out.println("size of input_small: " + input_small.length);
        byte[] encoded = op.video_encoder(input, 80000 * 1000);
        System.out.println(encoded.length);
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        byte[] decoded = op.video_decoder(encoded, encoded.length);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(ProcessBasic.class.getName()).log(Level.SEVERE, null, ex);
        }
        decoded = op.video_decoder(encoded, encoded.length);
        t.end();
        System.out.println(t.duration());
        Tools.byte_to_file(decoded, "c:/lab/data/f_recon.yuv");
    }
}
