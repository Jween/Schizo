### Version 0.7 @ 2019-01-30
+ 更新依赖库版本

### Version 0.6 @ 2018-07-27
+ 删除无用日志
+ 重写 disposable 回收逻辑, 避免 CompositeDisposable 引起的内存膨胀
+ SchizoBinder: 响应式的 binder 封装

### Version 0.5 @ 2018-07-20
+ 接口支持泛型

### Version 0.4 @ 2018-05-31
+ 添加 Observable 响应式接口支持, 用于多段返回的 api 接口

### Version 0.3 @ 2018-05-21
+ 修复 ServiceComponent 绑定阶段偶现主线程阻塞问题

### Version 0.2 @ 2018-05-07
+ 修复 ServiceComponent 绑定阶段偶现空指针异常
+ 修复 api 调用, binder 绑定后, 不默认在 io() 线程池的问题
+ 兼容无参数的 api
    
    
### Version 0.1 @ 2018-02-01
