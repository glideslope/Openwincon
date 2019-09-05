package fountain_multibyte;

public class LTCodeMulti extends InnerCodeMulti {
    double scale = Integer.MAX_VALUE / 3;
    boolean isRobustSoliton;
    public LTCodeMulti(boolean isRobustSoliton) {
        super();
        this.isRobustSoliton = isRobustSoliton;
    }
    public LTCodeMulti() {
        super();
        this.isRobustSoliton = true;
    }
    @Override
    protected int[] getPro(int num_source) {
        if (isRobustSoliton) {
            return getRobustSoliton(num_source);
        } else {
            return getIdealSoliton(num_source);
        }
    }
    private int[] getIdealSoliton(int num_source) {
        int[] rho = new int[num_source + 1];
        rho[1] = (int) (scale * (1.0 / num_source));
        for (int i = 2; i <= num_source; i++) {
            rho[i] = (int) (scale * (1.0 / (i * (i - 1.0))));
            rho[i] += rho[i-1];
        }
        rho[0] = rho[num_source];
        return rho;
    }
    private int[] getRobustSoliton(int num_source) {
        int[] rho = getIdealSoliton(num_source);
        int[] tau = getTau(num_source);
        int[] mu = new int[num_source + 1];
        for(int i=0; i<=num_source; i++) {
            mu[i] = rho[i] + tau[i];
        }
        return mu;
    }
    private int[] getTau(int num_source) {
        int[] tau = new int[num_source + 1];
        double c = 0.03;
        double delta = 0.5;
        double S = c * Math.log(num_source / delta) * Math.sqrt(num_source);
        int t = (int) (num_source / S);
        for (int i = 1; i < t; i++) {
            tau[i] = (int) (scale * (S / num_source) * (1.0 / i));
            if(i != 1) {
                tau[i] += tau[i-1];
            }
        }
        tau[t] = tau[t-1] + (int) (scale * (S / num_source) * Math.log(S / delta));
        for(int i=t+1; i<=num_source; i++) {
            tau[i] = tau[t];
        }
        tau[0] = tau[t];
        return tau;
    }
}
