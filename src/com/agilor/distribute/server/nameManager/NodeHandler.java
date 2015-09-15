package com.agilor.distribute.server.nameManager;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import agilor.distributed.storage.inter.jlient.Agilor;

import com.agilor.distribute.common.ComFuncs;
import com.agilor.distribute.common.Constant;
import com.agilor.distribute.common.Interface.ZnodeGetDataCallback;
import com.agilor.distribute.consistenthash.ConsistentHash;
import com.agilor.distribute.consistenthash.MD5Hash;
import com.agilor.distribute.consistenthash.Node;
import com.agilor.distribute.zookeeper.Executor;

public class NodeHandler {
	String znodeServerInfo;
	String znodeClientInfo;
	String hostPort;
	Executor taskExecutor;
	ZnodeGetDataCallback taskCallback;
	Node myNode;
	ConsistentHash nodeList;
//	ReentrantReadWriteLock consistentHashLock;

	public NodeHandler(String hostPort, String znodeServerInfo,
			String znodeClientInfo, Node myNode) {
		this.hostPort = hostPort;
		this.znodeServerInfo = znodeServerInfo;
		this.znodeClientInfo = znodeClientInfo;
		this.myNode = myNode;
		taskCallback = null;
//		consistentHashLock = new ReentrantReadWriteLock(false);
		nodeList = new ConsistentHash(new MD5Hash());
		try {
			taskExecutor = new Executor(hostPort, Constant.zRootNode
					+ znodeServerInfo);
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			System.err.println("taskExecutor in NodeHandler error");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("taskExecutor in NodeHandler error");
			e.printStackTrace();
		}
	}

