package Util;

public class MyColor {
	public static java.awt.Color getColorfromYUV(byte y, byte u, byte v) {
		int r = (298 * ((y & 0xFF) - 16) + 409 * ((v & 0xFF) - 128) + 128) >> 8;
		int g = (298 * ((y & 0xFF) - 16) - 100 * ((u & 0xFF) - 128) - 208 * ((v & 0xFF) - 128) + 128) >> 8;
		int b = (298 * ((y & 0xFF) - 16) + 516 * ((u & 0xFF) - 128) + 128) >> 8;
		r = r > 255 ? 255 : (r < 0 ? 0 : r);
		g = g > 255 ? 255 : (g < 0 ? 0 : g);
		b = b > 255 ? 255 : (b < 0 ? 0 : b);
		return new java.awt.Color(r, g, b);
	}
	public static int getIntfromYUV(byte y, byte u, byte v) {
		int r = (298 * ((y & 0xFF) - 16) + 409 * ((v & 0xFF) - 128) + 128) >> 8;
		int g = (298 * ((y & 0xFF) - 16) - 100 * ((u & 0xFF) - 128) - 208 * ((v & 0xFF) - 128) + 128) >> 8;
		int b = (298 * ((y & 0xFF) - 16) + 516 * ((u & 0xFF) - 128) + 128) >> 8;
		r = r > 255 ? 255 : (r < 0 ? 0 : r);
		g = g > 255 ? 255 : (g < 0 ? 0 : g);
		b = b > 255 ? 255 : (b < 0 ? 0 : b);
		return ((r << 16) | (g << 8) | b);
	}
}
