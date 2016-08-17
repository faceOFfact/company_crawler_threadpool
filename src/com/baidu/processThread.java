package com.baidu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class processThread extends Thread {  
	
	int timeBound = 3;
    
    private Queue<String> queue;  
    private Object o;  
    private Count size; 
    private Queue<oneResult> result;
    private baiduCounter bd;
	private int idx;
      
    public processThread(Queue<String> queue, Object o, Count size, Queue<oneResult> result, int idx){  
        this.queue = queue;    
        this.o = o;  
        this.size = size;
        this.result = result;
        this.idx = idx;
    }
    
    private int search(String keyword) {
    	String url = "http://www.baidu.com/s?pn=1&wd="+keyword;
        
        int total = -1;
	    try{
	    	//Document document = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; rv:5.0)").cookie("auth", "token").timeout(3000).get();
	    	//Document document = Jsoup.connect(url).timeout(10000).get();
	    	Connection conn = Jsoup.connect(url);
	    	conn.header("(Request-Line)", "POST /cgi-bin/login?lang=zh_CN HTTP/1.1");
			conn.header("Accept", "*/*");
			conn.header("Accept-Encoding", "gzip, deflate, sdch, br");
			conn.header("Accept-Language", "zh-CN,zh;q=0.8");
			conn.header("Connection", "keep-alive");
			conn.header("Host", "www.baidu.com");
			conn.header("is_xhr", "1");
			conn.header("Cache-Control", "no-cache");
			conn.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			conn.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
			conn.header("X-Requested-With", "XMLHttpRequest");
			Document document = conn.get();
			total = getBaiduSearchResultCount(document);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		//get number of results
	    return total;
	}
	
	private int getBaiduSearchResultCount(Document document){
        Elements ListDiv = document.getElementsByAttributeValue("class", "nums");
        String totalText = new String();
        for (Element element :ListDiv) {
            totalText = element.html();
        }
        
        String regEx="[^0-9]"; 
        int total = -1;
        Pattern pattern = Pattern.compile(regEx);
        try{
        	Matcher matcher = pattern.matcher(totalText);
        	totalText = matcher.replaceAll("");
        	//System.out.println(totalText);
            total = Integer.parseInt(totalText);
        }
        catch(Exception e){
        	System.err.println(e.toString());
        }
        
        return total;
    }
    
    @Override  
    public void run() {  
        try
        {  
            Thread.sleep(500);
        }
        catch (InterruptedException e) {  
            e.printStackTrace();  
        }
        
        PrintWriter pw = null;
        boolean isFirst = false;
        while(!queue.isEmpty()){  
            String companyName = queue.poll();
            
            try{
            	pw = new PrintWriter(new FileWriter(new File("results/part_result_"+idx+".txt"), isFirst));
            	isFirst = true;
            	ArrayList<Node> nodes = bd.root.children;
            	
    			float maxFactor = -1;
    			int res = 0;
    			String maxWord = "None";
    			String keyword = new String();
    			Node maxNode = new Node();
    			Node oneNode = new Node();
    			oneResult rst = new oneResult();
    			
    			for(int i = 0;i < nodes.size();i++){
    				oneNode = nodes.get(i);
    				ArrayList<String> keywords = oneNode.keywords;
    				
    				for(int j = 0;j < keywords.size();j++){
    					//System.out.println();
    					int times = 0;
    					keyword = keywords.get(j);
    					
    					res = search("intitle:"+companyName+" "+"intitle:"+keyword);
    					//System.out.println("searching "+companiesName.get(k)+" "+keyword);
    					while(res == -1 && times++ < timeBound){
    						Thread.sleep(1000);
    						System.out.println("searching "+companyName+" "+keyword+"----retry "+times);
    						res = search("intitle:"+companyName+" "+"intitle:"+keyword);
    					}
    					
    					if(res == -1)continue;
    					float factor = (float) ((float)res / bd.tradeResult.get(keyword) * Math.log(100000000/bd.top100companyResult.get(companyName)+1) / Math.log(2));
    					if(factor > maxFactor){
    						maxFactor = factor;
    						maxWord = keyword;
    						maxNode = oneNode;
    					}
    				}
    			}
    			
    			rst.name = companyName;
    			rst.keyword = maxWord;
    			rst.res = res;
    			rst.factor = maxFactor;
        			
    			float maxFactor2 = -1;
    			res = 0;
    			String maxWord2 = "None";
    			Node maxNode2 = new Node();
    			
    			for(int i = 0;i < maxNode.children.size();i++){
    				oneNode = maxNode.children.get(i);
    				for(int j = 0;j < oneNode.keywords.size();j++){
    					keyword = oneNode.keywords.get(j);
    					
    					int times = 0;
    					res = search("intitle:"+companyName+" "+"intitle:"+keyword);
    					//System.out.println("searching "+companiesName.get(k)+" "+keyword);
    					while(res == -1 && times++ < timeBound){
    						Thread.sleep(1000);
    						System.out.println("searching "+companyName+" "+keyword+"----retry "+times);
    						res = search("intitle:"+companyName+" "+"intitle:"+keyword);
    					}
    					
    					times = 0;
    					int keywordRes = search("intitle:"+keyword);
    					while(res == -1 && times++ < timeBound){
    						Thread.sleep(1000);
    						System.out.println("searching "+keyword+"----retry "+times);
    						keywordRes = search("intitle:"+keyword);
    					}
    					
    					if(res == -1)continue;
    					float factor = (float) ((float)res / keywordRes * Math.log(100000000/bd.top100companyResult.get(companyName)+1) / Math.log(2));
    					if(factor > maxFactor2){
    						maxFactor2 = factor;
    						maxWord2 = keyword;
    						maxNode2 = oneNode;
    					}
    				}
    			}
    			
    			rst.keyword2 = maxWord2;
    			rst.res2 = res;
    			rst.factor2 = maxFactor2;
    			result.add(rst);
    			pw.println(rst.name+"\t"+rst.keyword+"\t"+rst.res+"\t"+rst.factor+"\t"+rst.keyword2+"\t"+rst.res2+"\t"+rst.factor2);
    			System.out.println(rst.name+"\t"+rst.keyword+"\t"+rst.res+"\t"+rst.factor+"\t"+rst.keyword2+"\t"+rst.res2+"\t"+rst.factor2);
    			//System.out.println(result.size());
    			//System.out.println("search result:"+companyName+" "+maxWord+":"+maxFactor);
    			//pw.println(companyName+"\t"+maxNode.number+"\t"+maxWord+"\t"+maxFactor+"\t"+maxNode2.number+"\t"+maxWord2+"\t"+maxFactor2);
        		pw.close();
            }
            catch(Exception e){  
            	e.printStackTrace();
            }
            finally{
            	pw.close();
                synchronized(o){  
                    size.sub();  
                    if(size.size() == 0){  
                        o.notifyAll();  
                    }  
                      
                }  
            } 
              
        }  
    }  
}
