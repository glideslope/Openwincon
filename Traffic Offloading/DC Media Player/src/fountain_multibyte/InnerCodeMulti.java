package fountain_multibyte;

public class InnerCodeMulti {

    protected int[] degree;
    private boolean[] memory;
    public long seed_degree = 676L;
    public long select_seed;
    public int[][] testMatrix;
    public double alpha;
    public int Inum_source;
  
    public InnerCodeMulti() {
    }

    protected int[] getPro(int num_source) {
        int[] pro = new int[num_source + 1];
        double scale = Integer.MAX_VALUE / 3;
        int F = 2114;
        pro[1] = (int) (scale * (1.0 - (1.0 + 1.0 / F) / (1.0 + 0.01)));
        for (int i = 2; i <= num_source; i++) {
            pro[i] = pro[i - 1] + (int) (scale * ((1 - pro[1] / scale) / ((1 - 1.0 / F) * i * (i - 1))));
        }
        pro[0] = pro[num_source];
        return pro;
    }

    private int[] getDegree(int num_source, int num_encoded) {
        java.util.Random rand = new java.util.Random(seed_degree);


        int degree_select =0;
        if (degree_select == 1) {
            double[] temp = {0, 0.007969, 0.493570, 0.166220, 0.072646, 0.082558, 0.056058, 0.037229,
                    0.055590, 0.025023, 0.003135};

            int[] d = {0, 1, 2, 3, 4, 5, 8, 9, 19, 64, 66};
            int[] pro = new int[temp.length];
            double scale = Integer.MAX_VALUE / 3;

            pro[1] = (int) (scale * temp[1]);

            for (int i = 2; i < temp.length; i++) {
                pro[i] = pro[i - 1] + (int) (scale * temp[i]);
            }
            pro[0] = pro[pro.length - 1];


            degree = new int[num_encoded];
            for (int i = 0; i < num_encoded; i++) {
                int tmp = rand.nextInt(pro[0]);
                for (int j = 1; j <= pro.length; j++) {
                    if (tmp < pro[j]) {
                        degree[i] = d[j];
                        break;
                    }
                }
            }
            int degree1 = 0;
            int degree2 = 1;
            for (int i = 0; i < degree.length; i++) {
                if (degree[i] == 1) {
                    degree1++;
                } else if (degree[i] == 2) {
                    degree2++;
                }
            }
        } else {
            int[] pro = getPro(num_source);

            degree = new int[num_encoded];
            for (int i = 0; i < num_encoded; i++) {
                int tmp = rand.nextInt(pro[0]);
                for (int j = 1; j <= num_source; j++) {
                    if (tmp < pro[j]) {
                        degree[i] = j;
                        break;
                    }
                }
            }
        }
        return degree;
    }
    static int countMIB = 0;
    static int countLIB = 0;
    static int isCal = 0;
    public static int[] d1Location;

    public int[][] getMatrix_old(int num_source, int num_encoded) {
        degree = getDegree(num_source, num_encoded);
        java.util.Random rand = new java.util.Random(select_seed);
        int[][] matrix = new int[num_encoded][];

        for (int i = 0; i < num_encoded; i++) {
            boolean[] preventDuplicate = new boolean[num_source];
            matrix[i] = new int[degree[i]];
            for (int j = 0; j < degree[i]; j++) {
                while (true) {
                    int temp = rand.nextInt(num_source);
                    if (preventDuplicate[temp] == false) {
                        matrix[i][j] = temp;
                        preventDuplicate[temp] = true;
                        break;
                    }
                }
            }
        }
        return matrix;
    }
    public byte[] encode(byte[] source_symbols, int num_source, int num_encoded, int SymbolSize) {

        int[][] matrix = null;
        isCal = 0;
        matrix = getMatrix_old(num_source, num_encoded);
        testMatrix = matrix;


        byte[] encoded = new byte[num_encoded * SymbolSize];

        for (int i = 0; i < num_encoded; i++) {
            for (int j = 0; j < degree[i]; j++) {
                for (int k = 0; k < SymbolSize; k++) {
                    if((SymbolSize * matrix[i][j] + k)>=source_symbols.length){
                        encoded[(i * SymbolSize) + k] ^= 0;
                    }else{
                        encoded[(i * SymbolSize) + k] ^= source_symbols[(SymbolSize * matrix[i][j] + k)];
                    }
                }
            }
        }
        return encoded;
    }

    public byte[] decode(byte[] encoded_symbols, int num_encoded, int num_source, int SymbolSize, boolean[] non_error, boolean[] sucess) {
        int[][] matrix = null;
        isCal = 1;
        matrix = getMatrix_old(num_source, num_encoded);
        testMatrix = matrix;
        byte[] reconstructed = new byte[num_source * SymbolSize];
        boolean[] finished = new boolean[num_source];
        int[] local_degree = java.util.Arrays.copyOf(degree, num_encoded);
        int count = 0;
        for (int k = 0; k < 1000; k++) {
            int tmp_checker = 0;
            for (int i = 0; i < num_encoded; i++) {
                if (local_degree[i] == 1 && non_error[i]) {
                    for (int j = 0; j < degree[i]; j++) {
                        int sel = matrix[i][j];
                        if (sel != -1 && !finished[sel]) {
                            for (int q = 0; q < SymbolSize; q++) {
                                reconstructed[(sel * SymbolSize) + q] = encoded_symbols[(i * SymbolSize) + q];
                            }
                            finished[sel] = true;
                            sucess[sel] = true;
                            matrix[i][j] = -1;
                            local_degree[i]--;
                            count++;
                            tmp_checker++;
                        }
                    }
                }
            }
            for (int i = 0; i < num_encoded; i++) {
                if (non_error[i]) {
                    for (int j = 0; j < degree[i]; j++) {
                        int sel = matrix[i][j];
                        if (sel != -1 && finished[sel]) {
                            for (int q = 0; q < SymbolSize; q++) {
                                encoded_symbols[(i * SymbolSize) + q] ^= reconstructed[(sel * SymbolSize) + q];
                            }
                            matrix[i][j] = -1;
                            local_degree[i]--;
                            tmp_checker++;
                        }
                    }
                }
            }
            if (count >= num_source || tmp_checker == 0) {
                break;
            }
        }
        return reconstructed;
    }
}
