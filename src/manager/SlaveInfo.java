package manager;

import java.io.Serializable;
import java.net.InetAddress;

public class SlaveInfo implements Serializable  {
	private int listenPort;
	private InetAddress ip;
	private NodeStatus nodeStatus;
	private String nodeName;
	
	public SlaveInfo(int port, InetAddress ip) {
		this.listenPort = port;
		this.ip = ip;
		this.nodeStatus = NodeStatus.TERMINATED;
		this.nodeName = "";
	}
	
	public void setNodeStatus(NodeStatus ns) {
		this.nodeStatus = ns;
	}
	
	public NodeStatus getNodeStatus () {
		return this.nodeStatus;
	}
	
	public void setNodeName(String name) {
		this.nodeName = name;
	}
	
	public String getNodeName() {
		return this.nodeName;
	}
	
	public int getListenPort() {
		return this.listenPort;
	}
	
	public InetAddress getIP() {
		return this.ip;
	}
}
