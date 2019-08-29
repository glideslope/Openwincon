package mybeans;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

class MyTable extends javax.swing.table.AbstractTableModel {

    private java.util.Vector<Object> data;
    private String[] column_names;
    private int column_length;
    public MyTable(String[] column_names) {
        data = new java.util.Vector<Object>();
        this.column_names = column_names;
        column_length = column_names.length;
    }
    public int getRowCount() {
        return data.size() / column_length;
    }
    public int getColumnCount() {
        return column_length;
    }
    public Object getValueAt(int rowIndex, int columnIndex) {
        int position = rowIndex * column_length + columnIndex;
        if (position < data.size()) {
            return data.get(position);
        } else {
            return null;
        }
    }
    @Override
    public String getColumnName(int col) {
        return column_names[col];
    }
    public void add(Object element) {
        data.add(element);
    }
    public void remove() {
        data.removeAllElements();
    }
    @Override
    public boolean isCellEditable(int row, int col) {
        if (col < 1) {
            return true;
        } else {
            return false;
        }
    }
    @Override
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }
    @Override
    public void setValueAt(Object value, int row, int col) {
        data.set(row * column_length + col, value);
        fireTableCellUpdated(row, col);
    }
}
class CustomTableCellRenderer extends DefaultTableCellRenderer {
    String no_exist = "tmp1993";
    String selected_text = no_exist;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof String) {
            String text = (String) value;
            if (text.equals(selected_text)) {
                cell.setBackground(Color.gray);
            } else {
                if (isSelected) {
                    cell.setForeground(table.getSelectionForeground());
                    cell.setBackground(table.getSelectionBackground());
                } else {
                    cell.setForeground(table.getForeground());
                    cell.setBackground(table.getBackground());
                }
            }
        }
        return cell;
    }
    public void setSelectedText(String selected_text) {
        this.selected_text = selected_text;
    }
    public void noSelectedText() {
        selected_text = no_exist;
    }
}
