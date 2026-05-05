# 技术架构

## 总体架构

STT（语音识别）→ LLM（推理）→ TTS（语音输出）

---

## 技术选型

### LLM
- 模型：Gemma 2B（量化）
- 推理：MediaPipe LLM

---

### STT（语音识别）
- Vosk-Android
- 模型目录：`app/src/main/assets/model/`
- 运行时解包到应用私有目录后加载

---

### TTS（语音合成）
- Android System TTS（优先）

---

### UI
- Jetpack Compose

---

### 架构模式
- MVVM
- ViewModel + StateFlow

---

## 模块划分

### 1. LlmManager
负责：
- 加载模型
- 推理
- 流式输出

---

### 2. SpeechRecognizer
负责：
- 语音转文字
- 麦克风识别生命周期
- 识别异常提示

---

### 3. TtsManager
负责：
- 文本转语音

---

### 4. ChatViewModel
负责：
- 状态管理
- 串联所有模块
