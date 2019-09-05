package video;

import Player.YuvViewer;
import java.util.logging.Level;
import java.util.logging.Logger;
import process264Real.StructurePT;

public class H264decoder {

    byte[] conceal;     
    boolean is_first = true;   
    boolean isRunning = false;  
    
    YuvViewer viewer;

    public H264decoder() {

        this.viewer = null;
    }

    public byte[] decode(byte[] stream, int size, int type) {
        while (isRunning) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        isRunning = true;
        if (is_first) {
            if (type == StructurePT.QCIF) {           
                conceal = new byte[(int) (176 * 144 * 1.5)];
            } else if (type == StructurePT.CIF) {     
                conceal = new byte[(int) (176 * 144 * 1.5 * 4)];
            } else if (type == StructurePT.FCIF) {     
                conceal = new byte[(int) (176 * 144 * 1.5 * 4 * 4)];
            } else {
                Util.Tools.print_err("Video decoder: not supported size!");
                System.exit(-1);
            }
            is_first = false;
        }
        String tmp_file = "c:/testdata/tmp_de.mp4";
        String yuv_file = "c:/testdata/tmp_de.yuv";
        Util.Tools.delete_file(tmp_file);
        Util.Tools.delete_file(yuv_file);
        Util.Tools.byte_to_file(stream, tmp_file);
        String cmd_de = "c:/testdata/ffmpeg.exe -y -i " + tmp_file + " " + yuv_file;
        Util.Tools.exeCommand(cmd_de);      

        if (!Util.Tools.waitFile(yuv_file, 500)) {
            Util.Tools.print_err("Video decoding failure!");

            isRunning = false;

            return Util.Tools.times_array(conceal, StructurePT.frameperGOP);

        } else {        

            byte[] result = Util.Tools.file_to_byte(yuv_file);     
            conceal = java.util.Arrays.copyOfRange(result, result.length - conceal.length, result.length);   
            isRunning = false;

            Util.Tools.byte_to_file_concatenate(result,StructurePT.file);       

            return result;

        }
    }
}
