package cache_test;

import java.io.IOException;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Master {
	private ZooKeeper zk;
	
	void startZK(String hostPort) throws IOException {
		zk = new ZooKeeper(hostPort, 15000, null);
	}
	
	void doTask() {
		while (true) {
			try {
				List <String> tasks = zk.getChildren("/tasks", false);
				for(String task : tasks) {
					if(zk.exists("/tasks_client/" + task, false) != null) {
						//assign
						byte[] data = zk.getData("/tasks/" + task, false, new Stat());
						String workerStr = new String(data).split("\t")[1];
						String[] workerList = workerStr.split(";");
						for(String worker : workerList) {
							try {
							    zk.create("/assigned/" + worker + "/" + task, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
							} catch(Exception e) {
							}
						}
					} else {
						zk.delete("/tasks/" + task, -1);
						continue;
					}
				}
			} catch (Exception e) {
			}
			
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}
	
	public boolean runForMaster() {
		try {
			zk.create("/master", "".getBytes(),Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
			return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	public void waitToBeMaster() {
		while(true) {
			if(runForMaster()) {
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}
	
	public void bootstrap() {
		createParent("/workers", new byte[0]);
		createParent("/tasks", new byte[0]);
		createParent("/status", new byte[0]);
		createParent("/tasks_client", new byte[0]);
		createParent("/assigned", new byte[0]);
	}
	
	void createParent(String path, byte[] data) {
		try {
			zk.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (Exception e) {
		}
	}
	
	public static void main(String args[]) throws Exception {
		Master m = new Master();
		m.startZK(args[0]);
		m.waitToBeMaster();
		System.out.println("i am master");
		m.bootstrap();
		m.doTask();
	}
	
}
