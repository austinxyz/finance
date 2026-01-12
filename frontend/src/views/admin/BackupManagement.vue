<template>
    <div class="space-y-6 p-6">
      <!-- 页面标题 -->
      <div class="bg-white rounded-lg shadow p-6">
        <h1 class="text-2xl font-bold text-gray-900">数据备份管理</h1>
        <p class="text-sm text-gray-600 mt-1">管理数据库备份和恢复</p>
      </div>

    <!-- 状态卡片 -->
    <div class="flex gap-4">
      <div class="flex-1 bg-white rounded-lg shadow p-6" v-if="status">
        <div class="flex gap-6">
          <div class="flex flex-col gap-2">
            <span class="text-sm text-gray-600">服务状态</span>
            <span class="text-lg font-semibold" :class="status.healthy ? 'text-green-600' : 'text-red-600'">
              {{ status.healthy ? '✓ 正常' : '✗ 异常' }}
            </span>
          </div>
          <div class="flex flex-col gap-2" v-if="status.diskUsage">
            <span class="text-sm text-gray-600">磁盘使用</span>
            <span class="text-lg font-semibold text-gray-900">{{ status.diskUsage.usePercent }} ({{ status.diskUsage.used }} / {{ status.diskUsage.total }})</span>
          </div>
          <div class="flex flex-col gap-2" v-if="status.latestBackups?.daily">
            <span class="text-sm text-gray-600">最新备份</span>
            <span class="text-lg font-semibold text-gray-900">{{ formatTimestamp(status.latestBackups.daily.timestamp) }}</span>
          </div>
        </div>
      </div>

      <div class="flex flex-col gap-3">
        <button @click="triggerBackup" :disabled="loading" class="px-5 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
          <span v-if="loading">执行中...</span>
          <span v-else>手动备份</span>
        </button>
        <button @click="refreshData" :disabled="loading" class="px-5 py-2.5 bg-white text-gray-700 border border-gray-300 rounded-lg font-medium hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
          刷新
        </button>
      </div>
    </div>

    <!-- 备份文件列表 -->
    <div class="bg-white rounded-lg shadow p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-semibold text-gray-900">备份文件列表</h2>
        <div class="flex gap-2">
          <button
            v-for="type in backupTypes"
            :key="type.value"
            @click="selectedType = type.value; loadBackups()"
            :class="[
              'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              selectedType === type.value
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            {{ type.label }}
          </button>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table v-if="backups && backups.length > 0" class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">文件名</th>
              <th class="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">类型</th>
              <th class="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">大小</th>
              <th class="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">创建时间</th>
              <th class="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="backup in backups" :key="backup.filename" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-sm font-mono text-gray-900">{{ backup.filename }}</td>
              <td class="px-4 py-3">
                <span :class="[
                  'inline-block px-3 py-1 rounded-full text-xs font-medium',
                  backup.type === 'daily' ? 'bg-blue-100 text-blue-800' :
                  backup.type === 'weekly' ? 'bg-orange-100 text-orange-800' :
                  backup.type === 'monthly' ? 'bg-purple-100 text-purple-800' :
                  'bg-gray-100 text-gray-800'
                ]">
                  {{ getTypeLabel(backup.type) }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ formatSize(backup.size) }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ formatTimestamp(backup.timestamp) }}</td>
              <td class="px-4 py-3">
                <button @click="confirmRestore(backup)" class="px-3 py-1.5 bg-red-600 text-white text-sm rounded-lg font-medium hover:bg-red-700 transition-colors">
                  恢复
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="text-center py-12 text-gray-500">
          <p>暂无备份文件</p>
        </div>
      </div>
    </div>

    <!-- 日志查看 -->
    <div class="bg-white rounded-lg shadow p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-semibold text-gray-900">备份日志</h2>
        <div class="flex gap-2">
          <button
            v-for="logType in logTypes"
            :key="logType.value"
            @click="selectedLogType = logType.value; loadLogs()"
            :class="[
              'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              selectedLogType === logType.value
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            {{ logType.label }}
          </button>
        </div>
      </div>

      <div class="bg-gray-50 rounded-lg p-4 max-h-96 overflow-y-auto">
        <pre v-if="logs && logs.length > 0" class="text-xs font-mono text-gray-900 whitespace-pre-wrap">{{ logs.join('\n') }}</pre>
        <div v-else class="text-center py-8 text-gray-500">
          <p>暂无日志</p>
        </div>
      </div>
    </div>

    <!-- 恢复确认对话框 -->
    <div v-if="showRestoreDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click="showRestoreDialog = false">
      <div class="bg-white rounded-lg shadow-xl w-full max-w-md" @click.stop>
        <div class="p-6 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-red-600">⚠️ 确认恢复备份</h3>
        </div>
        <div class="p-6">
          <div class="bg-orange-50 border-l-4 border-orange-500 p-4 mb-4">
            <p class="font-semibold text-gray-900 mb-2">警告：此操作将覆盖当前数据库的所有数据！</p>
            <p class="text-sm text-gray-700 mb-1">恢复文件：<code class="bg-black bg-opacity-10 px-2 py-0.5 rounded text-xs">{{ selectedBackup?.filename }}</code></p>
            <p class="text-sm text-gray-700">创建时间：{{ formatTimestamp(selectedBackup?.timestamp) }}</p>
          </div>

          <div>
            <label for="confirmDbName" class="block text-sm font-medium text-gray-700 mb-2">
              请输入数据库名称以确认（当前数据库：<code class="text-red-600">{{ dbName }}</code>）
            </label>
            <input
              id="confirmDbName"
              v-model="confirmDbName"
              type="text"
              placeholder="输入数据库名称"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
              @keyup.enter="performRestore"
            />
          </div>
        </div>
        <div class="p-4 bg-gray-50 border-t border-gray-200 flex justify-end gap-3">
          <button @click="showRestoreDialog = false" class="px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-lg font-medium hover:bg-gray-50 transition-colors">
            取消
          </button>
          <button
            @click="performRestore"
            :disabled="!canRestore || loading"
            class="px-4 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <span v-if="loading">恢复中...</span>
            <span v-else>确认恢复</span>
          </button>
        </div>
      </div>
    </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import backupAPI from '../../api/backup';

