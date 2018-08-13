<template>
  <div id="app">
    <app-login v-if="!authenticated"></app-login>
    <app-dashboard v-if="authenticated"></app-dashboard>
    <router-view/>
  </div>
</template>

<script>
import Vue from 'vue'

import Dashboard from './components/Dashboard'
import Login from './components/Login'
import Signup from './components/Signup'

import io from 'socket.io-client'

Vue.component('app-dashboard', Dashboard)
Vue.component('app-login', Login)
Vue.component('app-signup', Signup)

const moment = require('moment')

Vue.use(require('vue-moment'), {
  moment
})

export default {
  name: 'App',
  data () {
    return {
      authenticated: false,
      traces: [],
      messages: []
    }
  },
  methods: {
    newSocket (endpoint, peer, key, channel) {
      var _this = this
      this.socket = io(endpoint, {
        query: 'token=' + peer + ':' + key,
        forceNew: true
      })

      this.socket.on('connect', function () {
        console.log('connected')
        _this.traces.unshift({status: 'info', timestamp: Vue.moment().format('HH:mm:ss.SSS'), reason: 'Connected'})
        _this.authenticated = true
        _this.endpoint = endpoint
        _this.channel = channel
        _this.socket.emit('join', {channel: channel})
      })

      this.socket.on('message', function (message) {
        _this.messages.unshift(message)
      })

      this.socket.on('error', function (error) {
        if (!error.timestamp) {
          error.timestamp = Vue.moment().format('HH:mm:ss.SSS')
        }
        _this.traces.unshift(error)
      })

      this.socket.on('disconnect', function () {
        _this.traces.unshift({status: 'error', timestamp: Vue.moment().format('HH:mm:ss.SSS'), reason: 'Disconnected'})
      })

      this.socket.on('info', function (info) {
        // _this.traces.unshift(info)
      })
    }
  }
}
</script>

<style>
* {
  font-family: 'Open Sans', sans-serif;
}
</style>
