package mybeans;

import java.awt.Graphics;
import java.beans.*;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkInterfaces extends javax.swing.JPanel implements Serializable {

    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private PropertyChangeSupport propertySupport; 

    public String getSampleProperty() {
        return sampleProperty;
    }

    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
    MyTable table_model;   
    javax.swing.JTable table;
    javax.swing.JScrollPane table_panel;    
    CustomTableCellRenderer renderer;   
    
    String[] title_list = {"Check", "Display name", "Name", "InetAddress", "Up", "Loopback", "Virtual", "MTU"};     
    int[] width_list = {30, 110, 30, 100, 30, 30, 30, 30};  
    final int blink_duration = 30;  
    
    public NetworkInterfaces() {
        super();
        propertySupport = new PropertyChangeSupport(this);
        table_model = new MyTable(title_list);
        table = new javax.swing.JTable(table_model);
        table.setBorder(
                javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createTitledBorder(""),
                        javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        table.setRowHeight(22);
        table.setRowMargin(5);
        renderer = new CustomTableCellRenderer();
        try {
            table.setDefaultRenderer(Class.forName("java.lang.String"), renderer);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        javax.swing.table.TableColumn column = null;
        for (int i = 0; i < width_list.length; i++) {
            column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int) (width_list[i] * 1.2));
        }
        captureInterface();
        table_panel = new javax.swing.JScrollPane(table);
        this.add(table_panel);
    }

    public void blink(String interface_name) {
        for (int i = 0; i < table_model.getRowCount(); i++) {
            renderer.setSelectedText(interface_name);
            table.repaint();
            try {
                Thread.sleep(blink_duration);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            renderer.noSelectedText();
            table.repaint();
        }
    }
    
    public void captureInterface() {
        table_model.remove();
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            Logger.getLogger(NetworkInterfaces.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (NetworkInterface netint : Collections.list(nets)) {

            displayInterfaceInformation(netint);

        }
        repaint();
    }
    
    public void displayInterfaceInformation(NetworkInterface netint) {
        if (!netint.getInetAddresses().hasMoreElements()) {
            return;
        }

        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        boolean testa = false;

        table_model.add(new Boolean(false));       
        table_model.add(netint.getDisplayName());
        table_model.add(netint.getName());

        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            table_model.add(inetAddress.toString());
            testa = true;
            break;
        }
        if (testa == false) {
            table_model.add("no ip");
        }
        try {
            table_model.add(Boolean.valueOf(netint.isUp()));
            table_model.add(Boolean.valueOf(netint.isLoopback()));
            table_model.add(Boolean.valueOf(netint.isVirtual()));
            table_model.add(netint.getMTU());

        } catch (SocketException ex) {
            Logger.getLogger(NetworkInterfaces.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    protected void paintComponent(Graphics g) {
        table_panel.setPreferredSize(new java.awt.Dimension(this.getWidth(), this.getHeight() - 5));
        table_panel.repaint();
    }

    public NetworkInterface[] getSelectedInterfaces() {
        int total = 0;
        for (int i = 0; i < table_model.getRowCount(); i++) {
            if (table_model.getValueAt(i, 0).equals(new Boolean(true))) {
                total++;
            }
        }
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            Logger.getLogger(NetworkInterfaces.class.getName()).log(Level.SEVERE, null, ex);
        }
        int index = 0;
        int real_index = 0;
        NetworkInterface[] result = new NetworkInterface[total];

        ArrayList array = Collections.list(nets);       

        for (index = 0; index < table_model.getRowCount(); index++) {
            System.out.println(table_model.getValueAt(index, 3));
            if (table_model.getValueAt(index, 0).equals(new Boolean(true))) {
                for (int i = 0; i < array.size(); i++) {
                    if (((NetworkInterface) array.get(i)).getInetAddresses().hasMoreElements()) {
                        String temp = ((NetworkInterface) array.get(i)).getInetAddresses().nextElement().toString();
                        if (table_model.getValueAt(index, 3).equals(temp)) {
                            result[real_index++] = ((NetworkInterface) array.get(i));
                        }
                    }
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {
        javax.swing.JFrame f = new javax.swing.JFrame();
        NetworkInterfaces nf = new NetworkInterfaces();
        nf.setPreferredSize(new java.awt.Dimension(500, 100));
        f.add(nf);
        f.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        f.setSize(500, 400);
        f.setVisible(true);
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(NetworkInterfaces.class.getName()).log(Level.SEVERE, null, ex);
            }
            nf.blink("/127.0.0.1");
        }

    }
}