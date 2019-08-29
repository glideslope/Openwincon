package process264Real.block_process;

import java.io.FileNotFoundException;
import java.io.IOException;
import Util.ExecutionTimer;
import java.math.BigDecimal;
import process264Real.StructurePT;

public class CalcCoderate {

    public double calcCoderate(int k, double plr, double blr) {
        int t;
        double cOpt=StructurePT.coderate_min;
        double gTp=1;
        double calc_result;

        System.out.println("k ? : " + k);
        k = (int) Math.ceil(k * StructurePT.symbolSize / StructurePT.packetpayloadsize );		
        System.out.println("k2 ? : " + k);

        for (double i = StructurePT.coderate_max; i > StructurePT.coderate_min; i -= StructurePT.coderate_step) {
            t = (int) Math.ceil(k / i);
            calc_result = calc_blr(t, k, plr);
            if (calc_result < blr) {
                if(blr-calc_result <gTp)
                {
                    gTp = blr - calc_result;

                    BigDecimal bd = new BigDecimal(i);
                    cOpt = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
                return Math.max(i - 0.3, StructurePT.coderate_min);
            }
        }
        return StructurePT.coderate_min;

    }

    public double calcCoderateHo(int tpSum, double plr, double blr) {
        int tpMin;
        for (double i = StructurePT.coderate_max; i > StructurePT.coderate_min; i -= StructurePT.coderate_step) {
            tpMin = (int) Math.ceil(tpSum * i);
            double calc_result = calc_blr(tpSum, tpMin, plr);
            if (calc_result < blr) {
                return Math.max(i - 0.3, StructurePT.coderate_min);
            }
        }
        return StructurePT.coderate_min;
    }

    public double calc_blr(int t, int k, double p) {   
        return Gaussian.Phi(k, t * (1 - p), Math.sqrt(t * (1 - p) * p));
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        CalcCoderate cal = new CalcCoderate();
        for (double p = 0.0; p < 1; p += 0.1) {
            double result = cal.calcCoderate(100000000, p, 0.00000001);
            System.out.println(p + " :: " + result);
        }
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        for (double p = 0.0; p < 1; p += 0.1) {
            double result = cal.calcCoderate(100000000, p, 0.00000001);
        }
        System.out.println("Execution time : " + t.end().duration());
    }
}
