package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
/**
 * Description:
 *
 * @date:2023/1/2 20:30
 * @author:Karthus77
 */

public class ReceiverSlidingWindow {
    private Client client;
    private int size = 16;
    private int base = 0;
    private TCP_PACKET[] packets = new TCP_PACKET[this.size];//接收数据
    Queue<int[]> dataQueue = new LinkedBlockingQueue();
    private int counts = 0;

    public ReceiverSlidingWindow(Client client) {
        this.client = client;
    }

    public int receivePacket(TCP_PACKET packet) {
        int currentSequence = (packet.getTcpH().getTh_seq() - 1) / 100;
        if (currentSequence < this.base) {//失序或者超时达到
            // ACK [base - size, base - 1]
            int left = this.base - this.size;
            int right = this.base - 1;
            if (left <= 0) {
                left = 1;
            }
            if (left <= currentSequence && currentSequence <= right) {
                return currentSequence;//回复ack
            }
        } else if (this.base <= currentSequence && currentSequence < this.base + this.size) {
            this.packets[currentSequence - this.base] = packet;
            if (currentSequence == this.base) {
                this.slid();
            }
            return currentSequence;
        }
        return -1;
    }

    private void slid() {
        int maxIndex = 0;
        while (maxIndex + 1 < this.size
                && this.packets[maxIndex + 1] != null) {
            maxIndex++;//移动到首个未被接收位置
        }
        for (int i = 0; i < maxIndex + 1; i++) {
            this.dataQueue.add(this.packets[i].getTcpS().getData());//将移动的数据包加入
        }
        for (int i = 0; maxIndex + 1 + i < this.size; i++) {
            this.packets[i] = this.packets[maxIndex + 1 + i];//将数据包左移
        }
        for (int i = this.size - (maxIndex + 1); i < this.size; i++) {
            this.packets[i] = null;//清空被移动的数据包
        }
        this.base += maxIndex + 1;
        if (this.dataQueue.size() >= 20 || this.base == 1000) {
            this.deliver_data();
        }
    }
    /**
     * 交付数据: 将数据写入文件
     */
    public void deliver_data() {
        // 检查 this.dataQueue，将数据写入文件
        try {
            File file = new File("recvData.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            while (!this.dataQueue.isEmpty()) {
                int[] data = this.dataQueue.poll();

                // 将数据写入文件
                for (int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();  // 清空输出缓存
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}