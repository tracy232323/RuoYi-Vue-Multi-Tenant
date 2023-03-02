import request from '@/utils/request'

// 查询本系统组织
export function getOrgTreeApi(providerId = 'hr', userId = '828046') {
// export function getOrgTreeApi(providerId, userId) {
  return request({
    url: `/demo/get/${providerId}/${userId}/tree`,
    method: 'get',
  })
}

/**
 * 查询组织下的人员
 * @param {*} data 
 * @returns 
 */
export function getOrgPersonnelApi(data) {
  // export function getOrgTreeApi(providerId, userId) {
    return request({
      url: `/demo/getNodeAllUser`,
      method: 'post',
      data
    })
  }


/**
 * 查询本系统组织的被授权对象
 * @param {*} providerId: 选中节点的providerId
 * @param {*} nodeId: 选中节点的nodeId 
 * @returns 
 */
export function getOrgAuthorizeListeApi(providerId, nodeId) {
  return request({
    url: `/demo/get/${providerId}/${nodeId}/node/map`,
    method: 'get'
  })
}


// 设置权限
export function setAuthorizeApi(data) {
  return request({
    url: '/demo/addAuth',
    method: 'post',
    data
  })
}

// 删除授权节点
export function deleteAuthorizeApi(data) {
  return request({
    url: '/demo/delAuth',
    method: 'post',
    data
  })
}


//获取hr树
export const getRootTrees = (data) => {
	return request({
		url: '/demo/getOringTree',
		method: 'post',
		data
	})
}

//新增用户 /demo/addAuthUser
export const setAddAuthUser = (data) => {
	return request({
		url: '/demo/addAuthUser',
		method: 'post',
		data
	})
}

//根据providerId和nodeId获取当前节点中被授权对象信息列表
export const getNodeList = (providerId, nodeId) => {
	return request({
		url: `/demo/get/${providerId}/${nodeId}/node/map`,
		method: 'get'
	})
}

/**
 * 查询节点变动日志
 * @param {*} data 
 * @returns 
 */
export function getNodeLogApi(nodeId) {
  return request({
    url: `/demo/get/node/log/${nodeId}`,
    method: 'get',
  })
}

/**
 * 导出节点操作日志
 * @param {*} nodeId 
 * @returns 
 */
export function exportNodeLogApi(nodeId) {
  return request({
    url: `/demo/get/node/log/${nodeId}/excel`,
    method: 'get',
  })
}


/**
 * 查询节点变动日志
 * @param {*} data 
 * @returns 
 */
export function getPositionLogApi(nodeId) {
  return request({
    url: `/demo/get/position/log/${nodeId}`,
    method: 'get',
  })
}

/**
 * 导出岗位日志
 * @param {*} nodeId 
 * @returns 
 */
export function exportPositionLogApi(nodeId) {
  return request({
    url: `/demo/get/position/log/${nodeId}/excel`,
    method: 'get',
  })
}