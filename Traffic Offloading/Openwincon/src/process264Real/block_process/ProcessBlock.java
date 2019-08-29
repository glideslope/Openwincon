package process264Real.block_process;

import Util.ExecutionTimer;
import process264Real.StructurePT;

public class ProcessBlock {

    private ProcessBasic box;       
    private CalcCoderate blr_calc;     

    public ProcessBlock(FEC.common.TypeFEC fType) {
        blr_calc = new CalcCoderate();
        
        box = new ProcessBasic(fType, StructurePT.YUV_SIZE);

    }
    
    public double plr;      
    public int k;      
    public int t;      
    public int symbol_size;     
    public int q = 10;      
    public byte[] source;       
    public byte[] video_encoded;        
    public byte[] fountain_encoded;     
    public boolean[] non_error;     
    public void setSource(byte[] source, int symbol_size) {
        this.source = java.util.Arrays.copyOf(source, source.length);
        this.symbol_size = symbol_size;		
        if (box.fType == FEC.common.TypeFEC.RS223) {
            this.symbol_size = 1;
        }
        k = source.length / this.symbol_size;   
        if (k % this.symbol_size > 0) {
            k++;
        }
        System.out.println("in pre video setSource : source num  " + k);
    }
    public void setFountainEncoded(byte[] fountain_encoded) {
        this.fountain_encoded = java.util.Arrays.copyOf(fountain_encoded, fountain_encoded.length);
    }

    
    public void setPlr(double plr) {
        if (plr > 1.0) {
            System.out.println("error: PLR can't be larger than 1.0");
        } else if (plr < 0.0) {
            System.out.println("error: PLR can't be smaller than 0.0");
        }
        this.plr = plr;
    }
    
    public double getCoderate() {
        if (box.getFecType() == FEC.common.TypeFEC.RS223) {
            return 223.0 / 255.0;
        } else {
            return blr_calc.calcCoderate(k, plr, StructurePT.blr_constraint);      
        }
    }

    public double getCoderateHo(int tpSum) {

        return blr_calc.calcCoderateHo(tpSum, plr, StructurePT.blr_constraint);    

    }

    public int process(double coderate) throws Exception {
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        video_encoded = box.video_encoder(source, q); 
        System.out.println("============ video encoding: " + timer.end().duration());
        timer.start();
        k = video_encoded.length / this.symbol_size;        
        if (video_encoded.length % this.symbol_size > 0) {
            k++;
        }
        timer.start();
        t = (int) ((int) k / coderate);

        fountain_encoded = box.fec_encoder(video_encoded, t, k, symbol_size);
        System.out.println("============ fountain encoding: " + timer.end().duration());

        return k;
    }

    public boolean[] deprocess(int k, int t, int s_size) throws Exception {
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        boolean[] success = new boolean[k];
        video_encoded = box.fec_decoder(fountain_encoded, t, k, s_size, non_error, success);
        System.out.println("fountain decoding: " + timer.end().duration());
        timer.start();

        int GOP_size = Util.Tools.get_GOP_size();


        System.out.println("============================================== fountain decoding =====");
        source = box.video_decoder(video_encoded, GOP_size);
        System.out.println("video decoding: " + timer.end().duration());
        return success;
    }
}
