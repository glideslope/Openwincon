package FEC.rs;

public class RsDecode {

    public static final int RS_PERM_ERROR = -1;
    public static final int RS_CORRECT_ERROR = -2;
    private static final Galois galois = Galois.getInstance();
    private int npar;  
    
    public RsDecode(int npar) {
        this.npar = npar;
    }

    public int decode(int[] data, int length) {
        if (length < npar || length > 255) {
            return RS_PERM_ERROR;
        }

        int[] syn = new int[npar];
        if (galois.calcSyndrome(data, length, syn)) {
            return 0;
        }

        int[] sigma = new int[npar / 2 + 2];
        int[] omega = new int[npar / 2 + 1];

        int[] sg0 = new int[npar];
        int[] sg1 = new int[npar];
        sg0[1] = 1;
        sg1[0] = 1;
        int jisu0 = 1;
        int jisu1 = 0;
        int m = -1;

        for (int n = 0; n < npar; n++) {
            int d = syn[n];
            for (int i = 1; i <= jisu1; i++) {
                d ^= galois.mul(sg1[i], syn[n - i]);
            }
            if (d != 0) {
                int logd = galois.toLog(d);
                int[] wk = new int[npar];
                for (int i = 0; i <= n; i++) {
                    wk[i] = sg1[i] ^ galois.mulExp(sg0[i], logd);
                }
                int js = n - m;
                if (js > jisu1) {
                    m = n - jisu1;
                    jisu1 = js;
                    if (jisu1 > npar / 2) {
                        return -1;
                    }
                    for (int i = 0; i <= jisu0; i++) {
                        sg0[i] = galois.divExp(sg1[i], logd);
                    }
                    jisu0 = jisu1;
                }
                sg1 = wk;
            }
            System.arraycopy(sg0, 0, sg0, 1, Math.min(sg0.length - 1, jisu0));
            sg0[0] = 0;
            jisu0++;
        }
        galois.mulPoly(omega, sg1, syn);
        System.arraycopy(sg1, 0, sigma, 0, Math.min(sg1.length, sigma.length));
        int jisu = jisu1;

        if (jisu <= 0) {
            return RS_CORRECT_ERROR;
        }

        int[] pos = new int[jisu];

        boolean temp = false;
        int r=0;
        int last = sigma[1];
        if (jisu == 1) {
            pos[0] = last;
            return 0;
        }
        int posIdx = jisu - 1;
        for (int i = 0; i < length; i++) {

            int z = 255 - i;
            int wk = 1;
            for (int j = 1; j <= jisu; j++) {
                wk ^= galois.mulExp(sigma[j], (z * j) % 255);
            }
            if (wk == 0) {
                int pv = galois.toExp(i);
                last ^= pv;
                pos[posIdx--] = pv;
                if (posIdx == 0) {
                    pos[0] = last;
                    r=0;
                    temp = true;
                }
            }
            if(temp==true) break;
        }

        if(temp==false){
            r=RS_CORRECT_ERROR;
        }

        if (r < 0) {
            return r;
        }
        for (int i = 0; i < jisu; i++) {
            int ps = pos[i];
            int zlog = 255 - galois.toLog(ps);
            int ov = omega[0];
            for (int j = 1; j < jisu; j++) {
                ov ^= galois.mulExp(omega[j], (zlog * j) % 255);
            }

            int dv = sigma[1];
            for (int j = 2; j < jisu; j += 2) {
                dv ^= galois.mulExp(sigma[j + 1], (zlog * j) % 255);
            }

            data[length - 1 - galois.toLog(ps)] ^= galois.mul(ps, galois.div(ov, dv));
        }
        return jisu;
    }

    public int decode(int[] data) {
        return decode(data, data.length);
    }
    public static void main(String[] args) {
        int[] data = new int[]{
                32, 65, 205, 69, 41, 220, 46, 128, 236,
                42, 159, 74, 221, 244, 169, 239, 150, 138, 70, 237, 85, 224, 96, 74, 219, 61
        };
        RsDecode dec = new RsDecode(17);
        int r = dec.decode(data);
        System.out.println("r=" + r);
        for (int i = 0; i < 8; i++) {
            data[i] ^= (int) (Math.random() * 256);
        }
        System.out.println(java.util.Arrays.toString(data));
        r = dec.decode(data);
        System.out.println("r=" + r);
        System.out.println(java.util.Arrays.toString(data));
    }
}
