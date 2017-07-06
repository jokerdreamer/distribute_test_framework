package cache_test;
import java.io.IOException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class Client {
	ZooKeeper zk;
	String hostPort;
	String task;
	boolean submitOk = false;
	Process child;
	void startZK(String hostPort) throws IOException {
		zk = new ZooKeeper(hostPort, 15000, null);
	}	
	
	void waitUntil() {
    	String path = "/tasks/" + task;
    	while(true) {
    		try {
				if(zk.exists(path, false) == null) {
				    child.destroy();
				    return;
				} else {
					Thread.sleep(1000);
				}
			} catch (Exception e) {
			}
    	}
	}
    void submitTask(String task, String data) {
    	this.task = task;
    	String path = "/tasks/" + task;
		try {
			zk.create("/tasks_client/" + task, data.getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
			zk.create("/tasks/" + task, data.getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
		try {
		    if(zk.exists(path, false) != null) {
		    	this.submitOk = true;
		    }
		} catch (Exception e) {
		}
	}
    
    void runCmd(String cmd) {
    	try {
			child = Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {
		}
    }
	
    void waitOk(){
    	while(true) {
    	    try {
				if(zk.exists("/status/" + this.task, false) != null) {
					return;
				} else {
					Thread.sleep(1000);
				}
			} catch (Exception e) {
			}
    	}
    }
    public static void main(String args[]) {
    	//host:port task script duration url concurency thread workerlist script
    	Client c = new Client();
    	try {
			c.startZK(args[0]);
		} catch (Exception e) {
		}
    	c.submitTask(args[1], args[3] + "\t" + args[7]);
    	if(c.submitOk == false) {
    		System.exit(-1);
    	}
    	try {
			c.waitOk();
		} catch (Exception e) {
		}
    	
    	c.runCmd(String.format("./wrk/wrk -d %s -c %s -t %s -s %s %s", args[3], args[5], args[6], args[8], args[4]));
    	c.waitUntil();
    }
}
