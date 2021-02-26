package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;
import java.util.Iterator;

import net.floodlightcontroller.packet.MACAddress;
import java.lang.Thread;
import edu.wisc.cs.sdn.vnet.Iface;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device {

	Map<MACAddress, Iface> switchTable = new HashMap<>();
	// mac address to start time mapping, used to check if switch table entry is
	// valid
	Map<MACAddress, Long> ttlMap = new HashMap<>();
	static long TIMEOUT = 15000; // time for mac address to live in ms
    Thread thread;
	ReentrantLock lock = new ReentrantLock(); // lock to control access to switch table and ttl map

	private class SwitchRunnable implements Runnable {
		
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			// removed expired entries from switch table and then sleep for 1 second and
			// repeat the process
			while (true) {
				try {
					lock.lock();
					Iterator<Entry<MACAddress, Long>> it = ttlMap.entrySet().iterator();
					while(it.hasNext()) {
						Map.Entry<MACAddress, Long> entry = it.next();
						if( System.currentTimeMillis() - entry.getValue() > TIMEOUT) {
							switchTable.remove(entry.getKey());
							System.out.println("removed MACAddress : " + entry.getKey().toString());
							it.remove();
						}
					}
					
				} finally {
					lock.unlock();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * Creates a router for a specific host.
	 * 
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile) {
		super(host, logfile);
		Runnable runnable = new SwitchRunnable();
        thread = new Thread(runnable);
        thread.start();
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * 
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface     the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface) {
		System.out.println("*** -> Received packet: " + etherPacket.toString().replace("\n", "\n\t"));

		/********************************************************************/
		/* TODO: Handle packets */
        
		MACAddress source = etherPacket.getSourceMAC();
		MACAddress dest = etherPacket.getDestinationMAC();
		// adds/updates switch table upon revieval of packet
		try {
			lock.lock();
			switchTable.put(source, inIface);
			ttlMap.put(source, System.currentTimeMillis());
		} finally {
			lock.unlock();
		}
		Map<String, Iface> interfaces = getInterfaces();
		if (switchTable.containsKey(dest)) {
			sendPacket(etherPacket, switchTable.get(dest));
		} else {
			for (Map.Entry<String, Iface> entry : interfaces.entrySet()) {
				if (!entry.getValue().getName().equals(inIface.getName())) {
					sendPacket(etherPacket, entry.getValue());
				}
			}
		}

	}

}