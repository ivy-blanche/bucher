<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick, computed } from 'vue'
import * as monaco from 'monaco-editor'

// ==================== Types ====================
export interface Sample {
  input: string
  output: string
  explanation?: string
}

export interface Problem {
  title: string
  content: string
  inputFormat?: string
  outputFormat?: string
  samples?: Sample[]
  constraints?: string
  timeLimit?: number
  memoryLimit?: number
}

export interface TestResult {
  name: string
  passed: boolean
  input: string
  expected: string
  actual: string
  time?: number
  memory?: number
  error?: string
}

// ==================== Props / Emits ====================
const props = withDefaults(defineProps<{
  problem?: Problem
  questions?: Problem[]
  modelValue?: string
  current?: number
  language?: string
  readonly?: boolean
  running?: boolean
  submitLabel?: string
}>(), {
  language: 'java',
  readonly: false,
  running: false,
  current: 0,
  submitLabel: '提交',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:current': [value: number]
  run: [code: string, stdin: string]
  submit: [code: string]
}>()

// ==================== Constants ====================
const DEFAULT_CODE = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 在此处编写你的代码

    }
}`

// ==================== Multi-Question ====================
const isMulti = computed(() => !!(props.questions && props.questions.length > 0))
const totalQuestions = computed(() => props.questions?.length || 1)
const currentIdx = computed({
  get: () => props.current,
  set: (v: number) => emit('update:current', v),
})
const effectiveProblem = computed(() => {
  if (props.questions && props.questions.length > 0) {
    return props.questions[props.current]
  }
  return props.problem!
})

const questionNavCollapsed = ref(false)

function goToQuestion(index: number) {
  if (index === props.current) return
  emit('update:current', index)
}

function prevQuestion() {
  if (props.current > 0) {
    emit('update:current', props.current - 1)
  }
}

function nextQuestion() {
  if (props.questions && props.current < props.questions.length - 1) {
    emit('update:current', props.current + 1)
  }
}

// ==================== Editor State ====================
const editorContainer = ref<HTMLDivElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const currentCode = ref(props.modelValue || DEFAULT_CODE)

// ==================== Console State ====================
type ConsoleTab = 'console' | 'result'
const activeTab = ref<ConsoleTab>('console')
const stdin = ref('')
const consoleOutput = ref('')
const testResults = ref<TestResult[]>([])
const execStatus = ref<'idle' | 'running' | 'success' | 'error'>('idle')

// ==================== Monaco Worker ====================
self.MonacoEnvironment = {
  getWorker(_: string, label: string) {
    const getWorker = (moduleUrl: string, label: string) =>
      new Worker(new URL(moduleUrl, import.meta.url), { name: label })

    switch (label) {
      case 'json': return getWorker('monaco-editor/esm/vs/language/json/json.worker.js', label)
      case 'css':
      case 'scss':
      case 'less': return getWorker('monaco-editor/esm/vs/language/css/css.worker.js', label)
      case 'html':
      case 'handlebars':
      case 'razor': return getWorker('monaco-editor/esm/vs/language/html/html.worker.js', label)
      case 'typescript':
      case 'javascript': return getWorker('monaco-editor/esm/vs/language/typescript/ts.worker.js', label)
      default: return getWorker('monaco-editor/esm/vs/editor/editor.worker.js', label)
    }
  },
}

// ==================== Editor Lifecycle ====================
function createEditor() {
  if (!editorContainer.value) return

  editor = monaco.editor.create(editorContainer.value, {
    value: currentCode.value,
    language: props.language,
    theme: 'vs',
    fontSize: 14,
    lineNumbers: 'on',
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    automaticLayout: true,
    tabSize: 4,
    insertSpaces: true,
    wordWrap: 'on',
    readOnly: props.readonly,
    renderLineHighlight: 'line',
    cursorBlinking: 'smooth',
    smoothScrolling: true,
    padding: { top: 12, bottom: 12 },
    fontFamily: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
    fontLigatures: true,
  })

  editor.onDidChangeModelContent(() => {
    currentCode.value = editor?.getValue() || ''
    emit('update:modelValue', currentCode.value)
  })
}

onMounted(() => {
  nextTick(() => createEditor())
})

onBeforeUnmount(() => {
  editor?.dispose()
  editor = null
})

// ==================== Watchers ====================
watch(() => props.modelValue, (val) => {
  if (val !== undefined && val !== currentCode.value && editor) {
    currentCode.value = val
    editor.setValue(val)
  }
})

watch(() => props.readonly, (val) => {
  editor?.updateOptions({ readOnly: val })
})

watch(() => props.running, (val) => {
  if (val) {
    execStatus.value = 'running'
  }
})

// ==================== Actions ====================
function handleRun() {
  emit('run', currentCode.value, stdin.value)
}

function handleSubmit() {
  emit('submit', currentCode.value)
}

function handleReset() {
  currentCode.value = DEFAULT_CODE
  editor?.setValue(DEFAULT_CODE)
  emit('update:modelValue', DEFAULT_CODE)
}

function setConsoleOutput(text: string) {
  consoleOutput.value = text
  execStatus.value = 'success'
  activeTab.value = 'console'
}

function setTestResults(results: TestResult[]) {
  testResults.value = results
  execStatus.value = results.every(r => r.passed) ? 'success' : 'error'
  activeTab.value = 'result'
}

function setExecError(message: string) {
  consoleOutput.value = message
  execStatus.value = 'error'
  activeTab.value = 'console'
}

defineExpose({ setConsoleOutput, setTestResults, setExecError })

// ==================== Computed ====================
const passedCount = computed(() => testResults.value.filter(r => r.passed).length)
const totalCount = computed(() => testResults.value.length)
const statusText = computed(() => {
  switch (execStatus.value) {
    case 'idle': return '就绪'
    case 'running': return '运行中...'
    case 'success': return activeTab.value === 'result' ? `通过 ${passedCount.value}/${totalCount.value}` : '运行成功'
    case 'error': return activeTab.value === 'result' ? `失败 ${totalCount.value - passedCount.value}/${totalCount.value}` : '运行错误'
    default: return ''
  }
})
const statusClass = computed(() => `status-${execStatus.value}`)
</script>

<template>
  <div class="oj-editor" :class="{ 'has-questions': isMulti }">
    <!-- ===== Far Left: Question Navigator ===== -->
    <div v-if="isMulti" class="oj-qnav" :class="{ collapsed: questionNavCollapsed }">
      <button class="qnav-toggle" @click="questionNavCollapsed = !questionNavCollapsed" :title="questionNavCollapsed ? '展开题目列表' : '收起题目列表'">
        <svg v-if="!questionNavCollapsed" viewBox="0 0 16 16" width="14" height="14">
          <line x1="4" y1="3" x2="4" y2="13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          <line x1="12" y1="3" x2="12" y2="13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <svg v-else viewBox="0 0 16 16" width="14" height="14">
          <line x1="3" y1="4" x2="13" y2="4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          <line x1="3" y1="8" x2="13" y2="8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          <line x1="3" y1="12" x2="13" y2="12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </button>

      <template v-if="!questionNavCollapsed">
        <div class="qnav-header">
          <span class="qnav-title">{{ totalQuestions }} 题</span>
        </div>
        <div class="qnav-grid">
          <button
            v-for="(q, i) in questions"
            :key="i"
            class="qnav-btn"
            :class="{ active: i === currentIdx }"
            @click="goToQuestion(i)"
          >
            {{ i + 1 }}
          </button>
        </div>
      </template>
    </div>

    <!-- ===== Problem Description ===== -->
    <div class="oj-panel oj-problem">
      <div class="panel-header problem-header">
        <div class="problem-nav">
          <button
            class="nav-arrow"
            :disabled="!isMulti || currentIdx === 0"
            @click="prevQuestion"
            title="上一题"
          >
            <svg viewBox="0 0 16 16" width="14" height="14">
              <polyline points="10,4 6,8 10,12" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <span class="nav-indicator">
            <template v-if="isMulti">
              第 <strong>{{ currentIdx + 1 }}</strong> / {{ totalQuestions }} 题
            </template>
            <template v-else>题目描述</template>
          </span>
          <button
            class="nav-arrow"
            :disabled="!isMulti || currentIdx >= totalQuestions - 1"
            @click="nextQuestion"
            title="下一题"
          >
            <svg viewBox="0 0 16 16" width="14" height="14">
              <polyline points="6,4 10,8 6,12" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="panel-body problem-body">
        <h2 class="problem-title">{{ effectiveProblem.title }}</h2>

        <div class="problem-section problem-content" v-html="effectiveProblem.content" />

        <div v-if="effectiveProblem.inputFormat" class="problem-section">
          <h4 class="section-label">输入格式</h4>
          <div class="section-content">{{ effectiveProblem.inputFormat }}</div>
        </div>

        <div v-if="effectiveProblem.outputFormat" class="problem-section">
          <h4 class="section-label">输出格式</h4>
          <div class="section-content">{{ effectiveProblem.outputFormat }}</div>
        </div>

        <div v-if="effectiveProblem.samples && effectiveProblem.samples.length > 0" class="problem-section">
          <div v-for="(sample, i) in effectiveProblem.samples" :key="i" class="sample-group">
            <h4 class="section-label">样例 {{ i + 1 }}</h4>
            <div class="sample-box">
              <div class="sample-header">输入</div>
              <pre class="sample-code">{{ sample.input }}</pre>
            </div>
            <div class="sample-box">
              <div class="sample-header">输出</div>
              <pre class="sample-code">{{ sample.output }}</pre>
            </div>
            <div v-if="sample.explanation" class="sample-explanation">
              <strong>说明：</strong>{{ sample.explanation }}
            </div>
          </div>
        </div>

        <div v-if="effectiveProblem.constraints" class="problem-section">
          <h4 class="section-label">数据范围</h4>
          <div class="section-content">{{ effectiveProblem.constraints }}</div>
        </div>

        <div v-if="effectiveProblem.timeLimit || effectiveProblem.memoryLimit" class="problem-limits">
          <span v-if="effectiveProblem.timeLimit" class="limit-tag">
            <svg viewBox="0 0 16 16" width="14" height="14">
              <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.2" fill="none"/>
              <line x1="8" y1="4" x2="8" y2="8" stroke="currentColor" stroke-width="1.2"/>
              <line x1="8" y1="8" x2="11" y2="10" stroke="currentColor" stroke-width="1.2"/>
            </svg>
            时间限制：{{ effectiveProblem.timeLimit >= 1000 ? `${effectiveProblem.timeLimit / 1000}s` : `${effectiveProblem.timeLimit}ms` }}
          </span>
          <span v-if="effectiveProblem.memoryLimit" class="limit-tag">
            <svg viewBox="0 0 16 16" width="14" height="14">
              <rect x="2" y="3" width="12" height="10" rx="1" stroke="currentColor" stroke-width="1.2" fill="none"/>
              <line x1="2" y1="7" x2="14" y2="7" stroke="currentColor" stroke-width="1.2"/>
            </svg>
            内存限制：{{ effectiveProblem.memoryLimit >= 1024 ? `${effectiveProblem.memoryLimit / 1024}GB` : `${effectiveProblem.memoryLimit}MB` }}
          </span>
        </div>
      </div>
    </div>

    <!-- ===== Center: Code Editor ===== -->
    <div class="oj-panel oj-editor-panel">
      <div class="panel-header editor-header">
        <span class="panel-title">代码</span>
        <button class="editor-reset-btn" @click="handleReset" title="重置为默认代码">
          <svg viewBox="0 0 16 16" width="14" height="14">
            <path d="M2 8a6 6 0 0 1 10.47-4M14 8a6 6 0 0 1-10.47 4" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round"/>
            <polyline points="13,2 14,8 8,9" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            <polyline points="3,14 2,8 8,7" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          重置
        </button>
      </div>
      <div ref="editorContainer" class="editor-container" />
      <div class="editor-footer">
        <div class="editor-actions">
          <button
            class="action-btn action-run"
            :disabled="execStatus === 'running'"
            @click="handleRun"
          >
            <svg viewBox="0 0 16 16" width="14" height="14">
              <polygon points="5,3 13,8 5,13" fill="currentColor"/>
            </svg>
            运行
          </button>
          <button
            class="action-btn action-submit"
            :disabled="execStatus === 'running'"
            @click="handleSubmit"
          >
            <svg viewBox="0 0 16 16" width="14" height="14">
              <polyline points="2,8 6,12 14,4" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            {{ props.submitLabel }}
          </button>
        </div>
      </div>
    </div>

    <!-- ===== Right: Console ===== -->
    <div class="oj-panel oj-console">
      <div class="panel-header console-header">
        <div class="console-tabs">
          <button
            class="console-tab"
            :class="{ active: activeTab === 'console' }"
            @click="activeTab = 'console'"
          >
            <svg viewBox="0 0 16 16" width="14" height="14">
              <polyline points="4,5 7,8 4,11" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="9" y1="11" x2="12" y2="11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            控制台
          </button>
          <button
            class="console-tab"
            :class="{ active: activeTab === 'result' }"
            @click="activeTab = 'result'"
          >
            <svg viewBox="0 0 16 16" width="14" height="14">
              <polyline points="2,8 6,12 14,4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            执行结果
          </button>
        </div>
        <span v-if="execStatus !== 'idle'" class="exec-status" :class="statusClass">
          <template v-if="execStatus === 'running'">
            <span class="running-spinner" />
          </template>
          {{ statusText }}
        </span>
      </div>

      <div class="panel-body console-body">
        <!-- Console Tab -->
        <div v-if="activeTab === 'console'" class="console-content">
          <div class="stdout-area">
            <pre v-if="consoleOutput" class="stdout-text">{{ consoleOutput }}</pre>
            <div v-else class="stdout-placeholder">
              <svg viewBox="0 0 48 48" width="32" height="32">
                <rect x="6" y="10" width="36" height="28" rx="3" stroke="currentColor" stroke-width="1.5" fill="none"/>
                <polyline points="18,22 22,26 30,18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <p>点击「运行」查看输出结果</p>
            </div>
          </div>
        </div>

        <!-- Result Tab -->
        <div v-else class="result-content">
          <div v-if="testResults.length === 0" class="stdout-placeholder">
            <svg viewBox="0 0 48 48" width="32" height="32">
              <rect x="8" y="12" width="32" height="24" rx="3" stroke="currentColor" stroke-width="1.5" fill="none"/>
              <line x1="16" y1="22" x2="32" y2="22" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <line x1="16" y1="28" x2="28" y2="28" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <p>暂无执行结果</p>
          </div>
          <div v-else class="result-list">
            <div
              v-for="(result, i) in testResults"
              :key="i"
              class="result-item"
              :class="{ passed: result.passed, failed: !result.passed }"
            >
              <div class="result-item-header">
                <span class="result-icon">{{ result.passed ? '✓' : '✕' }}</span>
                <span class="result-name">{{ result.name || `测试点 ${i + 1}` }}</span>
                <span v-if="result.time !== undefined" class="result-time">{{ result.time }}ms</span>
              </div>
              <div v-if="!result.passed" class="result-detail">
                <div class="result-diff">
                  <div class="diff-row">
                    <span class="diff-label">输入：</span>
                    <pre class="diff-value">{{ result.input }}</pre>
                  </div>
                  <div class="diff-row">
                    <span class="diff-label expected">期望：</span>
                    <pre class="diff-value">{{ result.expected }}</pre>
                  </div>
                  <div class="diff-row">
                    <span class="diff-label actual">实际：</span>
                    <pre class="diff-value">{{ result.actual }}</pre>
                  </div>
                </div>
                <div v-if="result.error" class="result-error">
                  <span class="diff-label">错误：</span>
                  <pre class="diff-value">{{ result.error }}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== Layout ==================== */
.oj-editor {
  display: grid;
  grid-template-columns: 1fr 1.4fr 1fr;
  height: 100%;
  background: #f5f6fa;
  color: #3a4a5e;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  transition: grid-template-columns 0.25s ease;
}

.oj-editor.has-questions {
  grid-template-columns: auto 1fr 1.4fr 1fr;
}

.oj-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
}

.oj-panel:last-child {
  border-right: none;
}

/* ==================== Panel Header ==================== */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 40px;
  min-height: 40px;
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  user-select: none;
}

.panel-title {
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.35);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* Scrollbar */
.panel-body::-webkit-scrollbar {
  width: 6px;
}
.panel-body::-webkit-scrollbar-track {
  background: transparent;
}
.panel-body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.08);
  border-radius: 3px;
}
.panel-body::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.14);
}

/* ==================== Q-Nav (collapsible) ==================== */
.oj-qnav {
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  width: 64px;
  min-width: 64px;
  transition: width 0.25s ease, min-width 0.25s ease;
  position: relative;
}

.oj-qnav.collapsed {
  width: 0;
  min-width: 0;
}

.qnav-toggle {
  position: absolute;
  top: 8px;
  right: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 24px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-right: none;
  border-radius: 4px 0 0 4px;
  background: #fff;
  color: rgba(0, 0, 0, 0.35);
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.oj-qnav.collapsed .qnav-toggle {
  right: -22px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0 4px 4px 0;
}

.qnav-toggle:hover {
  color: #0066ff;
  background: #f0f4ff;
}

.qnav-header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  min-height: 40px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.qnav-title {
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.35);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.qnav-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 12px 10px;
  overflow-y: auto;
  align-content: start;
}

.qnav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 6px;
  background: #fff;
  color: #3a4a5e;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}

.qnav-btn:hover {
  border-color: rgba(0, 102, 255, 0.25);
  color: #0066ff;
  background: rgba(0, 102, 255, 0.03);
}

.qnav-btn.active {
  border-color: #0066ff;
  background: rgba(0, 102, 255, 0.08);
  color: #0066ff;
  box-shadow: 0 0 0 2px rgba(0, 102, 255, 0.12);
}

/* ==================== Problem ==================== */
.oj-problem {
  background: #fff;
}

.problem-header {
  justify-content: center;
}

.problem-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 6px;
  background: #fff;
  color: #3a4a5e;
  cursor: pointer;
  transition: all 0.15s ease;
}

.nav-arrow:hover:not(:disabled) {
  border-color: rgba(0, 102, 255, 0.2);
  color: #0066ff;
  background: rgba(0, 102, 255, 0.03);
}

.nav-arrow:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

.nav-indicator {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.4);
  min-width: 80px;
  text-align: center;
}

.nav-indicator strong {
  color: #0066ff;
  font-weight: 700;
}

.problem-body {
  padding: 20px;
}

.problem-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a2a3a;
  margin-bottom: 16px;
  line-height: 1.4;
}

.problem-section {
  margin-bottom: 18px;
}

.problem-content {
  font-size: 14px;
  line-height: 1.8;
  color: #3a4a5e;
}

.problem-content :deep(p) {
  margin-bottom: 8px;
}

.problem-content :deep(code) {
  background: rgba(0, 102, 255, 0.08);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  color: #0066cc;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.5);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.section-content {
  font-size: 14px;
  line-height: 1.7;
  color: #4a5a6e;
}

/* Samples */
.sample-group {
  margin-bottom: 16px;
}

.sample-box {
  margin-bottom: 8px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.sample-header {
  padding: 4px 12px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.35);
  background: rgba(0, 0, 0, 0.02);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.sample-code {
  padding: 10px 12px;
  margin: 0;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  line-height: 1.6;
  color: #3a4a5e;
  background: rgba(0, 0, 0, 0.02);
  white-space: pre-wrap;
  word-break: break-all;
}

.sample-explanation {
  margin-top: 4px;
  font-size: 13px;
  color: #7a8a9e;
  line-height: 1.6;
  padding: 0 2px;
}

/* Limits */
.problem-limits {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.limit-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}

/* ==================== Center: Editor ==================== */
.oj-editor-panel {
  background: #fafafa;
}

.editor-header {
  background: #f0f0f0;
}

.editor-lang-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.5);
}

.lang-icon {
  opacity: 0.5;
}

.editor-reset-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  background: transparent;
  color: rgba(0, 0, 0, 0.4);
  font-size: 11px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.editor-reset-btn:hover {
  color: rgba(0, 0, 0, 0.7);
  border-color: rgba(0, 0, 0, 0.2);
  background: rgba(0, 0, 0, 0.03);
}

.editor-container {
  flex: 1;
  min-height: 0;
}

/* Editor Footer */
.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #f0f0f0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-run {
  background: #2d7d46;
  color: #fff;
}

.action-run:hover:not(:disabled) {
  background: #359a54;
}

.action-submit {
  background: #1a6bb0;
  color: #fff;
}

.action-submit:hover:not(:disabled) {
  background: #1f80d0;
}

.editor-cursor-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cursor-pos {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.25);
}

/* ==================== Right: Console ==================== */
.oj-console {
  background: #fff;
}

.console-header {
  gap: 8px;
}

.console-tabs {
  display: flex;
  gap: 2px;
}

.console-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 12px;
  height: 40px;
  border: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.3);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.console-tab:hover {
  color: rgba(0, 0, 0, 0.55);
}

.console-tab.active {
  color: #0066ff;
  border-bottom-color: #0066ff;
  background: rgba(0, 102, 255, 0.04);
}

.exec-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}

.status-running { color: #0066ff; }
.status-success { color: #52c41a; }
.status-error { color: #ff4d4f; }

.running-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(0, 102, 255, 0.2);
  border-top-color: #0066ff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Console Body */
.console-body {
  display: flex;
  flex-direction: column;
}

.console-content,
.result-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* Stdout Area */
.stdout-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px;
}

.stdout-text {
  margin: 0;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  line-height: 1.6;
  color: #3a4a5e;
  white-space: pre-wrap;
  word-break: break-all;
}

.stdout-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 160px;
  gap: 12px;
  color: rgba(0, 0, 0, 0.1);
}

.stdout-placeholder p {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.18);
}

/* Result Tab */
.result-list {
  padding: 8px;
  overflow-y: auto;
  flex: 1;
}

.result-item {
  margin-bottom: 8px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.result-item.passed {
  border-color: rgba(82, 196, 26, 0.25);
}

.result-item.failed {
  border-color: rgba(255, 77, 79, 0.25);
}

.result-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 500;
}

.passed .result-item-header {
  background: rgba(82, 196, 26, 0.05);
}

.failed .result-item-header {
  background: rgba(255, 77, 79, 0.05);
}

.result-icon {
  font-size: 14px;
  font-weight: 700;
}

.passed .result-icon { color: #52c41a; }
.failed .result-icon { color: #ff4d4f; }

.result-name {
  flex: 1;
  color: #3a4a5e;
}

.result-time {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.3);
}

.result-detail {
  padding: 8px 12px 12px;
  background: rgba(0, 0, 0, 0.02);
}

.result-diff {
  margin-bottom: 8px;
}

.diff-row {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}

.diff-label {
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.4);
  min-width: 36px;
}

.diff-label.expected { color: #52c41a; }
.diff-label.actual { color: #ff4d4f; }

.diff-value {
  margin: 0;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #3a4a5e;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-error {
  padding-top: 6px;
  border-top: 1px solid rgba(255, 77, 79, 0.1);
}

.result-error .diff-value {
  color: #e53935;
}
</style>
