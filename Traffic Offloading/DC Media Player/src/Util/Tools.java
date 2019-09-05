package Util;

import Util.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import process264Real.StructurePT;

public class Tools {

	private Tools() {
	}
	public static int get_video_buffer_size() {
		int size = StructurePT.width * StructurePT.height * 3 / 2 * StructurePT.frameperGOP * 3;

		if (StructurePT.YUV_SIZE == StructurePT.QCIF) {
			size = size * 2;
		} else if (StructurePT.YUV_SIZE == StructurePT.CIF) {
			size = size * 4;
		} else if (StructurePT.YUV_SIZE == StructurePT.FCIF) {
			size = size * 6;
		} else {
			System.out.println("wrong size!!!!");
		}

		return size;
	}

	public static int get_Frame_size() {
		return StructurePT.width * StructurePT.height * 3 / 2;
	}

	public static int get_GOP_size() {
		return get_Frame_size() * StructurePT.frameperGOP;
	}

	public static byte[] object_to_bytes(Object obj) {
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.ObjectOutputStream oos = null;
		try {
			oos = new java.io.ObjectOutputStream(baos);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		try {
			oos.writeObject(obj);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		try {
			oos.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return baos.toByteArray();
	}

	public static Object bytes_to_object(byte[] stream) throws IOException {
		java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(stream);
		java.io.ObjectInputStream ois = null;
		Object obj = null;
		try {
			ois = new java.io.ObjectInputStream(bais);
		} catch (java.io.IOException ex) {
			ex.printStackTrace();
		}
		try {
			obj = ois.readObject();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return obj;
	}

	public static Object bytes_to_object_Real(byte[] stream) {
		java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(stream);
		java.io.ObjectInputStream ois = null;
		Object obj = null;
		try {
			ois = new java.io.ObjectInputStream(bais);

			try {
				if (ois == null) {
					Util.Tools.print_err(">>>>>>>>>>>>>>>>>> expected error");
				}
				if (bais == null) {
					Util.Tools.print_err("<<<<<<<<<<<<<<<<<<<expected error");
				}
				obj = ois.readObject();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}
			ois.close();
			bais.close();
		} catch (java.io.IOException ex) {
			ex.printStackTrace();
			return null;
		} catch (java.lang.NullPointerException ex) {
			ex.printStackTrace();
			return null;
		} catch (java.lang.OutOfMemoryError ex) {
			ex.printStackTrace();
			return null;
		}
		return obj;
	}
	public static void byte_to_file(byte[] stream, String filename) {
		java.io.File f = new java.io.File(filename);
		java.io.FileOutputStream fos = null;
		try {
			fos = new java.io.FileOutputStream(f);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		try {
			fos.write(stream);
			fos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void byte_to_file_first(byte[] stream, String filename) {
		java.io.File f = new java.io.File(filename);
		java.io.FileOutputStream fos = null;
		try {
			fos = new java.io.FileOutputStream(f, false);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		try {
			fos.write(stream);
			fos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void byte_to_file_concatenate(byte[] stream, String filename) {
		java.io.File f = new java.io.File(filename);
		java.io.FileOutputStream fos = null;
		try {
			fos = new java.io.FileOutputStream(f, true);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		try {
			fos.write(stream);
			fos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static byte[] file_to_byte(String filename) {
		java.io.File f = new java.io.File(filename);
		java.io.FileInputStream fis = null;
		byte[] stream = new byte[(int) f.length()];
		try {
			fis = new java.io.FileInputStream(f);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		try {
			fis.read(stream);
			fis.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return stream;
	}

	public static void object_to_file(Object obj, String filename) {
		byte_to_file(object_to_bytes(obj), filename);
	}

	public static Object file_to_object(String filename) {
		Object o = null;
		try {
			bytes_to_object(file_to_byte(filename));
		} catch (IOException ex) {
			Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
		}
		return o;
	}

	public static int exeCommand(String command) {
		int exitVal = 0;
		try {
			Process p = Runtime.getRuntime().exec(command);
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());
			outputGobbler.start();
			errorGobbler.start();
			exitVal = p.waitFor();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return exitVal;
	}
	public static double getTime(long compared_time) {
		return (System.currentTimeMillis() - compared_time) / 1000.0;
	}

	public static int clip(int original, int max, int min) {
		int result = Math.max(min, original);
		return Math.min(max, result);
	}

	public static byte[] interleave(byte[] data, int size) {
		byte[] inter = new byte[data.length];
		int cycle = (int) Math.ceil(data.length / (double) size);
		for (int i = 0; i < cycle; i++) {
			for (int j = 0; j < size; j++) {
				inter[j * cycle + i] = data[i * size + j];
			}
		}
		return inter;
	}

	public static byte[] deterleave(byte[] data, int size) {
		return interleave(data, (int) Math.ceil(data.length / (double) size));
	}

	static public byte[] pad(byte[] data, int num) {
		if (data.length == num) {
			return data;
		}
		byte[] result = new byte[num];
		int max = Math.min(data.length, num);
		for (int i = 0; i < max; i++) {
			result[i] = data[i];
		}
		return result;
	}
	public static String fixedWidthDoubletoString(double x, int w, int d) {
		java.text.DecimalFormat fmt = new java.text.DecimalFormat();
		fmt.setMaximumFractionDigits(d);
		fmt.setMinimumFractionDigits(d);
		fmt.setGroupingUsed(false);
		String s = fmt.format(x);
		while (s.length() < w) {
			s = " " + s;
		}
		return s;
	}

	public static int[] byte_to_int(byte[] data) {
		int[] result = new int[data.length];
		for (int i = 0; i < data.length; i++) {
			result[i] = data[i] & 0xFF;
		}
		return result;
	}

	public static byte[] int_to_byte(int[] data) {
		byte[] result = new byte[data.length];
		for (int i = 0; i < data.length; i++) {
			result[i] = (byte) (data[i]);
		}
		return result;
	}

	public static byte[] times_array(byte[] data, int times) {
		byte[] result = new byte[data.length * times];
		for (int i = 0; i < times; i++) {
			for (int j = 0; j < data.length; j++) {
				result[i * data.length + j] = data[j];
			}
		}
		return result;
	}
	public static int sum_array(int[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}

	public static int compare(byte[] orig, byte[] recon, boolean isPrint) {
		int num_error = 0;
		for (int i = 0; i < orig.length; i++) {
			if (orig[i] != recon[i]) {
				if (isPrint) {
					System.out.println("error on " + i + " :: " + orig[i] + " :: " + recon[i]);
				}
				num_error++;
			}
		}
		if (isPrint) {
			System.out.println("number or error :: " + num_error);
		}
		return num_error;
	}

	public static int countTrue(boolean[] array) {
		int count = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == true) {
				count++;
			}
		}
		return count;
	}

	public static int countFalse(boolean[] array) {
		return array.length - countTrue(array);
	}

	public static boolean waitFile(String filename, int timeout) {
		long start_time = System.currentTimeMillis();
		java.io.File file = new java.io.File(filename);
		while (!file.canRead()) {
			try {
				Thread.sleep(2);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			if ((System.currentTimeMillis() - start_time) > timeout) {
				return false;
			}
		}
		return true;
	}

	public static void delete_file(String filename) {
		java.io.File file = new java.io.File(filename);
		file.delete();
	}

	public static void fileCopy(String source, String target) {
		//----- Getting file channels
		java.nio.channels.FileChannel in = null;
		java.nio.channels.FileChannel out = null;
		try {
			in = new java.io.FileInputStream(source).getChannel();
			out = new java.io.FileOutputStream(target).getChannel();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		try {
			//----- JavaVM does its best to do this as native I/O operations.
			in.transferTo(0, in.size(), out);
			out.close();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static enum TypeVideo {
		//----- 176 * 144

		QCIF,
		//----- QCIF * 2 = 352 * 288
		CIF,
		//----- CIF * 2 = 704 * 576
		FCIF
	}

	public static double calcMSE(byte[] original, byte[] result) {
		long sum = 0;
		int diff;
		for (int i = 0; i < original.length; i++) {
			int ioriginal = original[i] & 0xFF;
			int iresult = result[i] & 0xFF;
			diff = ioriginal - iresult;
			sum += diff * diff;
		}
		double MSE = sum / original.length;
		return MSE;
	}

	public static double MSEtoPSNR(double MSE, double max) {
		return 10 * Math.log10((max * max) / MSE);
	}

	public static double calcPSNR(byte[] original, byte[] result) {
		double MSE = calcMSE(original, result);
		double PSNR = MSEtoPSNR(MSE, 255.0);
		return PSNR;
	}

	public static double calcPSNR_y(byte[] original, byte[] result) {
		return MSEtoPSNR(calcMSE_y_general(original, result, TypeVideo.QCIF), 255.0);
	}

	public static double calcMSE_y_general(byte[] original, byte[] result, TypeVideo type) {
		int width = 176;
		int height = 144;
		if (type == TypeVideo.CIF) {
			width *= 2;
			height *= 2;
		}
		if (type == TypeVideo.FCIF) {
			width *= 4;
			height *= 4;
		}
		double sum = 0;
		int diff;
		int y_size = width * height;
		int frame_size = (int) (y_size * 1.5);
		int length = Math.min(original.length, result.length);
		if (length == 0) {
			print_err("the length of data is zero : calcMSE_y_general");
			return -1;
		}
		for (int i = 0; i < length; i++) {
			if (i % frame_size < y_size) {
				int ioriginal = original[i] & 0xFF;
				int iresult = result[i] & 0xFF;
				diff = ioriginal - iresult;
				sum += diff * diff;
			}
		}
		double MSE = sum / (original.length * 2.0 / 3.0);
		return MSE;
	}

	
	public static double calcPSNR_y_general(byte[] original, byte[] result, TypeVideo type) {
		double MSE = calcMSE_y_general(original, result, type);
		double PSNR = MSEtoPSNR(MSE, 255.0);
		return PSNR;
	}

	public static void print_err(String str) {
		System.out.println("---> pit30z error : " + str);
	}

	public static void merge_YUV(String targetFile1, String targetFile2, String mergedFile) {
		java.io.File file_1 = new java.io.File(targetFile1);
		java.io.File file_2 = new java.io.File(targetFile2);
		java.io.File merged_File = new java.io.File(mergedFile);

		byte[] stream_1 = new byte[(int) file_1.length()];
		byte[] stream_2 = new byte[(int) file_2.length()];

		try {
			java.io.FileInputStream fis_1 = new java.io.FileInputStream(file_1);
			java.io.FileInputStream fis_2 = new java.io.FileInputStream(file_2);
			fis_1.read(stream_1);
			fis_2.read(stream_2);
			fis_1.close();
			fis_2.close();

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		try {
			java.io.FileOutputStream fos_1 = new java.io.FileOutputStream(merged_File, true);
			;

			fos_1.write(stream_1);
			fos_1.write(stream_2);
			fos_1.close();
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
		}



	}
}

class StreamGobbler extends Thread {

	java.io.InputStream is;

	StreamGobbler(java.io.InputStream is) {
		this.is = is;
	}

	@Override
	public void run() {
		try {
			java.io.InputStreamReader isr = new java.io.InputStreamReader(is);
			java.io.BufferedReader br = new java.io.BufferedReader(isr);
			for (String line = null; (line = br.readLine()) != null;) {
				System.out.println(line);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