// 状态数据
const loading = ref(false);
const status = ref(null);
const backups = ref([]);
const logs = ref([]);

// 过滤器
const selectedType = ref('all');
const selectedLogType = ref('backup');

const backupTypes = [
  { value: 'all', label: '全部' },
  { value: 'daily', label: '每日' },
  { value: 'weekly', label: '每周' },
  { value: 'monthly', label: '每月' }
];

const logTypes = [
  { value: 'backup', label: '备份日志' },
  { value: 'restore', label: '恢复日志' }
];

// 恢复确认对话框
const showRestoreDialog = ref(false);
const selectedBackup = ref(null);
const confirmDbName = ref('');
const dbName = ref('finance');  // Will be updated from status API

const canRestore = computed(() => {
  return confirmDbName.value === dbName.value;
});

// 初始化
onMounted(() => {
  refreshData();
});

// 刷新所有数据
async function refreshData() {
  await Promise.all([
    loadStatus(),
    loadBackups(),
    loadLogs()
  ]);
}

// 加载服务状态
async function loadStatus() {
  try {
    const data = await backupAPI.getStatus();
    status.value = data;

    // Update database name from status
    if (data.dbName) {
      dbName.value = data.dbName;
    }
  } catch (error) {
    console.error('Failed to load status:', error);
  }
}

// 加载备份列表
async function loadBackups() {
  try {
    loading.value = true;
    const data = await backupAPI.listBackups(selectedType.value === 'all' ? null : selectedType.value);
    backups.value = data;
  } catch (error) {
    console.error('Failed to load backups:', error);
    alert('加载备份列表失败：' + error.message);
  } finally {
    loading.value = false;
  }
}

// 加载日志
async function loadLogs() {
  try {
    const data = await backupAPI.getLogs(selectedLogType.value, 100);
    logs.value = data;
  } catch (error) {
    console.error('Failed to load logs:', error);
  }
}

// 手动触发备份
async function triggerBackup() {
  if (!confirm('确认要立即执行备份吗？')) {
    return;
  }

  try {
    loading.value = true;
    await backupAPI.triggerBackup();
    alert('备份已触发！');
    await refreshData();
  } catch (error) {
    console.error('Failed to trigger backup:', error);
    alert('触发备份失败：' + error.message);
  } finally {
    loading.value = false;
  }
}

// 确认恢复
function confirmRestore(backup) {
  selectedBackup.value = backup;
  confirmDbName.value = '';
  showRestoreDialog.value = true;
}

// 执行恢复
async function performRestore() {
  if (!canRestore.value) {
    alert('请输入正确的数据库名称');
    return;
  }

  try {
    loading.value = true;
    await backupAPI.restoreBackup(selectedBackup.value.filename, confirmDbName.value);
    alert('数据库恢复成功！页面将自动刷新。');
    showRestoreDialog.value = false;

    // 恢复后刷新页面
    setTimeout(() => {
      window.location.reload();
    }, 1000);
  } catch (error) {
    console.error('Failed to restore backup:', error);
    alert('恢复失败：' + error.message);
  } finally {
    loading.value = false;
  }
}

// 工具函数
function formatSize(bytes) {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

function formatTimestamp(timestamp) {
  if (!timestamp) return '-';
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function getTypeLabel(type) {
  const typeMap = {
    daily: '每日',
    weekly: '每周',
    monthly: '每月',
    manual: '手动'
  };
  return typeMap[type] || type;
}
</script>
