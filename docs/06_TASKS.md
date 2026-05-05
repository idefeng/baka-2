# 开发任务拆解（Vibe Coding）

## 阶段1：基础对话

### Task 1
Create a Jetpack Compose app with a chat UI.

- message list
- input box
- send button

---

### Task 2
Create ChatViewModel using StateFlow.

- message list state
- sendMessage function

---

### Task 3
Create LlmManager.

- load model from local path
- generate response
- return string

---

### Task 4
Connect ViewModel with LlmManager.

- user input → LLM → append response

---

## 阶段2：Prompt接入

### Task 5
Wrap user input with system prompt.

---

## 阶段3：语音（可选）

### Task 6
Integrate Vosk STT.

- 添加麦克风权限
- 接入 Vosk Android
- 将识别结果回填输入框

---

### Task 7
Integrate Android TTS.

---

## 完成标准

- 可以连续对话
- 有纠错输出
- 无明显卡顿
