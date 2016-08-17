package com.baidu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class baiduCounter {
	
	static public Queue<String> top100company = new ConcurrentLinkedQueue<String>();
	static public HashMap<String, Float> top100companyResult = new HashMap<>();
	static public HashMap<String, Float> tradeResult = new HashMap<>();
	static public Queue<oneResult> combinedResult = new ConcurrentLinkedQueue<oneResult>();
	static public Node root = new Node();
	
	static int threadCount = 10;	//number of crawlers

	private void crawling(Queue<String> toSearch, Queue<oneResult> toGet) {
		try{
			ExecutorService pool = Executors.newFixedThreadPool(threadCount);
			Count ctn = new Count(toSearch.size());
			Object o = new Object();
			
			for(int i = 0;i < threadCount;i++){  
	            Thread t = new processThread(toSearch, o, ctn, toGet, i);  
	            pool.execute(t);  
	        }
			synchronized(o){  
	            if((!pool.isTerminated() && ctn.i!=0) || toSearch.isEmpty() ){  
	                o.wait(1000*60);  
	            }  
	        }
			pool.shutdown();
		}
		catch(InterruptedException e){  
            e.printStackTrace();  
        }
    }
	
	private void read100Company() throws IOException{
		/**********************读入前100个公司名字**********************/
		/**********************公司名字进入待爬取队列**********************/
		/**********************公司-结果对进入HashMap**********************/
		/**********************一级行业-结果对进入HashMap**********************/
		Map<String, Float> cast = new HashMap<>();
		
		String s = new String();
		BufferedReader br = new BufferedReader(new FileReader( new File("toSearch/corp_keyword_avg_score.txt")));
		while ((s=br.readLine()) != null) {
			String[] text = s.split("\t");
			if(text[0].equals("keyword"))continue;
			cast.put(text[0], Float.parseFloat(text[1]));
		}
		br.close();
		
		ArrayList<Map.Entry<String, Float>> list = new ArrayList<Map.Entry<String, Float>>(cast.entrySet());  
        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {  
            //降序排序  
            @Override  
            public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {  
                //return o1.getValue().compareTo(o2.getValue());  
                return o2.getValue().compareTo(o1.getValue());  
            }  
        });  
        
        for(Map.Entry<String, Float> mapping : list) {
            top100company.add(mapping.getKey());
            if(top100company.size() == 100)break;
        }
        
        s = new String();
		br = new BufferedReader(new FileReader( new File("results/top100company_result.txt")));
		while ((s=br.readLine()) != null) {
			String[] text = s.split("\t");
			top100companyResult.put(text[0], Float.parseFloat(text[1]));
		}
		br.close();
		
		s = new String();
		br = new BufferedReader(new FileReader( new File("results/trade_result.txt")));
		while ((s=br.readLine()) != null) {
			String[] text = s.split("\t");
			tradeResult.put(text[0], Float.parseFloat(text[1]));
		}
		br.close();
	}
	
	private void readTradeNames() throws NumberFormatException, IOException{
		/**********************构建行业层级树**********************/
		ArrayList<Node> readData = new ArrayList<Node>();
		ArrayList<String> tmp = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader( new File("toSearch/new_trade_file.txt")));
		String s = new String();
		while ((s=br.readLine()) != null) {
			String[] text = s.split("\t");
			int a = Integer.parseInt(text[0]);
			int b = Integer.parseInt(text[1]);
			int c = Integer.parseInt(text[2]);
			
			tmp = new ArrayList<String>();
			if(a != 0){
				String[] words = text[4].split(":");
				for(int i = 0;i < words.length;i++)
					tmp.add(words[i]);
				
				Node t = new Node(a, b, c, text[3], tmp);
				readData.add(t);
			}
			else{
				Node t = new Node(a, b, 0, text[3], null);
				readData.add(t);
			}
		}
		br.close();
		//System.out.println(readData.get(9).keywords);
		
		for(int i = 0;i <= 4;i++)
			for(int j = 0;j < readData.size();j++)
				if(i == readData.get(j).level){
					if(i == 0){
						root = readData.get(j);
						root.father = null;
						break;
					}
					//findFather(readData.get(j));
					Node node = readData.get(j);
					if(node.level == 1){
						root.children.add(node);
						continue;
					}
					
					for(int k = 0;k < readData.size();k++){
						Node one = readData.get(k);
						if(node.level == one.level+1 && node.fatherIndex == one.number){
							one.children.add(node);
							node.father = one;
							break;
						}
					}	
				}
		
	}
	
	private void writeInFile(Queue<oneResult> combinedResult, String path, boolean continueToWrite) throws IOException{
		PrintWriter pw = new PrintWriter(new FileWriter(new File(path)));
        while(!combinedResult.isEmpty()){
        	oneResult one = combinedResult.poll();
        	pw.println(one.name+"\t"+one.keyword+"\t"+one.res+"\t"+one.factor+"\t"+one.keyword2+"\t"+one.res2+"\t"+one.factor2);
        }
        pw.close();
	}
	
	public static void main(String[] args) throws IOException {
		baiduCounter bdcounter = new baiduCounter();
		bdcounter.readTradeNames();
		bdcounter.read100Company();
		bdcounter.crawling(top100company, combinedResult);
		//bdcounter.writeInFile(combinedResult, "results/final_result.txt", false);
	}

}
