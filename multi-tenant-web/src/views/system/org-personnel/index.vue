<template>
  <div class="app-container">
    <el-container>
      <el-aside style="background:white;">
        <el-tree :data="organizationData" :props="orgTreeProps" @node-click="handleNodeClick"></el-tree>
      </el-aside>
      <el-main>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button @click="deriveClick">导出excel</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="false" :data="personnelList">
          <!-- <el-table-column type="selection" width="55" align="center" /> -->
          <el-table-column label="序号" align="center" type="index">
            <template slot-scope="scope">
              <span>{{ scope.$index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="姓名" align="center" prop="name" />
          <el-table-column label="单位" align="center" prop="unit" />
          <el-table-column label="部门" align="center" prop="dept" />
          <el-table-column label="岗位" align="center" prop="position" />
        </el-table>

        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
          @pagination="getPersonnel" />

      </el-main>
    </el-container>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
import { getOrgTreeApi, getOrgPersonnelApi, exportDerivePho } from "@/api/system/org-personnel";
import { mapGetters } from 'vuex'

export default {
  name: 'OrgPersonnel',
  data () {
    return {
      // 组织树相关
      organizationData: [],
      orgTreeProps: {
        children: 'children',
        label: 'name'
      },
      currentOrgData: undefined,

      // 组织人员table
      loading: true,
      personnelList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
    };
  },
  computed: {
    ...mapGetters(['providerId', 'userId'])
  },
  methods: {
    //导出按钮
    deriveClick () {
      // console.log(this.currentOrgData, 'deriveClick');
      const states = this.currentOrgData
      const url = `/demo/getNodeAllUser/${states.type}/${states.providerId}/${states.nodeId}/export`
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

    },
    getOrgTree () {
      getOrgTreeApi(this.providerId, this.userId).then(res => {
        // const root = this.processTreeData(res)
        // this.organizationData = [root];
        // console.log(res);
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
        children: orgData.children.map(childOrgData => this.processTreeData(childOrgData))
      };
      return node;
    },
    handleNodeClick (data) {
      // 1：单位 2：部门 3：岗位
      this.currentOrgData = data
      console.log(this.currentOrgData, '+');
      this.getPersonnel()
    },
    getPersonnel () {
      const sendData = {
        type: this.currentOrgData.type,
        providerId: this.currentOrgData.providerId,
        orgId: this.currentOrgData.nodeId,
        pageNum: this.queryParams.pageNum - 1,
        pageSize: this.queryParams.pageSize
      }
      getOrgPersonnelApi(sendData).then(res => {
        console.log(res, 'list');
        this.total = res.total
        this.personnelList = res.records
      }).catch(error => {
        this.$message.error(error)
      })
    }
  },
  created () {
    this.getOrgTree();
  }
}
</script>