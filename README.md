# 1. 框架基本情况
## 框架介绍
remote procedure call：远程过程调用
屏蔽了编码、解码、底层通信的程序框架，只需要框架基础上编写过程代码，使程序调用远程方法像调用本地方法一样（过程是指业务处理的程序代码）

## 技术栈 
* Netty
* Zookeeper

## 运行环境 
* Java版本：JDK8
* 开发工具：eclipse 
* 项目管理工具：Maven

## 调用流程
* 服务端在启动后，会将它提供的服务列表发布到注册中心，客户端向注册中心订阅服务地址；
* 客户端会通过本地代理模块调用远程方法，代理模块将方法、参数等数据编码成消息；
* 客户端从服务列表中选取其中一个的服务地址，并将消息发送给服务端；
* 服务端收到消息后，解码得到请求信息；
* 服务端根据请求信息调用对应的服务方法，然后将响应结果返回给客户端。


## 注意
> 此框架需要一定的Netty和Zookeeper基础，至少要会使用的程度。  
> 如果有任何疑问，欢迎在issues留言。  
> 由于在编写过程中为了方便测试，项目中存在大量`不规范`的输出日志，请见谅。

# 2. 客户端与服务器的设计
___服务消费者___
## 客户端（RpcClient）
* 同步调用，用JDK动态代理创建代理实例，传入代理实现类RpcProxyImpl

## 代理实现类（RpcProxyImpl）
* 当消费者调用接口的方法时，转换成调用invoke()
* 新建请求对象，设置对象的名字、方法名、参数等
* 通过服务发现找到最合适的服务节点
* 连接服务提供者节点，发送请求
* 等待响应结果

___服务提供者___
## 服务器（RpcServer）
* 分别添加解码器、LengthFieldBasedFrameDecoder、编码器、业务处理器
* 启动服务节点，同步阻塞
* 服务发布到注册中心
* 缓存服务接口的实例对象

# 3. 网络通信
___服务消费者___
## 连接管理器（RpcConnectManager）
* 提交到线程池异步发起连接，分别添加编码器、LengthFieldBasedFrameDecoder、解码器、业务处理器
* 提交的同时，主线程轮询获取一个可用的业务处理器（连接）
* 连接成功后，监听并添加业务处理器，唤醒在等待获取连接的线程
* 对成功的连接进行缓存

## 业务处理器（RpcClientHandler）
* 接收到响应结果后，channelRead() 中判断是否有响应结果，并释放锁
* 将请求封装成自定义的异步模型，冲刷到channel中，然后立刻返回异步模型

___服务提供者___
## 业务处理器（RpcSeverHandler）
* 把请求消息的ID赋给响应消息，便于后续异步处理
* 解析请求，通过反射获取具体的本地实例，再调用方法，返回响应结果
* 把响应结果冲刷出来

# 4. 通信协议与序列化
## 通信协议
___RPCrequest___
* 每个消息都拥有一个requestID，在服务器响应请求后，ID用于异步处理响应结果
* 请求的classname、methodname
* 请求的参数及参数类型

___RPCresponse___
* requestID
* 响应结果
* 异常结果

## 编码
* 继承MessageToByteEncoder类，重写encode()
* 把对象序列化后，放到Bytebuffer中

## 解码
* 继承ByteToMessageDecoder类，重写decode()
* 判断消息是否合法（意思是至少有消息头的数据）
* 验证消息完整性
* 从Bytebuffer中读取消息，反序列化成对象

## 序列化
消息体内容是不确定具体长度的，需要使用序列化，可以有效减少网络传输的带宽，提升 RPC 框架的整体性能。在此选用Protostuff。
Protostuff是基于protobuf的序列化框架，不需要定义.proto配置文件，可以通过工具类把对象映射成protobuf的文件。

# 5. 服务治理
## 注册中心
服务消费者在发起 RPC 调用之前，需要知道服务提供者有哪些节点是可用的，而且服务提供者节点会存在上线和下线的情况。所以服务消费者需要感知服务提供者的节点列表的动态变化，一般采用注册中心来实现服务的注册和发现。

* 服务器节点上线后向注册中心注册服务列表，节点下线时需要从注册中心将节点元数据信息移除。
* 客户端向服务器发起调用时，自己从注册中心获取服务器的服务列表，然后在通过负载均衡算法选择其中一个服务节点进行调用

## 注册中心初始化
* 通过Curator工厂创建 zk客户端并启动，创建ServiceDiscovery实例

## 服务发布
* 创建ServiceInstance服务实例，设置服务的参数
* 由ServiceDiscovery调用服务注册方法

## 服务发现
* 由ServiceDiscovery调用服务发现方法，获取服务节点列表
* 通过负载均衡算法得到某个节点

## 负载均衡
策略
* Round-Robin 轮询
* Weighted Round-Robin 权重轮询
* Least Connections 最少连接数
* Random 随机
* Consistent Hash 一致性 Hash `(本框架选用此算法)`

## 一致性 Hash 算法
* 采用哈希环结构（用TreeMap实现），以消费者和提供者节点的哈希值作为key，服务实例为value，放置在哈希环上，按顺时针查找距离消费者最近的服务节点进行调用
* 当服务节点较少时，容易负载不均，需引入虚拟节点，经Hash计算后均匀分布在环中
`优点：在节点扩/缩容时，尽可能保证客户端发起的 RPC 调用还是固定分配到相同的服务节点上，把带来的影响降到最低`

# 6.测试与运行
1.在src/test/java/test/rpc目录下找到服务消费者和服务提供者的启动类，它们分别是ConsumerStarter.java和ProviderStarter.java
2.根据自己环境的实际情况编写服务代码，并分别放在test.rpc.consumer与test.rpc.provider下，本框架中服务接口与实现类是test.rpc.consumer.TemperatureService和test.rpc.provider.TemperatureServiceImpl
3.你需要根据自己环境的实际情况修改ProviderStarter.java下的主机号与端口号，并且在ZookeeperRegistryService.java中修改注册中心的主机号与端口号，还有zk的路径（善用搜索功能）
4.成功启动zookeeper后，再运行ProviderStarter.java和ConsumerStarter.java，如果在客户端下能正常输出`result：xxx`字样，代表成功运行，也可以尝试自己的服务接口
5.在test.registry包中的CuratorTest.java是zookeeper的测试类，可以检验zk能否成功启动，与项目无关。
