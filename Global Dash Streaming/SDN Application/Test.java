/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.ac.postech;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Runtime;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    final static int PORT_RSSI = 20000;
    final static int PORT_CONTROL = 30000;
    final static int NUM_CONNECTION = 5;
	
    final static double MIN_RSSI = -99;
	
    final static int MAX_SEGMENT = 5;
    final static int MAX_BUFFER = 10;

    final static int THRESH_BUFFER = 9;
	
    final static double VAL_DURATION = 2.0;
    final static double VAL_TIMESLOT = 1.0;
    static double val_timeslot = VAL_TIMESLOT;
	
    final static double MAX_ITER = 1000;
	
    final static double MAX_LAM = 1;
    final static double MIN_LAM = 0;
	
    final static double ERROR_LAM = 0.00001;
	
    final static int MAX_X = 1000;
    final static int DELTA_X = 5;
	
    final static int[] ARRAY_BITRATE = {200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800, 3000};

    private static ArrayList<String> array_ue;
    private static ArrayList<String> array_ap;
	
    /* <MAC 1, MAC 2> */
    private static Map<String, String> map_mac;
	
    /* <UE name, UE class> */
    private static Map<String, UE> map_ue;


    /* <AP name, UE list> */
    /* Single에 사용 */
    private static Map<String, ArrayList<String>> map_connection;
	
    /* <UE, rate> */
    private static Map<String, Integer> map_rate;
	
    /* <AP, timeslot> */
    private static Map<String, Double> map_timeslot;
	
    /* <UE, video> 	  */
    private static Map<String, String> map_video;
	
    /* <UE, segment> */
    private static Map<String, Integer> map_segment;
	
    /* <UE, buffer> */
    private static Map<String, Integer> map_buffer;
	
    private static Map<String, Long> map_stamp;
	
    /* <video + rate, PSNR> */
    private static Map<String, ArrayList<Double>> map_psnr;
	
    private static Map<String, ArrayList<Double[]>> map_para;
	
    final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");

    @Activate
    protected void activate() {
        log.info("Started");
   
        array_ue = new ArrayList<String>();
        array_ap = new ArrayList<String>();
		
        map_mac = new HashMap<String, String>();
        map_ue = new HashMap<String, UE>();
        map_connection = new HashMap<String, ArrayList<String>>();
        map_rate = new HashMap<String, Integer>();
        map_timeslot = new HashMap<String, Double>();
        map_video = new HashMap<String, String>();
        map_psnr = new HashMap<String, ArrayList<Double>>();
        map_para = new HashMap<String, ArrayList<Double[]>>();
        map_segment = new HashMap<String, Integer>();
        map_buffer = new HashMap<String, Integer>();
        map_stamp = new HashMap<String, Long>();
		
        readCSV();

	new ThreadRSSI().start();
        for (int i = 0; i < NUM_CONNECTION; i++)
            new ThreadControl(PORT_CONTROL + i).start();

        /* 클라이언트와 통신 전 기본 값 적용 */
        giveDefaultValue("bunny");

        while (true) {
            try {
				
                /* 알고리즘 시작부 */
                doPreparation(1);
                setTimeslot();

                int int_iter = 0;
                int int_over;
                double lam_pre = MAX_LAM;
                double lam_max = MAX_LAM;
                double lam_min = MIN_LAM;
                double lam_mid = (MAX_LAM + MIN_LAM) / 2;
							
                /* 솔루션을 찾기 위한 변수 */
                double max_psnr = 0;
                Map<String, Integer> max_rate = new HashMap<String, Integer>();
							
                String max_ap, min_ap;
                /* 반복 시작 */
                while(true) {
                    int_iter ++;
							 
                    /* 최대 횟수를 넘은 경우 (RSSI 정보가 불 충분한 경우) */
                    if (int_iter > MAX_ITER)
                        break;
								
                    /* 대략적인 비트레이트 계산 */
                    calculateBitrate(lam_mid);
                    /* 비트레이트 양자화 */
                    quantizeBitrate(array_ue);
								
                    /* 타임슬롯 체크 */
                    int_over = checkTimeslot();
							 
                    /* int_over == 0 */
                    if (int_over == 0) {
                        /* 최대 PSNR 갱신 체크 */
                        double sum_psnr = calculateSumPSNR();
                        max_psnr = compareMaxPSNR(max_rate, max_psnr, sum_psnr);
									
                        if (Math.abs(lam_mid - lam_pre) < ERROR_LAM)
                            break;
                        else {
                            lam_pre = lam_mid;
                            lam_max = lam_mid;
                            lam_mid = (lam_min + lam_max) / 2;
                            continue;
                        }
                    }
							 
                    /* int_over == 2 */
                    else if(int_over == 2) {
                        lam_pre = lam_mid;
                        lam_min = lam_mid;
                        lam_mid = (lam_min + lam_max) / 2;
                        continue;
                    }
							 
                    /* int_over == 1 */	
                    /* 타임슬롯 많이 차지하는 AP 찾기 */
                    min_ap = array_ap.get((map_timeslot.get(array_ap.get(0)) < map_timeslot.get(array_ap.get(1)))? 0: 1);
                    max_ap = array_ap.get((map_timeslot.get(array_ap.get(0)) < map_timeslot.get(array_ap.get(1)))? 1: 0);
		
                    /* 대역폭 비율 고려하여 정렬 */
                    ArrayList<String> array_sorted = calculateBandwidthRatio(min_ap, max_ap);
                    /* x 값 백업 */
                    Map<String, UE> map_copy = backupX();
							 
                    /* 타임슬롯 줄일 수 있는 UE 조사 */
                    ArrayList<String> array_possible = findReducible(array_sorted, map_copy, max_ap);	
                    /* 타임슬롯 줄이기 */
                    reduceTimeslot(array_possible, min_ap, max_ap);
		
                    /* 타임슬롯 체크 */
                    int_over = checkTimeslot();
							 
                    /* int_over == 0 */
                    if (int_over == 0) {
                        int_iter ++;
								 
                        /* 최대 PSNR 갱신 체크 */
                        double sum_psnr = calculateSumPSNR();
                        max_psnr = compareMaxPSNR(max_rate, max_psnr, sum_psnr);
									
                        if (Math.abs(lam_mid - lam_pre) < ERROR_LAM)
                            break;
                        else {
                            lam_pre = lam_mid;
                            lam_max = lam_mid;
                            lam_mid = (lam_min + lam_max) / 2;
                            continue;
                        }
                    }

                    /* int_over == 1, 2 */
                    else {
                        /* x 값 복구 */
                        restoreX(map_copy);
									
                        lam_pre = lam_mid;
                        lam_min = lam_mid;
                        lam_mid = (lam_min + lam_max) / 2;
                        continue;
                    }		
                }

                Thread.sleep(2000);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void readCSV() {
		
        File file_allow = new File("/home/sdn/onos/tools/test/bin/Test/app/list_allow.csv");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file_allow));
            String str_line;
            String str_type;
            String str_mac;
            String str_group;
	        
            Map<String, String> map_group = new HashMap<String, String>(); 
	        
            int int_row = 0;
            while ((str_line = reader.readLine()) != null) {
                int_row ++;
	        	
                /* 첫줄은 읽지 않는다 */
                if(int_row == 1)
                    continue;
	        	
                str_type = str_line.split(",")[0].trim();
                str_mac = str_line.split(",")[1].replaceAll(":|-", "").trim().toLowerCase();
	        	
                if (str_type.toUpperCase().equals("AP"))
                    array_ap.add(str_mac);
                else if (str_type.toUpperCase().equals("UE")) {
                    str_group = str_line.split(",")[2].trim();
	        		
                    if(map_group.get(str_group) == null) {
                        array_ue.add(str_mac);
                        map_ue.put(str_mac, new UE());
                        map_mac.put(str_mac, str_mac);
                        map_group.put(str_group, str_mac);
                    }else {
                        map_mac.put(str_mac, (String) map_group.get(str_group));
                    }
                }
            }    
            reader.close();
        }catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        File file_folder = new File("/home/sdn/onos/tools/test/bin/Test/app/csv/psnr");
        File[] array_file = file_folder.listFiles();
        String[] array_element;
        String str_video, str_bitrate;

        try {
            for (int i = 0; i < array_file.length; i++) {
                array_element = array_file[i].toString().split("_");
                str_video = array_element[1];
                str_bitrate = array_element[2].replace(".csv", "");
	        	
                ArrayList<Double> array_psnr = new ArrayList<Double>();
	        	
                BufferedReader reader = new BufferedReader(new FileReader(array_file[i]));
                String str_line;
                while ((str_line = reader.readLine()) != null) 
                    array_psnr.add(Double.parseDouble(str_line.split(",")[1].trim()));

                    map_psnr.put(str_video + str_bitrate, array_psnr);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        file_folder = new File("/home/sdn/onos/tools/test/bin/Test/app/csv/para");
        array_file = file_folder.listFiles();
        double double_para[] = new double[2];
        	
        try {
            for (int i = 0; i < array_file.length; i++) {
                array_element = array_file[i].toString().split("_");
                str_video = array_element[1].replace(".csv", "");
	        	
                ArrayList<Double[]> array_para = new ArrayList<Double[]>();
	        	
                BufferedReader reader = new BufferedReader(new FileReader(array_file[i]));
                String str_line;
		        
                while ((str_line = reader.readLine()) != null) {
                    double_para[0] = Double.parseDouble(str_line.split(",")[1].trim());
                    double_para[1] = Double.parseDouble(str_line.split(",")[2].trim());
                    array_para.add(new Double[]{double_para[0], double_para[1]});
                }
                map_para.put(str_video, array_para);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void doPreparation(int mode) {

        if (mode == 0)
            map_connection.clear();
		
        /* ratio 계산하기 */
        for (String ue: array_ue) {

            double max_rssi = MIN_RSSI;
            String max_ap = array_ap.get(0);
		 
            for (String ap: array_ap) {
                map_ue.get(ue).setRatio(ap, 0);
				 
                // RSSI 정보(즉 연결이 안된 경우)가 없는 경우 신호가 약하다고 판단
                if ((map_ue.get(ue)).hasRSSI(ap) == false) {
                    map_ue.get(ue).setRSSI(ap, MIN_RSSI);
                    continue;
                }
				 
                double rssi = map_ue.get(ue).getRSSI(ap);
				 
                if (rssi > max_rssi) {
                    max_rssi = rssi;
                    max_ap = ap;
                }
            }
			
            map_ue.get(ue).setRatio(max_ap, MAX_X);
			
            if (mode == 0) {
                if (map_connection.containsKey(max_ap))
                    map_connection.get(max_ap).add(ue);
                else {
                    ArrayList<String> array_connection = new ArrayList<String>();
                    array_connection.add(ue);
                    map_connection.put(max_ap, array_connection);
                }
            }
        }
    }
	

    private static void setTimeslot() {
        val_timeslot = VAL_TIMESLOT;
		
        ArrayList<Integer> array_buffer = new ArrayList<>();
        for (String ue: array_ue) {
            if (map_stamp.containsKey(ue))
                array_buffer.add(map_buffer.get(ue));
        }
		
        if(array_buffer.size() > 0) {
            array_buffer.sort(null);
            int x = array_buffer.get((int)(array_buffer.size() / 2)) - THRESH_BUFFER;
            val_timeslot += (0.01439 * x * x * x + 0.02866 * x * x + 0.02828 * x + 0.005873);
            if (val_timeslot > 1.5 * VAL_TIMESLOT)
                val_timeslot = 1.5 * VAL_TIMESLOT;
            else if(val_timeslot < 0.5 * VAL_TIMESLOT)
                val_timeslot = 0.5 * VAL_TIMESLOT;
        }
    }
    
    private static void calculateBitrate(double lam) {
        ArrayList<Integer> array_bandwidth = new ArrayList<Integer>();
        double sum_bandwidth = 0;
        for (String ue: array_ue) {
            for (String ap: array_ap) {
                int x = map_ue.get(ue).getRatio(ap);
                double bandwidth = getBandwidth(map_ue.get(ue).getRSSI(ap));
                sum_bandwidth += (((double)x) / 1000) * bandwidth;
            }
            array_bandwidth.add((int)(sum_bandwidth));
            String str_video = map_video.get(ue);
            int int_segment = map_segment.get(ue);
            double double_para = map_para.get(str_video).get(int_segment - 1)[0];
            int rate = (int)(double_para / lam);
			
            /* 여기서 rate는 인덱스가 아님 */
            map_rate.put(ue, rate);
        }
    }

    private static void quantizeBitrate(ArrayList<String> array_connection) {
        ArrayList<Integer> array_upper = new ArrayList<Integer>();
        ArrayList<Integer> array_lower = new ArrayList<Integer>();
		
        for (String ue: array_connection) {
            if (map_rate.get(ue) <= ARRAY_BITRATE[0]) {
                array_lower.add(0);
                continue;
            }
			
            for (int i = ARRAY_BITRATE.length - 1; i >= 0; i--) {
                if (ARRAY_BITRATE[i] <= map_rate.get(ue)) {
                    array_lower.add(i);
                    break;
                }
            }
        }
		
        for (String ue: array_connection) {
            if (map_rate.get(ue) >= ARRAY_BITRATE[ARRAY_BITRATE.length - 1]) {
                array_upper.add(ARRAY_BITRATE.length - 1);
                continue;
            }
			
            for (int i = 0; i < ARRAY_BITRATE.length; i++) {
                if (ARRAY_BITRATE[i] >= map_rate.get(ue)) {
                    array_upper.add(i);
                    break;
                }
            }
        }
		
        for (String ue: array_connection) {
            int idx_ue = array_connection.indexOf(ue);
            int rate_upper = ARRAY_BITRATE[array_upper.get(idx_ue)];
            int rate_lower = ARRAY_BITRATE[array_lower.get(idx_ue)];
			
            if (Math.abs(rate_upper - map_rate.get(ue)) - Math.abs(rate_lower - map_rate.get(ue)) > 0)
                map_rate.put(ue, rate_lower);
            else
                map_rate.put(ue, rate_upper);
        }
    }
    
    private static int checkTimeslot() {
        int int_over = 0;
		
        for (String ap: array_ap) {
            double sum_timeslot = 0;
            for (String ue: array_ue) {

                int x = map_ue.get(ue).getRatio(ap);
                int rate = map_rate.get(ue);
                int bandwidth = getBandwidth(map_ue.get(ue).getRSSI(ap));
                double timeslot = (((double)x) / 1000) * rate * VAL_DURATION / bandwidth;
				
                sum_timeslot += timeslot;
            }
			
            map_timeslot.put(ap, sum_timeslot);
            if (sum_timeslot > val_timeslot)
                int_over ++;
        }
		
        return int_over;
    }

    private static double calculateSumPSNR() {
        double sum_psnr = 0;
        for (String ue: array_ue) {
            int rate = map_rate.get(ue);
				
            sum_psnr += getPSNR(ue, rate);
        }
		
        return sum_psnr;
    }

    private static double compareMaxPSNR(Map<String, Integer> max_rate, double max_psnr, double sum_psnr){
        double return_psnr = max_psnr; 
        if(sum_psnr > max_psnr) {
            return_psnr = sum_psnr;
            for (String ue: array_ue) {
                int rate = map_rate.get(ue);
					
                max_rate.put(ue, rate);
            }
        }
		
        return return_psnr;
    }

    private static ArrayList<String> calculateBandwidthRatio(String min_ap, String max_ap) {
        Map<String, Double> map = new HashMap<String, Double>();
        for (String ue: array_ue) {
            double max_rssi = map_ue.get(ue).getRSSI(max_ap);
            double min_rssi = map_ue.get(ue).getRSSI(min_ap);
            double max_bandwidth = getBandwidth(max_rssi);
            double min_bandwidth = getBandwidth(min_rssi);
				
            map.put(ue, Math.abs((max_bandwidth / min_bandwidth - 1)));
        }

        return sortValue(map);
    }
    
    private static Map<String, UE> backupX() {
        Map<String, UE> map = new HashMap<String, UE>();
        for (String ue: array_ue) {
            map.put(ue, new UE());
            for (String ap: array_ap) {
                int x = map_ue.get(ue).getRatio(ap);
					
                map.get(ue).setRatio(ap, x);
            }
        }
		
        return map;
    }

    private static ArrayList<String> findReducible(ArrayList<String> array_sorted, Map<String, UE> map, String max_ap){
        ArrayList<String> array = new ArrayList<String>();
        for (String ue: array_sorted) {
            if (map.get(ue).getRatio(max_ap) == 0)
                continue;
            array.add(ue);
        }
		 
        return array;
    }

    private static void restoreX(Map<String, UE> map) {
        for (String ue: array_ue) {
            for (String ap: array_ap) {
                int x = map.get(ue).getRatio(ap);
					
                map_ue.get(ue).setRatio(ap, x);
            }
        }
    }
	
    private static void reduceTimeslot(ArrayList<String> array, String min_ap, String max_ap) {
        boolean is_end = false;
        for (String ue: array) {
            if (is_end)
                return;
			
            while(true) {
                int x = map_ue.get(ue).getRatio(max_ap);
			 
                /* 더 이상 줄일 수 없는 경우 */
                if (x == 0)
                    break;
				
                x -= DELTA_X;
                map_ue.get(ue).setRatio(max_ap, x);
                map_ue.get(ue).setRatio(min_ap, MAX_X - x);
				
                /* 바뀐 타임슬롯 체크 */
                int int_over = checkTimeslot();
				
                /* 만약 타임슬롯 상태가 바뀐 경우 */
                if (int_over == 0) {
                    is_end = true;
                    break;
                }
				
                double sum_timeslot = 0;
                for (String ap: array_ap) {
                    double timeslot = map_timeslot.get(ap);
					
                    sum_timeslot += timeslot;
                }
					
                /* 어떻게 조절하든 타임슬롯이 모두 넘칠 경우 */
                if (sum_timeslot > array_ap.size() * val_timeslot) {
                    is_end = true;
                    x += DELTA_X;

                    map_ue.get(ue).setRatio(max_ap, x);
                    map_ue.get(ue).setRatio(min_ap, MAX_X - x);
                    break;
                }
            }
        }
    }

    private static int getBandwidth(double rssi) {
        int bandwidth = (int) (8196.5 * Math.exp(rssi * 0.0047));
        return bandwidth;
    }
	
    private static double getPSNR(String ue, int bitrate) {
        String str_video = map_video.get(ue);
        int int_segment = map_segment.get(ue);
		
        Double double_para[] = map_para.get(str_video).get(int_segment - 1);
        return double_para[0] * Math.log(bitrate) + double_para[1];
    }

	
    public static ArrayList<String> sortValue(Map map){
        ArrayList<String> list_key = new ArrayList<String>();
        list_key.addAll(map.keySet());
		
        Collections.sort(list_key, new Comparator() {

            @Override
            public int compare(Object arg0, Object arg1) {
                // TODO Auto-generated method stub
				
                Object value0 = map.get(arg0);
                Object value1 = map.get(arg1);
				
                return ((Comparable) value1).compareTo(value0);
            }
        });
		
        Collections.reverse(list_key);
        return list_key;
    }
	

    public static class UE{
		
        /* <AP, RSSI> */
        private Map<String, Double> map_rssi;
		
        /* <AP, x ratio> */
        private Map<String, Integer> map_ratio;
		
        public UE() {
            map_rssi = new HashMap<String, Double>();
            map_ratio = new HashMap<String, Integer>();
        }
		
        public boolean hasRSSI(String ap) {
            return map_rssi.containsKey(ap);
        }
		
        public double getRSSI(String ap) {
            return map_rssi.get(ap);
        }
		
        public void setRSSI(String ap, Double rssi) {
            map_rssi.put(ap, rssi);
        }
		
        public int getRatio(String ap) {
            return map_ratio.get(ap);
        }
		
        public void setRatio(String ap, int ratio) {
            map_ratio.put(ap, ratio);
        }
    }
    
    private static void giveDefaultValue(String video) {
        for (String ue: array_ue) {
            map_video.put(ue, video);
            map_segment.put(ue, 1);
            map_buffer.put(ue, 10);
        }
    }

    public static class ThreadRSSI extends Thread{

        @Override
        public void run() {

	    // TODO Auto-generated method stub
	    while(true) {

	        ServerSocket socket_server = null;
		try {
                    socket_server = new ServerSocket(PORT_RSSI);
                    Socket socket_client = socket_server.accept();
					
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket_client.getInputStream(), "UTF-8"));
					
                    String str_recv = reader.readLine();
                    if (str_recv != null){
                        String array_data[] = str_recv.split("/");
                        String str_ap = array_data[0];
					
                        /* 허용된 AP일 경우만 데이터 받음 */
                        if (array_ap.indexOf(str_ap) >= 0) {

                            int len_data = array_data.length;
                            System.out.println("Received RSSI info from AP " + str_ap);
                            for (int i = 1; i < len_data; i++) {
                                String array_client[] = array_data[i].split(",");
							
                                String str_ue = map_mac.get(array_client[0]);
                                /* 허용된 UE가 아닐 경우 pass */
                                if (str_ue == null)
                                    continue;
                                int rssi_current = Integer.parseInt(array_client[1]);
                                double rssi_before = map_ue.get(str_ue).getRSSI(str_ap);
							
                                /* EWMA */
                                map_ue.get(str_ue).setRSSI(str_ap, 0.8 * rssi_before + 0.2 * rssi_current);
                            }
                        }
                    }
					
                    reader.close();
		    socket_client.close();
		    socket_server.close();
				
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static class ThreadControl extends Thread{

        private int port;
        public ThreadControl(int port) {
            this.port = port;
        }
		
        @Override
        public void run() {
            // TODO Auto-generated method stub
            while(true) {
                ServerSocket socket_server = null;
                try {
                    socket_server = new ServerSocket(port);
                    Socket socket_client = socket_server.accept();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket_client.getInputStream(), "UTF-8"));
                    String str_line = reader.readLine();
					
                    Date time = new Date();
                    String str_time = TIME_FORMAT.format(time);
					
                    String str_bitrate_origin = str_line.split("/")[0].trim();
                    int len_bitrate_origin = str_bitrate_origin.length();
                    int int_rate_origin = Integer.parseInt((String) str_bitrate_origin.subSequence(0, len_bitrate_origin - 1));
				
                    String str_ue = map_mac.get(str_line.split("/")[1].trim());
					
                    String str_video = str_line.split("/")[2].trim();
                    map_video.put(str_ue, str_video);
					
                    int int_segment = Integer.parseInt(str_line.split("/")[3].trim());
                    map_segment.put(str_ue, int_segment);
					
                    if (int_segment == MAX_SEGMENT + 1) {
                        map_stamp.put(str_ue, System.currentTimeMillis());
                    }else if(int_segment > MAX_SEGMENT + 1) {
                        int filled = (int)(VAL_DURATION) + map_buffer.get(str_ue);
                        int consumed = (int)(System.currentTimeMillis() - map_stamp.get(str_ue)) / 1000;
                        int buffer = filled - consumed;
                        if (buffer >= MAX_BUFFER)
                            buffer = MAX_BUFFER;
                        else if (buffer <= 0)
                            buffer = 0;
                        map_buffer.put(str_ue, buffer);
                        map_stamp.put(str_ue, System.currentTimeMillis());
                        System.out.println(str_ue + "'s buffer: " + buffer);
                    }
					
                    System.out.println(str_time + " " + str_ue + " " + str_video + " " + int_segment);
					
                    int int_rate_adjusted = map_rate.get(str_ue);
                    double double_x = map_ue.get(str_ue).getRatio(array_ap.get(0)) / 1000.0;
                    String str_message = int_rate_adjusted + "K/" + double_x;
                    socket_client.getOutputStream().write(str_message.getBytes());
					
                    System.out.println("UE " + str_ue +"'s bitrate is " + int_rate_adjusted);
                    System.out.println("UE " + str_ue +"'s delivery ratio is " + double_x);

                    reader.close();
                    socket_client.close();
                    socket_server.close();
					
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

}