一、通用强制总则
1. 所有代码、资源、命名统一小写 / 规范格式，禁止随意命名、拼音命名、无意义命名。
2. 禁止硬编码：文字、颜色、尺寸、字符串全部抽入资源文件。
3. 主线程禁止网络、数据库、文件 IO、耗时操作。
4. Release 包禁止打印敏感日志、禁止测试代码残留。
5. 所有新增功能遵循 MVVM 分层，禁止超大 Activity/Fragment。
二、UI 布局规范（硬性）
1. 控件宽高、间距、内边距 统一使用 dp
2. 文字大小 统一使用 sp
3. 全程禁止使用 px（仅分割线允许 1px）
4. Padding、Margin 严禁使用 sp，防止字体变大布局错乱
5. 优先使用 ConstraintLayout，减少布局嵌套
6. 禁止写死宽高，尽量 wrap_content /match_parent/ 比例适配
控件 ID 统一命名
- tv_xxx：文本
- et_xxx：输入框
- btn_xxx：按钮
- iv_xxx：图片
- rv_xxx：列表
- layout_xxx：自定义布局
三、资源文件命名规范（强制统一）
1. Layout
- Activity：activity_xxx.xml
- Fragment：fragment_xxx.xml
- 列表 Item：item_xxx.xml
- 弹窗：dialog_xxx.xml
- 公共布局：include_xxx.xml
2. Drawable
- 图标：ic_xxx
- 背景：bg_xxx
- 形状：shape_xxx
- 选择器：selector_xxx
3. Values
- 颜色：color_xxx
- 尺寸：dp_xx /sp_xx
- 字符串：统一语义英文 / 拼音简短命名
资源规则
- 全部小写 + 下划线，禁止大写、驼峰、中文、空格
四、代码编码规范
1. 命名规范
- 类、接口：大驼峰（LoginActivity）
- 方法、变量：小驼峰（getUserInfo）
- 常量：全大写下划线（BASE_URL）
- 包名：全小写
2. 代码结构
- 单一职责：一个类只做一类事
- 网络、数据、业务全部剥离出 Activity/Fragment
- 页面只负责 UI 展示与监听回调
3. 异步规范
- 优先使用 协程 / Flow（Kotlin）
- 禁止原生 Thread 裸写、禁止废弃 API
- 页面销毁必须取消所有请求、监听、定时器，杜绝内存泄漏
4. 上下文使用
- UI 弹窗、视图：使用 ActivityContext
- 全局存储、工具类：使用 ApplicationContext
- 禁止静态持有 Activity
五、MVVM 分层规范（团队统一架构）
1. View（Activity/Fragment）：只绑定数据、处理 UI 交互，不写业务逻辑
2. ViewModel：处理页面业务、状态、数据回调，不持有 View 引用
3. Repository：统一整合网络 / 本地数据
4. Data：实体类、网络请求、数据库
禁止：业务堆积在 Activity、网络直接写页面中
六、权限与安全规范
1. 所有危险权限必须动态申请，禁止只在清单注册
2. 账号、Token、隐私数据禁止明文存 SP，优先加密存储
3. 禁止打印手机号、密码、Token 敏感日志
4. 禁止明文 HTTP 请求，全部 HTTPS
七、适配规范
1. 全部页面适配多分辨率，不写死固定宽高
2. 字体跟随系统，全部使用 sp
3. 适配深色模式、系统字体缩放
4. 高版本 API 必须做版本兼容判断
八、性能规范
1. 列表必须使用 RecyclerView 复用 Item
2. 图片必须做压缩、缓存、防止 OOM
3. 杜绝过度绘制、多层嵌套布局
4. 页面退出清空引用，杜绝内存泄漏
九、Manifest 清单规范
1. 所有组件手动配置 exported 权限
2. 无用权限、组件及时删除
3. 全局主题统一配置，不单独页面乱改主题
十、提交规范
1. 禁止提交测试地址、测试日志、测试弹窗
2. 禁止提交无效代码、注释代码、冗余资源
3. 每次提交备注清晰：新增 / 修复 / 优化内容

---
终极极简口诀（全员背诵）
布局全 dp，文字全 sp
资源不硬写，命名不随意
主线不耗时，页面不堆逻辑
分层 MVVM，杜绝内存泄漏
敏感不打印，适配全覆盖