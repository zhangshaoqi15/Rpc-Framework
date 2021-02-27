package test.rpc.codec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * 解码器
 */
public class RpcDecoder extends ByteToMessageDecoder {
	private Class<?> genericClass;
	
	public RpcDecoder(Class<?> genericClass) {
		this.genericClass = genericClass;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		//判断报文是否合法（意思是至少有消息头的数据）
		if(in.readableBytes() < 4) {
			return;
		}
		
		//记录当前位置
		in.markReaderIndex();
		//读取消息头（报文长度）
		int length = in.readInt();
		//如果长度不对，说明目前尚未传输完成
		if(in.readableBytes() < length) {
			in.resetReaderIndex();
			return;
		}
		
		byte[] data = new byte[length];
		in.readBytes(data);
		//反序列化并把对象填充到buffer中，可能会传给下一个handler
		Object object = Serialization.deserialize(data, genericClass);
		out.add(object);
	}

}
