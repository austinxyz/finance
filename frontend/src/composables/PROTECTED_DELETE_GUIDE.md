# 受保护数据删除功能集成指南

## 概述

本指南说明如何在Vue组件中集成受保护数据的删除确认功能。

## 使用方法

### 方式1：使用 executeDelete 包装器（推荐）

这是最简单的方式，自动处理所有错误和确认逻辑。

```vue
<script setup>
import { useProtectedDelete } from '@/composables/useProtectedDelete'
import { assetAPI } from '@/api'

const { executeDelete } = useProtectedDelete()

const handleDeleteRecord = async (recordId) => {
  // executeDelete会自动处理受保护数据异常
  const success = await executeDelete(
    () => assetAPI.deleteRecord(recordId),
    {
      successMessage: '记录删除成功',
      errorMessage: '记录删除失败'
    }
  )

  if (success) {
    // 刷新列表或其他后续操作
    await loadRecords()
  }
}
</script>

<template>
  <button @click="handleDeleteRecord(record.id)">删除</button>
</template>
```

### 方式2：手动处理（灵活控制）

适用于需要自定义逻辑的场景。

```vue
<script setup>
import { useProtectedDelete } from '@/composables/useProtectedDelete'
import { assetAPI } from '@/api'

const { isProtectedError, handleProtectedDelete } = useProtectedDelete()

const handleDeleteRecord = async (recordId) => {
  try {
    // 第一次尝试删除
    await assetAPI.deleteRecord(recordId)
    ElMessage.success('删除成功')
    await loadRecords()
  } catch (error) {
    // 检查是否为受保护数据异常
    if (isProtectedError(error)) {
      // 显示确认对话框，用户确认后重新执行删除
      await handleProtectedDelete(error, async () => {
        await assetAPI.deleteRecord(recordId)
        await loadRecords()
      })
    } else {
      // 其他错误正常显示
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}
</script>
```

### 方式3：在确认对话框前增加预检查

适用于需要先确认，再检查保护状态的场景。

```vue
<script setup>
import { useProtectedDelete } from '@/composables/useProtectedDelete'
import { ElMessageBox } from 'element-plus'
import { assetAPI } from '@/api'

const { executeDelete } = useProtectedDelete()

const handleDeleteAccount = async (accountId) => {
  // 先显示普通确认对话框
  try {
    await ElMessageBox.confirm(
      '确定要删除此账户吗？',
      '确认删除',
      {
        type: 'warning'
      }
    )

    // 用户确认后，执行删除（自动处理受保护数据）
    await executeDelete(
      () => assetAPI.deleteAccount(accountId),
      {
        successMessage: '账户删除成功',
        errorMessage: '账户删除失败'
      }
    )

    await loadAccounts()
  } catch (error) {
    // 用户取消或其他错误
  }
}
</script>
```

## 需要集成的组件列表

### 优先级1：核心数据删除（必须）

这些是直接删除用户数据的操作，必须添加保护：

1. **资产管理** (`frontend/src/views/assets/`)
   - `AccountHistory.vue` - 删除资产记录
   - `AccountManagement.vue` - 删除资产账户

2. **负债管理** (`frontend/src/views/liabilities/`)
   - `LiabilityHistory.vue` - 删除负债记录
   - `LiabilityDetail.vue` - 删除负债账户

3. **支出管理** (`frontend/src/views/expenses/`)
   - `ExpenseList.vue` - 删除支出记录
   - `ExpenseBatchUpdate.vue` - 批量删除

4. **收入管理** (`frontend/src/views/income/`)
   - `IncomeList.vue` - 删除收入记录

5. **投资交易** (`frontend/src/views/investment/`)
   - `InvestmentTransactions.vue` - 删除投资交易记录

### 优先级2：配置和预算（推荐）

1. **预算管理** (`frontend/src/views/expenses/`)
   - `ExpenseBudget.vue` - 删除支出预算

2. **收入预算** (`frontend/src/views/income/`)
   - `IncomeBudget.vue` - 删除收入预算

### 优先级3：其他操作（可选）

这些操作通常不直接删除核心数据，但如果涉及到用户数据可以考虑添加：

- 汇率管理
- 分类管理（如果有删除功能）

## 集成步骤

对于每个需要集成的组件：

1. **导入composable**
   ```js
   import { useProtectedDelete } from '@/composables/useProtectedDelete'
   ```

2. **在setup中使用**
   ```js
   const { executeDelete } = useProtectedDelete()
   ```

3. **修改删除函数**
   - 将原有的 `try-catch` 包裹的删除逻辑
   - 改为使用 `executeDelete` 或 `handleProtectedDelete`

4. **测试**
   - 用受保护的家庭数据测试删除操作
   - 确认会弹出特殊的确认对话框
   - 确认输入"DELETE"后才能执行

## 示例：完整的组件更新

### 更新前

```vue
<script setup>
import { assetAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' })
    await assetAPI.deleteRecord(id)
    ElMessage.success('删除成功')
    await loadRecords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>
```

### 更新后

```vue
<script setup>
import { assetAPI } from '@/api'
import { ElMessageBox } from 'element-plus'
import { useProtectedDelete } from '@/composables/useProtectedDelete'

const { executeDelete } = useProtectedDelete()

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' })

    // 使用executeDelete自动处理受保护数据
    const success = await executeDelete(
      () => assetAPI.deleteRecord(id),
      {
        successMessage: '删除成功',
        errorMessage: '删除失败'
      }
    )

    if (success) {
      await loadRecords()
    }
  } catch (error) {
    // 用户取消
  }
}
</script>
```

## 后端返回的错误格式

后端在检测到受保护数据删除时，会返回如下格式的错误：

```json
{
  "message": "受保护的家庭数据不允许执行操作: 删除资产账户: My Account (Family ID: 1)",
  "type": "ProtectedDataException"
}
```

composable会自动识别这种错误并显示特殊的确认对话框。

## 自定义确认消息

如果需要自定义确认消息，可以在API错误响应中设置更详细的message。

## 注意事项

1. **保持现有确认对话框**: 建议保留原有的第一层确认对话框，受保护数据确认是第二层保护
2. **用户体验**: 只有在操作受保护数据时才会出现"输入DELETE"的严格确认
3. **错误处理**: executeDelete会自动处理所有错误，包括网络错误、业务错误等
4. **异步操作**: 所有删除操作都应该是async函数

## 测试清单

- [ ] 使用受保护家庭的数据测试删除操作
- [ ] 确认会显示特殊的确认对话框
- [ ] 测试输入错误的文本（应该禁用确认按钮或显示错误）
- [ ] 测试取消操作
- [ ] 测试确认后的删除流程
- [ ] 测试网络错误的处理
- [ ] 使用非受保护家庭的数据测试（应该正常删除，不显示特殊对话框）
