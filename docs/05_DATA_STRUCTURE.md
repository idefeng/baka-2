# 数据结构（极简）

## ChatMessage

```json
{
  "id": "uuid",
  "role": "user | ai",
  "content": "文本内容",
  "timestamp": 1710000000
}
```
## ErrorRecord（可选）
```json
{
  "input": "用户原句",
  "correction": "修正句",
  "note": "错误说明"
}
```