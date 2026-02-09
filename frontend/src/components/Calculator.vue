<template>
  <div v-if="show" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="close">
    <div class="bg-white rounded-lg shadow-xl p-6 w-80" @click.stop>
      <!-- Header -->
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-semibold text-gray-900">计算器</h3>
        <button @click="close" class="text-gray-400 hover:text-gray-600">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Display -->
      <div class="bg-gray-100 rounded-lg p-4 mb-4">
        <div class="text-right text-sm text-gray-500 h-6">{{ expression || '&nbsp;' }}</div>
        <input
          v-model="display"
          type="text"
          inputmode="decimal"
          @input="handleDisplayInput"
          @focus="handleDisplayFocus"
          placeholder="0"
          class="w-full text-right text-2xl font-mono font-semibold text-gray-900 mt-2 bg-transparent border-none outline-none focus:ring-2 focus:ring-blue-300 rounded px-2"
        />
      </div>

      <!-- Buttons -->
      <div class="grid grid-cols-4 gap-2">
        <button @click="clear" class="col-span-2 bg-red-500 hover:bg-red-600 text-white rounded-lg py-3 font-semibold transition">
          清除
        </button>
        <button @click="deleteLast" class="bg-gray-300 hover:bg-gray-400 text-gray-800 rounded-lg py-3 font-semibold transition">
          删除
        </button>
        <button @click="addOperator('/')" class="bg-blue-500 hover:bg-blue-600 text-white rounded-lg py-3 font-semibold transition">
          ÷
        </button>

        <button @click="addNumber('7')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          7
        </button>
        <button @click="addNumber('8')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          8
        </button>
        <button @click="addNumber('9')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          9
        </button>
        <button @click="addOperator('*')" class="bg-blue-500 hover:bg-blue-600 text-white rounded-lg py-3 font-semibold transition">
          ×
        </button>

        <button @click="addNumber('4')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          4
        </button>
        <button @click="addNumber('5')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          5
        </button>
        <button @click="addNumber('6')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          6
        </button>
        <button @click="addOperator('-')" class="bg-blue-500 hover:bg-blue-600 text-white rounded-lg py-3 font-semibold transition">
          −
        </button>

        <button @click="addNumber('1')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          1
        </button>
        <button @click="addNumber('2')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          2
        </button>
        <button @click="addNumber('3')" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          3
        </button>
        <button @click="addOperator('+')" class="bg-blue-500 hover:bg-blue-600 text-white rounded-lg py-3 font-semibold transition">
          +
        </button>

        <button @click="addNumber('0')" class="col-span-2 bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          0
        </button>
        <button @click="addDecimal" class="bg-gray-200 hover:bg-gray-300 text-gray-900 rounded-lg py-3 font-semibold transition">
          .
        </button>
        <button @click="calculate" class="bg-green-500 hover:bg-green-600 text-white rounded-lg py-3 font-semibold transition">
          =
        </button>
      </div>

      <!-- Apply Button -->
      <button @click="applyResult" class="w-full mt-4 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg py-3 font-semibold transition">
        应用结果
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'apply'])

const display = ref('0')
const expression = ref('')
const currentNumber = ref('')
const operator = ref(null)
const previousNumber = ref(null)
const justCalculated = ref(false)

// Reset calculator
const clear = () => {
  display.value = '0'
  expression.value = ''
  currentNumber.value = ''
  operator.value = null
  previousNumber.value = null
  justCalculated.value = false
}

// Delete last character
const deleteLast = () => {
  if (justCalculated.value) {
    clear()
    return
  }

  if (currentNumber.value.length > 0) {
    currentNumber.value = currentNumber.value.slice(0, -1)
    display.value = currentNumber.value || '0'
    updateExpression()
  }
}

// Add number
const addNumber = (num) => {
  if (justCalculated.value) {
    clear()
    justCalculated.value = false
  }

  // 如果当前显示是 '0' 且currentNumber为空，替换而不是追加
  if (display.value === '0' && currentNumber.value === '') {
    currentNumber.value = num
  } else {
    currentNumber.value += num
  }

  display.value = currentNumber.value
  updateExpression()
}

