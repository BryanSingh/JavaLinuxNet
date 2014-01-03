package org.it4y.demo;

import org.it4y.jni.libnetlink;
import org.it4y.jni.linuxutils;
import org.it4y.net.netlink.NetlinkMsgFactory;
import org.it4y.net.netlink.NlMessage;
import org.it4y.util.Hexdump;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by luc on 1/1/14.
 */
public class TNetlinkRoutingListener extends TestRunner {
    private libnetlink.rtnl_handle handle;
    private ByteBuffer messageBuffer=ByteBuffer.allocateDirect(8129);
    private int groups=0;
    private int initstate=0;


    public TNetlinkRoutingListener() {
        super("tnetlinkrouting-listener");
        handle=new libnetlink.rtnl_handle();

    }

    @Override
    public void run() {
        //we are intrested in Routing/link/address events
        groups = libnetlink.linux.rtnetlink.RTMGRP_IPV4_ROUTE |
                 libnetlink.linux.rtnetlink.RTMGRP_IPV4_IFADDR |
                 libnetlink.linux.rtnetlink.RTMGRP_IPV4_MROUTE |
                 libnetlink.linux.rtnetlink.RTMGRP_NEIGH |
                 libnetlink.linux.rtnetlink.RTMGRP_LINK;
        groups=0xffffffff;
        System.out.println("Groups: 0x" + Integer.toHexString(groups));
        linuxutils.rtnl_open_byproto(handle, groups,libnetlink.linux.netlink.NETLINK_ROUTE);
        System.out.println(Hexdump.bytesToHex(handle.handle, 4));
        while(true) {
            if (initstate <4) {
             //we can handle only 1 wilddump at the same time, so we have a little stepping program
             switch(initstate) {
                case 0: linuxutils.rtnl_wilddump_request(handle, 0,libnetlink.linux.rtnetlink.RTM_GETLINK);initstate++;break;
                case 1: linuxutils.rtnl_wilddump_request(handle, 0,libnetlink.linux.rtnetlink.RTM_GETADDR);initstate++;break;
                case 2: linuxutils.rtnl_wilddump_request(handle, 0,libnetlink.linux.rtnetlink.RTM_GETROUTE);initstate++;break;
                default:System.out.println("Init finished");initstate=4;
             }
            }
            //rtnl_listen is blocking until rtnl_accept interface returns rtl_accept_STOP.
            //when stop, the listen will return and thread can continue...
            messageBuffer.rewind();
            messageBuffer.order(ByteOrder.LITTLE_ENDIAN);
            linuxutils.rtnl_listen(handle, messageBuffer, new libnetlink.rtnl_accept() {
                @Override
                public int accept(ByteBuffer message) {
                    //make sure ByteBuffer mar/position are set correct.
                    message.rewind();
                    NlMessage msg=NetlinkMsgFactory.processRawPacket(message);
                    if (msg != null) {
                        System.out.println(msg.toString());
                        //continue or stop listening ?
                        return msg.moreMessages() ? libnetlink.rtl_accept_CONTINUE:libnetlink.rtl_accept_STOP;
                    }
                    return libnetlink.rtl_accept_STOP;
                }
            });
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
                break;
            }
        }
        linuxutils.rtnl_close(handle);
    }
}