package com.baidu;

import java.util.ArrayList;

public class Node {
	int level;
	int fatherIndex;
	//int childrenNum;
	int number;
	String trade;
	ArrayList<String> keywords = new ArrayList<String>();
	ArrayList<Node> children = new ArrayList<Node>();
	Node father;
	
	//int searchNum = 0;
	
	public Node(int a, int b, int c, String trade, ArrayList<String> keywords){
		this.number = a;
		this.fatherIndex = b;
		this.level = c;
		this.trade = trade;
		this.keywords = keywords;
	}
	
	public Node(){}
}
