import { createRouter, createWebHistory } from 'vue-router';
import MainLayout from '../components/MainLayout.vue';
import Dashboard from '../views/Dashboard.vue';

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: {
          title: '财务概览',
          description: '查看您的整体财务状况'
        }
      },
      // 资产管理
      {
        path: 'assets/accounts',
        name: 'AssetAccounts',
        component: () => import('../views/assets/AccountList.vue'),
        meta: {
          title: '资产账户',
          description: '管理您的资产账户'
        }
      },
      {
        path: 'assets/accounts/:id',
        name: 'AssetAccountDetail',
        component: () => import('../views/assets/AccountDetail.vue'),
        meta: {
          title: '账户详情',
          description: '查看账户资产记录'
        }
      },
      {
        path: 'assets/batch-update',
        name: 'BatchUpdate',
        component: () => import('../views/assets/BatchUpdate.vue'),
        meta: {
          title: '批量更新',
          description: '批量更新资产金额'
        }
      },
      {
        path: 'assets/history',
        name: 'AssetHistory',
        component: () => import('../views/assets/AccountHistory.vue'),
        meta: {
          title: '历史记录',
          description: '查看和管理资产历史记录'
        }
      },
      // 数据分析
      {
        path: 'analysis/trend',
        name: 'TrendAnalysis',
        component: () => import('../views/analysis/TrendAnalysis.vue'),
        meta: {
          title: '趋势分析',
          description: '查看资产变化趋势'
        }
      },
      {
        path: 'analysis/allocation',
        name: 'AssetAllocation',
        component: () => import('../views/analysis/AssetAllocation.vue'),
        meta: {
          title: '资产配置',
          description: '分析资产配置结构'
        }
      },
      // 设置
      {
        path: 'settings/users',
        name: 'UserManagement',
        component: () => import('../views/settings/UserManagement.vue'),
        meta: {
          title: '用户管理',
          description: '管理系统用户'
        }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
