import request from './request';

const backupAPI = {
  /**
   * 手动触发备份
   */
  triggerBackup() {
    return request.post('/backup/trigger', {}, {
      timeout: 600000  // 10 minutes timeout for backup operations
    });
  },

  /**
   * 列出所有备份文件
   * @param {string} type - 备份类型 (all, daily, weekly, monthly)
   */
  listBackups(type = null) {
    const params = type ? { type } : {};
    return request.get('/backup/list', { params });
  },

  /**
   * 恢复备份
   * @param {string} filename - 备份文件名
   * @param {string} confirmDbName - 确认的数据库名称
   */
  restoreBackup(filename, confirmDbName) {
    return request.post('/backup/restore', {
      filename,
      confirmDbName
    }, {
      timeout: 600000  // 10 minutes timeout for restore operations
    });
  },

  /**
   * 获取备份日志
   * @param {string} type - 日志类型 (backup, restore)
   * @param {number} lines - 行数
   */
  getLogs(type = 'backup', lines = 100) {
    return request.get('/backup/logs', {
      params: { type, lines }
    });
  },

  /**
   * 获取备份服务状态
   */
  getStatus() {
    return request.get('/backup/status');
  },

  /**
   * 健康检查
   */
  healthCheck() {
    return request.get('/backup/health');
  }
};

export default backupAPI;