	public void stratHandler() {
		taskCallback = new ZnodeGetDataCallback() {

			@Override
			public void todo(Executor parent, byte[] data) {
				// TODO Auto-generated method stub
				String str_tmp = new String(data);
				try {
					JSONObject receiveJo = new JSONObject(str_tmp);
					JSONArray receiveJa = receiveJo.getJSONArray("content");
					TreeSet<Node> tmpTreeSet = new TreeSet<Node>();

					for (int i = 0; i < receiveJa.length(); i++) {
						JSONObject tmpJo = receiveJa.getJSONObject(i);
						tmpTreeSet
								.add(new Node(tmpJo.getString("ip"),
										tmpJo.getInt("virtualNum"), tmpJo
												.getString("name"), tmpJo
												.getInt("id")));
					}
					Iterator<Node> iterator1 = tmpTreeSet.iterator();
					if(nodeList.getNodes()==null){
						//first init
						System.out.println("first init");
						while (iterator1.hasNext()) {
							Node tmpItem=iterator1.next();
							nodeList.add(tmpItem);
//							System.out.println(tmpItem);
						}
					}
					if (tmpTreeSet.contains(myNode)) {
						// I'm not new node
						System.out.println("I'm not new node");
						for (Iterator<Node> iter1 = tmpTreeSet.iterator(); iter1
								.hasNext();) {
//							consistentHashLock.readLock().lock();
							Node tmpNode = iter1.next();
							System.out.println(myNode.getName());
							if (nodeList.getNodes().contains(tmpNode) == false) {
								// transfer data to new node
								try {
									parent.zk.create(
											Constant.zRootNode
													+ znodeServerInfo + "/"
													+ tmpNode.getName() + "/"
													+ myNode.getName(),
											"start transfer".getBytes(),
											Ids.OPEN_ACL_UNSAFE,
											CreateMode.PERSISTENT);


									parent.zk.delete(
											Constant.zRootNode
													+ znodeServerInfo + "/"
													+ tmpNode.getName() + "/"
													+ myNode.getName(), -1);
									System.out.println("delete :"
											+ Constant.zRootNode
											+ znodeServerInfo + "/"
											+ tmpNode.getName() + "/"
											+ myNode.getName());
								} catch (KeeperException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
//							consistentHashLock.readLock().unlock();
						}

					} else {
						try {
							// I'm new node
							System.out.println("I'm new node or delete node");
							// create client
							Stat client = parent.zk.exists(Constant.zRootNode
									+ znodeClientInfo, false);
							if (client == null) {
								JSONObject tmpJo = new JSONObject();
								JSONArray tmpJa = new JSONArray();
								tmpJa.put(myNode.nodeToMap());
								tmpJo.put(Constant.zkNodeClientFinalListName,
										tmpJa);
								parent.zk.create(Constant.zRootNode
										+ znodeClientInfo, tmpJo.toString()
										.getBytes(), Ids.OPEN_ACL_UNSAFE,
										CreateMode.PERSISTENT);
							} else {
								JSONObject clientJO = ComFuncs
										.byte2Json(parent.zk.getData(
												Constant.zRootNode
														+ znodeClientInfo,
												null, client));
								JSONArray tmpJa = null;
								if (clientJO
										.has(Constant.zkNodeClientTmpListName) == false) {
									tmpJa = new JSONArray();
								} else {
									tmpJa = clientJO
											.getJSONArray(Constant.zkNodeClientTmpListName);
								}
								tmpJa.put(myNode.nodeToMap());
								clientJO.put(Constant.zkNodeClientTmpListName,
										tmpJa);
								if (clientJO
										.has(Constant.zkNodeClientFinalListName)) {
									tmpJa = clientJO
											.getJSONArray(Constant.zkNodeClientFinalListName);
									tmpJa.put(myNode.nodeToMap());
									clientJO.put(
											Constant.zkNodeClientFinalListName,
											tmpJa);
								} else {
									System.out.println("error!");
								}
								parent.zk.setData(Constant.zRootNode
										+ znodeClientInfo, clientJO.toString()
										.getBytes(), -1);
								// parent.setData(clientJO.toString());
							}

							parent.zk.create(Constant.zRootNode
									+ znodeServerInfo + "/" + myNode.getName(),
									"addNode".getBytes(), Ids.OPEN_ACL_UNSAFE,
									CreateMode.PERSISTENT);
							Iterator<Node> iter1 = tmpTreeSet.iterator();
							ReentrantReadWriteLock finishTrsCounterLock = new ReentrantReadWriteLock(
									false);
							while (iter1.hasNext()) {
								Node tmpNode = iter1.next();
								if (tmpNode.getName().compareTo(
										myNode.getName()) != 0) {
									new Executor(hostPort, Constant.zRootNode
											+ znodeServerInfo + "/"
											+ myNode.getName() + "/"
											+ tmpNode.getName(),
											new ZnodeGetDataCallback() {

												@Override
												public void todo(Executor mine,
														byte[] data) {
													// TODO
													// Auto-generated
													// method stub
													System.out
															.println(new String(
																	data));
												}

												@Override
												public void onClose(
														Executor parent) {
													// TODO Auto-generated
													// method stub
													finishTrsCounterLock
															.writeLock().lock();
													parent.finishTrsCounter++;
													if (parent.finishTrsCounter == tmpTreeSet
															.size()) {
														System.out
																.println("finish Transfer");
														try {
															parent.zk
																	.delete(Constant.zRootNode
																			+ znodeServerInfo
																			+ "/"
																			+ myNode.getName(),
																			-1);
														} catch (InterruptedException e) {
															// TODO
															// Auto-generated
															// catch block
															e.printStackTrace();
														} catch (KeeperException e) {
															// TODO
															// Auto-generated
															// catch block
															e.printStackTrace();
														}
														try {
															JSONObject clientJO = ComFuncs
																	.byte2Json(parent.zk
																			.getData(
																					Constant.zRootNode
																							+ znodeClientInfo,
																					null,
																					client));
															JSONArray tmpJa = null;
															if (clientJO
																	.has(Constant.zkNodeClientTmpListName)) {
																tmpJa = clientJO
																		.getJSONArray(Constant.zkNodeClientTmpListName);
																for (int i = 0; i < tmpJa
																		.length(); i++) {
																	JSONObject tmpJo = (JSONObject) tmpJa
																			.get(i);
																	if (tmpJo
																			.has("name")&& (tmpJo
																					.getString("name"))
																					.compareTo(myNode.getName()) == 0) {
																		tmpJa.remove(i);
																	}
																}
																clientJO.put(
																		Constant.zkNodeClientTmpListName,
																		tmpJa);
																parent.zk
																		.setData(
																				Constant.zRootNode
																						+ znodeClientInfo,
																				clientJO.toString()
																						.getBytes(),
																				-1);
																System.out
																		.println(Constant.zkNodeClientTmpListName
																				+ " modified");
															} else {
																System.out
																		.println(Constant.zkNodeClientTmpListName
																				+ " dont exist");
															}
														} catch (KeeperException e) {
															// TODO
															// Auto-generated
															// catch block
															e.printStackTrace();
														} catch (InterruptedException e) {
															// TODO
															// Auto-generated
															// catch block
															e.printStackTrace();
														}

													}
													finishTrsCounterLock
															.writeLock()
															.unlock();
												}
											}).start();
								}
							}
//							consistentHashLock.writeLock().lock();
							nodeList.add(myNode);
							receiveJa.put(myNode.nodeToMap());
							JSONObject jo_tmp = new JSONObject(str_tmp);
							jo_tmp.put("content", receiveJa);
							parent.setData(receiveJo.toString());
//							consistentHashLock.writeLock().unlock();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							if (parent.zk != null) {
								if (nodeList.getNodes().isEmpty() == false
										&& nodeList.getNodes().size() == 1) {
									try {
										parent.zk.delete(Constant.zRootNode
												+ znodeServerInfo + "/"
												+ myNode.getName(), -1);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (KeeperException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						}
					}
				} catch (JSONException e) {
					System.out.println(e.toString());

				}
			}

			@Override
			public void onClose(Executor p) {
				// TODO Auto-generated method stub
			}
		};
		taskExecutor.setGetdataCallback(taskCallback);
		taskExecutor.start();
	}

	// public void startExecutor() {
	// taskExecutor.start();
	// }
	private void addTag2NewNode(){
		Agilor agilor=new Agilor();
		agilor.open();
//		agilor.getClient().GetTagInfo(tagName);
	}
}
