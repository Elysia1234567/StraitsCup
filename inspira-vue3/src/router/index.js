import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../pages/HomePage.vue';
import InfiniteGridPage from '../pages/InfiniteGridPage.vue';
import AgentPage from '../pages/AgentPage.vue';
import DataPage from '../pages/DataPage.vue';

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

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/infinite-grid',
      name: 'infinite-grid',
      component: InfiniteGridPage,
    },
    {
      path: '/agent',
      name: 'agent',
      component: AgentPage,
    },
    {
      path: '/data',
      name: 'data',
      component: DataPage,
    },
  ],
});

export default router;
