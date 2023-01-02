package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import java.util.Timer;
import com.ouc.tcp.message.TCP_PACKET;

/**
 * Description:
 *
 * @date:2023/1/2 16:42
 * @author:Karthus77
 */


public class SenderSlidingWindow {
    private Client client;
    private int size = 16;
    private int base = 0;
    private int nextIndex = 0;
    private TCP_PACKET[] packets = new TCP_PACKET[this.size];

    private Timer timer;
    private TaskPacketsRetransmit task;

    public SenderSlidingWindow(Client client) {
        this.client = client;
    }

    public boolean isFull() {
        return this.size <= this.nextIndex;
    }

    public void putPacket(TCP_PACKET packet) {
        this.packets[this.nextIndex] = packet;
        if (this.base == this.nextIndex) {
            this.timer = new Timer();
            this.task = new TaskPacketsRetransmit(this.client, this.packets);
            this.timer.schedule(this.task, 1000, 1000);
        }
        this.nextIndex++;
    }

    public void receiveACK(int currentSequence) {
        if (this.base <= currentSequence && currentSequence < this.base + this.size) {//判断包是否在窗口大小内
            for (int i = 0; currentSequence - this.base + 1 + i < this.size; i++) {//移动窗口，将窗口整体右移到未确认的数据包的位置
                this.packets[i] = this.packets[currentSequence - this.base + 1 + i];
                this.packets[currentSequence - this.base + 1 + i] = null;
            }
            this.nextIndex -=currentSequence - this.base + 1;//更新nextIndex
            this.base = currentSequence + 1;//更新base对应的seq
            this.timer.cancel();//停止计时
            if (nextIndex!=0) {//窗口中仍有剩余的包
                this.timer = new Timer();
                this.task = new TaskPacketsRetransmit(this.client, this.packets);
                this.timer.schedule(this.task, 1000, 1000);
            }
        }
    }
}

