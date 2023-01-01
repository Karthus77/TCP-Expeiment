package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		CRC32 crc32 = new CRC32();
		TCP_HEADER header = tcpPack.getTcpH();//获取TCP报文段首部
		crc32.update(header.getTh_seq());//计算TCP首部的seq字段
		crc32.update(header.getTh_ack());//计算TCP首部的ack字段
		for (int i : tcpPack.getTcpS().getData()) {
			crc32.update(i);//计算TCP数据字段
		}
		return (short) crc32.getValue();//返回计算得到的校验码
	}
}
