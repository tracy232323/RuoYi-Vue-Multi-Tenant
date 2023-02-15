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


