package process264Real.block_process;

public class TargetVideoInfo {

    static public int sizeQCIF = 176 * 144 * 3 / 2;
    static public int sizeCIF = 352 * 288 * 3 / 2;
    static public int sizeFCIF = 704 * 576 * 3 / 2;
    static public int sizeHD = 1280 * 720 * 3 / 2;   
    static public int QCIF = 0;
    static public int CIF = 1;
    static public int FCIF = 2;
    static public int HD = 3;  
    public int frame_num;      
    public int fileSize;       
    public int GOP_num;        
    public int videoType;      
    public int framesPerGOP;   

    public TargetVideoInfo(int filesize, int type, int framesPerGOP) {
        this.fileSize = filesize;
        videoType = type;
        this.framesPerGOP = framesPerGOP;
    }

    public void Cal_FrameNum_GOPNum() {
        if (videoType == QCIF) {
            frame_num = fileSize / sizeQCIF;
            GOP_num = frame_num / framesPerGOP;
        } else if (videoType == CIF) {
            frame_num = fileSize / sizeCIF;
            GOP_num = frame_num / framesPerGOP;
        } else if (videoType == FCIF) {
            frame_num = fileSize / sizeFCIF;
            GOP_num = frame_num / framesPerGOP;
        }
        else if (videoType == HD) {
            frame_num = fileSize / sizeHD;
            GOP_num = frame_num / framesPerGOP;
        }
    }

    public void printVideoInfo() {
        System.out.println("==========Video Information============");
        System.out.println("File Size :" + fileSize);
        if (videoType == QCIF) {
            System.out.println("Type: QCIF" + "  Frames : " + frame_num);
        } else if (videoType == CIF) {
            System.out.println("Type: CIF" + "  Frames : " + frame_num);
        } else if (videoType == FCIF) {
            System.out.println("Type: FCIF" + "  Frames : " + frame_num);
        }
        else if (videoType == HD) {
            System.out.println("Type: HD" + "  Frames : " + frame_num);
        }
        System.out.println("frames Per GOP : " + framesPerGOP + "  GOP_num : " + GOP_num);
        System.out.println("=======================================");
    }
}
