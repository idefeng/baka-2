# CHANGELOG

## 0.1.0

- 创建 Android Kotlin + Jetpack Compose 项目骨架。
- 实现 Phase 1 文本聊天 MVP 的 UI、状态管理和 Prompt 包装。
- 添加本地 LLM 管理接口与占位实现，预留 MediaPipe LLM 接入点。
- 添加单元测试覆盖 Prompt 包装与消息发送流程。
- 接入 MediaPipe LLM Inference API，默认读取 `/data/local/tmp/llm/gemma.task`。
- 收紧日语老师 Prompt、降低采样随机性，并增加输出格式化，减少跑题和重复内容。
- 将 AI 回复扩展为自然回应、中文释义、用法场景、纠错、更自然表达五段，提升学习信息量。
- 修复五段 Prompt 超过 `maxTokens=256` 导致 MediaPipe native 层崩溃的问题，将上下文上限调整为 1024。
- 增加模型状态展示，区分本地模型待命、加载/推理中、就绪和失败。
- 接入 Vosk Android 离线语音识别入口，支持麦克风权限申请、语音监听状态和识别结果回填输入框。
- 更新模型文件忽略规则，避免 Vosk assets 模型目录被提交到仓库。
- 构建期自动生成 Vosk 模型 `uuid` 文件，修复点击语音时报 `model/uuid` 的解包失败。
- 增加 Vosk 模型目录预检，模型不完整时明确提示缺少的关键文件。
- 将 Vosk `uuid` 改为基于模型文件内容生成，模型更新后会触发重新解包，避免加载旧缓存模型。
- 修复聊天页面未规避顶部状态栏的问题。
- 调整 Vosk 识别回调处理，停顿后的稳定片段不再直接结束录音，并累计多段识别文本。
