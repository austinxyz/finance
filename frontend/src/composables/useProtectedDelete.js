import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 处理受保护数据删除的composable
 *
 * 使用方法：
 * const { handleProtectedDelete, isProtectedError } = useProtectedDelete()
 *
 * try {
 *   await someDeleteAPI()
 * } catch (error) {
 *   if (isProtectedError(error)) {
 *     await handleProtectedDelete(error, async () => {
 *       // 用户确认后执行的删除逻辑
 *       await someDeleteAPI({ force: true })
 *     })
 *   }
 * }
 */
export function useProtectedDelete() {
  const dialogVisible = ref(false)
  const dialogMessage = ref('')
  const pendingAction = ref(null)

  /**
   * 检查错误是否为受保护数据异常
   */
  const isProtectedError = (error) => {
    if (!error || !error.response) return false

    const errorMessage = error.response?.data?.message || ''
    const errorData = error.response?.data

    // 检查是否为受保护数据异常
    return (
      errorMessage.includes('受保护') ||
      errorMessage.includes('protected') ||
      errorData?.type === 'ProtectedDataException'
    )
  }

  /**
   * 显示受保护数据删除确认对话框
   *
   * @param {Error} error - 捕获的错误对象
   * @param {Function} onConfirm - 用户确认后的回调函数
   * @returns {Promise<boolean>} - 返回是否执行了删除操作
   */
  const handleProtectedDelete = (error, onConfirm) => {
    return new Promise((resolve) => {
      // 提取错误消息
      const message = error.response?.data?.message || '您正在尝试删除受保护家庭的数据'

      // 显示Element Plus的确认对话框
      ElMessageBox.confirm(
        `${message}\n\n此操作将永久删除数据，无法撤销！\n\n请在下方输入框中输入 DELETE 以确认：`,
        '⚠️ 受保护数据删除警告',
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning',
          customClass: 'protected-delete-dialog',
          showInput: true,
          inputPlaceholder: '请输入 DELETE',
          inputValidator: (value) => {
            return value === 'DELETE' ? true : '请输入 DELETE 以确认删除'
          },
          inputErrorMessage: '请输入 DELETE 以确认删除',
          beforeClose: async (action, instance, done) => {
            if (action === 'confirm') {
              try {
                instance.confirmButtonLoading = true
                instance.confirmButtonText = '删除中...'

                // 执行确认后的操作
                await onConfirm()

                ElMessage.success('删除成功')
                resolve(true)
                done()
              } catch (err) {
                ElMessage.error(err.response?.data?.message || '删除失败')
                resolve(false)
                done()
              } finally {
                instance.confirmButtonLoading = false
                instance.confirmButtonText = '确认删除'
              }
            } else {
              resolve(false)
              done()
            }
          }
        }
      ).catch(() => {
        // 用户取消
        resolve(false)
      })
    })
  }

  /**
   * 包装删除操作，自动处理受保护数据异常
   *
   * @param {Function} deleteAction - 删除操作函数
   * @param {Object} options - 选项
   * @returns {Promise<boolean>} - 返回是否成功删除
   */
  const executeDelete = async (deleteAction, options = {}) => {
    const {
      successMessage = '删除成功',
      errorMessage = '删除失败'
    } = options

    try {
      await deleteAction()
      ElMessage.success(successMessage)
      return true
    } catch (error) {
      // 如果是受保护数据异常，显示特殊确认对话框
      if (isProtectedError(error)) {
        const confirmed = await handleProtectedDelete(error, deleteAction)
        return confirmed
      }

      // 其他错误直接显示错误消息
      ElMessage.error(error.response?.data?.message || errorMessage)
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
