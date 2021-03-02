package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;

import java.util.Map;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device {
	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	/**
	 * Creates a router for a specific host.
	 * 
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile) {
		super(host, logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable() {
		return this.routeTable;
	}

	/**
	 * Load a new routing table from a file.
	 * 
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile) {
		if (!routeTable.load(routeTableFile, this)) {
			System.err.println("Error setting up routing table from file " + routeTableFile);
			System.exit(1);
		}

		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}

	/**
	 * Load a new ARP cache from a file.
	 * 
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile) {
		if (!arpCache.load(arpCacheFile)) {
			System.err.println("Error setting up ARP cache from file " + arpCacheFile);
			System.exit(1);
		}

		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * 
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface     the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface) {
		System.out.println("*** -> Received packet: " + etherPacket.toString().replace("\n", "\n\t"));
		if (etherPacket.getEtherType() == Ethernet.TYPE_IPv4) {
			IPv4 pkt = (IPv4) etherPacket.getPayload();
			short receivedChecksum = pkt.getChecksum();
			pkt.setChecksum((short) 0);
			byte[] buffer = pkt.serialize();
			IPv4 payload = (IPv4) pkt.deserialize(buffer, 0, pkt.getTotalLength());

			if(receivedChecksum == payload.getChecksum()) {
				pkt.setTtl((byte)(pkt.getTtl()-1));
				if (pkt.getTtl() == 0) return; // drop packet

				Map<String,Iface> rtInterfaces = this.getInterfaces();

				for (Map.Entry<String, Iface> entry: rtInterfaces.entrySet()){
					if (entry.getValue().getIpAddress() == pkt.getDestinationAddress()){
						System.out.println("Packet dropped. destination address matched one of the router's interfaces' IP address");
						return;
					}
				}

				int destAddr = pkt.getDestinationAddress();
				RouteEntry nextHop = routeTable.lookup(destAddr);
				if (nextHop == null) return; // no route found
				// System.out.println("Router.java : handlePacket():  " + "next hop address: " + nextHop.getInterface().getIpAddress());

 				int nextHopIP = nextHop.getGatewayAddress();
                System.out.println("Router.java: handlePacket(): next hop ip address: " + IPv4.fromIPv4Address(nextHopIP));				
				ArpEntry nextHopEntry = arpCache.lookup((nextHopIP == 0) ? pkt.getDestinationAddress() : nextHopIP);
                        
				MACAddress nextHopMAC = nextHopEntry.getMac();
				System.out.println("Router.java : handlePacket(): mac address of next hop" + nextHopMAC.toString());
				Ethernet newPacket = new Ethernet();
				pkt.setChecksum((short) 0);
				newPacket.setEtherType(Ethernet.TYPE_IPv4);
				newPacket.setPayload(pkt);
				newPacket.setDestinationMACAddress(nextHopMAC.toBytes());
				newPacket.setSourceMACAddress(nextHop.getInterface().getMacAddress().toBytes());
				
				sendPacket(newPacket, nextHop.getInterface());
				System.out.println("*** -> Sent packet: " + newPacket.toString().replace("\n", "\n\t"));


			}


			
		}
		/********************************************************************/
		/* TODO: Handle packets */

		/********************************************************************/
	}
}
