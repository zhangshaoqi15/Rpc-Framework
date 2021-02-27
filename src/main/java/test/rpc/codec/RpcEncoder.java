package test.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * ������
 */
public class RpcEncoder extends MessageToByteEncoder<Object>{
	private Class<?> genericClass;
	
	public RpcEncoder(Class<?> genericClass) {
		this.genericClass = genericClass;
	}
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		//�Ѷ������л��󣬷ŵ�buffer��
		if(genericClass.isInstance(msg)) {
			//���л���byte��������
			byte[] data = Serialization.serialize(msg);
			//������Ϣͷ
			out.writeInt(data.length);
			//������Ϣ��
			out.writeBytes(data);
		}
		
	}
	
}
