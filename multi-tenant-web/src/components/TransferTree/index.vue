<template>
  <div>
    <el-dialog
      v-loading="closeLoading"
      :title="title"
      :visible.sync="flag"
      width="880px"
      :before-close="handleClose"
    >
      <div class="counts">
        <div class="transferLeft">
          <el-select
            style="width: 100%"
            placeholder="这里输入姓名进行查找..."
            disabled
            v-model="filterText"
          >
          </el-select>
          <div class="countsTrees">
            <el-tree
              v-loading="loading"
              ref="tree"
              :props="props"
              :load="loadNode"
              lazy
              accordion
              show-checkbox
              @check-change="handleCheckChange"
              node-key="ids"
            >
              <span
                :class="data.isSelect ? 'actives' : 'custom-tree-node'"
                slot-scope="{ node, data }"
                @click="treeChanges(node, data)"
              >
                <!-- {{ data.type }} -->
                <i :class="filterIcon(data.type)"></i>
                <span>{{ data.name }}</span>
              </span>
            </el-tree>
          </div>
        </div>
        <div class="btnCenter">
          <el-button
            type="primary"
            plain
            icon="el-icon-arrow-right"
            class="btnAlls"
            @click="rightClikc"
            :disabled="btnRight"
          ></el-button>
          <div style="height: 10px"></div>
          <el-button
            type="primary"
            plain
            icon="el-icon-arrow-left"
            @click="leftClick"
            :disabled="btnLeft"
          ></el-button>
        </div>
        <div class="transferRight">
          <div class="headers">
            <div class="checkAll">
              <el-checkbox
                :indeterminate="isIndeterminate"
                v-model="checkAll"
                @change="handleCheckAllChange"
                :disabled="!selectRightArr.length"
              ></el-checkbox>
            </div>
            <div class="titles"><span>已选人员</span></div>
          </div>
          <div class="checkBoxs">
            <!-- {{ checkedCities }}====={{ selectRightArr }} -->
            <el-checkbox-group
              v-model="checkedCities"
              @change="handleCheckedCitiesChange"
            >
              <el-checkbox
                v-for="item in selectRightArr"
                :label="item.id"
                :key="item.id"
                style="margin-top: 10px"
                >{{ item.name }}</el-checkbox
              >
            </el-checkbox-group>
            <div class="notDates" v-show="!selectRightArr.length">暂无数据</div>
          </div>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <!-- {{ selectArr }} -->
        <!-- 部门id: {{ departmentIds }}====组织id： {{ providerIds }} -->
        <el-button type="primary" @click="confirmClick" :disabled="btnConfirm"
          >确 定</el-button
        >
        <el-button @click="handleClose">取 消</el-button>
      </span>
    </el-dialog>
  </div>
</template>
           
<script>
import {
  getRootTrees,
  setAddAuthUser,
  getNodeList
} from '../../api/system/org-personnel'
// 唯一key
const getUniqueID = () => {
  var time = Date.now().toString(36)
  var random = Math.random().toString(36)
  random = random.substring(2, random.length)
  // 返回唯一ID
  return random + time
}

