<template>
  <div class="login">
    <el-row type="flex" class="row-bg" justify="center">
      <el-col :span="6">
        <img src="../assets/logo.png">
      </el-col>
      <el-col :span="12" class="form">
        <el-form label-width="120px">
          <el-form-item label="Endpoint">
            <el-input placeholder="https://localhost:9696" v-model="endpoint"></el-input>
          </el-form-item>
          <el-form-item label="Peer ID">
            <el-input v-model="peer"></el-input>
          </el-form-item>
          <el-form-item label="Peer Password">
            <el-input type="password" v-model="key" auto-complete="off"></el-input>
          </el-form-item>
          <el-form-item label="Channel">
            <el-input v-model="channel" placeholder="logs/your-space/your-api"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="authenticate">Connect</el-button>
          </el-form-item>
        </el-form>
      </el-col>
      <el-col :span="6">
      </el-col>
    </el-row>
  </div>
</template>

<script>

export default {
  data () {
    return {
      endpoint: '',
      peer: '',
      key: '',
      channel: ''
    }
  },
  methods: {
    authenticate () {
      this.$parent.newSocket(this.endpoint, this.peer, this.key, this.channel)
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  img {
    width: 80%;
    height: 80%;
  }
  .login {
    margin-top: 60px;
  }
  .form {
    margin-top: 20px;
  }
</style>
