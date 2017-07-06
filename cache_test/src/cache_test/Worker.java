package cache_test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

public class Worker {
	private String serverId = getLocalIP();
	private String name;
	private ZooKeeper zk;
	Process child = null;
	private String dir;

	public void setDir(String dir) {
		this.dir = dir;
	}

	public boolean register() {
		name = "worker-" + serverId;
		try {
			zk.create("/assigned/" + name, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (Exception e) {
		}
		try {
			zk.create("/workers/" + name, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			return true;
		} catch (Exception e) {
		}

		return false;
	}

	public void waitToBeWorker() {
		while (true) {
			if (register()) {
				System.out.println("i am worker");
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}

	public static String getLocalIP() {
		String ip = "";
		try {
			Enumeration<?> e1 = (Enumeration<?>) NetworkInterface.getNetworkInterfaces();
			while (e1.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) e1.nextElement();
				if (!ni.getName().equals("eth0")) {
					continue;
				} else {
					Enumeration<?> e2 = ni.getInetAddresses();
					while (e2.hasMoreElements()) {
						InetAddress ia = (InetAddress) e2.nextElement();
						if (ia instanceof Inet6Address)
							continue;
						ip = ia.getHostAddress();
					}
					break;
				}
			}
		} catch (Exception e) {
		}
		return ip;
	}

	public void startZK(String hostPort) {
		try {
			zk = new ZooKeeper(hostPort, 15000, null);
		} catch (Exception e) {
		}
	}

	public void getTask() {
		while (true) {
			try {
				List<String> tasks = zk.getChildren("/assigned/" + name, false);
				for (String task : tasks) {
					byte[] data = zk.getData("/assigned/" + name + "/" + task, false, new Stat());
					String meta = new String(data);
					String[] metaList = meta.split("\t");
					String[] workerList = metaList[1].split(";");
					int duration = Integer.parseInt(metaList[0]);
					int index = -1;
					for (int i = 0; i < workerList.length; i++) {
						if (workerList[i].equals(name)) {
							index = i;
						}
					}
					if (workerList[0].equals(name)) {
						try {
							zk.create("/status/" + task, "0".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
						} catch (Exception e) {
							zk.setData("/status/" + task, "0".getBytes(), -1);
						}
					} else {
						while (zk.exists("/status/" + task, false) == null) {
							Thread.sleep(1000);
						}
						while (Integer.parseInt(new String(zk.getData("/status/" + task, false, new Stat()))) != index
								- 1) {
							Thread.sleep(1000);
						}
						zk.setData("/status/" + task, ("" + index).getBytes(), -1);
					}
					while (Integer.parseInt(
							new String(zk.getData("/status/" + task, false, new Stat()))) != (workerList.length - 1)) {
						Thread.sleep(1000);
					}
					try {
						child = Runtime.getRuntime().exec(
								"tsar -l -i 5 -D --nginx --nginx_code --cpu --mem -s accept,qps,rt,200,206,302,403,404,499,500,502,504,util");
						String filePath = this.dir + "/output." + task;
						long startTime = System.currentTimeMillis();
						BufferedReader br = new BufferedReader(new InputStreamReader(child.getInputStream()));
						BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true));
						String line = br.readLine();
						while (System.currentTimeMillis() - startTime < (duration) * 1000) {
							Thread.sleep(1000);
							line = br.readLine();
							bw.write(line + "\n");
							bw.flush();
							System.out.println("task output : " + line);
						}
						bw.close();
						child.destroy();
						if (workerList[0].equals(name)) {
							try {
								zk.delete("/tasks/" + task, -1);
							} catch (Exception e) {
							}
							try {
								zk.delete("/status/" + task, -1);
							} catch (Exception e) {
							}
						}
						try {
						zk.delete("/assigned/" + name + "/" + task, -1);
						} catch(Exception e) {
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
					}
				}
			} catch (Exception e1) {
			}

			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}

	public static void main(String args[]) {
		Worker w = new Worker();
		w.startZK(args[0]);
		w.setDir(args[1]);
		w.waitToBeWorker();
		w.getTask();
	}
}
