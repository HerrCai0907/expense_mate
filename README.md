# Expense Mate

原生 Android / Kotlin 记账应用，支持标签、时间预设、支出分析和记录查询。

## 代码结构

源码位于 `app/src/main/kotlin/dev/expensemate`，包名和 Android namespace 为 `dev.expensemate`。

| 目录 / 文件 | 职责 |
| --- | --- |
| `MainActivity.kt` | 生命周期、依赖组装、页面切换及数据库关闭 |
| `model/` | 账单、外观模式、时间预设等数据模型 |
| `domain/` | 支出分析、金额校验、标签优先级、记录排序和格式化；不依赖 Android |
| `data/` | SQLite 账单存储、标签与最近使用顺序、外观与时间预设偏好 |
| `state/` | 录入草稿、分析筛选与排序、页面状态和 Bundle 保存恢复 |
| `ui/entry/` | 录入页面、标签下拉框、日期时间和备注弹窗 |
| `ui/settings/` | 设置页面、时间预设管理弹窗 |
| `ui/analysis/` | 分析页面、记录页面、分布图及共用分析控件 |
| `ui/` | 基础控件、配色、系统栏和悬浮导航按钮 |

页面通过构造函数接收所需状态、存储和 UI 工具；通过导航回调请求切换，不持有 `MainActivity`。共享控件使用组合，不把业务逻辑集中到基类，也不把 Activity 的方法作为扩展函数分散到多个文件。

新增业务规则放入 `domain/`，持久化放入 `data/`，新页面放入对应 `ui/` 子目录。`MainActivity` 只组装依赖与页面，不承接页面逻辑。

## 数据兼容

源码包名与安装标识分开管理。`applicationId` 暂时保留 `com.example.expensemate`，确保现有安装能够继续使用原数据库和偏好；不要仅为了目录命名修改它。数据库名称、表结构、版本、SharedPreferences 名称及键、Bundle 状态键均沿用原格式。

## 构建和验证

需要 JDK、Android SDK 35；按本机情况配置 `JAVA_HOME` 和 `ANDROID_HOME`，或在未入库的 `local.properties` 中指定 SDK。

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

测试位于 `app/src/test/kotlin/dev/expensemate`。原有 `tests/ExpenseAnalysisTest.java` 的 15 项分析回归已接入 Gradle 单元测试。其他测试覆盖金额边界、标签优先级、记录排序、时间格式、状态恢复、旧数据库与偏好兼容、页面导航、主题切换和 Activity 重建。Android 相关测试通过 Robolectric 在本机运行。
