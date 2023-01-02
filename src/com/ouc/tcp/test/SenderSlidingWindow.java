package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

/**
 * Description:
 *
 * @date:2023/1/2 16:42
 * @author:Karthus77
 */


public class SenderSlidingWindow {
    private Client client;
    public int cwnd = 1;
    private volatile int ssthresh = 16;
    private int count = 0;  // 拥塞避免： cwnd = cwnd + 1 / cwnd，每一个对新包的 ACK count++，所以 count == cwnd 时，cwnd = cwnd + 1
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>();
    private Hashtable<Integer, UDT_Timer> timers = new Hashtable<>();
    private int lastACKSequence = -1;
    private int lastACKSequenceCount = 0;

    public SenderSlidingWindow(Client client) {
        this.client = client;
    }

    public boolean isFull() {
        return this.cwnd <= this.packets.size();
    }

    public void putPacket(TCP_PACKET packet) {
        int currentSequence = (packet.getTcpH().getTh_seq() - 1) / 100;
        this.packets.put(currentSequence, packet);
        this.timers.put(currentSequence, new UDT_Timer());
        this.timers.get(currentSequence).schedule(new RetransmitTask(this.client, packet, this), 1000, 1000);
    }

    public void receiveACK(int currentSequence) {
        if (currentSequence == this.lastACKSequence) {//出现重复
            this.lastACKSequenceCount++;//记录重复数量
            if (this.lastACKSequenceCount == 4) {//快恢复
                TCP_PACKET packet = this.packets.get(currentSequence + 1);
                if (packet != null) {
                    this.client.send(packet);
                    this.timers.get(currentSequence + 1).cancel();
                    this.timers.put(currentSequence + 1, new UDT_Timer());
                    this.timers.get(currentSequence + 1).schedule(new RetransmitTask(this.client, packet, this), 1000, 1000);
                }
                reStart();//转入慢启动
            }
        } else {//正常接收
            for (int i = this.lastACKSequence + 1; i <= currentSequence; i++) {
                this.packets.remove(i);
                if (this.timers.containsKey(i)) {
                    this.timers.get(i).cancel();//取消计时
                    this.timers.remove(i);
                }
            }
            this.lastACKSequence = currentSequence;
            this.lastACKSequenceCount = 1;
            if (this.cwnd < this.ssthresh) {
                System.out.println("slow start");
                System.out.println("ssthresh: "+ssthresh);
                System.out.println("cwnd: "+cwnd+"-->"+cwnd*2);
                this.cwnd=cwnd*2;//慢开始
                System.out.println("########### window expand ############");
            } else {//拥塞避免
                    System.out.println("congestion avoidance");
                    System.out.println("ssthresh: "+ssthresh);
                    System.out.println("cwnd: "+cwnd+"-->"+(cwnd+1));
                    this.cwnd++;
                    System.out.println("########### window expand ############");
            }
        }
    }

    public void reStart() {//超时重传
        System.out.println("ssthresh: "+ssthresh+"-->"+(this.cwnd/2));
        this.ssthresh = this.cwnd / 2;
        System.out.println("cwnd: "+cwnd+"-->"+1);
        this.cwnd = 1;
    }
}

class RetransmitTask extends TimerTask {
    private Client client;
    private TCP_PACKET packet;
    private SenderSlidingWindow window;

    public RetransmitTask(Client client, TCP_PACKET packet, SenderSlidingWindow window) {
        this.client = client;
        this.packet = packet;
        this.window = window;
    }

    @Override
    public void run() {
        System.out.println("--- Time Out ---");
        this.window.reStart();
        this.client.send(this.packet);
    }
}

