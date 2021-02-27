package test.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 编码器
 */
public class RpcEncoder extends MessageToByteEncoder<Object>{
	private Class<?> genericClass;
	
	public RpcEncoder(Class<?> genericClass) {
		this.genericClass = genericClass;
	}
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		//把对象序列化后，放到buffer中
		if(genericClass.isInstance(msg)) {
			//序列化成byte数组类型
			byte[] data = Serialization.serialize(msg);
			//定义消息头
			out.writeInt(data.length);
			//定义消息体
			out.writeBytes(data);
		}
		
	}
	
}
