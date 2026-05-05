# LiveSense Japanese

一个完全离线运行的个人日语口语练习工具。Phase 1 先实现文本输入、聊天 UI、Prompt 包装和结构化回复展示；语音识别、语音合成与真实 MediaPipe LLM 接入放到后续阶段。

## 当前功能

- Jetpack Compose 聊天页面
- `ChatViewModel` + `StateFlow` 状态管理
- 用户消息与 AI 消息连续追加
- 日语老师 Prompt 包装
- MediaPipe LLM 本地推理接入
- 收敛采样参数，并对模型输出做学习型五段格式化兜底

## 运行方式

1. 使用 Android Studio 打开项目根目录。
2. 确认 Android SDK 已安装。
3. 运行 `app` 到真机或模拟器。

命令行测试示例：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew testDebugUnitTest
```

## 模型说明

当前使用 MediaPipe LLM Inference API，默认模型路径为：

```text
/data/local/tmp/llm/gemma.task
```

模型文件不应提交到 Git 仓库。开发阶段可使用 adb 推送模型：

```bash
adb shell mkdir -p /data/local/tmp/llm/
adb push /path/to/gemma.task /data/local/tmp/llm/gemma.task
```

注意：MediaPipe LLM 需要兼容的 `.task` 模型包，普通 `.bin` 文件不能直接作为 `tasks-genai` 模型加载。
