import type { Component } from 'vue'
import type { LocationQueryRaw } from 'vue-router'

export interface NavigationItem {
  key: string
  label: string
  routeName: string
  query?: LocationQueryRaw
  icon: Component
}
