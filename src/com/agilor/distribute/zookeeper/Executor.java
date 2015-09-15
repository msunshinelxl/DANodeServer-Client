package com.agilor.distribute.zookeeper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import com.agilor.distribute.common.Interface;
import com.agilor.distribute.common.Interface.ZnodeGetDataCallback;
import com.agilor.distribute.common.NodeStateType;

public class Executor extends Thread implements Watcher,
		DataMonitor.DataMonitorListener {
//	static ReentrantReadWriteLock consistentHashLock;
//	static Node myNode;
//	static ConsistentHash nodeList;
	
//	public static void main(String[] args) {
//		String hostPort = "101.200.77.14:2181";
//		String zRootNode = "/agilorRootPath";
//		consistentHashLock = new ReentrantReadWriteLock(false);
//		myNode = new Node("0.0.0.0", null, 300, "node2", 1);
//		nodeList = new ConsistentHash(new MD5Hash());
//		// System.arraycopy(args, 3, exec, 0, exec.length);
//		try {
//
//			new Executor(hostPort, zRootNode + "/NodeInfo",
//					new ZnodeGetDataCallback() {
//
//						@Override
//						public void todo(Executor parent, byte[] data) {
//							// TODO Auto-generated method stub
//							String str_tmp = new String(data);
//							try {
//								JSONObject receiveJo = new JSONObject(str_tmp);
//								JSONArray receiveJa = receiveJo.getJSONArray("content");
//								TreeSet<Node> tmpTreeSet = new TreeSet<Node>();
//								
//								for (int i = 0; i < receiveJa.length(); i++) {
//									JSONObject tmpJo = receiveJa.getJSONObject(i);
//									tmpTreeSet.add(new Node(tmpJo
//											.getString("ip"), null, tmpJo
//											.getInt("virtualNum"), tmpJo
//											.getString("name"), tmpJo
//											.getInt("id")));
//								}
//								if (tmpTreeSet.contains(myNode)) {
//									// I'm not new node
//									System.out.println("I'm not new node");
//									//add node loop
//									System.out.println(tmpTreeSet);
//							        for(Iterator<Node> iter1 = tmpTreeSet.iterator(); iter1.hasNext(); ) {
//										consistentHashLock.readLock().lock();
//										Node tmpNode=iter1.next();
//										System.out.println(tmpNode.getName());
//										if(nodeList.getNodes().contains(tmpNode)==false){
//											//transfer data to new node
//											try {
//												parent.zk.create(zRootNode + "/NodeInfo/"+tmpNode.getName()+"/"
//														+ myNode.getName(),
//														"start transfer".getBytes(),
//														Ids.OPEN_ACL_UNSAFE,
//														CreateMode.PERSISTENT);
//												System.out.println(myNode.getName()+"-->"+tmpNode.getName());
//												sleep(1000);
//												parent.zk.delete(zRootNode + "/NodeInfo/"+tmpNode.getName()+"/"
//														+ myNode.getName(), -1);
//												System.out.println("delete :"+zRootNode + "/NodeInfo/"+tmpNode.getName()+"/"
//														+ myNode.getName());
//											} catch (KeeperException e) {
//												// TODO Auto-generated catch block
//												e.printStackTrace();
//											} catch (InterruptedException e) {
//												// TODO Auto-generated catch block
//												e.printStackTrace();
//											}
//										}
//										consistentHashLock.readLock().unlock();
//							        }
//								} else {
//									// I'm new node
//									System.out.println("I'm new node");
//									
//									
//									try {
//										//create client
//										Stat client= parent.zk.exists(zRootNode+"/ClientNodeInfo", false);
//										if(client==null){
//											
//											JSONObject tmpJo=new JSONObject();
//											JSONArray tmpJa=new JSONArray();
//											tmpJa.put(myNode.nodeToMap());
//											tmpJo.put(Constant.zkNodeClientFinalListName, tmpJa);
//											parent.zk.create(zRootNode + "/ClientNodeInfo",
//													tmpJo.toString().getBytes(),
//													Ids.OPEN_ACL_UNSAFE,
//													CreateMode.PERSISTENT);
//										}else{
//											JSONObject clientJO=ComFuncs.byte2Json(parent.zk.getData(zRootNode+"/ClientNodeInfo", null, client));
//											JSONArray tmpJa=null;
//											if(clientJO.has(Constant.zkNodeClientTmpListName)==false){
//												tmpJa=new JSONArray();												
//											}else{
//												tmpJa=clientJO.getJSONArray(Constant.zkNodeClientTmpListName);
//											}
//											tmpJa.put(myNode.nodeToMap());
//											clientJO.put(Constant.zkNodeClientTmpListName, tmpJa);
//											if(clientJO.has(Constant.zkNodeClientFinalListName)){
//												tmpJa=clientJO.getJSONArray(Constant.zkNodeClientFinalListName);
//												tmpJa.put(myNode.nodeToMap());
//												clientJO.put(Constant.zkNodeClientFinalListName, tmpJa);
//											}else{
//												System.out.println("error!");
//											}
//											parent.zk.setData(zRootNode+"/ClientNodeInfo", clientJO.toString().getBytes(), -1);
//											//parent.setData(clientJO.toString());
//										}
//										parent.zk.create(zRootNode + "/NodeInfo/"
//												+ myNode.getName(),
//												"addNode".getBytes(),
//												Ids.OPEN_ACL_UNSAFE,
//												CreateMode.PERSISTENT);
//										Iterator<Node> iter1 = tmpTreeSet
//												.iterator();
//										ReentrantReadWriteLock finishTrsCounterLock = new ReentrantReadWriteLock(false);
//										while (iter1.hasNext()) {
//											Node tmpNode=iter1.next();
//											if (tmpNode
//													.getName()
//													.compareTo(myNode.getName()) != 0) {
//												new Executor(
//														hostPort,
//														zRootNode
//																+ "/NodeInfo/"
//																+ myNode.getName()
//																+ "/"+tmpNode.getName(),
//														new ZnodeGetDataCallback() {
//
//															@Override
//															public void todo(
//																	Executor mine,
//																	byte[] data) {
//																// TODO
//																// Auto-generated
//																// method stub
//																System.out.println(new String(data));
//															}
//
//															@Override
//															public void onClose(Executor parent) {
//																// TODO Auto-generated method stub
//																parent.writeLockToDo(finishTrsCounterLock,new LockCallBackInterface() {
//																	
//																	@Override
//																	public void inDoing() {
//																		// TODO Auto-generated method stub
//																		parent.finishTrsCounter++;
////																		System.out.println("current counter:"+parent.finishTrsCounter+":"+tmpTreeSet.size());
//																		if(parent.finishTrsCounter==tmpTreeSet.size())
//																		{
//																			System.out.println("finish Transfer");
//																			try {
//																				parent.zk.delete(zRootNode + "/NodeInfo/"
//																							+ myNode.getName(), -1);
//																			} catch (InterruptedException e) {
//																				// TODO Auto-generated catch block
//																				e.printStackTrace();
//																			} catch (KeeperException e) {
//																				// TODO Auto-generated catch block
//																				e.printStackTrace();
//																			}
//																			try {
//																				JSONObject clientJO=ComFuncs.byte2Json(parent.zk.getData(zRootNode+"/ClientNodeInfo", null, client));
//																				JSONArray tmpJa=null;
//																				if(clientJO.has(Constant.zkNodeClientTmpListName)){
//																					tmpJa=clientJO.getJSONArray(Constant.zkNodeClientTmpListName);
//																					for(int i=0;i<tmpJa.length();i++){
//																						JSONObject tmpJo=(JSONObject) tmpJa.get(i);
//																						if(tmpJo.has("name")&&(tmpJo.getString("name")).compareTo(myNode.getName())==0 ){
//																							tmpJa.remove(i);
//																						}
//																					}
//																					clientJO.put(Constant.zkNodeClientTmpListName,tmpJa);
//																					parent.zk.setData(zRootNode+"/ClientNodeInfo", clientJO.toString().getBytes(), -1);
//																					System.out.println(Constant.zkNodeClientTmpListName+" modified");
//																				}else{
//																					System.out.println(Constant.zkNodeClientTmpListName+" dont exist");
//																				}
//																			} catch (KeeperException e) {
//																				// TODO Auto-generated catch block
//																				e.printStackTrace();
//																			} catch (InterruptedException e) {
//																				// TODO Auto-generated catch block
//																				e.printStackTrace();
//																			}
//																			
//																		}
//																	}
//																});
//															}
//														}).start();
//											}
//										}
//										parent.writeLockToDo(consistentHashLock,
//												new LockCallBackInterface() {
//
//													@Override
//													public void inDoing() {
//														Iterator<Node> iterator1 = tmpTreeSet
//																.iterator();
//														while (iterator1.hasNext()) {
//															nodeList.add(iterator1
//																	.next());
//														}
//														nodeList.add(myNode);
//														receiveJa.put(myNode.nodeToMap());
//														JSONObject jo_tmp = new JSONObject(
//																str_tmp);
//														jo_tmp.put("content", receiveJa);
//														parent.setData(receiveJo
//																.toString());
//													}
//												});
//									} catch (Exception e) {
//										// TODO Auto-generated catch block
//										e.printStackTrace();
//									}finally{
//										if(parent.zk!=null){
//											if(nodeList.getNodes().isEmpty()==false&& nodeList.getNodes().size()==1){
//												try {
//													parent.zk.delete(zRootNode + "/NodeInfo/"
//																+ myNode.getName(), -1);
//												} catch (InterruptedException e) {
//													// TODO Auto-generated catch block
//													e.printStackTrace();
//												} catch (KeeperException e) {
//													// TODO Auto-generated catch block
//													e.printStackTrace();
//												}	
//											}
//										}
//									}
//								}
//							} catch (JSONException e) {
//								System.out.println(e.toString());
//							}
//						}
//
//						@Override
//						public void onClose(Executor p) {
//							// TODO Auto-generated method stub
//
//						}
//					}).start();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}



	String znode;

	DataMonitor dm;

	public ZooKeeper zk;

	ZnodeGetDataCallback getdataCallback;
	
	public int finishTrsCounter=0;
	
	private AtomicReference<NodeStateType> stat;
	
	public Executor(String hostPort, String znode,
			ZnodeGetDataCallback getdataCallback) throws KeeperException,
			IOException {
		init(hostPort,znode);
		this.getdataCallback = getdataCallback;
		
	}
	
	public Executor(String hostPort, String znode) throws KeeperException,
	IOException {
		init(hostPort,znode);
		this.getdataCallback = null;
	}
	
	private void init(String hostPort, String znode)throws KeeperException,
	IOException{
		this.znode = znode;
		zk = new ZooKeeper(hostPort, 3000, this);
		dm = new DataMonitor(zk, znode, null, this);
		stat=new AtomicReference<NodeStateType>();
	}

//	public interface LockCallBackInterface {
//		public void inDoing();
//	}

//	private void readLockToDo(ReentrantReadWriteLock lock,
//			LockCallBackInterface todo) {
//		lock.readLock().lock();
//		try {
//			todo.inDoing();
//		} finally {
//			lock.readLock().unlock();
//		}
//	}
//
//	private void writeLockToDo(ReentrantReadWriteLock lock,
//			LockCallBackInterface todo) {
//		lock.writeLock().lock();
//		;
//		try {
//			todo.inDoing();
//		} finally {
//			lock.writeLock().unlock();
//		}
//	}

	public void run() {
		try {
			synchronized (this) {
				stat.set(NodeStateType.IS_ALAVE);
				while (!dm.dead) {
					wait();
				}
				System.out.println(znode + "  died");
				zk.close();
				if(getdataCallback!=null)
					getdataCallback.onClose(this);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setGetdataCallback(ZnodeGetDataCallback callback){
		getdataCallback=callback;
	}
	
	@Override
	public void process(WatchedEvent event) {
		// TODO Auto-generated method stub
		dm.process(event);
	}

	@Override
	public void exists(byte[] data) {
		// TODO Auto-generated method stub
		if (data == null) {
			String str_tmp = new String(data);
			System.out.print(str_tmp);

		} else {
			if(getdataCallback!=null)
				getdataCallback.todo(this, data);
		}
	}

	@Override
	public void closing(int rc) {
		// TODO Auto-generated method stub
		synchronized (this) {
			notifyAll();
		}
	}

	public void setData(String data) {
		try {
			zk.setData(znode, data.getBytes(), -1);
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createTmpDict() {
		try {
			zk.create(znode, "{}".getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
