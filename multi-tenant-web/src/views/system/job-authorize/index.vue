<template>
  <div class="app-container">
    <!-- {{ departmentIds }} -->
    <el-container>
      <el-aside style="background: white">
        <el-tree :data="organizationData" :props="orgTreeProps" highlight-current @node-click="handleNodeClick">
        </el-tree>
      </el-aside>
      <el-main>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="el-icon-plus" size="mini" :disabled="!departmentIds" @click="addPhoClick"
              v-hasPermi="['system:dict:add']">
              新增人员
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" icon="el-icon-edit" size="mini"
              :disabled="!checkedAuthorizeObjs || !checkedAuthorizeObjs.length" v-hasPermi="['system:dict:edit']"
              @click="permissionDialogVisible = true">
              授权
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="el-icon-delete" size="mini"
              :disabled="!checkedAuthorizeObjs || !checkedAuthorizeObjs.length" @click="deleteAuthorize">
              删除
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button icon="el-icon-delete" size="mini" :disabled="!currentOrgData" @click="exportAuthorize">
              导出
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" icon="el-icon-view" size="mini" :disabled="!currentOrgData" @click="nodeLogClicked">
              查看节点变动日志
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" icon="el-icon-view" size="mini" :disabled="!currentOrgData"
              @click="positionLogClicked">
              查看岗位变动日志
            </el-button>
          </el-col>
          <!-- <el-col :span="1.5">
            <el-button type="info" icon="el-icon-view" size="mini" :disabled="!currentOrgData"
              @click="exportNodeLogClicked">
              导出节点变动日志
            </el-button>
          </el-col> -->
          <!-- <el-col :span="1.5">
            <el-button type="info" icon="el-icon-view" size="mini" :disabled="!currentOrgData"
              @click="exportPositionLogClicked">
              导出岗位变动日志
            </el-button>
          </el-col> -->
        </el-row>

        <el-table v-loading="loading" :data="authorizeList" @selection-change="handleSelectionAuthChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="序号" align="center" type="index">
            <template slot-scope="scope">
              <span>{{ scope.$index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="授权对象" align="center" prop="path" />
          <el-table-column label="管理" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.isManage ? '√' : '╳' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="浏览" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.isShow ? '√' : '╳' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-main>
    </el-container>

    <!-- 历史节点变动日志列表 -->
    <el-dialog title="节点变动日志" :visible.sync="nodeLogVisible" width="800" append-to-body>
      <el-button type="info" icon="el-icon-view" size="mini" :disabled="!currentOrgData" @click="exportNodeLogClicked">
        导出节点变动日志
      </el-button>
      <el-table v-loading="loading" :data="nodeLogList">
        <el-table-column label="序号" align="center" type="index">
          <template slot-scope="scope">
            <span>{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变动时间" align="center" prop="createTime" />
        <el-table-column label="变动内容" align="center" prop="context" />
      </el-table>
    </el-dialog>

    <!-- 历史岗位变动日志列表 -->
    <el-dialog title="岗位变动日志" :visible.sync="positionLogVisible" width="800" append-to-body>
      <el-button type="info" icon="el-icon-view" size="mini" :disabled="!currentOrgData"
        @click="exportPositionLogClicked">
        导出岗位变动日志
      </el-button>
      <el-table v-loading="loading" :data="positionLogList">
        <el-table-column label="序号" align="center" type="index">
          <template slot-scope="scope">
            <span>{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变动时间" align="center" prop="createTime" />
        <el-table-column label="变动内容" align="center" prop="context" />
      </el-table>
    </el-dialog>

    <!-- 穿梭轮  -->
    <transfers v-if="flag" :flag.sync="flag" :title="title" :rootData="rootData" :departmentIds="departmentIds"
      :providerIds="providerIds" @refreshFn="handleNodeClick"></transfers>
    <el-dialog title="设置用户权限" :visible.sync="permissionDialogVisible" width="500px" append-to-body>
      <el-form label-width="80px">
        <el-form-item>
          <div>
            <el-checkbox v-model="manageChecked" @change="manageCheckedChanged">管理</el-checkbox>
          </div>
          <div>
            <el-checkbox v-model="showChecked" :disabled="manageChecked">浏览</el-checkbox>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmAuthorization">确 定</el-button>
        <el-button @click="permissionDialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
  
<script>
import {
  getOrgTreeApi,
  getOrgAuthorizeListeApi,
  setAuthorizeApi,
  deleteAuthorizeApi,
  getRootTrees,
  getNodeLogApi,
  getPositionLogApi,
} from '@/api/system/org-personnel'
import { getToken } from '@/utils/auth'
// import TreeTransfer from 'el-tree-transfer'
import { mapGetters } from 'vuex'
import transfers from '../../../components/TransferTree'

export default {
  name: 'Authorize',
  components: {
    // TreeTransfer
    transfers
  },
  data () {
    return {
      // 穿梭轮------------state
      flag: false,
      title: '增加用户',
      // 顶层root
      rootData: [],
      departmentIds: null,
      providerIds: '',
      // 穿梭轮------------end
      // 组织树相关
      organizationData: [],
      orgTreeProps: {
        children: 'children',
        label: 'name'
      },
      currentOrgData: undefined,

      // 授权表
      loading: false,
      authorizeList: [],
      checkedAuthorizeObjs: [],

      // 授权
      permissionDialogVisible: false,
      manageChecked: false,
      showChecked: false,

      nodeLogVisible: false,
      nodeLogList: [],

      positionLogVisible: false,
      positionLogList: [],
    }
  },
  computed: {
    ...mapGetters(['providerId', 'userId'])
  },
  methods: {
    // 获取最顶层
    async getTopRoot () {
      try {
        const res = await getRootTrees({})
        console.log(res, '顶层root')
        this.rootData = res
      } catch (error) { }
    },
    // 新增用户按钮
    addPhoClick (val) {
      this.flag = true
      console.log(val, this.flag)
    },

    getOrgTree () {
      getOrgTreeApi(this.providerId, this.userId).then(res => {
        console.log(res, 'root++');
        // const root = this.processTreeData(res)
        // this.organizationData = [root]
        this.organizationData = [res]
      })
    },
    processTreeData (orgData) {
      const node = {
        id: orgData.nodeInfo.id,
        fatherId: orgData.nodeInfo.fatherId,
        nodeId: orgData.nodeInfo.nodeId,
        name: orgData.nodeInfo.name,
        type: orgData.nodeInfo.type,
        providerId: orgData.nodeInfo.providerId,
        children: orgData.children.map(childOrgData =>
          this.processTreeData(childOrgData)
        )
      }
      return node
    },
    handleNodeClick (data) {
      console.log(data, '节点点击');
      this.departmentIds = data.nodeId
      this.providerIds = data.providerId
      // type 1：单位 2：部门 3：岗位
      this.currentOrgData = data
      this.getAuthorizeList()
    },
    getAuthorizeList () {
      this.loading = true
      getOrgAuthorizeListeApi(
        this.currentOrgData.providerId,
        this.currentOrgData.nodeId
      ).then(res => {
        if (res.code === 200) {
          this.authorizeList = res.data
          this.loading = false
        }
      })
    },
    handleSelectionAuthChange (selection) {
      this.checkedAuthorizeObjs = selection
    },
    manageCheckedChanged (checked) {
      if (checked) {
        this.showChecked = true
      }
    },
    confirmAuthorization () {
      const sendData = {
        ids: this.checkedAuthorizeObjs.map(obj => obj.id),
        isManage: this.manageChecked ? 1 : 0,
        isShow: this.showChecked ? 1 : 0
      }
      setAuthorizeApi(sendData).then(res => {
        if (res.code === 200) {
          this.$message.success('授权成功')
          this.permissionDialogVisible = false
          this.getAuthorizeList()
        }
      })
    },
    addPersonClicked () { },
    deleteAuthorize () {
      this.$confirm('确认删除授权节点吗?', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          const sendData = {
            ids: this.checkedAuthorizeObjs.map(obj => obj.id)
          }
          return deleteAuthorizeApi(sendData)
        })
        .then(res => {
          if (res.code === 200) {
            this.$message.success('删除成功')
            this.getAuthorizeList()
          }
        })
        .catch(function () { })
    },
    exportAuthorize () {
      this.download(`/demo/get/${this.currentOrgData.providerId}/${this.currentOrgData.nodeId}/node/map/excel`)
    },
    nodeLogClicked () {
      console.log(this.currentOrgData, 'ids?');
      this.nodeLogVisible = true
      getNodeLogApi(this.currentOrgData.providerId, this.currentOrgData.nodeId
      ).then(res => {
        if (res.code === 200) {
          this.nodeLogList = res.data
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    positionLogClicked () {
      this.positionLogVisible = true
      getPositionLogApi(this.currentOrgData.providerId, this.currentOrgData.nodeId).then(res => {
        if (res.code === 200) {
          this.positionLogList = res.data
        } else {
          this.$message.error(res.msg)
        }
      })
    },
    exportNodeLogClicked () {
      console.log(this.currentOrgData, 'this.currentOrgData.id');
      this.download(`/demo/get/node/log/${this.currentOrgData.providerId}/${this.currentOrgData.nodeId}/excel`)
    },
    exportPositionLogClicked () {
      console.log(this.currentOrgData, 'this.currentOrgData.id');
      // debugger
      this.download(`/demo/get/position/log/${this.currentOrgData.providerId}/${this.currentOrgData.nodeId}/excel`)
    },
    download (url) {
      const xmlRequest = new XMLHttpRequest()
      xmlRequest.open('GET', process.env.VUE_APP_BASE_API + url)
      xmlRequest.setRequestHeader('Content-Type', 'application/json;charset=UTF-8')
      xmlRequest.setRequestHeader('Authorization', 'Bearer ' + getToken())
      xmlRequest.responseType = 'blob'
      xmlRequest.send()
      xmlRequest.onload = function (event) {
        if (xmlRequest.readyState === 4 && xmlRequest.status === 200) {
          const dispositionStr = xmlRequest.getResponseHeader('Content-disposition')
          if (!dispositionStr) {
            this.$message.error('下载失败')
            return
          }
          const dispositionArr = dispositionStr.split(';')
          const fileName = decodeURIComponent(dispositionArr[1].replace('filename=', ''))
          const a = document.createElement('a')
          a.download = fileName
          a.href = URL.createObjectURL(xmlRequest.response)
          a.click()
        }
      }
    }
  },
  created () {
    this.getOrgTree()
    this.getTopRoot()
  }
}
</script>