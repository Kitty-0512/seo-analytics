<template>
  <div class="ai-page">
    <div class="examples">
      <button
        v-for="q in examples"
        :key="q"
        type="button"
        class="example-chip"
        @click="ask(q)"
      >
        {{ q }}
      </button>
    </div>

    <div class="chat-panel">
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="empty-state">
          选择上方示例，或输入问题查询 SEO 数据
        </div>
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="message"
          :class="msg.role"
        >
          <div class="bubble">
            <div class="text">{{ msg.content }}</div>
            <details v-if="msg.sql" class="sql-fold">
              <summary>SQL</summary>
              <pre>{{ msg.sql }}</pre>
            </details>
            <details v-if="msg.ragContext?.length" class="sql-fold">
              <summary>RAG 上下文 ({{ msg.ragContext.length }})</summary>
              <pre>{{ JSON.stringify(msg.ragContext, null, 2) }}</pre>
            </details>
            <div v-if="msg.dataPreview?.length" class="data-block">
              <table class="plausible-table">
                <thead>
                  <tr>
                    <th v-for="col in msg.columns" :key="col">{{ col }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, ri) in msg.dataPreview" :key="ri">
                    <td v-for="col in msg.columns" :key="col">
                      <span class="num">{{ row[col] }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div v-if="sending" class="message assistant">
          <div class="bubble">
            <div class="skeleton skeleton-line" style="width: 160px" />
            <div class="skeleton skeleton-line" style="width: 220px; margin-top: 8px" />
          </div>
        </div>
      </div>

      <div class="composer">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          placeholder="Ask about traffic, keywords, pages…"
          resize="none"
          @keydown.enter.ctrl="send"
        />
        <el-button type="primary" :disabled="sending || !question.trim()" @click="send">
          Send
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiChat } from '@/api/aiApi'
import { useSiteStore } from '@/stores/siteStore'

const siteStore = useSiteStore()
const question = ref('')
const sending = ref(false)
const messages = ref([])
const messagesRef = ref(null)

const examples = [
  '过去 7 天点击量最高的关键词是哪些？',
  'GSC 和 Bing 的 CTR 对比如何？',
  '展现量上升但点击下降的页面有哪些？',
]

function ask(q) {
  question.value = q
  send()
}

async function send() {
  const q = question.value.trim()
  if (!q || sending.value) return
  if (!siteStore.currentSiteId) {
    ElMessage.warning('Please select a site first')
    return
  }

  messages.value.push({ role: 'user', content: q })
  question.value = ''
  sending.value = true
  await scrollBottom()

  try {
    const res = await aiChat({ siteId: siteStore.currentSiteId, question: q })
    const data = res.data || {}
    const rows = Array.isArray(data.data) ? data.data : []
    messages.value.push({
      role: 'assistant',
      content: data.answer || 'No answer',
      sql: data.sql,
      ragContext: Array.isArray(data.rag_context) ? data.rag_context : [],
      dataPreview: rows.slice(0, 20),
      columns: rows.length ? Object.keys(rows[0]) : [],
    })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: e.message || 'Request failed' })
  } finally {
    sending.value = false
    await scrollBottom()
  }
}

async function scrollBottom() {
  await nextTick()
  if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
}
</script>

<style scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px - 48px);
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.example-chip {
  appearance: none;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
  font: inherit;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
}

.example-chip:hover {
  color: #6366f1;
  border-color: #c7d2fe;
}

.chat-panel {
  flex: 1;
  min-height: 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.messages {
  flex: 1;
  overflow: auto;
  padding: 24px;
}

.message {
  display: flex;
  margin-bottom: 16px;
}

.message.user { justify-content: flex-end; }

.bubble {
  max-width: 720px;
  padding: 12px 16px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  line-height: 1.6;
  font-size: 14px;
}

.message.user .bubble {
  background: #6366f1;
  border-color: #6366f1;
  color: #fff;
}

.sql-fold {
  margin-top: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f9fafb;
  padding: 8px 10px;
}

.sql-fold summary {
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
}

.sql-fold pre {
  margin: 8px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'DM Mono', ui-monospace, monospace;
  font-size: 12px;
  color: #111827;
}

.data-block {
  margin-top: 12px;
  overflow: auto;
}

.composer {
  border-top: 1px solid #e5e7eb;
  padding: 12px 16px;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
</style>
