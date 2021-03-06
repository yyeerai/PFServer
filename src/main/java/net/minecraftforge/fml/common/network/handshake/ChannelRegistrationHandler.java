/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.common.network.handshake;

import cn.pfcraft.server.PFServer;
import com.google.common.collect.ImmutableSet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ChannelRegistrationHandler extends SimpleChannelInboundHandler<FMLProxyPacket> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FMLProxyPacket msg) throws Exception
    {
        Side side = msg.getTarget();
        NetworkManager manager = msg.getOrigin();
        if (msg.channel().equals("REGISTER") || msg.channel().equals("UNREGISTER"))
        {
            byte[] data = new byte[msg.payload().readableBytes()];
            msg.payload().readBytes(data);
            String channels = new String(data, StandardCharsets.UTF_8);
            String[] split = channels.split("\0");
            Set<String> channelSet = ImmutableSet.copyOf(split);
            FMLCommonHandler.instance().fireNetRegistrationEvent(manager, channelSet, msg.channel(), side);
            msg.payload().release();
            // handle REGISTER/UNREGISTER channel in Bukkit
            org.bukkit.craftbukkit.entity.CraftPlayer player = ((net.minecraft.network.NetHandlerPlayServer) ctx.attr(NetworkDispatcher.FML_DISPATCHER).get().getNetHandler()).getPlayer();
            if (msg.channel().equals("REGISTER")) {
                for (String channel : channelSet)
                    player.addChannel(channel);
            } else {
                for (String channel : channelSet)
                    player.removeChannel(channel);
            }
        }
        else
        {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        PFServer.LOGGER.error("ChannelRegistrationHandler exception", cause);
        super.exceptionCaught(ctx, cause);
    }
}
