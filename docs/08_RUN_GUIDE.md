# 构建与运行

## 环境

- Android Studio 最新版
- Kotlin
- Gradle

---

## 步骤

1. 创建项目
2. 引入 MediaPipe LLM
3. 放入模型文件

路径示例：
/data/local/tmp/gemma.bin

---

## 运行

- 真机运行（推荐）
- 确保有足够内存

---

## 注意

- 首次加载模型较慢
- 建议使用小模型（2B）
- Vosk STT 需要把离线识别模型放到 `app/src/main/assets/model/`
- `app/src/main/assets/model/` 不应提交到 Git 仓库
- 构建脚本会自动生成 Vosk 所需的 `assets/model/uuid`
- 完整模型目录至少需要包含 `am/final.mdl` 和 `conf/model.conf`
