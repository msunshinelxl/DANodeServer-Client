package com.agilor.distribute.common;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.agilor.distribute.common.Interface.ConsistentHashVirtualNodeTravel;
import com.agilor.distribute.consistenthash.Node;
public class ComFuncs {
	public static JSONObject byte2Json(byte[] inputData){
		String tmpString=new String(inputData);
		JSONObject res=new JSONObject(tmpString);
		return res;
	}
	
	public static List<String> getAll(Node node,int index)
	{
		List<String>res=new ArrayList<String>();
		travelInConsistentHash(node,new ConsistentHashVirtualNodeTravel(){

			@Override
			public void inFor(String vName) {
				// TODO Auto-generated method stub
				res.add(vName);
			}
			
		});
		return res;
	}
	
	public static void travelInConsistentHash(Node node,ConsistentHashVirtualNodeTravel indoing){
		for (int i = 0; i < node.getVirtualNum(); i++) {
			String id = node.getIp()+"#"+i;
			indoing.inFor(id);

		}
	}
}
