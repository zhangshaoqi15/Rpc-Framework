package test.rpc.codec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * ������
 */
public class RpcDecoder extends ByteToMessageDecoder {
	private Class<?> genericClass;
	
	public RpcDecoder(Class<?> genericClass) {
		this.genericClass = genericClass;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		//�жϱ����Ƿ�Ϸ�����˼����������Ϣͷ�����ݣ�
		if(in.readableBytes() < 4) {
			return;
		}
		
		//��¼��ǰλ��
		in.markReaderIndex();
		//��ȡ��Ϣͷ�����ĳ��ȣ�
		int length = in.readInt();
		//������Ȳ��ԣ�˵��Ŀǰ��δ�������
		if(in.readableBytes() < length) {
			in.resetReaderIndex();
			return;
		}
		
		byte[] data = new byte[length];
		in.readBytes(data);
		//�����л����Ѷ�����䵽buffer�У����ܻᴫ����һ��handler
		Object object = Serialization.deserialize(data, genericClass);
		out.add(object);
	}

}
