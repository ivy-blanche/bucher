<script setup lang="ts">
import OJEditor from '@/components/OJEditor.vue'
import type { Problem } from '@/components/OJEditor.vue'

const mockProblem: Problem = {
  title: '两数之和',
  content: `<p>给定一个整数数组 <code>nums</code> 和一个整数目标值 <code>target</code>，请你在该数组中找出和为目标值的那两个整数，并返回它们的数组下标。</p>
<p>你可以假设每种输入只会对应一个答案。但是，数组中同一个元素不能使用两遍。</p>
<p>你可以按任意顺序返回答案。</p>`,
  inputFormat: '第一行输入两个整数 n 和 target，第二行输入 n 个整数表示数组 nums。',
  outputFormat: '输出一行，包含两个整数，表示两个数的下标（从 0 开始）。',
  samples: [
    {
      input: '4 9\n2 7 11 15',
      output: '0 1',
      explanation: '因为 nums[0] + nums[1] == 9 ，返回 [0, 1]。',
    },
    {
      input: '3 6\n3 2 4',
      output: '1 2',
    },
  ],
  constraints: '2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\n-10^9 <= target <= 10^9',
  timeLimit: 1000,
  memoryLimit: 256,
}

function handleRun(code: string, stdin: string) {
  console.log('Run:', { code, stdin })
}

function handleSubmit(code: string) {
  console.log('Submit:', code)
}
</script>

<template>
  <div class="test-page">
    <OJEditor
      :problem="mockProblem"
      @run="handleRun"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.test-page {
  height: 100vh;
}
</style>