// Add decimal point
const addDecimal = () => {
  if (justCalculated.value) {
    clear()
    justCalculated.value = false
  }

  if (!currentNumber.value.includes('.')) {
    currentNumber.value += currentNumber.value ? '.' : '0.'
    display.value = currentNumber.value
    updateExpression()
  }
}

// Add operator
const addOperator = (op) => {
  if (currentNumber.value === '' && previousNumber.value === null) return

  if (justCalculated.value) {
    justCalculated.value = false
  }

  if (currentNumber.value !== '') {
    if (previousNumber.value !== null && operator.value !== null) {
      calculate()
    } else {
      previousNumber.value = parseFloat(currentNumber.value)
    }
    currentNumber.value = ''
    display.value = '0' // 清空显示，准备输入下一个数字
  }

  operator.value = op
  updateExpression()
}

// Calculate result
const calculate = () => {
  if (previousNumber.value === null || operator.value === null || currentNumber.value === '') return

  const num1 = previousNumber.value
  const num2 = parseFloat(currentNumber.value)
  let result = 0

  switch (operator.value) {
    case '+':
      result = num1 + num2
      break
    case '-':
      result = num1 - num2
      break
    case '*':
      result = num1 * num2
      break
    case '/':
      if (num2 === 0) {
        display.value = '错误'
        expression.value = '除数不能为0'
        setTimeout(clear, 2000)
        return
      }
      result = num1 / num2
      break
  }

  // Round to 2 decimal places
  result = Math.round(result * 100) / 100

  display.value = result.toString()
  expression.value = `${num1} ${getOperatorSymbol(operator.value)} ${num2} =`
  previousNumber.value = result
  currentNumber.value = result.toString()
  operator.value = null
  justCalculated.value = true
}

// Update expression display
const updateExpression = () => {
  if (previousNumber.value !== null && operator.value !== null) {
    // 显示运算符和之前的数字，如果有当前数字也显示
    if (currentNumber.value !== '') {
      expression.value = `${previousNumber.value} ${getOperatorSymbol(operator.value)} ${currentNumber.value}`
    } else {
      expression.value = `${previousNumber.value} ${getOperatorSymbol(operator.value)}`
    }
  } else if (previousNumber.value !== null) {
    expression.value = previousNumber.value.toString()
  } else {
    expression.value = currentNumber.value || ''
  }
}

// Get operator symbol for display
const getOperatorSymbol = (op) => {
  const symbols = {
    '+': '+',
    '-': '−',
    '*': '×',
    '/': '÷'
  }
  return symbols[op] || op
}

// Close calculator
const close = () => {
  emit('close')
}

// Apply result to parent component
const applyResult = () => {
  const value = parseFloat(display.value)
  if (!isNaN(value)) {
    emit('apply', value)
    close()
  }
}

// Handle direct input in display
const handleDisplayInput = (event) => {
  let value = event.target.value

  // Remove any non-numeric characters except decimal point and minus sign
  value = value.replace(/[^\d.-]/g, '')

  // Ensure only one decimal point
  const parts = value.split('.')
  if (parts.length > 2) {
    value = parts[0] + '.' + parts.slice(1).join('')
  }

  // Ensure minus sign only at the beginning
  const minusCount = (value.match(/-/g) || []).length
  if (minusCount > 1) {
    value = value.charAt(0) === '-' ? '-' + value.replace(/-/g, '') : value.replace(/-/g, '')
  }

  display.value = value
  currentNumber.value = value

  // 如果正在进行计算（有运算符和前一个数字），只更新当前数字
  if (operator.value !== null && previousNumber.value !== null) {
    // 保持计算状态，只更新表达式
    updateExpression()
  } else {
    // 如果没有正在进行的计算，重置状态
    expression.value = value
    operator.value = null
    previousNumber.value = null
  }

  justCalculated.value = false
}

// Handle focus on display input
const handleDisplayFocus = () => {
  // Select all text when focusing for easy replacement
  setTimeout(() => {
    const input = document.querySelector('input[type="text"]')
    if (input) {
      input.select()
    }
  }, 0)
}

// Reset when dialog opens
watch(() => props.show, (newVal) => {
  if (newVal) {
    clear()
  }
})
</script>
