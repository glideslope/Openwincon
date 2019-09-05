package video;

import Util.Tools;
import process264Real.StructurePT;

public class H264encoder {
    public byte[] encode(byte[] stream, int target_rate, int type) {
        int local_target_rate = (int) (target_rate / 1000.0);

        System.out.println("local_target_rate  :  " + local_target_rate);

        String frame_size = null;
        if (type == StructurePT.QCIF) {
            frame_size = "176x144";
        } else if (type == StructurePT.CIF) {
            frame_size = "352x288";
        } else if (type == StructurePT.FCIF) {
            frame_size = "704x576";
        } else {
        }

        String yuv_file = "c:/testdata/tmp_en.yuv";
        String tmp_file = "c:/testdata/tmp_en.mp4";
        Util.Tools.delete_file(tmp_file);
        Util.Tools.delete_file(yuv_file); 
        Util.Tools.byte_to_file(stream, yuv_file);

        String cmd_en = "c:/testdata/x264.exe --no-psnr --no-ssim --threads 4 --ref 1 --fps " + StructurePT.frameperGOP + " --bitrate " + local_target_rate
                + " --output " + tmp_file + " " + yuv_file + " " + frame_size;
        System.out.println(cmd_en);
        Util.Tools.exeCommand(cmd_en);
        System.out.println("local targer rate : " + local_target_rate);
        if (!Util.Tools.waitFile(tmp_file, 500)) {
            Util.Tools.print_err("Video encoding failure!");
            System.exit(-1);
        }
        return Util.Tools.file_to_byte(tmp_file);
    }
}
