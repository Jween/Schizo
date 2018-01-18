# Schizo

一个超屌的多进程框架, 正处于开发阶段

## 任务拆解

+ 协议与协议: 请求(bean) -> 协议(json) -> 传输(aidl, string) -> 协议(json) -> 响应(bean)
    + 传输层可定制, 未来改成 byte array 或者 byte string
    + 协议层可定制, 当前使用 json
    + 请求与协议可定制, 当前使用 gson
+ 服务: 分进程服务最小单位(SchizoService)
    + 封装 aidl
    + 分发和管理请求
    + 接口注入
    + 多线程处理(待实现)
+ 客户: 使用者可以是任何模块
    + 组件:客户端的 aidl 封装, RxJava 封装
    + 组件管理: 根据 action + api + parameters 定位组件, 沟通服务

+ 编译
    + 根据服务器实现的接口注入(API), 自动生成客户端调用代码(待实现, 目前手动, 见 sample 模块示例)
    + 自动处理依赖关系(待实现)
