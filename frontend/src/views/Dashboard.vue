<template>
  <div class="space-y-6">
    <!-- Welcome Section -->
    <div>
      <h1 class="text-3xl font-bold text-foreground">财务概览</h1>
      <p class="text-muted-foreground mt-2">
        欢迎使用个人理财管理系统
      </p>
    </div>

    <!-- Quick Stats -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <Card>
        <CardHeader>
          <CardTitle class="text-sm font-medium text-muted-foreground">总资产</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold text-primary">¥{{ formatAmount(totalAssets) }}</div>
          <p class="text-xs text-muted-foreground mt-1">Total Assets</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-sm font-medium text-muted-foreground">总负债</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold text-destructive">¥0.00</div>
          <p class="text-xs text-muted-foreground mt-1">Total Liabilities</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-sm font-medium text-muted-foreground">净资产</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">¥0.00</div>
          <p class="text-xs text-muted-foreground mt-1">Net Worth</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-sm font-medium text-muted-foreground">月度增长</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold text-green-600">+0.0%</div>
          <p class="text-xs text-muted-foreground mt-1">Monthly Growth</p>
        </CardContent>
      </Card>
    </div>

    <!-- 资产负债分布 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 资产分布 -->
      <Card>
        <CardHeader>
          <CardTitle>资产分布</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-sm text-muted-foreground text-center py-8">
            暂无资产数据
          </div>
        </CardContent>
      </Card>

      <!-- 负债分布 -->
      <Card>
        <CardHeader>
          <CardTitle>负债分布</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-sm text-muted-foreground text-center py-8">
            暂无负债数据
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 快捷操作 & 最近记录 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 快捷操作 -->
      <Card>
        <CardHeader>
          <CardTitle>快捷操作</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="space-y-2">
            <Button @click="$router.push('/assets/accounts')" class="w-full justify-start" variant="outline">
              <Plus class="w-4 h-4 mr-2" />
              添加资产账户
            </Button>
            <Button @click="$router.push('/liabilities/accounts')" class="w-full justify-start" variant="outline">
              <Plus class="w-4 h-4 mr-2" />
              添加负债账户
            </Button>
            <Button @click="$router.push('/analysis/trends')" class="w-full justify-start" variant="outline">
              <LineChart class="w-4 h-4 mr-2" />
              查看趋势分析
            </Button>
            <Button @click="$router.push('/goals')" class="w-full justify-start" variant="outline">
              <Target class="w-4 h-4 mr-2" />
              设置财务目标
            </Button>
          </div>
        </CardContent>
      </Card>

      <!-- 最近记录 -->
      <Card>
        <CardHeader>
          <CardTitle>最近记录</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-sm text-muted-foreground text-center py-8">
            暂无最近记录
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 净资产趋势图 -->
    <Card>
      <CardHeader>
        <CardTitle>净资产趋势</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="text-sm text-muted-foreground text-center py-12">
          暂无趋势数据
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Card from '../components/ui/Card.vue';
import CardHeader from '../components/ui/CardHeader.vue';
import CardTitle from '../components/ui/CardTitle.vue';
import CardContent from '../components/ui/CardContent.vue';
import Button from '../components/ui/Button.vue';
import { Plus, LineChart, Target } from 'lucide-vue-next';
import { assetAccountAPI } from '@/api'

const userId = ref(1) // TODO: 从用户登录状态获取
const accounts = ref([])
const loading = ref(false)

// 计算总资产
const totalAssets = computed(() => {
  return accounts.value.reduce((sum, acc) => sum + (acc.latestAmount || 0), 0)
})

// 格式化金额
function formatAmount(amount) {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 加载账户数据
async function loadAccounts() {
  loading.value = true
  try {
    const response = await assetAccountAPI.getAll(userId.value)
    if (response.success) {
      accounts.value = response.data
    }
  } catch (error) {
    console.error('加载账户失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAccounts()
})
</script>
