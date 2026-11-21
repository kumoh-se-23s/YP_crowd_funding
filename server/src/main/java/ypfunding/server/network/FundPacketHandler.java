package ypfunding.server.network;

import ypfunding.common.protocol.packets.request.RequestFundPacket;

public class FundPacketHandler implements RequestHandler<RequestFundPacket>{
    @Override
    public void handle(RequestEvent event, RequestFundPacket packet) {
        event.getServer().getAppContext().getFundService().applyFund(event.getLoginUser(), packet.getRewardId(), packet.getQuantity());
        event.success();
    }
}
