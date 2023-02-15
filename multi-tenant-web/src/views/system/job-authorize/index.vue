<template>
  <div class="app-container">
    <!-- {{ departmentIds }} -->
    <el-container>
      <el-aside style="background: white">
        <el-tree
          :data="organizationData"
          :props="orgTreeProps"
          highlight-current
          @node-click="handleNodeClick"
        >
        </el-tree>
      </el-aside>
      <el-main>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              icon="el-icon-plus"
              size="mini"
              :disabled="!departmentIds"
              @click="addPhoClick"
              v-hasPermi="['system:dict:add']"
            >
              新增人员
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="success"
              icon="el-icon-edit"
              size="mini"
              :disabled="!checkedAuthorizeObjs || !checkedAuthorizeObjs.length"
              v-hasPermi="['system:dict:edit']"
              @click="permissionDialogVisible = true"
            >
              授权
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="mini"
              :disabled="!checkedAuthorizeObjs || !checkedAuthorizeObjs.length"
              @click="deleteAuthorize"
            >
              删除
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" icon="el-icon-view" size="mini">
              查看更新日志
            </el-button>
          </el-col>
        </el-row>

        <el-table
          v-loading="loading"
          :data="authorizeList"
          @selection-change="handleSelectionAuthChange"
        >
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
          <!-- <el-table-column fixed="left" label="操作" align="center" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['system:template:edit']"
              >修改</el-button>
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['system:template:remove']"
              >删除</el-button>
            </template>
          </el-table-column> -->
        </el-table>

        <!-- <pagination
          v-show="total > 0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"/> -->
      </el-main>
    </el-container>

    <!-- 
    <el-dialog :visible.sync="addPersonDialogVisible" width="800" append-to-body>

      <tree-transfer  height='540px' filter openAll
        title="增加用户" :from_data='fromData' :to_data='toData'
        :defaultProps="{ label:'label' }"></tree-transfer>

      <div slot="footer" class="dialog-footer">
        <el-button type="primary">确 定</el-button>
        <el-button @click="addPersonDialogVisible = false">取 消</el-button>
      </div>
    </el-dialog> -->
    <!-- 穿梭轮  -->
    <transfers
      v-if="flag"
      :flag.sync="flag"
      :title="title"
      :rootData="rootData"
      :departmentIds="departmentIds"
      :providerIds="providerIds"
      @refreshFn="handleNodeClick"
    ></transfers>
    <el-dialog
      title="设置用户权限"
      :visible.sync="permissionDialogVisible"
      width="500px"
      append-to-body
    >
      <el-form label-width="80px">
        <el-form-item>
          <div>
            <el-checkbox v-model="manageChecked" @change="manageCheckedChanged"
              >管理</el-checkbox
            >
          </div>
          <div>
            <el-checkbox v-model="showChecked" :disabled="manageChecked"
              >浏览</el-checkbox
            >
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmAuthorization"
          >确 定</el-button
        >
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
  getRootTrees
} from '@/api/system/org-personnel'
// import TreeTransfer from 'el-tree-transfer'
import { mapGetters } from 'vuex'
import transfers from '../../../components/TransferTree'

export default {
  name: 'Authorize',
  components: {
    // TreeTransfer
    transfers
  },
  data() {
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

      // addPersonDialogVisible: false,
      fromData: [
        {
          id: '1',
          pid: 0,
          label: '一级 1',
          children: [
            {
              id: '1-1',
              pid: '1',
              label: '二级 1-1',
              disabled: true,
              children: []
            },
            {
              id: '1-2',
              pid: '1',
              label: '二级 1-2',
              children: [
                {
                  id: '1-2-1',
                  pid: '1-2',
                  children: [],
                  label: '二级 1-2-1'
                },
                {
                  id: '1-2-2',
                  pid: '1-2',
                  children: [],
                  label: '二级 1-2-2'
                }
              ]
            }
          ]
        }
      ],
      toData: [],

      // 授权
      permissionDialogVisible: false,
      manageChecked: false,
      showChecked: false
    }
  },
  computed: {
    ...mapGetters(['providerId', 'userId'])
  },
  methods: {
    // 获取最顶层
    async getTopRoot() {
      try {
        const res = await getRootTrees({})
        console.log(res, '顶层root')
        this.rootData = res
      } catch (error) {}
    },
    // 新增用户按钮
    addPhoClick(val) {
      this.flag = true
      console.log(val, this.flag)
    },

    getOrgTree() {
      getOrgTreeApi(this.providerId, this.userId).then(res => {
        const root = this.processTreeData(res)
        this.organizationData = [root]
      })
    },
    processTreeData(orgData) {
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
    handleNodeClick(data) {
      // debugger
      console.log(data, '+++')
      this.departmentIds = data.nodeId
      this.providerIds = data.providerId
      // type 1：单位 2：部门 3：岗位
      this.currentOrgData = data
      this.getAuthorizeList()
    },
    getAuthorizeList() {
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
    handleSelectionAuthChange(selection) {
      this.checkedAuthorizeObjs = selection
    },
    manageCheckedChanged(checked) {
      if (checked) {
        this.showChecked = true
      }
    },
    confirmAuthorization() {
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
    addPersonClicked() {},
    deleteAuthorize() {
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
        .catch(function() {})
    }
  },
  created() {
    this.getOrgTree()
    this.getTopRoot()
  }
}
</script>