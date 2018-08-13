<template>
  <el-container>
    <el-aside width="200px">
      <app-vmenu />
    </el-aside>
    <el-container>
      <el-header><app-header /></el-header>
      <el-main><app-main /></el-main>
      <el-footer><app-footer /></el-footer>
    </el-container>
  </el-container>
</template>

<script>
import Vue from 'vue'

import Header from './Header'
import Footer from './Footer'
import VerticalMenu from './VerticalMenu'
import Main from './Main'

Vue.component('app-main', Main)
Vue.component('app-header', Header)
Vue.component('app-footer', Footer)
Vue.component('app-vmenu', VerticalMenu)

export default {
  data () {
    return {
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

</style>
