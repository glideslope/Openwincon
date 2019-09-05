package Util;

public class MyTable extends javax.swing.table.AbstractTableModel {

	private int w;

	private java.util.Vector<String> data;

	private String[] columnNames;

	public MyTable(String[] columnNames) {
		data = new java.util.Vector<String>();
		this.columnNames = columnNames;
		w = columnNames.length;
	}

	public int getRowCount() {
		return data.size() / w;
	}

	public int getColumnCount() {
		return w;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		int pos = rowIndex * w + columnIndex;
		if (pos < data.size()) {
			return data.get(pos);
		}
		else {
			return null;
		}
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public void add(String element) {
		data.add(" " + element.trim());
	}

	public void remove() {
		data.removeAllElements();
	}
}