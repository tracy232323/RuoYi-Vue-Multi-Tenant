<template>
  <div class="app-container">
    <el-container>
      <el-aside style="background:white;">
        <el-tree :data="organizationData" :props="orgTreeProps" @node-click="handleNodeClick"></el-tree>
      </el-aside>
      <el-main>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button>导出excel</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="false" :data="personnelList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="序号" align="center" type="index">
            <template slot-scope="scope">
              <span>{{ scope.$index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="姓名" align="center" prop="name" />
          <el-table-column label="单位" align="center" prop="comp" />
          <el-table-column label="部门" align="center" prop="dept" />
          <el-table-column label="岗位" align="center" prop="post" />
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
  </div>
</template>

<script>
import { getOrgTreeApi, getOrgPersonnelApi } from "@/api/system/org-personnel";
import { mapGetters } from 'vuex'

export default {
  name: 'OrgPersonnel',
  data() {
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
      personnelList: []
    };
  },
  computed: {
    ...mapGetters(['providerId', 'userId'])
  },
  methods: {
    getOrgTree() {
      getOrgTreeApi(this.providerId, this.userId).then(res => {
        const root = this.processTreeData(res)
        this.organizationData = [root];
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
        children: orgData.children.map(childOrgData => this.processTreeData(childOrgData))
      };
      return node;
    },
    handleNodeClick(data) {
      // 1：单位 2：部门 3：岗位
      this.currentOrgData = data
      this.getPersonnel()
      
    },
    getPersonnel() {
      const sendData = {
        type: this.currentOrgData.type,
        providerId: this.currentOrgData.providerId,
        // positionId: this.currentOrgData.type === 3 ? 
        orgId: this.currentOrgData.nodeId
      }
      
      getOrgPersonnelApi(sendData).then(res => {
        if (res.code === 200) {
          this.personnelList = res.data
        }
      })
    }
  },
  created() {
    this.getOrgTree();
  }
}
</script>