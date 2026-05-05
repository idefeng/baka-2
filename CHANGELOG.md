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
