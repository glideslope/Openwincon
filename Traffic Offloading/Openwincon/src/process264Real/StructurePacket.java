package process264Real;

import FEC.common.TypeFEC;
import NetworkCost.CostModel;

public class StructurePacket implements java.io.Serializable {
    public int packet_index;        
    public int gop_index;      
    public int k; 
    public int t; 
    public int symbol_size;
    public String destination_address;  
    public long time;          
    public int fountain_id_start;       
    public byte[] payload;     
    TypePacket pType;  
    TypeFEC fType;      
    public double plr;      
    public int num_addresses;      
    public double[] bandwidth;
    public int path_index;      

    CostModel[] costModel;     
}
