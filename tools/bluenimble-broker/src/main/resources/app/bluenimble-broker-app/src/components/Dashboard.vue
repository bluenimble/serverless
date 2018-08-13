<template>
  <el-container>
    <el-aside width="300px">
      <div class="status">
        <div><span>Connected to</span> <span style="float:right; font-weight: bold;">{{ endpoint }}</span></div>
        <div><span>Channel</span> <span style="float:right; font-weight: bold;">{{ channel }}</span></div>
      </div>
      <el-table :data="traces" stripe :row-class-name="status" style="width: 100%">
        <el-table-column
          prop="timestamp"
          label="Time"
          width="120">
        </el-table-column>
        <el-table-column
          prop="reason"
          label="Reason"
          width="180">
        </el-table-column>
      </el-table>
    </el-aside>
    <el-container>
      <el-table :data="messages" stripe :row-class-name="level" :cell-class-name="cellClassName"
        style="width: 100%; margin-left: 20px; margin-top: 70px;" empty-text="No Logs available">
        <el-table-column type="expand">
          <template slot-scope="props" v-if="props.row.trace">
            <pre>{{ props.row.trace }}</pre>
          </template>
        </el-table-column>
        <el-table-column
          prop="timestamp"
          label="Time"
          width="120">
        </el-table-column>
        <el-table-column
          prop="level"
          label="Level"
          width="80">
        </el-table-column>
        <el-table-column
          prop="message"
          label="Message"
          width="600">
        </el-table-column>
      </el-table>
    </el-container>
  </el-container>
</template>

<script>
export default {
  data () {
    return {
    }
  },
  computed: {
    endpoint () {
      return this.$parent.endpoint
    },
    channel () {
      return this.$parent.channel
    },
    messages () {
      return this.$parent.messages
    },
    traces () {
      return this.$parent.traces
    }
  },
  methods: {
    status ({row, rowIndex}) {
      return row.status
    },
    level ({row, rowIndex}) {
      return row.level.toLowerCase()
    },
    cellClassName ({row, column, rowIndex, columnIndex}) {
      if (columnIndex === 2) {
        return row.level.toLowerCase() + '-cell'
      }
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style>
  .status {
    font-size: 14px;
    background-color: lightblue;
    font-weight: 500;
    border-radius: 4px;
    padding: 10px;
    opacity: .8;
    line-height: 1.8em;
  }
  .debug td:first-child {
    border-left: 3px solid #434343;
  }
  .info td:first-child {
    border-left: 3px solid blue;
  }
  .error td:first-child {
    border-left: 2px solid red;
  }
  .warning td:first-child {
    border-left: 2px solid orange;
  }
  .success td:first-child {
    border-left: 2px solid green;
  }

  .debug-cell {
    color: #434343;
    font-weight: bold;
  }
  .info-cell {
    color: blue;
    font-weight: bold;
  }
  .error-cell {
    color: red;
    font-weight: bold;
  }
  .warning-cell {
    color: orange;
    font-weight: bold;
  }
</style>
