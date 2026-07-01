<script setup lang="ts">
import { darkTheme, NConfigProvider, NGlobalStyle, NMessageProvider } from 'naive-ui'
import AppLayout from './components/layout/AppLayout.vue'
import { naiveThemeOverrides } from './theme/naive'
</script>

<template>
  <NConfigProvider :theme="darkTheme" :theme-overrides="naiveThemeOverrides">
    <NMessageProvider>
      <AppLayout />
    </NMessageProvider>
    <NGlobalStyle />
  </NConfigProvider>
</template>
