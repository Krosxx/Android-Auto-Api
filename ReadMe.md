# Android-Accessibility-Api

[![](https://jitpack.io/v/Krosxx/Android-Auto-Api.svg)](https://jitpack.io/#/Android-Krosxx-Api)

中文 | [English](ReadMe_EN.md)

> 安卓无障碍服务Api, 为了简化无障碍服务使用，并使用 Kotlin 以提供简洁的Api。


<br>

**由于4.0版本代码进行重构，之前接入的项目需要修改 `import package`, 并且 `
implementation 'com.github.Krosxx.Android-Accessibility-Api:accessibility-api:4.0.0'`。UiAutomator 可参考 [Demo App](app)。**  
**最新 library : `implementation 'com.github.Krosxx:Android-Auto-Api:Tag'`**


## 基础导航

(图片加载过慢可到 [Gitee](https://www.gitee.com/Vove/Android-Accessibility-Api) 查看)

<img width=300 src="screenshots/action_nav.gif"></img>


<details>
<summary>查看代码</summary>

```kotlin
//无障碍服务声明 未开启会跳转设置页面提示开启服务，并抛出异常 终止执行
requireBaseAccessibility()
toast("下拉通知栏..")
delay(1000)
toast("快捷设置..")
//操作之间需要适当延时等待
delay(1000)
//下拉通知栏快捷设置
quickSettings()
delay(1000)
//返回操作
back()
delay(500)
//返回操作
back()
delay(1000)
//电源菜单
powerDialog()
delay(500)
back()
delay(1000)
//进入最近任务页面
recents()
delay(1000)
back()
delay(1000)
//Home 按键 / 返回桌面
home()
delay(100)
```

</details>

更多操作:

| 方法          | 说明                        |
| ------------- | --------------------------- |
| lockScreen()  | 锁屏，需要Android P         |
| screenShot()  | 触发系统截屏，需要Android P |
| splitScreen() | 触发系统分屏，需要Android P |



## 视图检索

### 提取文字

<img width=300 src="screenshots/action_pick_text.gif"></img>

```kotlin
requireBaseAccessibility()
//使用 ScreenTextFinder() 来搜索屏幕上的文字
val ts = ScreenTextFinder().find().joinToString("\n\n")

withContext(Dispatchers.Main) {
    AlertDialog.Builder(act).apply {
        setTitle("提取文字：")
        setMessage(ts)
        show()
    }
}
```

### 视图搜索

1. 提供一个基础类 `ViewFinder`， 并封装一个 `ViewFinderWithMultiCondition` 来指定搜索条件，实现快速搜索;查看所有方法：[view_finder_api.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/api/view_finder_api.kt)
1. 最新添加 `SmartFinder`，支持多条件(AND, OR)搜索，扩展性极高  

`ViewFinder` 主要方法：

**注意：在`3.0.0`及之后版本，搜索方法需在协程作用域内使用，若需要`java`内调用请使用`2.1.1`版本**

|                             方法                             |                             说明                             |
| :----------------------------------------------------------: | :----------------------------------------------------------: |
|   findFirst(includeInvisible: Boolean = false): ViewNode?    | 立即搜索，返回满足条件的第一个结果<br>includeInvisible: 是否包含不可见元素 |
| findAll(includeInvisible: Boolean = false): Array\<ViewNode> |               立即搜索，返回满足条件的所有结果               |
|         waitFor(waitMillis: Long = 30000): ViewNode?         |   等待搜索，在指定时间内循环搜索（视图更新），超时返回null   |
|      require(waitMillis: Long = WAIT_MILLIS): ViewNode       |                       等待超时抛出异常                       |
|         findByDepths(vararg depths: Int): ViewNode?          |                       指定深度索引搜索                       |
|                       exist(): Boolean                       |                是否存在 (findFirst() != null)                |
|                      attachCoroutine()                       |              支持协程调用，支持cancel()打断搜索              |

**示例1：** 等待 Chrome 打开 > 展开菜单

<img width=300 src="screenshots/action_chrome.gif"></img>

```kotlin
//等待无障碍开启 默认时间30s，超时将抛出异常
waitBaseAccessibility()
toast("start chrome after 1s")
delay(1000)
//打开Chrome
val targetApp = "com.android.chrome"
act.startActivity(act.packageManager.getLaunchIntentForPackage(targetApp))
//等待页面
if (
    waitForApp(targetApp, 5000).also {
        toast("wait " + if (it) "success" else "failed")
    }
) {
    //id 搜索，点击打开菜单
    withId("menu_button").tryClick()
}
```

**示例2：** 文本操作

<img width=300 src="screenshots/action_text_op.gif"></img>

<details>
<summary>查看代码</summary>

```kotlin
requireBaseAccessibility()
//editor() 指定编辑框
editor().require().apply { // this is ViewNode
    repeat(5) {
        //追加文本
        appendText(".x")
        delay(500)
    }
    delay(1000)
    //清空文本
    text = ""
    delay(1000)
    //设置文本
    text = "123456"
    delay(1000)
    //选择文本
    setSelection(0, 5)
    delay(1000)
	//清除选择
    clearSelection()
    //失去焦点
    clearFocus()
}
```
</details>


2. 提供自定义搜索条件

搜索所有可点击的视图：

<img width=300 src="screenshots/action_custom_finder.gif"></img>


```kotlin
requireBaseAccessibility()
//自定义条件搜索
val s = findAllWith { it: AccessibilityNodeInfo -> 
    it.isClickable
}.joinToString("\n\n")

withContext(Dispatchers.Main) {
    AlertDialog.Builder(act).apply {
        setTitle("可点击节点：")
        setMessage(s)
        show()
    }
}
```

3. SmartFinder

> 扩展性极高，支持多条件(AND, OR)搜索
>
> 更多已支持的条件见：[SmartFinderConditions.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/viewfinder/SmartFinderConditions.kt)

```kotlin
//SF 为 SmartFinder 缩写
SF.text("SmartFinder测试").findFirst()
//等效：
SF.where(_text eq "SmartFinder测试").findFirst()

//搜索 文本为123 或者 id为text1
SF.text("123").or().id("text1").findFirst()
```

原始AND，OR

```kotlin
//搜索 所有isChecked
SF.where { 
    it.isChecked
}.find()

SF.where(IdCondition("view_id")).or(RTextEqCondition("[0-9]+")).find()
//等效：
SF.id("view_id").or().matchText("[0-9]+").find()
```

支持Group

```kotlin
// (text=="111" && desc=="111") || (text=="222" && desc=="222")
SF.where(SF.text("111").desc("111"))
            .or(SF.text("222").desc("222"))
            .find()
```

中缀表达式
```kotlin
//使用中缀表达式
(SF where text("1111") or text("2222")
        and id("111") or longClickable()).findAll()
```


3. 协程支持

添加 `attachCoroutine` 方法，搜索等待可及时中断

```kotlin
fun run(act: Activity) = runBlocking {
    val outterJob = coroutineContext[Job]
    val searchJob = GlobalScope.async {
        val t = SF.attachCoroutine()//attach当前协程上下文，需要主动调用
          .containsText("周三").waitFor(10000)
        AlertDialog.Builder(act).apply {
            setMessage(t.toString())
            withContext(Dispatchers.Main) {
                show()
            }
        }
    }
    searchJob.invokeOnCompletion {
        outterJob?.cancel()
    }
    delay(3000)
    //取消搜索测试
    searchJob.cancel()
}
```

4. 条件扩展

> 封装自定义搜索条件，使调用起来更简洁
> 库中搜索条件全部实现位于 `SmartFinderConditions.kt`

例如 定义使用正则匹配Node文本

##### Step 1.
```kotlin
class TextMatcherCondition(private val regex: String) : MatchCondition {
	//此处注意直接创建Regex，防止在搜索时重复创建；另外可直接检查正则表达式有效性
    private val reg = regex.toRegex()
    override fun invoke(node: AcsNode) =
        node.text?.toString()?.let {
            reg.matches(it)
        } ?: false
}
```

此时，已经可以这样使用：

```kotlin
SF.where(TextMatcherCondition("[0-9]+")).findAll()
```
追究简洁，可进行扩展方法：

##### Step 2.
```kotlin
fun ConditionGroup.matchText(reg: String) = link(TextMatcherCondition(reg))
```

之后可简化调用

```kotlin
SF.matchText("[0-9]+").findAll()
```


## 视图节点(ViewNode)

根据`ViewFinder`搜索得到 `ViewNode`，可进行的操作详见接口：[ViewOperation.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/viewnode/ViewOperation.kt)

## 全局手势

全局手势可以点击/长按任意坐标，执行路径手势。

### 示例

<img width=300 src="screenshots/action_gesture.gif"></img>

<details>
<summary>查看代码</summary>

```kotlin
class DrawableAction : Action {
    override val name: String
        get() = "手势画图 - Rect - Circle - Oval"

    @RequiresApi(Build.VERSION_CODES.N)
    override suspend fun run(act: Activity) {
        requireBaseAccessibility()
        requireGestureAccessibility()
        act.startActivity(Intent(act, DrawableActivity::class.java))
        toast("1s后开始绘制，请不要触摸屏幕")
        delay(1000)

        //设置相对屏幕 非必须
        setScreenSize(500, 500)
        //指定点转路径手势
        gesture(
            2000L, arrayOf(
                100 t 100,
                100 t 200,
                200 t 200,
                200 t 100,
                100 t 100
            )
        )
        delay(800)
        //点击clear按钮
        withText("clear").tryClick()
        //使用Path
        drawCircle()
        delay(800)
        withText("clear").tryClick()
        drawOval()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun drawCircle() {
        val p = Path().apply {
            //此处路径坐标为绝对坐标
            addOval(RectF(500f, 500f, 800f, 800f), Path.Direction.CW)
        }
        gesture(2000L, p) {
            toast("打断")
        }
    }

    //AdapterRectF 会根据设置的相对屏幕大小换算
    @RequiresApi(Build.VERSION_CODES.N)
    fun drawOval() {
        val p = Path().apply {
            //使用AdapterRectF 会根据设置的相对屏幕尺寸将坐标转换
            addOval(AdapterRectF(200f, 200f, 300f, 300f), Path.Direction.CW)
        }
        gesture(2000L, p)
    }

    infix fun <A, B> A.t(that: B): Pair<A, B> = Pair(this, that)
}

```

</details>

### Api文档

**手势Api全部需要Android N+；代码必须执行于非主线程**

<details>
<summary>展开查看</summary>

| 方法                                                         | 说明                        |
| :----------------------------------------------------------- | :-------------------------- |
| fun setScreenSize(width: Int, height: Int)                   | 设置屏幕相对坐标            |
| fun gesture(<br>     duration: Long, <br>     points: Array<Pair<Int, Int>>,<br>     onCancel: Function0<Unit>? = null <br>): Boolean | 根据点坐标生成路径 执行手势 |
| fun gesture(<br>     duration: Long,<br>     path: Path,<br>     onCancel: Function0<Unit>? = null <br>): Boolean | 根据Path执行手势            |
| fun gestureAsync(<br>     duration: Long,<br>     points: Array<Pair<Int, Int>> <br>) | 异步执行手势                |
| fun gestures(<br>     duration: Long,<br>     ppss: Array<Array<Pair<Int, Int>>>,<br>     onCancel: Function0<Unit>? = null <br>): Boolean | 多路径手势                  |
| fun click(x: Int, y: Int)                                | 点击； x,y 相对坐标         |
| fun longClick(x: Int, y: Int)                                | 长按； x,y 相对坐标         |
| fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, dur: Int)      | 两点间滑动                  |
| fun scrollUp(): Boolean                                      | 向上滑动                    |
| fun scrollDown(): Boolean                                    | 向下滑动                    |

</details>



## 使用

由于部分系统版本启动支持手势的无障碍服务会造成系统卡顿(掉帧)，所以本库分为两个服务来设计（若无需手势功能，可不实现手势服务）。

### 引入 Android-Accessibility-Api

1. Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
	repositories {
		//...
		maven { url 'https://jitpack.io' }
	}
}
```

2. Add the dependency

```groovy
dependencies {
	implementation 'com.github.Krosxx:Android-Auto-Api:Tag'
}
```

the TAG is [![](https://jitpack.io/v/Krosxx/Android-Auto-Api.svg)](https://jitpack.io/#Krosxx/Android-Auto-Api)

**注意：在`3.0.0`及之后版本，搜索方法需在协程作用域内使用，若需要`java`内调用请使用`2.1.1`版本**


### 创建你的服务

#### 基础服务

用来支持 布局检索，视图操作

1. 定义你的 BaseAccessibilityService

<details>
<summary>展开查看 BaseAccessibilityService </summary>

```kotlin
class BaseAccessibilityService : AccessibilityApi() {

    //启用 页面更新 回调
    override val enableListenAppScope: Boolean = true
    
    //页面更新回调
    override fun onPageUpdate(currentScope: AppScope) {
        Log.d("TAG", "onPageUpdate: $currentScope")
    }
}
```

</details>

2. 服务注册

```xml
<service
    android:name=".service.BaseAccessibilityService"
    android:description="@string/base_ser_desc"
    android:label="BaseService Demo"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/base_accessibility_config" />
</service>
```

3. base_accessibility_config.xml

<details>
<summary>点击展开 res/xml/base_accessibility_config.xml</summary>


```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackAllMask"
    android:accessibilityFlags="flagIncludeNotImportantViews|flagReportViewIds|flagRetrieveInteractiveWindows|flagRequestEnhancedWebAccessibility"
    android:canRetrieveWindowContent="true"
    android:description="@string/base_ser_desc"
    android:notificationTimeout="10"
    android:canRequestEnhancedWebAccessibility="true"
    android:settingsActivity=".MainActivity"
    android:summary="基础导航/视图检索操作"/>
<!--    android:canRequestFilterKeyEvents="true"-->
<!--flagRequestFilterKeyEvents-->
```

其中 `android:accessibilityEventTypes="typeWindowStateChanged"` 可能会在视图搜索有延迟刷新视图树的问题， 
可使用 `android:accessibilityEventTypes="typeAllMask"` 替换

</details>

#### 手势服务

> 用于执行手势，Android N+可用

1. 定义 GestureAccessibilityService

<details>
<summary>展开查看 GestureAccessibilityService </summary>

```kotlin
class GestureAccessibilityService : AccessibilityService() {
    override fun onCreate() {
        super.onCreate()
        //must call
        AccessibilityApi.gestureService = this
    }
    override fun onDestroy() {
        super.onDestroy()
        //must call
        AccessibilityApi.gestureService = null
    }
    override fun onInterrupt() {}
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
```

</details>

2. 服务注册

```xml
<service
    android:name=".service.GestureAccessibilityService"
    android:description="@string/ges_ser_desc"
    android:label="Gesture Service Demo"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/gesture_accessibility_config" />
</service>
```

3. gesture_accessibility_config.xml

<details>
<summary>点击展开 res/xml/gesture_accessibility_config.xml</summary>


和基础服务配置区别仅为 `android:canPerformGestures="true"`

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes=""
    android:accessibilityFeedbackType=""
    android:accessibilityFlags=""
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="false"
    android:description="@string/ges_ser_desc"
    android:notificationTimeout="10"
    android:packageNames=""
    android:settingsActivity=".MainActivity"
    android:summary="执行手势" />
```

</details>

### 额外配置 

在 Application 中初始化：

**指定 `BASE_SERVICE_CLS` 及 `GESTURE_SERVICE_CLS`**

```kotlin
override fun onCreate() {
    super.onCreate()
    AccessibilityApi.init(this,
        BaseAccessibilityService::class.java,
        GestureAccessibilityService::class.java)
}
```



### 合并服务

如果你想使用一个服务来完成，可使用如下配置


1. 创建服务

```kotlin
class AppAccessibilityService : AccessibilityApi() {
    //启用 页面更新 回调
    override val enableListenAppScope: Boolean = true

    //页面更新回调
    override fun onPageUpdate(currentScope: AppScope) {
        Log.d("TAG", "onPageUpdate: $currentScope")
    }
}
```


2. 清单注册

```xml
<service
    android:name=".service.AppAccessibilityService"
    android:description="@string/ser_desc"
    android:label="Service Demo"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_config" />
</service>
```

3. `res/xml/accessibility_config.xml`


<details>
<summary>点击展开 accessibility_config.xml</summary>


```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackAllMask"
    android:accessibilityFlags="flagIncludeNotImportantViews|flagReportViewIds|flagRetrieveInteractiveWindows|flagRequestEnhancedWebAccessibility"
    android:canRetrieveWindowContent="true"
    android:description="@string/base_ser_desc"
    android:notificationTimeout="10"
    android:canPerformGestures="true"
    android:canRequestEnhancedWebAccessibility="true"
    android:settingsActivity=".MainActivity"
    android:summary="基础导航/视图检索操作"/>
<!--    android:canRequestFilterKeyEvents="true"-->
<!--flagRequestFilterKeyEvents-->
```

</details>

4. Application 初始化配置 

在 Application 中初始化：

```kotlin
override fun onCreate() {
    super.onCreate()
    AccessibilityApi.init(this, AppAccessibilityService::class.java)
}
```

----------------------------

更多 `Api` 可在下列文件查看

- [view_finder_api.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/api/view_finder_api.kt)
- [gesture_api.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/api/gesture_api.kt)
- [nav_api.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/api/nav_api.kt)
- [nav_api.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/api/nav_api.kt)
- [ViewNode](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/viewnode/ViewNode.kt)
- [SmartFinderConditions.kt](accessibility-api/src/main/java/cn/vove7/andro_accessibility_api/viewfinder/SmartFinderConditions.kt)
