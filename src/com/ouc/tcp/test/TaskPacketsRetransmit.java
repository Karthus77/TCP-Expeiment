package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

/**
 * Description:
 *
 * @date:2023/1/2 16:58
 * @author:Karthus77
 */
public class TaskPacketsRetransmit extends TimerTask {
    private Client senderClient;
    private TCP_PACKET[] packets;

    public TaskPacketsRetransmit(Client client, TCP_PACKET[] packets) {
        super();
        this.senderClient = client;
        this.packets = packets;
    }

    @Override
    public void run() {
        for (TCP_PACKET packet : this.packets) {
            if (packet == null) {
                break;
            } else {
                this.senderClient.send(packet);//发送数据报
            }
        }
    }
}
