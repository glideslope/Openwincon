package FEC.rs;

import java.util.Arrays;

public final class Galois {
    private static final Galois instance = new Galois();
    private int[] expTbl = new int[255 * 2];    
    private int[] logTbl = new int[255 + 1];    

    private Galois() {
        initGaloisTable();
    }

    public static Galois getInstance() {
        return instance;
    }

    private void initGaloisTable() {
        int d = 1;
        for(int i = 0; i < 255; i++) {
            expTbl[i] = expTbl[255 + i] = d;
            logTbl[d] = i;
            d <<= 1;
            if((d & 0x100) != 0) {
                d = (d ^ 0x1d) & 0xff;
            }
        }
    }

    public int toExp(int a) {
        return expTbl[a];
    }

    public int toLog(int a) {
        return logTbl[a];
    }

    public int mul(int a, int b)	{
        return (a == 0 || b == 0)? 0 : expTbl[logTbl[a] + logTbl[b]];
    }

    public int mulExp(int a, int b)	{
        return (a == 0)? 0 : expTbl[logTbl[a] + b];
    }

    public int div(int a, int b) {
        return (a == 0)? 0 : expTbl[logTbl[a] - logTbl[b] + 255];
    }

    public int divExp(int a, int b) {
        return (a == 0)? 0 : expTbl[logTbl[a] - b + 255];
    }

    public int inv(int a) {
        return expTbl[255 - logTbl[a]];
    }

    public void mulPoly(int[] seki, int[] a, int[] b) {
        Arrays.fill(seki, 0);
        for(int ia = 0; ia < a.length; ia++) {
            if(a[ia] != 0) {
                int loga = logTbl[a[ia]];
                int ib2 = Math.min(b.length, seki.length - ia);
                for(int ib = 0; ib < ib2; ib++) {
                    if(b[ib] != 0) {
                        seki[ia + ib] ^= expTbl[loga + logTbl[b[ib]]];	
                    }
                }
            }
        }
    }

    public boolean calcSyndrome(int[] data, int length, int[] syn) {    
        int hasErr = 0;
        for(int i = 0; i < syn.length;  i++) {
            int wk = 0;
            for(int idx = 0; idx < length; idx++) {
                wk = data[idx] ^ ((wk == 0)? 0 : expTbl[logTbl[wk] + i]);		
            }
            syn[i] = wk;
            hasErr |= wk;
        }
        return hasErr == 0;
    }

}
