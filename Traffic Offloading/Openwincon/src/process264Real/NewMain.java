package process264Real;

public class NewMain {

   
    public static void main(String[] args) {

        int num_addresses = 3;
        int paths[] = {5, 2, 3};
        int tp[] = {0, 0, 0};

        for (int tpSum = 0; tpSum < 30; tpSum++) {

            int sum=tpSum;
            for (int i = 0; i < num_addresses - 1; i++) {
                int temp = 1;

                for (int j = i + 1; j < num_addresses; j++) {
                    temp = temp * (paths[j] + 1);
                }

                tp[i] = sum / temp;
                sum %= temp;
            }

            tp[num_addresses - 1] = sum;

            for(int i = 0; i< num_addresses; i++)
            {
                System.out.print(tp[i]+"\t");
            }
            System.out.print(tpSum+"\n");

        }
    }
}