export default {
  name: 'transfers',
  props: {
    // 开关
    flag: {
      type: Boolean,
      default: false,
      require: true
    },
    // 标题
    title: {
      type: String,
      default: ''
    },
    // 根节点
    rootData: {
      type: Array,
      default: () => {}
    },
    // 部门ID
    departmentIds: {
      type: Number,
      default: null
    },
    // 当前组织ID
    providerIds: {
      type: String,
      default: ''
    },
    // 详情回显
    phoData: {
      type: Array,
      default: () => {}
    }
  },
  data() {
    return {
      closeLoading: false,
      loading: false,
      dialogVisible: false,
      filterText: '',
      // 按钮
      btnRight: true,
      btnLeft: true,
      btnConfirm: true,
      // tree--------------state
      props: {
        label: 'name',
        children: 'id',
        isLeaf: 'mainPosition'
        // disabled: 'virtual'
      },
      // 下一级
      childrenArr: [],
      // 二级单位的编码
      providerId: '',
      // checkbox--------------state
      checkAll: false,
      checkedCities: [],
      isIndeterminate: false,
      // 选中的-left
      selectArr: [],
      // 所有的-right
      selectRightArr: []
    }
  },
  created() {
    console.log(this.selectRightArr, 'created')
    // this.selectRightArr = this.phoData
    // 详情
    // this.getNodeLists()
  },
  mounted() {},
  computed: {},
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val)
    },
    selectArr(newVal, oldVal) {
      console.log(newVal.length)
      this.btnRight = !newVal.length
    },
    checkedCities(newVal, oldVal) {
      this.btnLeft = !newVal.length
    },
    selectRightArr(val) {
      this.btnConfirm = !val.length
    }
  },
  methods: {
    // 查详情
    async getNodeLists() {
      try {
        const res = await getNodeList(this.providerIds, this.departmentIds)
        console.log(res.data, '详情列表')
      } catch (error) {}
    },
    // 关闭
    handleClose(val) {
      this.$confirm(val === 'confirm' ? '确认新增？' : '确认关闭？')
        .then(async _ => {
          // 添加用户接口
          if (val === 'confirm') {
            this.closeLoading = true
            console.log('?')
            const obj = {
              list: this.selectRightArr.map(item => ({
                orgId: this.departmentIds,
                positionId: item.dutyIdS,
                providerId: item.hrId,
                userId: item.id,
                nodeProviderId: this.providerIds
              }))
            }
            const res = await setAddAuthUser(obj)
            res.code === 200
              ? this.$message({
                message: '新加用户成功',
                type: 'success'
              })
              : this.$message({
                message: `${res.msg}`,
                type: 'warning'
              })
            console.log(res, '+++++++++++++++++++++++++555')
          }
          this.closeLoading = false
          // 刷新函数
          this.$emit('refreshFn', {
            providerId: this.providerIds,
            nodeId: this.departmentIds
          })
          this.$emit('update:flag', false)
        })
        .catch(_ => {
          this.closeLoading = false
        })
    },
    // 确认
    confirmClick() {
      this.handleClose('confirm')
    },
    // tree
    handleCheckChange(data, checked, indeterminate) {
      console.log(data, checked, indeterminate, 'tree,Changes')
      if (checked) {
        console.log(data, 'checked,Changes')
        if (!this.selectArr.map(item => item.id).includes(data.id)) {
          this.selectArr.push(data)
        }
      } else {
        this.selectArr = this.selectArr.filter(item => item.id !== data.id)
      }
    },
    // 获取 root tree
    async getRootTree(orgId, type, providerId) {
      try {
        const res = await getRootTrees({
          orgId,
          type,
          providerId
        })
        console.log(res, '下一级+')
        this.childrenArr = res
      } catch (error) {}
    },
    // 点击叶子
    treeChanges(node, data) {
      console.log(data, 'treeChanges')
    },
    // icon过滤
    filterIcon(val) {
      return val === 3 || !val ? 'el-icon-user' : 'el-icon-office-building'
    },
    // 懒加载tree
    loadNode(node, resolve) {
      // console.log(node, '123456')
      resolve(
        this.rootData.map(item => ({
          ...item.root,
          providerId: item.id,
          disabled: true,
          ids: getUniqueID()
        }))
      )
      if (node.data) {
        if (node.data.providerId) this.providerId = node.data.providerId
        console.log(node, node.data.id, node.data.type, this.providerId, '1+')
        if (node.data.type) {
          // get下一级
          this.loading = true
          getRootTrees({
            orgId: node.data.id,
            type: node.data.type,
            positionId: node.data.id,
            providerId: this.providerId
          }).then(res => {
            let dutyIdS = null // 岗位ID
            if (node.data.type === 3) dutyIdS = node.data.id
            // console.log(node.data.type, dutyIdS, 'type++')
            // console.log(res, '展开')
            resolve(
              res.map(item => {
                return item.type === 3 || !item.type
                  ? {
                    ...item,
                    hrId: this.providerId,
                    dutyIdS,
                    ids: getUniqueID()
                  }
                  : {
                    ...item,
                    disabled: true,
                    hrId: this.providerId,
                    dutyIdS,
                    ids: getUniqueID()
                  }
              })
            )
            // 到人员 判断 然后禁止到下一级
            this.loading = false
          })
        } else {
          resolve([])
        }
      }
    },
    // checkbox
    handleCheckAllChange(val) {
      console.log(val, 'handleCheckAllChange')
      this.checkedCities = val ? this.selectRightArr.map(item => item.id) : []
      this.isIndeterminate = false
    },
    handleCheckedCitiesChange(value) {
      console.log(value, 'handleCheckedCitiesChange')
      const checkedCount = value.length
      this.checkAll = checkedCount === this.selectRightArr.length
      this.isIndeterminate =
        checkedCount > 0 && checkedCount < this.selectRightArr.length
    },
    // center btn
    rightClikc() {
      // 清空勾选
      this.selectRightArr.push(
        ...new Set(
          this.selectArr.filter(
            item => item.type !== 1 && item.type !== 2 && item.type !== 3
          )
        )
      )
      this.selectRightArr = [...new Set(this.selectRightArr)]
      this.$refs.tree.setCheckedKeys([])
      this.btnRight = true
    },
    leftClick() {
      this.selectRightArr = this.selectRightArr.filter(
        item => !this.checkedCities.includes(item.id)
      )
      // this.$refs.tree.setCheckedKeys(this.selectRightArr.map(item => item.id))
      // 移除人员 state
      // this.checkedCities.forEach(item => {
      //   this.filterFn(item, this.data)
      // })
      // // 改变选中成员收集
      // this.selectArr = this.selectCollect()
      // // 移除人员 end
      this.checkedCities = []
      this.isIndeterminate = this.checkAll = false
      // console.log(this.cities)
    }
    // 工具函数
  }
}
</script>
<style lang='scss'  scoped>
.counts {
  display: flex;
  justify-content: center;
  .transferLeft {
    width: 360px;
    height: 400px;
    // border: 1px solid #ccc;

    .countsTrees {
      padding: 10px 0 0 20px;
      height: 358px;
      width: 360px;
      overflow: auto;
      margin-top: 6px;
      border: 1px solid #dcdfe6;
    }
    .custom-tree-node {
      height: 22px;
      box-sizing: border-box;
      padding: 3px 5px 3px 10px;
      border-radius: 4px;
    }
    .actives {
      height: 22px;
      box-sizing: border-box;
      padding: 3px 5px 3px 10px;
      background: #c2e6fc;
      border-radius: 2px;
    }
  }
  .btnCenter {
    padding: 0 10px;
    width: 100px;
    height: 400px;
    display: flex;
    flex-flow: column;
    justify-content: center;
    .btnAlls {
    }
    .el-button + .el-button {
      margin: 0;
    }
  }
  .transferRight {
    width: 360px;
    height: 400px;
    border: 1px solid #dcdfe6;
    overflow-y: auto;
    .headers {
      display: flex;
      height: 36px;
      border-bottom: 1px solid #dcdfe6;
      .checkAll {
        height: 100%;
        width: 36px;
        border-right: 1px solid #dcdfe6;
        display: flex;
        justify-content: center;
        align-items: center;
      }
      .titles {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
    .checkBoxs {
      position: relative;
      padding: 10px;
      height: 340px;
      .el-checkbox-group {
        display: flex;
        flex-flow: column;
      }
      .notDates {
        position: absolute;
        left: 50%;
        bottom: 0;
        transform: translateX(-50%);
        font-weight: 500;
        color: #999;
      }
    }
  }
}
</style>