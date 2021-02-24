package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.Map;
import java.util.HashMap;
import net.floodlightcontroller.packet.MACAddress;
import java.lang.Thread;
import edu.wisc.cs.sdn.vnet.Iface;
/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	// switch table mapping mac address to interface
	Map<MACAddress, Iface> switchTable = new HashMap<>();
	// mac address to start time mapping, used to check if switch table entry is valid
	Map<MACAddress, Long> ttlMap = new HashMap<>();
	static long TIMEOUT = 15000; // time for mac address to live in ms
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
        MACAddress source = etherPacket.getSourceMAC();
		MACAddress dest = etherPacket.getDestinationMAC();
        switchTable.put(source, inIface);
		/*
        Runnable runnable = new Runnable(){
			public void run(){
				try {
					while(true){
						Thread.sleep(1000); // run every 1 second
						// searches through ttlMap and discards expired entries in the switch table
                        for(Map.Entry<MacAddress, Long> entry : ttlMap.entrySet()){
							long currentTime = System.currentTimeMillis();
							if(currentTime - entry.getValue() > TIMEOUT){
								switchTable.remove(entry.getKey());
								ttlMap.remove(entry.getKey());
							}
						}
					}
				} catch (Exception e) {
					//TODO: handle exception
				}
			}
		}
		
		// thread to check expired entries in switch table
		Thread updater = new Thread(runnable);
        updater.start();
        */

        Iface toInterface = null;
		
		Map<String,Iface> interfaces = getInterfaces();
		
        // line 41 - 47 : gets the interface to send the packet out of
		for (Object name : interfaces.keySet()){
			Iface interface = getInterface(name);
			if (dest.compareTo(interface.getMacAddress()) == 0){
				toInterface = interface;
				break;
			}
		}
        
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                             */
		
		/********************************************************************/
	}
}
