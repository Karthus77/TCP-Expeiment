package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import java.util.Timer;

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
    private int size = 16;
    private int base = 0;
    private int nextIndex = 0;
    private TCP_PACKET[] packets = new TCP_PACKET[this.size];//待发送的数据包
    private UDT_Timer[] timers = new UDT_Timer[this.size];//每个数据包的计时器

    public SenderSlidingWindow(Client client) {
        this.client = client;
    }

    public boolean isFull() {
        return this.size <= this.nextIndex;
    }

    public void putPacket(TCP_PACKET packet) {
        this.packets[this.nextIndex] = packet;//加入数据包
        this.timers[this.nextIndex] = new UDT_Timer();//加入计时器
        this.timers[this.nextIndex].schedule(new UDT_RetransTask(this.client, packet), 1000, 1000);//设定发送时间
        this.nextIndex++;
    }

    public void receiveACK(int currentSequence) {
        if (this.base <= currentSequence && currentSequence < this.base + this.size) {//窗口内的数据包序号
            if (this.timers[currentSequence - this.base] == null) {//ack重复
                return;
            }
            this.timers[currentSequence - this.base].cancel();//终止对应的计时器
            this.timers[currentSequence - this.base] = null;//置空

            if (currentSequence == this.base) {//窗口移动
                int maxACKedIndex = 0;
                while (maxACKedIndex + 1 < this.nextIndex
                        && this.timers[maxACKedIndex + 1] == null) {
                    maxACKedIndex++;//移动到第一个未确认处
                }

                for (int i = 0; maxACKedIndex + 1 + i < this.size; i++) {
                    this.packets[i] = this.packets[maxACKedIndex + 1 + i];//移动包
                    this.timers[i] = this.timers[maxACKedIndex + 1 + i];//移动计时器
                }

                for (int i = this.size - (maxACKedIndex + 1); i < this.size; i++) {
                    this.packets[i] = null;//清空之前移动的部分
                    this.timers[i] = null;
                }

                this.base += maxACKedIndex + 1;//更新窗口指针
                this.nextIndex -= maxACKedIndex + 1;//更新下一个包位置
            }
        }
    }
}

