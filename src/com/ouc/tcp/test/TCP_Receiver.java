/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;	//回复的ACK报文段
	private int expectedSequence = 0;//累积确认
		
	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码，生成ACK
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int currentSequence = (recvPack.getTcpH().getTh_seq() - 1) / 100;
			if (this.expectedSequence == currentSequence) {//收到累积的包
				// 生成 ACK 报文段（设置确认号）
				this.tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
				this.ackPack = new TCP_PACKET(this.tcpH, this.tcpS, recvPack.getSourceAddr());
				this.tcpH.setTh_sum(CheckSum.computeChkSum(this.ackPack));
				System.out.println();
				System.out.println("ACK: " + recvPack.getTcpH().getTh_seq());
				System.out.println();
				// 回复 ACK 报文段
				reply(ackPack);
				this.expectedSequence += 1;
				// 将接收到的正确有序的数据插入 data 队列，准备交付
				this.dataQueue.add(recvPack.getTcpS().getData());
				// 交付数据（每 20 组数据交付一次）
				if (this.dataQueue.size() == 20)
					deliver_data();
			}
			else//失序
			{
				this.tcpH.setTh_ack((expectedSequence-1)*100+1);
				this.ackPack = new TCP_PACKET(this.tcpH, this.tcpS, recvPack.getSourceAddr());
				this.tcpH.setTh_sum(CheckSum.computeChkSum(this.ackPack));
				reply(ackPack);
			}
		}
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				
				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)7);	//eFlag=0，信道无错误
				
		//发送数据报
		client.send(replyPack);
	}
	
}
