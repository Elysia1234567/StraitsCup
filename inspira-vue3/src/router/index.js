import { createRouter, createWebHistory } from 'vue-router';
import DefaultLayout from '../layouts/DefaultLayout.vue';
import HomePage from '../pages/HomePage.vue';
import InfiniteGridPage from '../pages/InfiniteGridPage.vue';
import AgentPage from '../pages/AgentPage.vue';
import DataPage from '../pages/DataPage.vue';
import GraphPage from '../pages/GraphPage.vue';

function applyEdgeVisibilityHistoryWorkaround() {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return;
  }

  const userAgent = window.navigator?.userAgent || '';
  const isEdge = /Edg\//.test(userAgent);
  if (!isEdge) {
    return;
  }

  const originalReplaceState = window.history.replaceState.bind(window.history);
  const originalPushState = window.history.pushState.bind(window.history);

  window.history.replaceState = function patchedReplaceState(...args) {
    if (document.visibilityState === 'hidden') {
      return;
    }

    return originalReplaceState(...args);
  };

  window.history.pushState = function patchedPushState(...args) {
    if (document.visibilityState === 'hidden') {
      return;
    }

    return originalPushState(...args);
  };
}

applyEdgeVisibilityHistoryWorkaround();

/**
 * 路由与布局
 * - 默认布局：顶栏 SiteHeader（Home / Agent / Models / Graph）+ 子页面
 * - /data：全屏 3D 展示，独立顶栏（与 InfiniteGrid 的「卡片跳转」一致，不在主导航中）
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: DefaultLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomePage,
        },
        {
          path: 'agent',
          name: 'agent',
          component: AgentPage,
        },
        {
          path: 'infinite-grid',
          name: 'infinite-grid',
          component: InfiniteGridPage,
        },
        {
          path: 'graph',
          name: 'graph',
          component: GraphPage,
        },
      ],
    },
    {
      path: '/data',
      name: 'data',
      component: DataPage,
    },
  ],
});

export default router;
