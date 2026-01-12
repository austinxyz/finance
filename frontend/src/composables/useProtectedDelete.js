import { ref } from 'vue'

/**
 * 处理受保护数据删除的composable
 *
 * 注意：此功能目前未完全实现，使用简单的确认对话框作为占位
 */
export function useProtectedDelete() {
  const dialogVisible = ref(false)
  const dialogMessage = ref('')
  const pendingAction = ref(null)

  const isProtectedError = (error) => {
    if (!error || !error.response) return false

    const errorMessage = error.response?.data?.message || ''
    const errorData = error.response?.data

    return (
      errorMessage.includes('受保护') ||
      errorMessage.includes('protected') ||
      errorData?.type === 'ProtectedDataException'
    )
  }

  const handleProtectedDelete = async (error, onConfirm) => {
    const message = error.response?.data?.message || '您正在尝试删除受保护家庭的数据'
    const confirmed = window.confirm(
      `${message}\n\n此操作将永久删除数据，无法撤销！\n\n确定要继续吗？`
    )

    if (confirmed) {
      try {
        await onConfirm()
        return true
      } catch (err) {
        console.error('删除失败:', err.response?.data?.message || err.message)
        return false
      }
    }
    return false
  }

  const executeDelete = async (deleteAction, options = {}) => {
    try {
      await deleteAction()
      return true
    } catch (error) {
      if (isProtectedError(error)) {
        return await handleProtectedDelete(error, deleteAction)
      }

      console.error(error.response?.data?.message || options.errorMessage || '删除失败')
      return false
    }
  }

  return {
    dialogVisible,
    dialogMessage,
    isProtectedError,
    handleProtectedDelete,
    executeDelete
  }
}
