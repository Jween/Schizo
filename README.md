# Schizo

一个极其易用的响应式分进程模块框架 (基于 RxJava

+ 可能是地球上最易于使用的 android IPC 框架
+ 使用者只需要写自己的 Server 端具体业务实现
+ 其余一切代码自动生成, 使用者**不需要**写 aidl, **不需要**管 callback, **不需要**理会 binder
+ 基于RxJava, 支持 Single 与 Observable 两种返回模式
    + Single: 用于一次请求一次返回, 例如客户端向服务端查询一个数据
    + Observable: 用于一次请求,分次返回, 例如客户端向服务端监听下载服务进度

+ 设计理念
    + 模块开发者只需要写模块提供的接口服务, 其余都不用管
    + 调用服务的人, 直接调用自动生成的可以访问服务的接口, 其余都不用管

## 使用

#### build.gradle

````groovy
annotationProcessor 'io.jween.schizo:processor:0.8'
implementation 'io.jween.schizo:schizo:0.8'
````

#### 代码

1. 继承 `SchizoService`
2. `@Action` 注解用于 Service, 值指定为可以调起该 Service 的 action
3. `@Api` 注解用于改类内的方法

搞定, 其余的接口, 分进程处理逻辑全部是自动生成的.

自动生成的接口类为 `TestServiceApi` (Service 的类名 + Api)

## Proguard (0.7 之后的版本已经添加到 aar，可以不用再手动添加)

````
-keepclassmembers class * {
    @io.jween.schizo.annotation.Api *;
}
````


## 示例: 

#### 服务端 TestService (该模块开发者提供服务实现)

**TestService.java**

````
@Action("io.jween.schizo.test1")
public class TestService extends SchizoService {

    @Api("book")
    Book getBook(String title) {
        return new Book(title, "Nobody");
    }
}
````

* 每一个 `@Api` 注解的方法, 对应于一个接口

**AndroidManifest.xml**

````
    <service
        android:name=".service.TestService"
        android:exported="false"
        android:process=":net" >
        <intent-filter>
            <action android:name="io.jween.schizo.test1" />
        </intent-filter>
    </service>
````

* `android:process=":net"`: 指定进程名
* `<action android:name="io.jween.schizo.test1" />`: 指定 Action, `@Action` 通过这个来定位接口类.

#### 客户端 TestServiceApi (框架自动生成)

````
public final class TestServiceApi {
  private static final String ACTION = "io.jween.schizo.test1";

  public static Single<Book> book(String title) {
    return ComponentManager.get(ACTION).process("book", title, Book.class);
  }
}
````

* `attach(Context)`: 引入该进程模块
* `detach()`: 释放该进程模块
* `book(String)`: 根据 `@Api` 自动生成的调用接口, 供客户端调用


### Observable Streaming 数据返回接口

````
    @Api("observeCounter") // 暴露给客户端的接口名
    Observable<String> testObserverApi(Integer interval) {
        Log.d(TAG, "observing counter, interval is " + interval);
        return Observable.interval(interval, TimeUnit.SECONDS)
                .map(new Function<Long, String>() {
                    @Override
                    public String apply(Long aLong) throws Exception {
                        Log.d(TAG, "server on next emit " + aLong);
                        return "Observing " + aLong;
                    }
                });
    }
````

* 返回使用 Observable<YourReturnType> 即可.
* 客户端直接调用 TestServiceApi.observeCounter(Integer) 即可.

## 依赖的库

schizo 库的多线程处理, 以及接口返回是响应式的, 协议封装与解析暂时使用 gson

````groovy
implementation 'io.reactivex.rxjava2:rxandroid:2.1.0'
implementation 'io.reactivex.rxjava2:rxjava:2.2.7'
implementation 'com.google.code.gson:gson:2.8.5'
````
    
## 里程碑与任务拆解

+ 协议与传输: 请求(bean) -> 协议(json) -> 传输(aidl, string) -> 协议(json) -> 响应(bean)
    + 传输层可定制, 未来改成 byte array 或者 byte string
    + 协议层可定制, 当前使用 json
    + 请求与协议可定制, 当前使用 gson
+ 服务端: 分进程服务最小单位(SchizoService)
    + 封装 aidl
    + 分发和管理请求
    + 接口注入
    + 多线程处理(基于 RxJava 2.x)
+ 客户端: 接口访问代码自动生成, 使用者可以是任何模块
    + 组件: 客户端的 aidl 封装, RxJava 封装
    + 组件管理: 根据 action + api + parameters 定位组件, 沟通服务

+ 编译
    + 根据服务器实现的接口注入(API), 自动生成客户端调用代码
    + 自动处理依赖关系


## License

Schizo 基于 Apache-2.0 协议开源, 协议详情参见 [LICENSE](LICENSE.md)     
See the [LICENSE](LICENSE.md) file for license rights and limitations (APACHE-2.0).   
